package me.timothy.bots;

import java.io.IOException;

import me.timothy.jreddit.RedditUtils;
import me.timothy.jreddit.SortType;
import me.timothy.jreddit.User;
import me.timothy.jreddit.info.CommentResponse;
import me.timothy.jreddit.info.Listing;
import me.timothy.jreddit.info.Thing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.parser.ParseException;

/**
 * Handles the functions of the bot (such as replying to comments,
 * checking if a comment summons the bot, etc) but does not contain
 * the connecting logic (looping through each comment that has to be checked
 * and checking it, for example). 
 * <br><br>
 * This class does NOT ever sleep, so you may run into API restrictions. In an 
 * effort to standardize the various error protocols that jReddit uses, this uses
 * a C-like error system.
 * 
 * @author Timothy
 */
public class Bot {
	
	/** The last error. */
	private Throwable lastError;
	
	/** The logger. */
	private Logger logger;
	
	/** The user. */
	private User user;

	/** The subreddit. */
	private String subreddit;

	/**
	 * Creates the bot for the specified subreddit.
	 *
	 * @param subreddit the subreddit to scan
	 */
	public Bot(String subreddit) {
		this.subreddit = subreddit;

		logger = LogManager.getLogger();
	}

	/**
	 * Logs in to reddit. Upon failure use getLastException to
	 * see the issue.
	 * 
	 * @param username Username of the reddit account
	 * @param password Password of the reddit account
	 * @return true on success, false on failure
	 */
	public boolean loginReddit(String username, String password) {
		try {
			user = new User(username, password);
			RedditUtils.loginUser(user);
			return true;
		}catch(Exception e) {
			lastError = e;
			user = null;
			return false;
		}
	}

	/**
	 * Returns a list of recent comments that might need to be scanned.
	 *
	 * @return a list of recent comments
	 * @throws IllegalStateException if the user is null
	 */
	public Listing getRecentComments() throws IllegalStateException {
		if(user == null) {
			throw new IllegalStateException("User is null"); 
		}

		try {
			return RedditUtils.getRecentComments(user, subreddit);
		} catch (IOException | ParseException e) {
			lastError = e;
			return null;
		}
	}

	/**
	 * Returns any new submissions.
	 *
	 * @return new submissions, or null if an error occurs
	 */
	public Listing getRecentSubmissions() throws IllegalStateException {
		if(user == null) {
			throw new IllegalStateException("User is null"); 
		}
		try {
			return RedditUtils.getSubmissions(user, subreddit, SortType.NEW);
		}catch(Exception ex) {
			lastError = ex;
			return null;
		}
	}

	/**
	 * Returns any new messages.
	 *
	 * @return new messages, or null if an error occurs
	 */
	public Listing getUnreadMessages() {
		if(user == null) {
			throw new IllegalStateException("User is null"); 
		}
		
		try {
			return RedditUtils.getUnreadMessages(user);
		} catch (IOException | ParseException e) {
			lastError = e;
			return null;
		}
				
	}
	
	
	/**
	 * Responds to the specified replyable with the specified message.
	 *
	 * @param replyable the thing to reply to
	 * @param message the message
	 * @return success
	 * @throws IllegalStateException the illegal state exception
	 */
	public boolean respondTo(Thing replyable, String message) throws IllegalStateException {
		if(user == null) {
			throw new IllegalStateException("null user");
		}
		try {
			CommentResponse resp = RedditUtils.comment(user, replyable.fullname(), message);
			logger.trace("Responded to " + replyable.id());
			if(resp.getErrors() != null && resp.getErrors().size() > 0)
				return false;
			
			return true;
		} catch (IOException | ParseException e) {
			lastError = e;
			return false;
		}
	}

	/**
	 * Marks the specified messages as read.
	 *
	 * @param ids the messages
	 * @return if the message was probably marked as read
	 * @throws IllegalStateException if rest client or user is null
	 */
	public boolean setReadMessage(String ids) throws IllegalStateException {
		if(user == null) {
			throw new IllegalStateException("null user");
		}
		
		try {
			RedditUtils.markAsRead(user, ids);
		} catch (IOException | ParseException e) {
			lastError = e;
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns the last error, if there is one.
	 * @return most recent error
	 */
	public Throwable getLastError() {
		return lastError;
	}

	/**
	 * Clears the last error.
	 */
	public void clearLastError() {
		lastError = null;
	}

	/**
	 * Gets the user.
	 *
	 * @return What user this buot is using
	 */
	public User getUser() {
		return user;
	}
}

