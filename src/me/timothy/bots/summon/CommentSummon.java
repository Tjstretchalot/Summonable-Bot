package me.timothy.bots.summon;

import me.timothy.jreddit.info.Comment;
/**
 * An extension to a summon that may parse comments
 * 
 * @author Timothy
 */
public interface CommentSummon extends Summon {

	/**
	 * Parses the comment
	 * @param comment the comment to parse
	 * @return if changes should be applied
	 */
	public boolean parse(Comment comment);
}
