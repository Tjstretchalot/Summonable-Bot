package me.timothy.bots.summon;

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
	
	private List<PMResponse> pmResponses;
	
	private String reportMessage;
	
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
	 * @param responseType the reponse type
	 * @param responseMessage the response message
	 * @param linkFlairTemplateId the flair that should be applied to the link
	 * @param pmResponses the pms that should be sent out (or null for none)
	 */
	public SummonResponse(ResponseType responseType, String responseMessage, String linkFlairTemplateId, List<PMResponse> pmResponses)
	{
		this(responseType, responseMessage, linkFlairTemplateId, pmResponses, null);
	}
	
	public SummonResponse(ResponseType responseType, String responseMessage, String linkFlairTemplateId, List<PMResponse> pmResponses, String reportMessage)
	{
		this.responseType = responseType;
		this.responseMessage = responseMessage;
		this.linkFlairTemplateId = linkFlairTemplateId;
		this.pmResponses = pmResponses;
		this.reportMessage = reportMessage;
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
}
