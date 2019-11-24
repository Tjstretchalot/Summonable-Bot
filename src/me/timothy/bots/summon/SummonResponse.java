package me.timothy.bots.summon;

import java.util.HashMap;
import java.util.List;

/**
 * Describes how a summon will be responding. Unless otherwise specified,
 * the response will be done through a comment to the relevant body. However,
 * there is support to message specific individuals as part of a summon 
 * response.
 * 
 * @author Timothy
 */
public class SummonResponse {
	/**
	 * A brief description of what the response is saying (valid/invalid)
	 * 
	 * @author Timothy
	 * 
	 */
	public enum ResponseType {
		/**
		 * Implies that this was a valid use of this summon
		 */
		VALID,

		/**
		 * Implies that this seems to be using the summon, but not in a correct
		 * manner
		 */
		INVALID,
		
		/**
		 * This indicates that there is no standard response in this response
		 * (i.e. just a report but no comment)
		 */
		SILENT
	}

	/**
	 * The type of response
	 */
	private ResponseType responseType;

	/**
	 * The message to respond with
	 */
	private String responseMessage;
	
	/**
	 * The type of flair to apply to the link this comment
	 * is on. Link flairs can be acquired using api/flairselector
	 */
	private String linkFlairTemplateId;
	
	/**
	 * The list of pm responses
	 */
	private List<PMResponse> pmResponses;
	
	/**
	 * The string to report the message with
	 */
	private String reportMessage;
	
	/**
	 * If we should ban userToBan
	 */
	private boolean banUser;
	
	/**
	 * The user to ban, if banUser is true
	 */
	private String userToBan;
	
	/**
	 * The note to other moderators if banUser is true
	 */
	private String banNote;
	
	/**
	 * The message to pass to userToBan if banUser is true
	 */
	private String banMessage;
	
	/**
	 * The reason for why we are banning userToBan, if banUser is true
	 */
	private String banReason;
	
	/**
	 * If we should unban userToUnban
	 */
	private boolean unbanUser;
	
	/**
	 * The user we should unban
	 */
	private String userToUnban;
	
	/**
	 * A collection of special handlers which will be passed to BotDriver#handleSpecial(key, val).
	 * Since we may use a single handler multiple times, the keys are lists
	 */
	private HashMap<String, List<Object>> specialHandlers;
	
	/**
	 * Creates a new response
	 * 
	 * @param responseType the response type
	 * @param responseMessage the response message
	 * @param linkFlairTemplateId the flair that should be applied to the link
	 */
	public SummonResponse(ResponseType responseType, String responseMessage, String linkFlairTemplateId) {
		this(responseType, responseMessage, linkFlairTemplateId, null);
	}
	
	/**
	 * Creates a new response
	 * 
	 * @param responseType the response type
	 * @param responseMessage the response message
	 * @param linkFlairTemplateId the flair that should be applied to the link
	 * @param pmResponses the pms that should be sent out (or null for none)
	 */
	public SummonResponse(ResponseType responseType, String responseMessage, String linkFlairTemplateId, List<PMResponse> pmResponses)
	{
		this(responseType, responseMessage, linkFlairTemplateId, pmResponses, null);
	}
	
	/**
	 * Creates a new response 
	 * 
	 * @param responseType the response type
	 * @param responseMessage the response message
	 * @param linkFlairTemplateId the template to flair the summoning thing with
	 * @param pmResponses the pm responses we need to do
	 * @param reportMessage the message to report the thing with
	 */
	public SummonResponse(ResponseType responseType, String responseMessage, String linkFlairTemplateId, List<PMResponse> pmResponses, String reportMessage)
	{
		this.responseType = responseType;
		this.responseMessage = responseMessage;
		this.linkFlairTemplateId = linkFlairTemplateId;
		this.pmResponses = pmResponses;
		this.reportMessage = reportMessage;
		this.specialHandlers = new HashMap<String, List<Object>>();
	}
	
	/**
	 * Creates a new response
	 * 
	 * @param responseType The response type
	 * @param responseMessage The response message
	 * @param linkFlairTemplateId The link flair template id
	 * @param pmResponses The pm responses
	 * @param reportMessage The message to report the thing with
	 * @param banUser If we should ban a user
	 * @param userToBan The user we should ban
	 * @param banMessage The message to tell the user that we banned
	 * @param banReason The reason we banned the user
	 * @param banNote The note to other moderators for why we banned the user
	 * @param unbanUser If we should unban a user
	 * @param userToUnban The user we should unban
	 * @param specialHandlers Any special handler notification code (see BotDriver#handleSpecial)
	 */
	public SummonResponse(ResponseType responseType, String responseMessage, String linkFlairTemplateId, List<PMResponse> pmResponses, 
			String reportMessage, boolean banUser, String userToBan, String banMessage, String banReason, String banNote, boolean unbanUser, 
			String userToUnban, HashMap<String, List<Object>> specialHandlers)
	{
		this.responseType = responseType;
		this.responseMessage = responseMessage;
		this.linkFlairTemplateId = linkFlairTemplateId;
		this.pmResponses = pmResponses;
		this.reportMessage = reportMessage;
		this.banUser = banUser;
		this.userToBan = userToBan;
		this.banMessage = banMessage;
		this.banReason = banReason;
		this.banNote = banNote;
		this.unbanUser = unbanUser;
		this.userToUnban = userToUnban;
		this.specialHandlers = specialHandlers == null ? new HashMap<String, List<Object>>() : specialHandlers;
	}
	
	/**
	 * Creates a new response with no link flair
	 * 
	 * @param responseType
	 *            the type of response
	 * @param responseMessage
	 *            the message of the response
	 */
	public SummonResponse(ResponseType responseType, String responseMessage) {
		this(responseType, responseMessage, null);
	}

	/**
	 * @return the response type
	 */
	public ResponseType getResponseType() {
		return responseType;
	}
	
	/**
	 * @return the response
	 */
	public String getResponseMessage() {
		return responseMessage;
	}
	
	/**
	 * @return the flair that should be applied to the link
	 */
	public String getLinkFlair() {
		return linkFlairTemplateId;
	}
	
	/**
	 * @return the report message for the link
	 */
	public String getReportMessage() {
		return reportMessage;
	}
	
	/**
	 * @return the pms that should be sent or null for none
	 */
	public List<PMResponse> getPMResponses(){
		return pmResponses;
	}
	
	/**
	 * @return if we should ban a user
	 * @see #getUsernameToBan()
	 * @see #getBanMessage()
	 * @see #getBanReason()
	 * @see #getBanNote()
	 */
	public boolean shouldBanUser() {
		return banUser;
	}
	
	/**
	 * @return the username to ban, if shouldBanUser
	 * @see #shouldBanUser()
	 */
	public String getUsernameToBan() {
		return userToBan;
	}
	
	/**
	 * @return the message to tell the user we're banning, if shouldBanUser
	 * @see #shouldBanUser()
	 */
	public String getBanMessage() {
		return banMessage;
	}
	
	/**
	 * @return the reason we're banning the user, if shouldBanUser
	 * @see #shouldBanUser()
	 */
	public String getBanReason() {
		return banReason;
	}
	
	/**
	 * @return the note to moderators if we're banning usernameToBan, if shouldBanUser
	 * @see #shouldBanUser()
	 * @see #getUsernameToBan()
	 */
	public String getBanNote() {
		return banNote;
	}
	
	/**
	 * @return if we should unban a user
	 * @see #getUsernameToUnban()
	 */
	public boolean shouldUnbanUser() {
		return unbanUser;
	}
	
	/**
	 * @return the user to unban, if shouldUnbanUser
	 * @see #shouldUnbanUser()
	 */
	public String getUsernameToUnban() {
		return userToUnban;
	}
	
	/**
	 * This can be used to passed special handling code for summon responses. For example, suppose
	 * one essential characteristic of a particular bot involves API calls to a backend. This can't
	 * be handled like banning/unbanning which are general functions, but it also shouldn't occur
	 * directly within the response processing (which should be fast and internal only). Thus
	 * a special handler is used, where the key is shared and unique (i.e. CANVAS_API_CALL)
	 * and the object is understood (ie., CanvasAPIResponse). We may use the same key multiple
	 * times, hence the values are lists of objects for that key.
	 * 
	 * @return the special handling objects, never null
	 */
	public HashMap<String, List<Object>> getSpecialHandlers() {
		return specialHandlers;
	}
}
