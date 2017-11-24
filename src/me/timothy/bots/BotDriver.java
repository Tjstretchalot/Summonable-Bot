package me.timothy.bots;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import me.timothy.bots.summon.CommentSummon;
import me.timothy.bots.summon.LinkSummon;
import me.timothy.bots.summon.PMResponse;
import me.timothy.bots.summon.PMSummon;
import me.timothy.bots.summon.SummonResponse;
import me.timothy.bots.summon.SummonResponse.ResponseType;
import me.timothy.jreddit.RedditUtils;
import me.timothy.jreddit.User;
import me.timothy.jreddit.info.BannedUsersListing;
import me.timothy.jreddit.info.Comment;
import me.timothy.jreddit.info.Errorable;
import me.timothy.jreddit.info.Link;
import me.timothy.jreddit.info.Listing;
import me.timothy.jreddit.info.LoginResponse;
import me.timothy.jreddit.info.Message;
import me.timothy.jreddit.info.ModeratorListing;
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
		if(!canInteractWithUsFast(comment.author())) {
			if(debug)
				logger.trace(String.format("Skipping %s because %s is not allowed to interact with us (fast)", comment.fullname(), comment.author()));
			return false;
		}
		
		if(comment.author().equalsIgnoreCase(config.getProperty("user.username"))) {
			if(debug)
				logger.trace(String.format("Skipping %s because thats my comment", comment.fullname()));
			return false;
		}

		database.addFullname(comment.fullname());
		boolean hadResponse = false;
		boolean checkedCanInteractWith = false;
		
		SummonResponse response;
		for(CommentSummon summon : commentSummons) {
			if(!summon.mightInteractWith(comment, database, config)) {
				if(debug)
					logger.printf(Level.TRACE, "%s specified it will not interact with %s", summon.getClass().getCanonicalName(), comment.fullname());
				continue;
			}
			
			if(!checkedCanInteractWith) {
				checkedCanInteractWith = true;
				
				if(!canInteractWithUsFull(comment.author()))
				{
					if(debug)
						logger.trace(String.format("Skipping %s because %s is not allowed to interact with us (full)", comment.fullname(), comment.author()));
					onFailedInteractCheck(comment);
					return false;
				}
			}
			
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
					if(response.getResponseType() != ResponseType.SILENT) {
						handleReply(comment, response.getResponseMessage());
						sleepFor(BRIEF_PAUSE_MS);
					}
					
					if(response.getLinkFlair() != null) {
						handleFlair(comment.linkID(), response.getLinkFlair());
						sleepFor(BRIEF_PAUSE_MS);
					}
					
					if(response.getReportMessage() != null) {
						handleReport(comment.fullname(), response.getReportMessage());
						sleepFor(BRIEF_PAUSE_MS);
					}
					
					if(response.shouldBanUser())
					{
						handleBanUserOnAllSubreddits(response.getUsernameToBan(), response.getBanMessage(), response.getBanReason(), response.getBanNote());
						sleepFor(BRIEF_PAUSE_MS);
					}
					
					if(response.shouldUnbanUser()) {
						handleUnbanUserOnAllSubreddits(response.getUsernameToUnban());
						sleepFor(BRIEF_PAUSE_MS);
					}
					
					handlePMResponses(response);
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
		
		if(!canInteractWithUsFast(submission.author()))
			return;

		boolean checkedInteractWithUsFull = false;
		SummonResponse response;
		for(LinkSummon summon : submissionSummons) {
			if(!summon.mightInteractWith(submission, database, config))
				continue;
			
			if(!checkedInteractWithUsFull) {
				checkedInteractWithUsFull = true;
				
				if(!canInteractWithUsFull(submission.author())) {
					onFailedInteractCheck(submission);
					return;
				}
			}
			
			response = null;
			try {
				response = summon.handleLink(submission, database, config);
			}catch(Exception ex) {
				logger.catching(ex);
				sleepFor(BRIEF_PAUSE_MS);
			}
			
			if(response != null && !silentMode) {
				if(response.getResponseType() != ResponseType.SILENT) {
					handleReply(submission, response.getResponseMessage());
					sleepFor(BRIEF_PAUSE_MS);
				}
				
				if(response.getReportMessage() != null) {
					handleReport(submission.fullname(), response.getReportMessage());
					sleepFor(BRIEF_PAUSE_MS);
				}
				
				if(response.shouldBanUser())
				{
					handleBanUserOnAllSubreddits(response.getUsernameToBan(), response.getBanMessage(), response.getBanReason(), response.getBanNote());
					sleepFor(BRIEF_PAUSE_MS);
				}
				
				if(response.shouldUnbanUser()) {
					handleUnbanUserOnAllSubreddits(response.getUsernameToUnban());
					sleepFor(BRIEF_PAUSE_MS);
				}
				
				handlePMResponses(response);
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
			
			if(mess.author() == null) {
				logger.trace("That message was sent with a null author so ignoring it");
				return;
			}
			
			if(!canInteractWithUsFull(mess.author()))
			{
				logger.trace("Skipping message " + mess.fullname() + " since " + mess.author() + " can't interact with us");
				onFailedInteractCheck(m);
				return;
			}
			
			SummonResponse response;
			for(PMSummon summon : pmSummons) {
				response = null;
				try {
					response = summon.handlePM(mess, database, config);
				}catch(Exception ex) {
					logger.catching(ex);
					sleepFor(BRIEF_PAUSE_MS);
				}
				
				if(response != null && !silentMode && response.getResponseType() != ResponseType.SILENT) {
					handleReply(mess, response.getResponseMessage());
					sleepFor(BRIEF_PAUSE_MS);
					
					handlePMResponses(response);
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
					logger.catching(ex);
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
	 * Reports the fullname with the specified message
	 * @param thingFullname the fullname of the thing to report
	 * @param reportMessage the message to report the thing with
	 */
	protected void handleReport(final String thingFullname, final String reportMessage) {
		new Retryable<Boolean>("handleReport", maybeLoginAgainRunnable) {

			@Override
			protected Boolean runImpl() throws Exception {
				RedditUtils.report(bot.getUser(), thingFullname, reportMessage);
				return Boolean.TRUE;
			}
			
		}.run();
	}
	
	/**
	 * Bans the specified user from the specified subredit IF the user is not already banned
	 * and not a moderator
	 * 
	 * @param userToBan the user to ban
	 * @param banMessage the message to pass to the user
	 * @param banReason the predefined string constants (in the subreddit options) for the "reason".
	 * @param banNote the note to other moderators, less than 100 characters.
	 */
	protected void handleBanUserOnAllSubreddits(final String userToBan, final String banMessage, final String banReason, final String banNote)
	{
		if(userToBan == null || banMessage == null || banReason == null || banNote == null)
			throw new IllegalArgumentException(String.format("userToBan=%s, banMessage=%s, banReason=%s, banNote=%s something is null", userToBan, banMessage, banReason, banNote));
		
		if(userToBan.equalsIgnoreCase(config.getProperty("user.username")))
			return;
		
		String[] subreddits = bot.getSubreddits();
		
		boolean failure = false;
		for(final String subreddit : subreddits) {
			Boolean result = handleBanUser(subreddit, userToBan, banMessage, banReason, banNote);
			
			if(result != Boolean.TRUE)
				failure = true;
		}
		
		if(!failure) {
			onSuccessfullyBannedUser(userToBan);
		}
	}
	
	/**
	 * Bans userToBan from the specified subreddit
	 * 
	 * @param subreddit subreddit to ban on
	 * @param userToBan user to ban
	 * @param banMessage the message given to the user
	 * @param banReason the reason (string 'other')
	 * @param banNote the note to moderators
	 * @return Boolean.TRUE if ban succeeded, Boolean.FALSE if ban will never succeed, null if we tried
	 * too many times (like, every request in the last 24 hours has failed)
	 */
	protected Boolean handleBanUser(final String subreddit, final String userToBan, final String banMessage, final String banReason, final String banNote)
	{
		return new Retryable<Boolean>("handleBan - " + userToBan + " on /r/" + subreddit, maybeLoginAgainRunnable) {
			boolean definitelyNotBannedThere = false;
			boolean definitelyNotModeratorThere = false;
			
			@Override
			protected Boolean runImpl() throws Exception {
				if(!definitelyNotBannedThere) {
					BannedUsersListing banListing = RedditUtils.getBannedUsersForSubredditByName(subreddit, userToBan, bot.getUser());
					if(banListing != null && banListing.numChildren() > 0) {
						logger.info(String.format("Failed to ban %s from %s - he was already banned there", userToBan, subreddit));
						return Boolean.FALSE; // already banned
					}
					
					definitelyNotBannedThere = true;
					sleepFor(BRIEF_PAUSE_MS);
				}
				
				if(!definitelyNotModeratorThere) {
					Boolean isMod = isModerator(subreddit, userToBan);
					if((isMod == null || isMod.booleanValue())) {
						logger.info(String.format("Failed to ban %s from %s - he is a moderator there", userToBan, subreddit));
						return Boolean.FALSE; // never attempt to ban moderators
					}
					
					definitelyNotModeratorThere = true;
				}
				
				RedditUtils.banFromSubreddit(subreddit, userToBan, banMessage, banReason, banNote, bot.getUser());
				logger.info(String.format("Banned %s from %s - banMessage=%s, banReason=%s, banNote=%s", userToBan, subreddit, banMessage, banReason, banNote));
				sleepFor(BRIEF_PAUSE_MS);
				
				return Boolean.TRUE;
			}
		}.run();
	}
	
	/**
	 * Determine if user is a moderator on subreddit
	 * 
	 * @param subreddit the subreddit
	 * @param user the user
	 * @return Boolean.TRUE if user is a moderator on subreddit, Boolean.FALSE if the user is not a moderator on subreddit,
	 * and null if we got too many exceptions from reddit when trying to figure it out, so we don't know
	 */
	protected Boolean isModerator(final String subreddit, final String user) {
		return new Retryable<Boolean>("isModerator - " + user + " on /r/" + subreddit, maybeLoginAgainRunnable) {

			@Override
			protected Boolean runImpl() throws Exception {
				ModeratorListing modListing = RedditUtils.getModeratorForSubredditByName(subreddit, user, bot.getUser());
				sleepFor(BRIEF_PAUSE_MS);
				return modListing != null && modListing.numChildren() > 0;
			}
			
		}.run();
	}
	
	/**
	 * Called after we successfully ban a user
	 * 
	 * @param username the username that was banned
	 */
	protected void onSuccessfullyBannedUser(final String username) {
	}
	
	/**
	 * Unban the specified user from the specified subreddit.
	 * 
	 * @param subreddit the subreddit to unban from
	 * @param userToUnban the user to unban
	 * @return false if the user wasn't unbanned but he is not currently banned, true if the user
	 * was unbanned and is not currently banned, null if we failed to unban the user and he may 
	 * or may not be banned.
	 */
	protected Boolean handleUnbanUser(final String subreddit, final String userToUnban) {
		return new Retryable<Boolean>("handleUnban - " + userToUnban + " on /r/" + subreddit, maybeLoginAgainRunnable) {
			@Override
			protected Boolean runImpl() throws Exception {
				BannedUsersListing listing = RedditUtils.getBannedUsersForSubredditByName(subreddit, userToUnban, bot.getUser());
				if(listing == null || listing.numChildren() == 0) {
					logger.info(String.format("Failed to unban %s from %s - he was not banned there", userToUnban, subreddit));
					return Boolean.FALSE; // not banned
				}
				
				sleepFor(BRIEF_PAUSE_MS);

				RedditUtils.unbanFromSubreddit(subreddit, userToUnban, bot.getUser());
				logger.info(String.format("Unbanned %s from %s", userToUnban, subreddit));
				
				sleepFor(BRIEF_PAUSE_MS);
				
				return Boolean.TRUE;
			}
		}.run();
	}
	
	/**
	 * Unbans the specified user from all the subreddits this bot monitors
	 * 
	 * @param userToUnban the username to unban
	 */
	protected void handleUnbanUserOnAllSubreddits(final String userToUnban)
	{
		String[] subreddits = bot.getSubreddits();
		
		boolean failure = false;
		for(final String subreddit : subreddits) {
			Boolean result = handleUnbanUser(subreddit, userToUnban);
			
			if(result != Boolean.TRUE) {
				failure = true;
			}
		}
		
		if(!failure) {
			onSuccessfullyUnbanUser(userToUnban);
		}
	}
	
	/**
	 * Called after we successfully unban a user
	 * @param username the username that was unbanned
	 */
	protected void onSuccessfullyUnbanUser(final String username) {
		
	}
	
	/**
	 * Determines if the specified username is allowed to interact with us.
	 * This should be a fairly fast check as it will be called prior to 
	 * checking if summons match, and may be incomplete. canInteractWithUsFull
	 * will be called after checking if at least one summon matches but 
	 * before applying that summon.
	 * 
	 * @param username
	 * @return
	 */
	protected boolean canInteractWithUsFast(final String username)
	{
		if(config.getList("banned").contains(username.toLowerCase()))
			return false;
		
		return true;
	}
	
	/**
	 * This is the full scan to check if a specified username is allowed
	 * to interact with us.
	 * 
	 * @param username the username
	 * @return true if it can interact, false otherwise
	 */
	protected boolean canInteractWithUsFull(final String username)
	{
		if(!canInteractWithUsFast(username))
			return false;
		
		return true;
	}
	
	/**
	 * Called when a thing was found not to be able to interact with us.
	 * 
	 * @param thing the thing that could not interact with us
	 */
	protected void onFailedInteractCheck(final Thing thing)
	{
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
			logger.catching(th);
		}
		System.exit(0);
	}
	
	/**
	 * Sends a message to the specified user with the specified
	 * title & message, using exponential back-off.
	 * 
	 * @param user the user to send the message to
	 * @param title the title of the message
	 * @param message the text of the message
	 * @return NULL if we received too many errors, true if we 
	 * successfully sent a message, false if the request is not valid
	 */
	protected Boolean sendMessage(final String to, final String title, final String message) {
		return new Retryable<Boolean>("Send PM", maybeLoginAgainRunnable) {

			@Override
			protected Boolean runImpl() throws Exception {
				Errorable errors = bot.sendPM(to, title, message);
				List<?> errorsList = errors.getErrors();
				if(errorsList != null && !errorsList.isEmpty()) {
					logger.printf(Level.WARN, "Failed to send (to=%s, title=%s, message=%s): %s", to, title, message, errorsList.toString());
					return false;
				}
				return true;
			}

		}.run();
	}
	
	/**
	 * Handles the PMResponses that are contained in the summon response, if any,
	 * by sending pms to the specified users with the specified title and content.
	 * 
	 * @param response the response which might have pmresponses.
	 */
	protected void handlePMResponses(SummonResponse response) {
		if(response.getPMResponses() == null || response.getPMResponses().size() == 0)
			return;
		
		logger.printf(Level.INFO, "Summon response generated %d pm responses.", response.getPMResponses().size());
		for(PMResponse pmResponse : response.getPMResponses())
		{
			logger.printf(Level.INFO, "Sending pm response (to=%s, title=%s, message=%s)", pmResponse.getTo(), pmResponse.getTitle(), pmResponse.getText());
			
			sendMessage(pmResponse.getTo(), pmResponse.getTitle(), pmResponse.getText());
			sleepFor(BRIEF_PAUSE_MS);
		}
	}
}
