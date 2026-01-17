package logic.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import entities.User;
import enums.UserType;
import logic.BistroDataBase_Controller;
import logic.LoginAttemptTracker;
import logic.PasswordUtil;
import logic.ServerLogger;
import common.InputCheck;
import dto.UserData;

/**
 * Handles user-related operations including login, registration, and account management.
 * Supports different user types: guests, members, employees, and managers.
 * Includes password hashing for staff accounts and login attempt tracking for security.
 */
public class UserService {
	
	//*************************Instance Variables*************************
	
	/** Database controller for all DB operations */
	private final BistroDataBase_Controller dbController;
	
	/** Logger for tracking service activity */
	private final ServerLogger logger;
	
	//************************* Constructor *************************
	
	/**
	 * Creates a new UserService with required dependencies.
	 * 
	 * @param dbController database controller for DB access
	 * @param logger server logger for logging events
	 */
	public UserService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}
	
	//************************* Instance Methods *************************
	
	/**
	 * Registers a new member in the system.
	 * Generates a unique 6-digit member code for login.
	 * 
	 * @param newMemberData list containing: firstName, lastName, email, phoneNumber, address
	 * @return the generated member code, or -1 if registration failed
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
	 * Creates a new staff account (employee or manager).
	 * Validates all input fields and checks for duplicate usernames.
	 * Password is hashed before storage.
	 * 
	 * @param staffData map containing: username, password, email, phoneNumber, userType, firstName, lastName, address
	 * @return the created User object, or null if creation failed
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
			userType = UserType.valueOf(userTypeStr);
			if (userType != UserType.EMPLOYEE && userType != UserType.MANAGER) {
				logger.log("[STAFF_CREATE] Invalid user type: " + userTypeStr);
				return null;
			}
		} catch (IllegalArgumentException e) {
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
			logger.log("[STAFF_CREATE] Successfully created new staff account: " + username);
		}
		return newUser;
	}
	
	/**
	 * Handles login for all user types (guest, member, employee, manager).
	 * For staff, checks if account is locked due to failed attempts.
	 * For guests, creates a new user record if one doesn't exist.
	 * 
	 * @param loginData map containing credentials based on user type
	 * @return the User object if login successful, null otherwise
	 */
	public User getUserInfo(Map<String, Object> loginData) {
		logger.log("[LOGIN] Received login data=" + loginData);
		User userfound = null;
		switch (String.valueOf(loginData.get("userType"))) {
		case "GUEST": {
		    String phone = (String) loginData.get("phoneNumber");
		    String email = (String) loginData.get("email");

		    phone = (phone == null) ? null : phone.trim();
		    email = (email == null) ? null : email.trim();
		    if (phone != null && phone.isEmpty()) phone = null;
		    if (email != null && email.isEmpty()) email = null;

		    logger.log("[LOGIN][GUEST] keys=" + loginData.keySet());
		    logger.log("[LOGIN][GUEST] phone=" + phone + " | email=" + email);

		    userfound = dbController.findOrCreateGuestUser(phone, email);
		    break;
		}
		case "MEMBER": {
			Object raw = loginData.get("memberCode");
			if (raw == null) {
				logger.log("[LOGIN] MEMBER missing key 'memberCode'. Keys=" + loginData.keySet());
				return null;
			}
			int memberCode;
			try {
				memberCode = (raw instanceof Integer) ? (Integer) raw : Integer.parseInt(raw.toString().trim());
			} catch (NumberFormatException ex) {
				logger.log("[LOGIN] MEMBER invalid memberCode value: " + raw);
				return null;
			}
			userfound = dbController.findMemberUserByCode(memberCode);
			System.out.println("Member login attempt: " + (userfound != null ? "Found member" : "Member not found"));
			break;
		}
		case "EMPLOYEE", "MANAGER":
			String username = String.valueOf(loginData.get("username"));
			String password = String.valueOf(loginData.get("password"));
			// Check if account is locked due to failed login attempts
			if (LoginAttemptTracker.isAccountLocked(username)) {
				logger.log("[LOGIN] Account locked due to too many failed attempts: " + username);
				return null; // Account is locked, return null to indicate failure
			}
			userfound = dbController.findEmployeeUser(username, password);
			break;
		default:
			logger.log("[ERROR] Unknown user type: " + String.valueOf(loginData.get("userType")));
			break;
		}
		return userfound;
	}
	
	/**
	 * Updates a member's profile information.
	 * 
	 * @param updatedUser UserData object with updated info
	 * @return true if update was successful
	 */
	public boolean updateMemberInfo(UserData updatedUser) {
		return dbController.setUpdatedMemberData(updatedUser);	
	}
	
	/**
	 * Checks if a staff username is already taken.
	 * 
	 * @param username the username to check
	 * @return true if username exists
	 */
	public boolean staffUsernameExists(String username) {
	    return dbController.employeeUsernameExists(username);
	}
	
	/**
	 * Authenticates a staff user by username and password.
	 * 
	 * @param username the staff username
	 * @param password the staff password (will be hashed for comparison)
	 * @return the User object if found, null otherwise
	 */
	public User findStaffUser(String username, String password) {
	    return dbController.findEmployeeUser(username, password);
	}

	/**
	 * Gets all customers (members and guests) in the system.
	 * 
	 * @return list of customer UserData objects
	 */
	public List<UserData> getAllCustomers() {
		return dbController.getAllCustomers();
	}

	/**
	 * Looks up a member's code by their email or phone number.
	 * Useful for "forgot member code" functionality.
	 * 
	 * @param email member's email address
	 * @param phoneNumber member's phone number
	 * @return the member code as a string, or null if not found
	 */
	public String findMemberCode(String email, String phoneNumber) {
		int memberCode = dbController.findMemberCodeByEmailOrPhone(email, phoneNumber);
		if (memberCode > 0) {
			return String.valueOf(memberCode);
		}
		return null;
	}
}
// End of UserService.java