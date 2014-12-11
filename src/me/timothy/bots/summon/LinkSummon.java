package me.timothy.bots.summon;

import me.timothy.bots.Database;
import me.timothy.bots.FileConfiguration;
import me.timothy.jreddit.info.Link;

/**
 * An extension to a summon that may parse links
 * 
 * @author Timothy
 */
public interface LinkSummon extends Summon {
	/**
	 * Handles the link 
	 * @param link the link to handle
	 * @param db the database to modify
	 * @param config the configuration options to use
	 * @return the response, or null
	 */
	public SummonResponse handleLink(Link link, Database db, FileConfiguration config);
}
