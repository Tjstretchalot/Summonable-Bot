package me.timothy.bots.responses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.timothy.bots.Database;
import me.timothy.bots.FileConfiguration;

/**
 * Formats responses using a response info!
 * 
 * @author Timothy
 */
public class ResponseFormatter {
	private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("<[^>]*>");
	private String format;
	private ResponseInfo info;
	
	/**
	 * Prepares a response formatter with the specified
	 * information to use
	 * @param format the format
	 * @param info the info
	 */
	public ResponseFormatter(String format, ResponseInfo info) {
		this.format = format;
		this.info = info;
	}
	
	/**
	 * Parses the current state of info and 
	 * gets the nice pretty response
	 * 
	 * @param config the current config
	 * @param db the db
	 * @return the response
	 */
	public String getFormattedResponse(FileConfiguration config, Database db) {
		Matcher matcher = REPLACEMENT_PATTERN.matcher(format);
		
		StringBuilder response = new StringBuilder();
		int indexThatResponseIsUpToInFormat = 0;
		while(matcher.find()) {
			int startIndexInFormatOfThisGroup = matcher.start();
			
			String whatToReplace = matcher.group();
			String keyToReplace = whatToReplace.substring(1, whatToReplace.length() - 1);
			FormattableObject formattableObject = info.getObject(keyToReplace);
			if(formattableObject == null) {
				throw new NullPointerException("Unknown key for formatted response " + keyToReplace + ", valid keys are from " + info);
			}
			String whatToReplaceWith = formattableObject.toFormattedString(info, keyToReplace, config, db);
			
			String theInbetweenText = format.substring(indexThatResponseIsUpToInFormat, startIndexInFormatOfThisGroup);
			
			response.append(theInbetweenText).append(whatToReplaceWith);
			indexThatResponseIsUpToInFormat = matcher.end();
		}
		response.append(format.substring(indexThatResponseIsUpToInFormat));
		return response.toString();
	}
	
	/**
	 * Looks at the given format and verifies that it matches the 
	 * @param format
	 * @param expectedKeys
	 * @return
	 */
	public static void verifyFormat(String format, String errorPrefix, ExpectedKey... expectedKeys) {
		HashSet<String> keys = new HashSet<>();
		for(ExpectedKey key : expectedKeys) {
			keys.add(key.key);
		}
		
		Matcher matcher = REPLACEMENT_PATTERN.matcher(format);
		
		List<String> additionalKeys = new ArrayList<>();
		while(matcher.find()) {
			String whatToReplace = matcher.group();
			String keyToReplace = whatToReplace.substring(1, whatToReplace.length() - 1);
			if(!keys.contains(keyToReplace)) {
				additionalKeys.add(keyToReplace);
			}
		}
		
		if(additionalKeys.size() == 0)
			return;
		
		StringBuilder errorFormatter = new StringBuilder(errorPrefix);
		errorFormatter.append("Found invalid replacements: ");
		for(int i = 0; i < additionalKeys.size(); i++) {
			if(i != 0) {
				errorFormatter.append(", ");
			}
			errorFormatter.append(additionalKeys.get(i));
		}
		
		errorFormatter.append("; valid keys are: \n");
		for(ExpectedKey key : expectedKeys) {
			errorFormatter.append("  '").append(key.key).append("': ").append(key.description).append("\n");
		}
		
		throw new AssertionError(errorFormatter.toString());
	}
	


	/**
	 * This is a simple tuple of key/value pairs. This is only used for verifying the format
	 * if verifyFormat is called, for better error messages and generally more robust 
	 * responses.
	 * 
	 * @author Timothy
	 */
	public static class ExpectedKey {
		/** The key that we know that we will be able to handle */
		public String key;
		
		/** The description for the key */
		public String description;
		
		/**
		 * Create a new expected key with the given key and description
		 * @param key the key that you know you will have info for
		 * @param description the description of this key, used for error messages
		 */
		public ExpectedKey(String key, String description) {
			if(key.contains("<"))
				throw new IllegalArgumentException("Illegal character in key '" + key + "': <");
			if(key.contains(">"))
				throw new IllegalArgumentException("Illegal character in key '" + key + "': >");
			
			this.key = key;
			this.description = description;
		}
	}
}
