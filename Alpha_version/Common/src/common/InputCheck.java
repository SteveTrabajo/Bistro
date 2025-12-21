package common;

import java.util.regex.Pattern;

//import javafx.scene.paint.Color;

public class InputCheck {
	
	private static final String IPv4_REGEX = 
	        "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.|$)){4}$";
	private static final Pattern IPv4_PATTERN = Pattern.compile(IPv4_REGEX);
	
	/*
	 * Validates if the given port string represents a valid port number (0-65535).
	 * 
	 * @param portStr The port string to validate.
	 * 
	 * @return true if the port is valid, false otherwise.
	 */
	public static boolean isValidPort(String portStr) {
		try {
			int port = Integer.parseInt(portStr);
			return port >= 0 && port <= 65535;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/*
	 * Validates if the given IP address string is a valid IPv4 address.
	 * 
	 * @param ip The IP address string to validate.
	 * 
	 * @return true if the IP address is valid, false otherwise.
	 */
	public static boolean isValidIPv4(String ip) {
	    return IPv4_PATTERN.matcher(ip).matches();
	}
	
	/*
	 * Checks if the provided IP address and port are valid.
	 * 
	 * @param ip The IP address to validate.
	 * 
	 * @param port The port to validate.
	 * 
	 * @return An error message if there are validation issues, otherwise an empty string.
	 */
	public static String isValidPortAndIP(String ip, String port) {
		String errorMessage = "";
		if (ip.trim().isEmpty()) {
			errorMessage += "You must enter an IP Address\n";
		}
		else if (!isValidIPv4(ip)) {
			errorMessage += "Invalid IP Address format\n";
		}
		
		if (port.trim().isEmpty()) {
			errorMessage += "You must enter a port\n";
		}
		else if (!isValidPort(port)) {
			errorMessage += "Port must have only digits (between the values 0-65535)\n";
		}
		return errorMessage;
	}
	
	
	private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,15}$";
	private static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);
	/*
	 * Validates if the given username meets the specified criteria.
	 * 
	 * @param username The username to validate.
	 * 
	 * @return An error message if there are validation issues, otherwise an empty string.
	 */
	public static String isValidUsername(String username) {
		String errorMessage = "";
		if (username.trim().isEmpty()) {
			errorMessage += "You must enter a username\n";
		}
		else if (username.length() < 4 || username.length() > 15) {
			errorMessage += "Username must be between 3 and 15 characters long\n";
		}
		else if (!username.matches(USERNAME_REGEX)) {
			errorMessage += "Username can only contain letters, digits, and underscores\n";
		}
		return errorMessage;
	}
	
	private static final String PHONE_REGEX = "^\\d{10}$ or ^[0-9]{10}$";
	private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
	private static final String EMAIL_REGEX = "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$";
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
	
	public static String isValidGuestInfo(String phoneNumber, String emailAddress) {
		String errorMessage = "";
		if ((phoneNumber.trim().isEmpty()) && (emailAddress.trim().isEmpty())) {
			errorMessage += "You must enter a phone number, an email address or both\n";
		} else {
			if (!phoneNumber.trim().isEmpty() && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
				errorMessage += "Phone number must be exactly 10 digits\n";
			}
			if (!emailAddress.trim().isEmpty() && !EMAIL_PATTERN.matcher(emailAddress).matches()) {
				errorMessage += "Invalid email address format\n";
			}
		}
		return errorMessage;
	}
	
}
