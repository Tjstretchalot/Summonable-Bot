package me.timothy.bots;

import java.io.IOException;
import java.util.List;

import me.timothy.jreddit.RedditUtils;
import me.timothy.jreddit.SortType;
import me.timothy.jreddit.User;
import me.timothy.jreddit.info.CommentResponse;
import me.timothy.jreddit.info.Errorable;
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
 * This class does NOT ever sleep, so you may run into API restrictions.
 * 
 * @author Timothy
 */
public class Bot {
	
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
	 * @throws ParseException if the response from reddit is unparsable
	 * @throws IOException if an i/o exception occurs
	 */
	public boolean loginReddit(String username, String password, String appClientID, String appClientSecret) throws IOException, ParseException {
		user = new User(username, password, appClientID, appClientSecret);
		RedditUtils.loginUser(user);
		return true;
	}

	/**
	 * Returns a list of recent comments that might need to be scanned.
	 *
	 * @return a list of recent comments
	 * @throws IllegalStateException if the user is null
	 * @throws ParseException if the response from reddit is unparsable
	 * @throws IOException if an i/o exception occurs
	 */
	public Listing getRecentComments() throws IllegalStateException, IOException, ParseException {
		if(user == null) {
			throw new IllegalStateException("User is null"); 
		}
		
		return RedditUtils.getRecentComments(user, subreddit);
	}

	/**
	 * Returns any new submissions.
	 *
	 * @return new submissions, or null if an error occurs
	 * @throws ParseException if the response from reddit is unparsable
	 * @throws IOException if an i/o exception occurs
	 */
	public Listing getRecentSubmissions() throws IllegalStateException, IOException, ParseException {
		if(user == null) {
			throw new IllegalStateException("User is null"); 
		}
		
		return RedditUtils.getSubmissions(user, subreddit, SortType.NEW);
	}

	/**
	 * Returns any new messages.
	 *
	 * @return new messages, or null if an error occurs
	 * @throws ParseException if the response from reddit is unparsable
	 * @throws IOException if an i/o exception occurs
	 */
	public Listing getUnreadMessages() throws IOException, ParseException {
		if(user == null) {
			throw new IllegalStateException("User is null"); 
		}
		
		return RedditUtils.getUnreadMessages(user);	
	}
	
	
	/**
	 * Responds to the specified replyable with the specified message.
	 *
	 * @param replyable the thing to reply to
	 * @param message the message
	 * @return successful as we can be. returns true if we get TOO_OLD
	 * @throws IllegalStateException the illegal state exception
	 * @throws ParseException if the response from reddit is unparsable
	 * @throws IOException if an i/o exception occurs
	 */
	public boolean respondTo(Thing replyable, String message) throws IllegalStateException, IOException, ParseException {
		if(user == null) {
			throw new IllegalStateException("null user");
		}
		CommentResponse resp = RedditUtils.comment(user, replyable.fullname(), message);
		logger.trace("Responded to " + replyable.id());
		if(resp.getErrors() != null && resp.getErrors().size() > 0) {
			List<?> errors = resp.getErrors();
			
			logger.trace("Got errors for reply to " + replyable.fullname() + ": " + errors.toString());
			
			for(Object o : errors) {
				if(o instanceof List) {
					List<?> error = (List<?>) o;
					for(Object o2 : error) {
						if(o2 instanceof String) {
							String errorMessage = (String) o2;
							
							if(errorMessage.equals("TOO_OLD")) {
								logger.trace("TOO_OLD error => response was a success (for our purposes)");
								return true;
							}
						}
					}
				}
			}
			return false;
		}

		return true;
	}
	
	/**
	 * Sends a personal message
	 * @param to who to send the message to
	 * @param title the title of the message
	 * @param message the text of the message
	 * @return if there are any errors
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public Errorable sendPM(String to, String title, String message) throws IOException, ParseException {
		return RedditUtils.sendPersonalMessage(user, to, title, message);
	}

	/**
	 * Marks the specified messages as read.
	 *
	 * @param ids the messages
	 * @return if the message was probably marked as read
	 * @throws IllegalStateException if rest client or user is null
	 * @throws ParseException if the response from reddit is unparsable
	 * @throws IOException if an i/o exception occurs
	 */
	public boolean setReadMessage(String ids) throws IllegalStateException, IOException, ParseException {
		if(user == null) {
			throw new IllegalStateException("null user");
		}
		
		RedditUtils.markAsRead(user, ids);
		
		return true;
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

