package me.timothy.bots;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class FileConfiguration {
	/** The logger. */
	private Logger logger;
	
	/** The folder */
	private Path folder;
	
	private Map<String, List<String>> lists;
	private Map<String, String> strings;
	private Map<String, Properties> properties;

	/**
	 * Initializes a file configuration but does not load
	 * any configuration.
	 */
	public FileConfiguration() {
		logger = LogManager.getLogger();
		
		folder = Paths.get(".").toAbsolutePath();
		lists = new HashMap<>();
		strings = new HashMap<>();
		properties = new HashMap<>();
	}

	/**
	 * Loads all the necessary configuration.
	 *
	 * @throws IOException             if an io-exception occurs (or a file is not found)
	 * @throws NullPointerException             if a required key is missing
	 */
	public void load() throws IOException, NullPointerException {
		addList("banned", false);
		addProperties("user", true, "username", "password", "appClientID", "appClientSecret");
	}
	
	/**
	 * Gets the absolute path of the folder that configuration
	 * is being loaded from
	 * @return the folder that configuration is being loaded from
	 */
	public Path getFolder() {
		return folder;
	}
	
	/**
	 * Sets the path to the folder that configuration is being loaded
	 * from. Cannot be null and must exist.
	 * @param path the folder to load config from
	 */
	public void setFolder(Path path) {
		if(path == null)
			throw new NullPointerException("folder cannot be null");
		
		if(!Files.exists(path)) {
			throw new IllegalArgumentException("File Configuration folder " + path.toString() + " does not exist");
		}
		
		folder = path.toAbsolutePath();
	}
	
	/**
	 * Gets the property where the part before the
	 * . is the name of the properties and following that
	 * is the key in the properties.
	 * <br><br>
	 * E.g. 
	 * <br><br>
	 * <code>
	 *   addProperty("user", true, "username", "password")<br>
	 *   .<br>
	 *   .<br>
	 *   .<br>
	 *   getProperty("user.username")
	 * </code>
	 * @param key the key
	 * @return the property
	 */
	public String getProperty(String key) {
		String[] spl = key.split("\\.");
		Properties props =  properties.get(spl[0]);
		
		if(props == null)
			return null;
		else
			return props.getProperty(spl[1]);
	}
	
	public List<String> getList(String name) {
		return lists.get(name);
	}
	
	public String getString(String name) {
		return strings.get(name);
	}
	
	/**
	 * Adds a property to the map where the file is in the current
	 * directory and the files name is name appended with .properties
	 * @param name the name of the file except for .properties
	 * @param required if an NPE should be thrown if the file doesn't exist
	 * @param requiredKeys the keys that must be in the properties file
	 * @throws NullPointerException if the file doesn't exist and it is required, or a key is missing from the properties
	 * @throws IOException if an i/o exception occurs
	 */
	public void addProperties(String name, boolean required, String...requiredKeys) throws NullPointerException, IOException {
		Path path = Paths.get(folder.toString(), name + ".properties");
		if(!Files.exists(path)) {
			if(required)
				throw new NullPointerException(path.toString() + " required but doesn't exist");
			else
				properties.put(name, new Properties());
		}else {
			properties.put(name, loadProperties(path, requiredKeys));
		}
	}
	
	/**
	 * Adds a list to the map where the file is in the current directory
	 * and the files name is name appended with .txt
	 * @param name the name of the file
	 * @param required if an NPE should be thrown if the file doesn't exist
	 * @throws IOException if an i/o exception occurs
	 */
	public void addList(String name, boolean required) throws IOException {
		Path path = Paths.get(folder.toString(), name + ".txt");
		if(!Files.exists(path)) {
			if(required)
				throw new NullPointerException(path.toString() + " required but doesn't exist");
			else
				lists.put(name, new ArrayList<String>());
		}else
			lists.put(name, loadStringList(path));
	}
	
	/**
	 * Adds a string to the map where the file is in the current directory
	 * and the files name is name appended with .txt
	 * @param name the name of the file
	 * @param required if an NPE should be thrown if the file doesn't exist
	 * @throws IOException if an i/o exception occurs
	 */
	public void addString(String name, boolean required) throws IOException {
		Path path = Paths.get(folder.toString(), name + ".txt");
		if(!Files.exists(path) && required) {
			throw new NullPointerException(path.toString() + " required but doesn't exist");
		}else
			strings.put(name, loadReplyString(path));
	}

	/**
	 * Loads properties from the specified file, as if by
	 * {@link java.util.Properties#load(java.io.Reader)}
	 * 
	 * @param path
	 *            the file to load from
	 * @param requiredKeys
	 *            the list of required keys
	 * @return the properties in the {@code file}
	 * @throws IOException
	 *             if an i/o exception occurs (like {@code file} not existing)
	 * @throws NullPointerException
	 *             if a required key is missing or the file is null
	 */
	protected Properties loadProperties(Path path, String... requiredKeys)
			throws IOException, NullPointerException {
		logger.debug("Loading properties from " + path.toString());
		Properties props = new Properties();
		try (FileReader fr = new FileReader(path.toFile())) {
			props.load(fr);
		}

		for (String reqKey : requiredKeys) {
			if (!props.containsKey(reqKey))
				throw new NullPointerException(path.toString()
						+ " is missing key " + reqKey);
		}
		return props;
	}

	/**
	 * Loads a reply string from a file, ignoring lines prefixed with a hash-tag
	 * (#). Any empty lines prior to the first non-empty line are ignored. Lines
	 * are separated Unix-like (only \n)
	 * 
	 * @param path
	 *            the file to load from
	 * @return the reply format
	 * @throws IOException
	 *             if an i/o exception occurs, like the file not existing
	 */
	protected String loadReplyString(Path path) throws IOException {
		logger.debug("Loading reply string from " + path.toString());
		String result = "";
		try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
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
	 * @param path
	 *            the file to load from
	 * @return each line in the file
	 * @throws IOException
	 *             if an i/o exception occurs, like the file not existing
	 */
	protected List<String> loadStringList(Path path) throws IOException {
		logger.debug("Loading string list from " + path.toString());
		List<String> result = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
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
	 * Get the map for strings -> strings
	 * @return the strings
	 */
	public Map<String, String> getStrings() {
		return strings;
	}
	
	/**
	 * Get the map for strings -> string lists
	 * @return the lists
	 */
	public Map<String, List<String>> getStringLists() {
		return lists;
	}
	
	/**
	 * Get the map of strings -> properties
	 * @return the properties
	 */
	public Map<String, Properties> getProperties() {
		return properties;
	}
}
