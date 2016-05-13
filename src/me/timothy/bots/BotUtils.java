package me.timothy.bots;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: Auto-generated Javadoc
/**
 * Supports some common formatting.
 *
 * @author Timothy
 */
public class BotUtils {
	
	/** The logger. */
	@SuppressWarnings("unused")
	private static Logger logger = LogManager.getLogger();
	
	/** The date formatter. */
	private static DateFormat dateFormatter;


	 
	/**
	 * Get the string to use to search for a dollar amount.
	 * 
	 * @return a string appropriate for searching for a dollar amount. Does
	 * not contain any groups.
	 */
	public static String getDollarAmountPatternString() {
		return "\\$?[\\d,]+\\.?\\d{0,2}\\$?(?: |$)";
	}
	
	/**
	 * Parses the group returned from {@link BotUtils#getDollarAmountPatternString()}
	 * 
	 * @param string the group
	 * @return the value in pennies
	 * @throws NumberFormatException if the quantity is not a valid dollar amount
	 */
	public static int parseDollarAmount(String string) throws NumberFormatException {
		string = string.replace(",", "");
		string = string.replace("$", "");
		
		boolean hasDecimal = string.indexOf(".") > -1;
		
		String wholePart = hasDecimal ? string.substring(0, string.indexOf(".")) : string; 
		String decimalPart = hasDecimal ? string.substring(string.indexOf(".") + 1) : "00";
		
		if(decimalPart.length() < 2) {
			decimalPart += "0";
		}else if(decimalPart.length() > 2) {
			throw new NumberFormatException(string + " has too much precision after the period");
		}
		
		return Integer.valueOf(wholePart) * 100 + Integer.valueOf(decimalPart);
		
	}
	
	/**
	 * Formats the specified number like you would expect for a dollar amount.
	 * Does not add a dollar sign
	 * 
	 * @param d
	 *            the amount in dollars
	 * @return a formated string
	 */
	public static String getCostString(double d) {
		DecimalFormat result = new DecimalFormat("0.00");

		return result.format(d);
	}

	/**
	 * Gets a date string from a timestamp that was acquired as if by
	 * {@code System.currentTimeMillis}
	 * 
	 * @param dateLoanGivenJUTC
	 *            when the loan was given
	 * @return a human-readable version
	 */
	public static String getDateStringFromJUTC(long dateLoanGivenJUTC) {
		if (dateFormatter == null) {
			dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
		}
		return dateFormatter.format(new Date(dateLoanGivenJUTC));
	}

	/**
	 * From a string that contains a username, strips (if it exists) the /u/ and
	 * sets to lowercase. This ensures that if you add it to a database it will
	 * be consistent.
	 * 
	 * @param str
	 *            the username string to parse
	 * @return a normalized username in lowercase not prefixed with /u/
	 */
	public static String getUser(String str) {
		return (str.startsWith("/u/") ? str.substring(3) : str).toLowerCase();
	}

	/**
	 * Parses the number as a double, multiplies by a hundred, and rounds to an
	 * int. Instead of a number format exception this throws a parse exception
	 * to ensure this scenario is handled appropriately
	 * 
	 * @param number
	 *            the number string to parse
	 * @return the number multiplied by a hundred
	 * @throws ParseException
	 *             if it's not a valid number
	 */
	public static int getPennies(String number) throws ParseException {
		double amountDollars;
		try {
			amountDollars = Double.valueOf(number);
		} catch (NumberFormatException ex) {
			throw new ParseException(ex.getMessage(), 0);
		}
		return (int) Math.round(amountDollars * 100);
	}

}
