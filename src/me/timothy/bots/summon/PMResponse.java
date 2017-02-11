package me.timothy.bots.summon;

/**
 * Describes a portion of a summon response that simply sends
 * a message to the specified user with the specified title
 * and text.
 * 
 * @author Timothy
 */
public class PMResponse {
	private String to;
	private String title;
	private String text;
	
	/**
	 * Creates a new pm response with the specified target, title,
	 * and text
	 * @param to who this pm should be sent to
	 * @param title the title of the pm
	 * @param text the text of the pm
	 */
	public PMResponse(String to, String title, String text)
	{
		this.to = to;
		this.title = title;
		this.text = text;
	}
	
	/**
	 * The username that will be pm'd
	 * 
	 * @return the username to pm
	 */
	public String getTo()
	{
		return to;
	}
	
	/**
	 * The title of the pm.
	 * 
	 * @return pm title
	 */
	public String getTitle()
	{
		return title;
	}
	
	/**
	 * The text of the pm
	 * 
	 * @return pm text
	 */
	public String getText()
	{
		return text;
	}
}
