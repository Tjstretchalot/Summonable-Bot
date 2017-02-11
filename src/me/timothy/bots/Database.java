package me.timothy.bots;


/**
 * Describes a database. At minimum, a database needs to be able
 * to remember fullnames so comments/submissions aren't used multiple times.
 * 
 * @author Timothy
 */
public abstract class Database {
	
	/**
	 * Adds the fullname to the database.
	 *
	 * @param id the id to add
	 */
	public abstract void addFullname(String id);

	/**
	 * Checks if the fullname is in the database.
	 *
	 * @param id the fullname to cehck
	 * @return if it is in the database
	 */
	public abstract boolean containsFullname(String id);
}
