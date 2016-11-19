package me.timothy.bots;

import java.io.IOException;
import java.text.ParseException;
import me.timothy.bots.summon.CommentSummon;
import me.timothy.bots.summon.LinkSummon;
import me.timothy.bots.summon.PMSummon;
import me.timothy.bots.summon.SummonResponse;
import me.timothy.jreddit.RedditUtils;
import me.timothy.jreddit.User;
import me.timothy.jreddit.info.Comment;
import me.timothy.jreddit.info.Link;
import me.timothy.jreddit.info.Listing;
import me.timothy.jreddit.info.LoginResponse;
import me.timothy.jreddit.info.Message;
import me.timothy.jreddit.info.Thing;

import org.apache.logging.log4j.Level;
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
	/** Time in seconds between touching the reddit api */
	protected static int BRIEF_PAUSE_MS = 5000;
	
	/** The comment summons. */
	protected final CommentSummon[] commentSummons;
	
	/** The pm summons. */
	protected final PMSummon[] pmSummons;
	
	/** The submission summons. */
	protected final LinkSummon[] submissionSummons;
	
	/** The config. */
	protected FileConfiguration config;
	
	/** The database. */
	protected Database database;
	
	/** The bot. */
	protected Bot bot;

	/** The logger. */
	protected Logger logger;
	
	/**
	 *  A runnable that simply calls maybeLoginAgain
	 *  
	 *  @see BotDriver#maybeLoginAgain()
	 */
	protected Runnable maybeLoginAgainRunnable;

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
			Bot bot, CommentSummon[] commentSummons,
			PMSummon[] pmSummons, LinkSummon[] submissionSummons) {
		this.commentSummons = commentSummons;
		this.submissionSummons = submissionSummons;
		this.pmSummons = pmSummons;
		
		this.database = database;
		this.config = config;
		this.bot = bot;

		this.logger = LogManager.getLogger();
		
		this.maybeLoginAgainRunnable = new Runnable() {
			public void run() {
				maybeLoginAgain();
			}
		};
	}

	/**
	 * Runs the bot.
	 */
	@Override
	public void run() {
		logger.trace("Logging in..");
		login();
		sleepFor(15000);

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
		logger.trace("Considering relogging in..");
		maybeLoginAgain();
		
		logger.trace("Scanning comments..");
		scanComments();
		
		logger.trace("Scanning submissions..");
		scanSubmissions();
		
		logger.trace("Scanning pm's..");
		scanPersonalMessages();
	}
	
	/**
	 * Checks if we need to refresh our access token, and if so,
	 * purges it and logs in.
	 */
	protected void maybeLoginAgain()
	{
		User user = bot.getUser();
		LoginResponse loginResponse = user.getLoginResponse();
		boolean shouldLogin = false;
		shouldLogin = shouldLogin || loginResponse == null;
		if(!shouldLogin) {
			long now = System.currentTimeMillis();
			long acquiredAt = loginResponse.acquiredAt();
			long timeInSecondsAfterAcquiredAtThatExpires = loginResponse.expiresIn();
			
			long timeInMillisAfterAcquiredAtThatExpires = timeInSecondsAfterAcquiredAtThatExpires * 1000;
			
			long timeInMillisThatExpires = acquiredAt + timeInMillisAfterAcquiredAtThatExpires;
			
			long timeUntilExpires = timeInMillisThatExpires - now;
			
			if(timeUntilExpires < 1000 * 60 * 5) { // 5 minutes
				shouldLogin = true;
			}
		}
		
		if(shouldLogin) {
			user.setLoginResponse(null);
			sleepFor(BRIEF_PAUSE_MS);
			new Retryable<Boolean>("Refresh login token") {
				@Override
				protected Boolean runImpl() throws IOException, org.json.simple.parser.ParseException {
					bot.loginReddit(config.getProperty("user.username"),
							config.getProperty("user.password"),
							config.getProperty("user.appClientID"),
							config.getProperty("user.appClientSecret"));
					return Boolean.TRUE;
				}
			}.run();
			sleepFor(BRIEF_PAUSE_MS);
		}
	}
	
	/**
	 * Loops through recent comments, ignoring comments by banned 
	 * users or remembered fullnames, and handles them via the appropriate
	 * summons.
	 */
	protected void scanComments() {
		Listing comments = getRecentComments();
		sleepFor(6000);

		for (int i = 0; i < comments.numChildren(); i++) {
			Comment comment = (Comment) comments.getChild(i);
			handleComment(comment, false, false);
		}
	}
	
	/**
	 * Handles a single comment
	 * 
	 * @param comment the comment to handle
	 * @param debug if debug messages should be printed
	 * @param silentMode if this should not reply
	 * @return if the comment was meaningfull
	 */
	protected boolean handleComment(Comment comment, boolean debug, boolean silentMode) {
		if(database.containsFullname(comment.fullname())) {
			if(debug)
				logger.trace(String.format("Skipping %s because the database contains that fullname", comment.fullname()));
			return false;
		}
		if(config.getList("banned").contains(comment.author().toLowerCase())) {
			if(debug)
				logger.trace(String.format("Skipping %s because %s is banned", comment.fullname(), comment.author()));
			return false;
		}
		
		if(comment.author().equalsIgnoreCase(config.getProperty("user.username"))) {
			if(debug)
				logger.trace(String.format("Skipping %s because thats my comment", comment.fullname()));
			return false;
		}

		database.addFullname(comment.fullname());
		boolean hadResponse = false;
		SummonResponse response;
		for(CommentSummon summon : commentSummons) {
			response = null;
			try {
				response = summon.handleComment(comment, database, config);
			}catch(Exception ex) {
				logger.catching(ex);
				sleepFor(BRIEF_PAUSE_MS);
			}
			
			
			if(response != null) {
				if(debug) {
					logger.printf(Level.TRACE, "%s gave response %s to %s", summon.getClass().getCanonicalName(), response.getResponseType().name(), comment.fullname());
				}
				hadResponse = true;
				if(!silentMode) {
					handleReply(comment, response.getResponseMessage());
					
					if(response.getLinkFlair() != null) {
						handleFlair(comment.linkID(), response.getLinkFlair());
					}
					sleepFor(BRIEF_PAUSE_MS);
				}
			}else if(debug) {
				logger.printf(Level.TRACE, "%s gave no response to %s", summon.getClass().getCanonicalName(), comment.fullname());
			}
		}
		return hadResponse;
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
		sleepFor(BRIEF_PAUSE_MS);
		
		if(submissions == null) {
			return;
		}

		for (int i = 0; i < submissions.numChildren(); i++) {
			Link submission = (Link) submissions.getChild(i);
			handleSubmission(submission, false);
		}
	}
	
	/**
	 * Handles a single submission
	 * 
	 * @param submission the submission the handle
	 * @param silentMode if the bot should not respond
	 */
	protected void handleSubmission(Link submission, boolean silentMode) {
		if(database.containsFullname(submission.fullname()))
			return;
		database.addFullname(submission.fullname());

		SummonResponse response;
		for(LinkSummon summon : submissionSummons) {
			response = null;
			try {
				response = summon.handleLink(submission, database, config);
			}catch(Exception ex) {
				logger.catching(ex);
				sleepFor(BRIEF_PAUSE_MS);
			}
			
			if(response != null && !silentMode) {
				handleReply(submission, response.getResponseMessage());
				sleepFor(BRIEF_PAUSE_MS);
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
		sleepFor(BRIEF_PAUSE_MS);
		for(int i = 0; i < messages.numChildren(); i++) {
			Thing m = (Thing) messages.getChild(i);
			handlePM(m, false);
		}
	}

	/**
	 * Handles a single message in our inbox
	 * 
	 * @param m the pm to handle
	 * @param silentMode if this should not respond
	 */
	protected void handlePM(Thing m, boolean silentMode) {
		if(m instanceof Comment) {
			Comment mess = (Comment) m;
			logger.info(mess.author() + " replied to me with:\n" + mess.body());
		}else if(m instanceof Message) {
			Message mess = (Message) m;
			logger.info(mess.author() + " pm'd me:\n" + mess.body());

			if(database.containsFullname(mess.fullname())) {
				logger.trace("Skipping message " + mess.fullname() + " since I already have it in my database");
				return;
			}
			database.addFullname(mess.fullname());
			SummonResponse response;
			for(PMSummon summon : pmSummons) {
				response = null;
				try {
					response = summon.handlePM(mess, database, config);
				}catch(Exception ex) {
					logger.catching(ex);
					sleepFor(BRIEF_PAUSE_MS);
				}
				
				if(response != null && !silentMode) {
					handleReply(mess, response.getResponseMessage());
					sleepFor(BRIEF_PAUSE_MS);
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
		new Retryable<Boolean>("markRead", maybeLoginAgainRunnable) {

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
					sleepFor(BRIEF_PAUSE_MS);
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
			success = bot.loginReddit(config.getProperty("user.username"),
					config.getProperty("user.password"),
					config.getProperty("user.appClientID"),
					config.getProperty("user.appClientSecret"));
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
		return new Retryable<Listing>("getRecentComments", maybeLoginAgainRunnable) {
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
		new Retryable<Boolean>("handleReply", maybeLoginAgainRunnable) {

			@Override
			protected Boolean runImpl() throws Exception {
				bot.respondTo(replyable, response);
				return Boolean.TRUE;
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
		return new Retryable<Listing>("getRecentSubmissions", maybeLoginAgainRunnable) {
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
		return new Retryable<Listing>("getRecentMessages", maybeLoginAgainRunnable) {
			@Override
			protected Listing runImpl() throws Exception {
				return bot.getUnreadMessages();
			}
		}.run();
	}
	
	/**
	 * Flairs a link
	 * @param linkId the link to flair
	 * @param flair the css class of the flair
	 */
	protected void handleFlair(final String linkId, final String flair) {
		new Retryable<Boolean>("handleFlair", maybeLoginAgainRunnable) {
			@Override
			protected Boolean runImpl() throws Exception {
				try {
					RedditUtils.flairLink(bot.getUser(), linkId, flair);
				}catch(IOException ex) {
					if(ex.getMessage().contains(("403"))) {
						logger.warn("Access forbidden for flairing " + linkId + " with " + flair);
						return Boolean.FALSE;
					}
					return null;
				}
				return Boolean.TRUE;
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
