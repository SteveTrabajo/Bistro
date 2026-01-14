package common;

import java.util.regex.Pattern;

public class InputCheck {
	// Regex pattern for validating IPv4 addresses
	private static final String IPv4_REGEX = "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.|$)){4}$";
	private static final Pattern IPv4_PATTERN = Pattern.compile(IPv4_REGEX);
	
	//

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

	public static String validateAddress(String address) {
		if (address == null || address.trim().isEmpty()) {
			return "Address is required.";
		}
		if (!address.matches("[a-zA-Z0-9\\s,.-]+")) {
			return "Error: Address contains invalid characters";
		}
		return "";
	}
	
	public static String validatePersonalDetails(String firstName, String lastName, String phoneNumber, String email, String address) {
		StringBuilder errorMsg = new StringBuilder();
		String temp;
		
		temp = validateFirstName(firstName);
		if(!temp.isEmpty()) errorMsg.append("• ").append(temp).append("\n");
		
		temp = validateLastName(lastName);
		if(!temp.isEmpty()) errorMsg.append("• ").append(temp).append("\n");
		
		temp = validatePhoneNumber(phoneNumber);
		if(!temp.isEmpty()) errorMsg.append("• ").append(temp).append("\n");
		
		temp = validateEmail(email);
		if(!temp.isEmpty()) errorMsg.append("• ").append(temp).append("\n");
		
		temp = validateAddress(address);
		if(!temp.isEmpty()) errorMsg.append("• ").append(temp).append("\n");
		
		return errorMsg.toString().trim();
	}
	
	// ==================== User Validation Methods ====================
	
	// Regex patterns for phone number and email validation
	private static final String PHONE_REGEX = "^(0|\\+972)5\\d{8}$";
	private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
	private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
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

		if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
			return "Invalid phone number. Must be 05XXXXXXXX or +9725XXXXXXXX";
		}
		
		return "";
	}

	/*
	 * Validates if the given email matches the strict format (user@domain.txt).
	 * * @param email The email to validate.
	 * * @return An error message if validation fails, otherwise an empty string.
	 */
	public static String validateEmail(String email) {
		if (email == null || email.trim().isEmpty()) {
			return "Email is required.";
		}
		
		// Updated to use the strict Pattern instead of just contains("@")
		if (!EMAIL_PATTERN.matcher(email).matches()) {
			return "Error: Invalid email format (example: user@domain.com)";
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

	    phoneNumber = (phoneNumber == null) ? "" : phoneNumber.trim();
	    emailAddress = (emailAddress == null) ? "" : emailAddress.trim();

	    if (phoneNumber.isEmpty() && emailAddress.isEmpty()) {
	        errorMessage += "You must enter a phone number, an email address or both\n";
	    } else {
	        if (!phoneNumber.isEmpty() && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
	            errorMessage += "Invalid Israeli phone format (05XXXXXXXX or +9725XXXXXXXX)\n";
	        }
	        if (!emailAddress.isEmpty() && !EMAIL_PATTERN.matcher(emailAddress).matches()) {
	            errorMessage += "Invalid email address format\n";
	        }
	    }
	    return errorMessage;
	}

	
	
	// Regex pattern for validating usernames
	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,20}$");
	private static final int MIN_PASSWORD_LENGTH = 4;
	
	/**
	 * Validates a username (3-20 chars, alphanumeric + dot, underscore, hyphen)
	 */
	public static String validateUsername(String username) {
		if (username == null || username.trim().isEmpty()) {
			return "Username cannot be empty";
		}
		
		username = username.trim();
		if (!USERNAME_PATTERN.matcher(username).matches()) {
			return "Username: 3-20 chars, letters/numbers/.-_ only";
		}
		return null;
	}
	
	/**
	 * Validates a password (minimum 4 characters)
	 */
	public static String validatePassword(String password) {
		if (password == null || password.isEmpty()) {
			return "Password cannot be empty";
		}
		
		if (password.length() < MIN_PASSWORD_LENGTH) {
			return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
		}
		
		return null;
	}
	
	/**
	 * Validates all staff account data at once
	 */
	public static String validateAllStaffData(String username, String password, String email, String phoneNumber) {
		String usernameError = InputCheck.validateUsername(username);
		if (usernameError != null) return usernameError;
		
		String passwordError = validatePassword(password);
		if (passwordError != null) return passwordError;
		
		String emailError = validateEmail(email);
		if (emailError != null) return emailError;
		
		String phoneError = validatePhoneNumber(phoneNumber);
		if (phoneError != null) return phoneError;
		
		return null;
	}

	private InputCheck() {
		// Private constructor to prevent instantiation
	}

	public static String validateWalkIn(boolean isMember, String memberCode, String phone, String email) {
        if (isMember) {
            return validateMemberCode6DigitsNoLeadingZero(memberCode);
        } else {
            return isValidGuestInfo(phone, email);
        }
    }
	
	public static String checkConfirmationCode(String code) {
		if (code == null || code.trim().isEmpty()) {
			return "Confirmation code is required.";
		}
		String c = code.trim();
		// Format: R-123456 or W-123456
		// regex breakdown: ^ = start, [RW]- = R- or W-, \d{6} = exactly 6 digits, $ = end
		if (!c.matches("^[RW]-\\d{6}$")) {
	        return "Confirmation code must start with 'R-' or 'W-' followed by 6 digits.";
	    }
	    return null;
	}
	
}
