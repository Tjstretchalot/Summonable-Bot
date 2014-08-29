package me.timothy.bots;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import me.timothy.bots.summon.Summon;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.jreddit.Replyable;
import com.github.jreddit.comment.Comment;
import com.github.jreddit.message.Message;
import com.github.jreddit.submissions.Submission;
import com.github.jreddit.utils.restclient.RestClient;

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
	private final Summon[] commentSummons;
	
	/** The pm summons. */
	private final Summon[] pmSummons;
	
	/** The submission summons. */
	private final Summon[] submissionSummons;
	
	/** The config. */
	private FileConfiguration config;
	
	/** The database. */
	private Database database;
	
	/** The rest client. */
	private RestClient restClient;
	
	/** The bot. */
	private Bot bot;

	/** The logger. */
	private Logger logger;

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
			RestClient restClient, Bot bot, Summon[] commentSummons,
			Summon[] pmSummons, Summon[] submissionSummons) {
		this.commentSummons = commentSummons;
		this.submissionSummons = submissionSummons;
		this.pmSummons = pmSummons;
		
		this.database = database;
		this.config = config;
		this.restClient = restClient;
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
				logger.trace("Scanning comments..");
				scanComments();
				
				logger.trace("Scanning submissions..");
				scanSubmissions();
				
				logger.trace("Scanning pm's..");
				scanPersonalMessages();
				
				System.out.flush();
				System.err.flush();
				sleepFor(30000);
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	/**
	 * Loops through recent comments, ignoring comments by banned 
	 * users or remembered fullnames, and handles them via the appropriate
	 * summons.
	 */
	private void scanComments() {
		List<Comment> comments = getRecentComments();
		sleepFor(2000);

		for (Comment comment : comments) {
			if(database.containsFullname(comment.getFullname()) || config.getBannedUsers().contains(comment.getAuthor().toLowerCase()))
				continue;
			
			for(Summon summon : commentSummons) {
				if(summon.parse(comment)) {
					database.addFullname(comment.getFullname());
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
	private void scanSubmissions() throws IOException, org.json.simple.parser.ParseException, ParseException {
		List<Submission> submissions = getRecentSubmissions();
		sleepFor(2000);
		
		for (Submission submission : submissions) {
			if(database.containsFullname(submission.getFullName()))
				continue;
			
			for(Summon summon : submissionSummons) {
				if(summon.parse(submission)) {
					database.addFullname(submission.getFullName());
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
	private void scanPersonalMessages() {
		List<Message> messages = getRecentMessages();
		markRead(messages);
		for(Message mess : messages) {
			if(mess.isComment()) {
				logger.info(mess.getAuthor() + " replied to me with:\n" + mess.getBody());
			}else {
				logger.info(mess.getAuthor() + " pm'd me:\n" + mess.getBody());
				
				for(Summon summon : pmSummons) {
					if(summon.parse(mess)) {
						summon.applyChanges(config, database);
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
	private void markRead(final List<Message> messages) {
		new Retryable<Boolean>("markRead") {

			@Override
			protected Boolean runImpl() {
				String ids = "";
				boolean first = true;
				for(Message m : messages) {
					if(!first)
						ids += ",";
					else
						first = false;
					ids += m.getFullName();
				}
				if(ids.length() != 0) {
					logger.debug("Marking " + ids + " as read");
					try {
						bot.setReadMessage(ids);
					} catch (IOException e) {
						logger.debug(e);
						return null;
					}
					sleepFor(2000);
				}
				return true;
			}
			
		}.run();
	}

	/**
	 * Logs into reddit based on the configuration. Terminates
	 * the program as if by fail() on failure.
	 */
	private void login() {
		boolean success = bot.loginReddit(config.getUserInfo()
				.getProperty("username"),
				config.getUserInfo().getProperty("password"));
		if (!success)
			fail("Failed to authenticate to reddit");
	}

	/**
	 * Gets a list of recent comments, utilizing exponential-backoff.
	 *
	 * @return recent comments
	 * @see me.timothy.bots.Retryable
	 */
	private List<Comment> getRecentComments() {
		return new Retryable<List<Comment>>("getRecentComments") {
			@Override
			protected List<Comment> runImpl() {
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
	private void handleReply(final Replyable replyable, final String response) {
		new Retryable<Boolean>("handleReply") {

			@Override
			protected Boolean runImpl() {
				try {
					replyable.respondTo(restClient, bot.getUser(), response);
					return true;
				} catch (IOException e) {
					logger.debug(e);
					return null;
				}
			}
			
		}.run();
	}

	/**
	 * Gets a list of recent submissions, utilizing exponential back-off.
	 *
	 * @return a list of recent submissions
	 * @see me.timothy.bots.Retryable
	 */
	private List<Submission> getRecentSubmissions() {
		return new Retryable<List<Submission>>("getRecentSubmissions") {
			@Override
			protected List<Submission> runImpl() {
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
	private List<Message> getRecentMessages() {
		return new Retryable<List<Message>>("getRecentMessages") {
			@Override
			protected List<Message> runImpl() {
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
	private void sleepFor(long ms) {
		try {
			logger.trace("Sleeping for " + ms + " milliseconds");
			Thread.sleep(ms);
		} catch (InterruptedException ex) {
			logger.error(ex);
			fail("Interrupted");
		}
	}

	/**
	 * Checks if the bot has any recent errors, and if so
	 * logs them on {@code level}.
	 *
	 * @param level the level to log the latest error, if applicable
	 */
	private void checkForError(Level level) {
		Throwable lastExc = bot.getLastError();

		if (lastExc != null) {
			logger.log(level, lastExc);
		}
	}

	/**
	 * Terminates the program after logging {@code message} at
	 * Level.ERROR, as well as logging the list of errors
	 * 
	 * @param message the message to put in Level.ERROR
	 * @param errors a list of applicable errors to output
	 */
	private void fail(String message, Throwable... errors) {
		logger.error(message);

		for(Throwable th : errors) {
			logger.error(th);
		}
		checkForError(Level.ERROR);
		System.exit(0);
	}
}
