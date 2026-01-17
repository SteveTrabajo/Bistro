package logic.services;


import java.util.List;
import java.util.Map;
import entities.User;
import enums.UserType;
import logic.BistroDataBase_Controller;
import logic.LoginAttemptTracker;
import logic.ServerLogger;
import common.InputCheck;
import dto.UserData;

/**
 * Service class for managing user-related operations such as registration,
 * login, and staff account creation.
 */
public class UserService {
	
	//************************* Instance Variables *************************
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	
	//************************* Constructor *************************
	public UserService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}
	
	//************************* Instance Methods *************************
	
	/**
	 * Registers a new member with the provided data.
	 * 
	 * @param newMemberData List of strings containing new member information
	 * [0]: firstName, [1]: lastName, [2]: email, [3]: phoneNumber, [4]: address
	 * @return true if registration was successful, false otherwise
	 */
	public int registerNewMember(List<String> newMemberData) {
		int memberCode= dbController.registerNewMember(newMemberData);
		if(memberCode != -1) {
			logger.log("[MEMBER_REGISTER] Successfully registered new member: " + newMemberData.get(0) + " " + newMemberData.get(1) + ", Member Code: " + memberCode);
			return memberCode;
		} else {
			logger.log("[MEMBER_REGISTER] Failed to register new member: " + newMemberData.get(0) + " " + newMemberData.get(1));
			return -1;
		}
	}
	
	/**
	 * Creates a new staff account (Employee or Manager) with the provided data.
	 * 
	 * @param staffData Map containing staff account information
	 * @return The created User object if successful, otherwise null
	 */
	public User createStaffAccount(Map<String, Object> staffData) {
		// Validate input data
		String username = (String) staffData.get("username");
		String password = (String) staffData.get("password");
		String email = (String) staffData.get("email");
		String phoneNumber = (String) staffData.get("phoneNumber");
		String userTypeStr = String.valueOf(staffData.get("userType"));
		String firstName = (String) staffData.get("firstName");
		String lastName  = (String) staffData.get("lastName");
		String address   = (String) staffData.get("address");

		
		// Validate all fields
		String validationError = InputCheck.validateAllStaffData(
		        username, password, email, phoneNumber,
		        firstName, lastName, address
		);
		if (validationError != null) {
		    logger.log("[STAFF_CREATE] Validation failed: " + validationError);
		    return null;
		}

		// Validate user type
		UserType userType;
		try {
			// Convert string to UserType enum and validate
			userType = UserType.valueOf(userTypeStr);
			if (userType != UserType.EMPLOYEE && userType != UserType.MANAGER) {
				logger.log("[STAFF_CREATE] Invalid user type: " + userTypeStr);
				return null;
			}
		} catch (IllegalArgumentException e) {
			// Invalid user type string
			logger.log("[STAFF_CREATE] Invalid user type: " + userTypeStr);
			return null;
		}
		// Check if username already exists
		if (dbController.employeeUsernameExists(username)) {
			logger.log("[STAFF_CREATE] Username already exists: " + username);
			return null;
		}
		// Create the employee account
		User newUser = dbController.createEmployeeUser(
		        username, password, email, phoneNumber, userType,
		        firstName, lastName, address
		);
		if (newUser != null) {
			// Log successful creation
			logger.log("[STAFF_CREATE] Successfully created new staff account: " + username);
		}
		return newUser;
	}
	
	/**
	 * Retrieves user information based on login data.
	 * @param loginData Map containing login credentials and user type
	 * @return The User object if found, otherwise null
	 */
	public User getUserInfo(Map<String, Object> loginData) {
		logger.log("[LOGIN] Received login data=" + loginData);
		User userfound = null;
		switch (String.valueOf(loginData.get("userType"))) {
		case "GUEST": {
			// Extract and sanitize phone number and email
		    String phone = (String) loginData.get("phoneNumber");
		    String email = (String) loginData.get("email");
		    // Trim whitespace and handle null/empty values
		    phone = (phone == null) ? null : phone.trim();
		    email = (email == null) ? null : email.trim();
		    if (phone != null && phone.isEmpty()) phone = null;
		    if (email != null && email.isEmpty()) email = null;
		    // Log the extracted values
		    logger.log("[LOGIN][GUEST] keys=" + loginData.keySet());
		    logger.log("[LOGIN][GUEST] phone=" + phone + " | email=" + email);
		    // Find or create guest user
		    userfound = dbController.findOrCreateGuestUser(phone, email);
		    break;
		}
		case "MEMBER": {
			// Extract and validate member code
			Object raw = loginData.get("memberCode");
			if (raw == null) {
				logger.log("[LOGIN] MEMBER missing key 'memberCode'. Keys=" + loginData.keySet());
				return null;
			}
			// Parse member code
			int memberCode;
			try {
				memberCode = (raw instanceof Integer) ? (Integer) raw : Integer.parseInt(raw.toString().trim());
			} catch (NumberFormatException ex) {
				logger.log("[LOGIN] MEMBER invalid memberCode value: " + raw);
				return null;
			}
			// Find member user by code
			userfound = dbController.findMemberUserByCode(memberCode);
			System.out.println("Member login attempt: " + (userfound != null ? "Found member" : "Member not found"));
			break;
		}
		case "EMPLOYEE", "MANAGER":
			// Extract username and password
			String username = String.valueOf(loginData.get("username"));
			String password = String.valueOf(loginData.get("password"));
			// Check if account is locked due to failed login attempts
			if (LoginAttemptTracker.isAccountLocked(username)) {
				logger.log("[LOGIN] Account locked due to too many failed attempts: " + username);
				return null; // Account is locked, return null to indicate failure
			}
			// Find employee user by username and password
			userfound = dbController.findEmployeeUser(username, password);
			break;
		default:
			logger.log("[ERROR] Unknown user type: " + String.valueOf(loginData.get("userType")));
			break;
		}
		return userfound;
	}
	
	/**
	 * Updates member information in the database.
	 * 
	 * @param updatedUser UserData object containing updated member information
	 * @return true if the update was successful, false otherwise
	 */
	public boolean updateMemberInfo(UserData updatedUser) {
		return dbController.setUpdatedMemberData(updatedUser);	
	}
	
	/**
	 * Checks if a staff username already exists in the database.
	 * 
	 * @param username The staff username to check
	 * @return true if the username exists, false otherwise
	 */
	public boolean staffUsernameExists(String username) {
	    return dbController.employeeUsernameExists(username);
	}
	
	/**
	 * Finds a staff user by username and password.
	 * 
	 * @param username The staff username
	 * @param password The staff password
	 * @return The User object if found, otherwise null
	 */
	public User findStaffUser(String username, String password) {
	    return dbController.findEmployeeUser(username, password);
	}

	/**
	 * Retrieves a list of all customers (members and guests) from the database.
	 * 
	 * @return List of UserData objects representing all customers
	 */
	public List<UserData> getAllCustomers() {
		return dbController.getAllCustomers();
	}

	/**
	 * Finds a member code by email or phone number.
	 * 
	 * @param email       Member email address (can be null/empty if phone provided)
	 * @param phoneNumber Member phone number (can be null/empty if email provided)
	 * @return Member code as a String if found, otherwise null
	 */
	public String findMemberCode(String email, String phoneNumber) {
		int memberCode = dbController.findMemberCodeByEmailOrPhone(email, phoneNumber);
		if (memberCode > 0) {
			return String.valueOf(memberCode);
		}
		return null;
	}
	
	
	/**
	 * Looks up a staff member (Employee or Manager) by email or phone number.
	 * Returns their credentials if found.
	 * @param email       Staff email address (can be null/empty if phone provided)
	 * @param phoneNumber Staff phone number (can be null/empty if email provided)
	 * @return "username:password" if found, "NOT_FOUND" if not found, or "ERROR_DB" if error.
	 */
	public String recoverStaffLogin(String email, String phoneNumber) {
		// Calls the lower-level database controller to execute the SQL query.
		return dbController.recoverStaffLogin(email, phoneNumber);
	}
	
}
// End of UserService.java