package me.timothy.bots.summon;

import me.timothy.bots.Database;
import me.timothy.bots.FileConfiguration;
import me.timothy.jreddit.info.Comment;
/**
 * An extension to a summon that may parse comments
 * 
 * @author Timothy
 */
public interface CommentSummon extends Summon {
	/**
	 * Handles the comment 
	 * @param comment the comment to handle
	 * @param db the database to modify
	 * @param config the configuration options to use
	 * @return the response, or null
	 */
	public SummonResponse handleComment(Comment comment, Database db, FileConfiguration config);
}
