package me.timothy.bots;

import java.io.IOException;
import java.text.ParseException;
import me.timothy.bots.summon.Summon;
import me.timothy.jreddit.info.Comment;
import me.timothy.jreddit.info.Link;
import me.timothy.jreddit.info.Listing;
import me.timothy.jreddit.info.Message;
import me.timothy.jreddit.info.Thing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: Auto-generated Javadoc
/**
 * The driver for the bot, based on summons. Uses bot-specific
 * logic for looping through the comments, submissions, and personal
 * messages looking for summons, and then applying them via the Summon's
 * implementation.
 * 
 * @author Timothy
 * @see me.timothy.bots.summon.Summon
 */
public class BotDriver implements Runnable {
	
	/** The comment summons. */
	protected final Summon[] commentSummons;
	
	/** The pm summons. */
	protected final Summon[] pmSummons;
	
	/** The submission summons. */
	protected final Summon[] submissionSummons;
	
	/** The config. */
	protected FileConfiguration config;
	
	/** The database. */
	protected Database database;
	
	/** The bot. */
	protected Bot bot;

	/** The logger. */
	protected Logger logger;

	/**
	 * Creates a bot driver based on the specified database, configuration info,
	 * rest client, and bot.
	 * 
	 * @param database the database
	 * @param config the configuration
	 * @param restClient rest client
	 * @param bot the bot
	 * @param commentSummons the comment summons
	 * @param pmSummons the pm summons
	 * @param submissionSummons the submission summons
	 */
	public BotDriver(Database database, FileConfiguration config,
			Bot bot, Summon[] commentSummons,
			Summon[] pmSummons, Summon[] submissionSummons) {
		this.commentSummons = commentSummons;
		this.submissionSummons = submissionSummons;
		this.pmSummons = pmSummons;
		
		this.database = database;
		this.config = config;
		this.bot = bot;

		this.logger = LogManager.getLogger();
	}

	/**
	 * Runs the bot.
	 */
	@Override
	public void run() {
		logger.trace("Logging in..");
		login();
		sleepFor(2000);

		try {
			while (true) {
				doLoop();
				
				System.out.flush();
				System.err.flush();
				sleepFor(30000);
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	/**
	 * Performs one loop for the bot
	 * @throws IOException if an i/o related exception occurs 
	 * @throws org.json.simple.parser.ParseException if a parse exception occurs
	 * @throws ParseException if a parse exception occurs
	 */
	protected void doLoop() throws IOException, org.json.simple.parser.ParseException, ParseException {
		logger.trace("Scanning comments..");
		scanComments();
		
		logger.trace("Scanning submissions..");
		scanSubmissions();
		
		logger.trace("Scanning pm's..");
		scanPersonalMessages();
	}
	/**
	 * Loops through recent comments, ignoring comments by banned 
	 * users or remembered fullnames, and handles them via the appropriate
	 * summons.
	 */
	protected void scanComments() {
		Listing comments = getRecentComments();
		sleepFor(2000);

		for (int i = 0; i < comments.numChildren(); i++) {
			Comment comment = (Comment) comments.getChild(i);
			if(database.containsFullname(comment.fullname()) || config.getBannedUsers().contains(comment.author().toLowerCase()))
				continue;
			
			for(Summon summon : commentSummons) {
				if(summon.parse(comment)) {
					database.addFullname(comment.fullname());
					String response = summon.applyChanges(config, database);
					handleReply(comment, response);
					sleepFor(2000);
				}
			}
		}
	}
	
	/**
	 * Loops through recent submissions, ignoring remembered fullnames,
	 * and checks for "req" (case insensitive) in the title. If req exists,
	 * performs an automatic check on the author of the submission as a top-level
	 * comment in the submission thread.
	 *
	 * @throws IOException if an i/o related exception occurs
	 * @throws ParseException if the parse via Summon.CHECK_SUMMON is invalid (should not happen since the check is automatic)
	 * @throws ParseException if the parse via Summon.CHECK_SUMMON is invalid (should not happen since the check is automatic)
	 */
	protected void scanSubmissions() throws IOException, org.json.simple.parser.ParseException, ParseException {
		Listing submissions = getRecentSubmissions();
		sleepFor(2000);

		for (int i = 0; i < submissions.numChildren(); i++) {
			Link submission = (Link) submissions.getChild(i);
			if(database.containsFullname(submission.fullname()))
				continue;
			
			for(Summon summon : submissionSummons) {
				if(summon.parse(submission)) {
					database.addFullname(submission.fullname());
					String response = summon.applyChanges(config, database);
					
					handleReply(submission, response);
				}
			}
		}
	}
	
	/**
	 * Loops through unread personal messages and replies to the bot. Prints them
	 * out, and in the case of personal messages checks if any summons are applicable
	 * and if they are, applies them.
	 */
	protected void scanPersonalMessages() {
		Listing messages = getRecentMessages();
		markRead(messages);
		sleepFor(2000);
		for(int i = 0; i < messages.numChildren(); i++) {
			Thing m = (Thing) messages.getChild(i);
			if(m instanceof Comment) {
				Comment mess = (Comment) m;
				logger.info(mess.author() + " replied to me with:\n" + mess.body());
			}else if(m instanceof Message) {
				Message mess = (Message) m;
				logger.info(mess.author() + " pm'd me:\n" + mess.body());
				
				for(Summon summon : pmSummons) {
					if(summon.parse(mess)) {
						String response = summon.applyChanges(config, database);
						
						if(response != null) {
							handleReply(m, response);
						}
					}
				}
			}
		}
	}

	/**
	 * Marks the specified messages as read. Utilizes exponential-backoff
	 * 
	 * @param messages the messages
	 * @see me.timothy.bots.Retryable
	 */
	protected void markRead(final Listing messages) {
		new Retryable<Boolean>("markRead") {

			@Override
			protected Boolean runImpl() throws Exception {
				String ids = "";
				boolean first = true;
				for(int i = 0; i < messages.numChildren(); i++) {
					Thing m = messages.getChild(i);
					if(!first)
						ids += ",";
					else
						first = false;
					ids += m.fullname();
				}
				boolean succ = false;
				if(ids.length() != 0) {
					logger.debug("Marking " + ids + " as read");
					succ = bot.setReadMessage(ids);
					sleepFor(2000);
				}
				return succ;
			}
			
		}.run();
	}

	/**
	 * Logs into reddit based on the configuration. Terminates
	 * the program as if by fail() on failure.
	 */
	protected void login() {
		boolean success = false;
		try {
			success = bot.loginReddit(config.getUserInfo()
					.getProperty("username"),
					config.getUserInfo().getProperty("password"));
		} catch (IOException | org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}
		if (!success)
			fail("Failed to authenticate to reddit");
	}

	/**
	 * Gets a list of recent comments, utilizing exponential-backoff.
	 *
	 * @return recent comments
	 * @see me.timothy.bots.Retryable
	 */
	protected Listing getRecentComments() {
		return new Retryable<Listing>("getRecentComments") {
			@Override
			protected Listing runImpl() throws Exception {
				return bot.getRecentComments();
			}
		}.run();
	}

	/**
	 * Replies to the thing, utilizing exponential back-off.
	 *
	 * @param replyable the thing to reply to
	 * @param response the thing to reply with
	 * @see me.timothy.bots.Retryable
	 */
	protected void handleReply(final Thing replyable, final String response) {
		new Retryable<Boolean>("handleReply") {

			@Override
			protected Boolean runImpl() throws Exception {
				return bot.respondTo(replyable, response);
			}
			
		}.run();
	}

	/**
	 * Gets a list of recent submissions, utilizing exponential back-off.
	 *
	 * @return a list of recent submissions
	 * @see me.timothy.bots.Retryable
	 */
	protected Listing getRecentSubmissions() {
		return new Retryable<Listing>("getRecentSubmissions") {
			@Override
			protected Listing runImpl() throws Exception {
				return bot.getRecentSubmissions();
			}
		}.run();
	}

	/**
	 * Gets a list of recent messages, utilizing exponential back-off.
	 *
	 * @return a list of recent messages
	 * @see me.timothy.bots.Retryable
	 */
	protected Listing getRecentMessages() {
		return new Retryable<Listing>("getRecentMessages") {
			@Override
			protected Listing runImpl() throws Exception {
				return bot.getUnreadMessages();
			}
		}.run();
	}
	
	/**
	 * Sleeps for the specified time in milliseconds, as if by
	 * {@code Thread.sleep(ms)}. Logs the exception and terminates
	 * the program on error.
	 * 
	 * @param ms the time in milliseconds to sleep
	 */
	protected void sleepFor(long ms) {
		try {
			logger.trace("Sleeping for " + ms + " milliseconds");
			Thread.sleep(ms);
		} catch (InterruptedException ex) {
			logger.error(ex);
			fail("Interrupted");
		}
	}

	/**
	 * Terminates the program after logging {@code message} at
	 * Level.ERROR, as well as logging the list of errors
	 * 
	 * @param message the message to put in Level.ERROR
	 * @param errors a list of applicable errors to output
	 */
	protected void fail(String message, Throwable... errors) {
		logger.error(message);

		for(Throwable th : errors) {
			th.printStackTrace();
			logger.error(th);
		}
		System.exit(0);
	}
}
