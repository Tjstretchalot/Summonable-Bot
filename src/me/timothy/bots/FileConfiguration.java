package me.timothy.bots;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: Auto-generated Javadoc
/**
 * Loads lots and lots of various strings/properties from there appropriate
 * files.
 *
 * @author Timothy
 */
public abstract class FileConfiguration {
	
	/** The Constant BANNED_USERS_FILE. */
	public static final String BANNED_USERS_FILE = "banned.txt";
	
	/** The Constant USER_INFO_FILE. */
	public static final String USER_INFO_FILE = "user.properties";
	
	/** The logger. */
	private Logger logger;
	
	/** The banned users. */
	private List<String> bannedUsers;
	
	/** The user info. */
	private Properties userInfo;

	/**
	 * Initializes a file configuration but does not load
	 * any configuration.
	 */
	public FileConfiguration() {
		logger = LogManager.getLogger();
	}

	/**
	 * Loads all the necessary configuration.
	 *
	 * @throws IOException             if an io-exception occurs (or a file is not found)
	 * @throws NullPointerException             if a required key is missing
	 */
	public void load() throws IOException, NullPointerException {
		if(Files.exists(Paths.get(BANNED_USERS_FILE))) {
			bannedUsers = loadStringList(Paths.get(BANNED_USERS_FILE).toFile());
		}else {
			bannedUsers = new ArrayList<>();
		}
		
		userInfo = loadProperties(Paths.get(USER_INFO_FILE).toFile(), "username", "password");
	}

	/**
	 * Loads properties from the specified file, as if by
	 * {@link java.util.Properties#load(java.io.Reader)}
	 * 
	 * @param file
	 *            the file to load from
	 * @param requiredKeys
	 *            the list of required keys
	 * @return the properties in the {@code file}
	 * @throws IOException
	 *             if an i/o exception occurs (like {@code file} not existing)
	 * @throws NullPointerException
	 *             if a required key is missing or the file is null
	 */
	protected Properties loadProperties(File file, String... requiredKeys)
			throws IOException, NullPointerException {
		logger.debug("Loading properties from " + file.getCanonicalPath());
		Properties props = new Properties();
		try (FileReader fr = new FileReader(file)) {
			props.load(fr);
		}

		for (String reqKey : requiredKeys) {
			if (!props.containsKey(reqKey))
				throw new NullPointerException(file.getName()
						+ " is missing key " + reqKey);
		}
		return props;
	}

	/**
	 * Loads a reply string from a file, ignoring lines prefixed with a hash-tag
	 * (#). Any empty lines prior to the first non-empty line are ignored. Lines
	 * are separated Unix-like (only \n)
	 * 
	 * @param file
	 *            the file to load from
	 * @return the reply format
	 * @throws IOException
	 *             if an i/o exception occurs, like the file not existing
	 */
	protected String loadReplyString(File file) throws IOException {
		logger.debug("Loading reply string from " + file.getCanonicalPath());
		String result = "";
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String ln;
			boolean first = true;
			while ((ln = br.readLine()) != null) {
				if (ln.startsWith("#") || (first && ln.isEmpty()))
					continue;
				if (!first) {
					result += "\n";
				} else {
					first = false;
				}
				result += ln;
			}
		}
		return result;
	}

	/**
	 * Loads a list of strings from the file, where there is 1 string per line.
	 * Lines prefixed with /u/ are modified as if by
	 * {@link java.lang.String#substring(int)} with parameter 3. All strings
	 * are lower-cased.
	 * 
	 * @param file
	 *            the file to load from
	 * @return each line in the file
	 * @throws IOException
	 *             if an i/o exception occurs, like the file not existing
	 */
	protected List<String> loadStringList(File file) throws IOException {
		logger.debug("Loading string list from " + file.getCanonicalPath());
		List<String> result = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String ln;
			while ((ln = br.readLine()) != null) {
				if (ln.startsWith("/u/")) {
					ln = ln.substring(3);
				}
				result.add(ln.toLowerCase());
			}
		}
		return result;
	}

	/**
	 * Gets the banned users.
	 *
	 * @return the list of banned users
	 */
	public List<String> getBannedUsers() {
		return bannedUsers;
	}

	/**
	 * Gets the user info.
	 *
	 * @return the user info
	 */
	public Properties getUserInfo() {
		return userInfo;
	}
}
