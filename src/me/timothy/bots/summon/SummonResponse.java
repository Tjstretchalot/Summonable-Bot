package me.timothy.bots.summon;

/**
 * Describes how a summon will be responding
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
		INVALID
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
	 * Creates a new response
	 * 
	 * @param responseType the response type
	 * @param responseMessage the response message
	 * @param linkFlairTemplateId the flair that should be applied to the link
	 */
	public SummonResponse(ResponseType responseType, String responseMessage, String linkFlairTemplateId) {
		this.responseType = responseType;
		this.responseMessage = responseMessage;
		this.linkFlairTemplateId = linkFlairTemplateId;
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
	
	public String getLinkFlair() {
		return linkFlairTemplateId;
	}
}
