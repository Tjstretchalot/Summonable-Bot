package me.timothy.bots.summon;

import me.timothy.bots.Database;
import me.timothy.bots.FileConfiguration;

import com.github.jreddit.comment.Comment;
import com.github.jreddit.message.Message;
import com.github.jreddit.submissions.Submission;

// TODO: Auto-generated Javadoc
/**
 * Contains information about a summoning. Meant to be initialized
 * once and then reused.
 * 
 * @author Timothy
 */
public abstract class Summon {
	
	/**
	 * Parses the submission.
	 *
	 * @param submission the submission to parse
	 * @return if any new changes need to be applied
	 * @throws UnsupportedOperationException if this type of summon isn't implemented
	 */
	public boolean parse(Submission submission) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Parses the comment.
	 *
	 * @param comment the comment to parse
	 * @return if any new changes need to be applied
	 * @throws UnsupportedOperationException if this type of summon isn't implemented
	 */
	public boolean parse(Comment comment) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Parses the message.
	 *
	 * @param message the message to parse
	 * @return if any new changes need to be applied
	 * @throws UnsupportedOperationException if this type of summon isn't implemented
	 */
	public boolean parse(Message message) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
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
