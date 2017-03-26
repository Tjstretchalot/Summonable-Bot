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
	 * This should do nothing except fast checks to verify this is a potential candidate
	 * for this comment summon.
	 * 
	 * @param comment the comment
	 * @param db the database
	 * @param config file configuration
	 * @return if this comment is a potential candidate for a summon response
	 */
	public boolean mightInteractWith(Comment comment, Database db, FileConfiguration config);
	
	/**
	 * Handles the comment. May assume mightInteractWith returned true.
	 * 
	 * @param comment the comment to handle
	 * @param db the database to modify
	 * @param config the configuration options to use
	 * @return the response, or null
	 */
	public SummonResponse handleComment(Comment comment, Database db, FileConfiguration config);
}
