package me.timothy.bots.summon;

import me.timothy.bots.Database;
import me.timothy.bots.FileConfiguration;

// TODO: Auto-generated Javadoc
/**
 * Contains information about a summoning. Meant to be initialized
 * once and then reused.
 * 
 * @author Timothy
 */
public interface Summon {
	/**
	 * Applies the internal state as changes to the database. If parse was never called
	 * or the last parse failed, this has undefined behavior.
	 * 
	 * @param config the configuration options to use
	 * @param database the database to modify 
	 * @return the response that should be given, if applicable
	 */
	public abstract String applyChanges(FileConfiguration config, Database database);
}
