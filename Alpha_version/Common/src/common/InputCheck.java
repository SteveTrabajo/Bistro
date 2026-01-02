package common;

import java.util.regex.Pattern;

public class InputCheck {
	// Regex pattern for validating IPv4 addresses
	private static final String IPv4_REGEX = "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.|$)){4}$";
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
	 * @return An error message if there are validation issues, otherwise an empty
	 * string.
	 */
	public static String isValidPortAndIP(String ip, String port) {
		String errorMessage = "";
		if (ip.trim().isEmpty()) {
			errorMessage += "You must enter an IP Address\n";
		} else if (!isValidIPv4(ip)) {
			errorMessage += "Invalid IP Address format\n";
		}

		if (port.trim().isEmpty()) {
			errorMessage += "You must enter a port\n";
		} else if (!isValidPort(port)) {
			errorMessage += "Port must have only digits (between the values 0-65535)\n";
		}
		return errorMessage;
	}

	// Reusable helper for live filtering (client TextFormatter can call this)
	public static boolean isDigitsUpTo(String text, int maxLen) {
		if (text == null)
			return false;
		return text.matches("\\d{0," + maxLen + "}");
	}

	// Validation for member login: exactly 6 digits, cannot start with 0
	public static String validateMemberCode6DigitsNoLeadingZero(String code) {
		if (code == null)
			return "Member code is required.";
		String c = code.trim();

		if (!c.matches("^[1-9]\\d{5}$")) {
			return "Member code must be exactly 6 digits and cannot start with 0.";
		}
		return "";
	}

	// ==================== Name Validation Methods ====================
	
	/*
	 * Validates if the given first name contains only English letters.
	 * 
	 * @param firstName The first name to validate.
	 * 
	 * @return An error message if validation fails, otherwise an empty string.
	 */
	public static String validateFirstName(String firstName) {
		if (firstName == null || firstName.trim().isEmpty()) {
			return "First name is required.";
		}
		if (!firstName.matches("[a-zA-Z]+")) {
			return "Error: First name must contain only English letters";
		}
		return "";
	}

	/*
	 * Validates if the given last name contains only English letters.
	 * 
	 * @param lastName The last name to validate.
	 * 
	 * @return An error message if validation fails, otherwise an empty string.
	 */
	public static String validateLastName(String lastName) {
		if (lastName == null || lastName.trim().isEmpty()) {
			return "Last name is required.";
		}
		if (!lastName.matches("[a-zA-Z]+")) {
			return "Error: Last name must contain only English letters";
		}
		return "";
	}

	// ==================== Phone and Email Validation Methods ====================
	
	// Regex patterns for phone number and email validation
	private static final String PHONE_REGEX = "^\\d{10}$";
	private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
	private static final String EMAIL_REGEX = "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$";
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

	/*
	 * Validates if the given phone number contains exactly 10 digits.
	 * 
	 * @param phoneNumber The phone number to validate.
	 * 
	 * @return An error message if validation fails, otherwise an empty string.
	 */
	public static String validatePhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			return "Phone number is required.";
		}
		if (!phoneNumber.matches("\\d{10}")) {
			return "Error: Phone number must contain exactly 10 digits";
		}
		return "";
	}

	/*
	 * Validates if the given email contains the @ symbol.
	 * 
	 * @param email The email to validate.
	 * 
	 * @return An error message if validation fails, otherwise an empty string.
	 */
	public static String validateEmail(String email) {
		if (email == null || email.trim().isEmpty()) {
			return "Email is required.";
		}
		if (!email.contains("@")) {
			return "Error: Email must contain @";
		}
		return "";
	}

	/*
	 * Validates the guest's phone number and email address.
	 * 
	 * @param phoneNumber The phone number to validate.
	 * 
	 * @param emailAddress The email address to validate.
	 * 
	 * @return An error message if there are validation issues, otherwise an empty
	 * string.
	 */
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

	private InputCheck() {
		// Private constructor to prevent instantiation
	}
	
}
