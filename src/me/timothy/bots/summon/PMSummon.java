package me.timothy.bots.summon;

import me.timothy.bots.Database;
import me.timothy.bots.FileConfiguration;
import me.timothy.jreddit.info.Message;


/**
 * An extension to a summon that may parse
 * personal messages
 * 
 * @author Timothy
 */
public interface PMSummon extends Summon {
	/**
	 * Handles the message 
	 * @param message the message to handle
	 * @param db the database to modify
	 * @param config the configuration options to use
	 * @return the response, or null
	 */
	public SummonResponse handlePM(Message message, Database db, FileConfiguration config);
}
