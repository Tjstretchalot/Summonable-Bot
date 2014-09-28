package me.timothy.bots.summon;

import me.timothy.jreddit.info.Link;

/**
 * An extension to a summon that may parse links
 * 
 * @author Timothy
 */
public interface LinkSummon extends Summon {
	
	/**
	 * Parses the submission.
	 *
	 * @param submission the submission to parse
	 * @return if any new changes need to be applied
	 */
	public boolean parse(Link submission);
}
