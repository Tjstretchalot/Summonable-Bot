package me.timothy.bots.summon;

import me.timothy.jreddit.info.Message;

/**
 * An extension to a summon that may parse
 * personal messages
 * 
 * @author Timothy
 */
public interface PMSummon extends Summon {
	
	/**
	 * Parses the message
	 * 
	 * @param message the message to parse
	 * @return if their should be a response
	 */
	public boolean parse(Message message);
}
