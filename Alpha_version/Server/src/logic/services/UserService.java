package logic.services;

import java.util.Map;

import entities.User;
import enums.UserType;
import logic.BistroDataBase_Controller;
import logic.LoginAttemptTracker;
import logic.PasswordUtil;
import logic.ServerLogger;
import common.InputCheck;
import dto.UserData;

public class UserService {
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	
	public UserService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}

	public User getUserInfo(Map<String, Object> loginData) {
		logger.log("[LOGIN] Received login data=" + loginData);
		User userfound = null;
		switch (String.valueOf(loginData.get("userType"))) {
		case "GUEST":
			userfound = dbController.findOrCreateGuestUser((String) loginData.get("phoneNumber"),
					(String) loginData.get("email"));
			break;
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
	 * Creates a new staff account with password hashing and validation.
	 * 
	 * @param staffData Map containing: username, password, email, phoneNumber, userType
	 * @return The created User object, or null if creation failed
	 */
	public User createStaffAccount(Map<String, Object> staffData) {
		// Validate input data
		String username = (String) staffData.get("username");
		String password = (String) staffData.get("password");
		String email = (String) staffData.get("email");
		String phoneNumber = (String) staffData.get("phoneNumber");
		String userTypeStr = String.valueOf(staffData.get("userType"));

		
		// Validate all fields
		String validationError = InputCheck.validateAllStaffData(username, password, email, phoneNumber);
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
		User newUser = dbController.createEmployeeUser(username, password, email, phoneNumber, userType);
		if (newUser != null) {
			logger.log("[STAFF_CREATE] Successfully created new staff account: " + username);
		}
		return newUser;
	}

	public boolean updateMemberInfo(UserData updatedUser) {
		return dbController.setUpdatedMemberData(updatedUser);	
	}
	
	public boolean staffUsernameExists(String username) {
	    return dbController.employeeUsernameExists(username);
	}
	
	public User findStaffUser(String username, String password) {
	    return dbController.findEmployeeUser(username, password);
	}
}

