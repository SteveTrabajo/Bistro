package logic.services;

import java.util.Map;

import entities.User;
import enums.UserType;
import logic.BistroDataBase_Controller;
import logic.LoginAttemptTracker;
import logic.PasswordUtil;
import logic.ServerLogger;
import common.InputCheck;

public class UserService {
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	
	public UserService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}

	public User getUserInfo(String loginData) {
		logger.log("[LOGIN] Received login data = " + loginData);

		if (loginData == null || loginData.isBlank()) {
			logger.log("[LOGIN] Empty login data");
			return null;
		}

		String[] parts = loginData.split("\\|");
		String userType = parts[0];

		User userFound = null;

		switch (userType) {

		case "GUEST": {
			parts = loginData.split("\\|", -1);

			if (parts.length < 3) {
				logger.log("[LOGIN] GUEST invalid format: " + loginData);
				return null;
			}

			String phone = parts[1].trim();
			String email = parts[2].trim();

			if (phone.isEmpty())
				phone = null;
			if (email.isEmpty())
				email = null;

			if (phone == null && email == null) {
				logger.log("[LOGIN] GUEST must provide phone or email");
				return null;
			}

			return dbController.findOrCreateGuestUser(phone, email);
		}

		case "MEMBER": {
			if (parts.length < 2) {
				logger.log("[LOGIN] MEMBER invalid format");
				return null;
			}
			try {
				int memberCode = Integer.parseInt(parts[1]);
				userFound = dbController.findMemberUserByCode(memberCode);
			} catch (NumberFormatException e) {
				logger.log("[LOGIN] MEMBER invalid memberCode: " + parts[1]);
				return null;
			}
			break;
		}

		case "EMPLOYEE":
		case "MANAGER": {
			if (parts.length < 3) {
				logger.log("[LOGIN] STAFF invalid format");
				return null;
			}
			String username = parts[1];
			String password = parts[2];

			if (LoginAttemptTracker.isAccountLocked(username)) {
				logger.log("[LOGIN] Account locked: " + username);
				return null;
			}

			userFound = dbController.findEmployeeUser(username, password);
			break;
		}

		default:
			logger.log("[LOGIN] Unknown user type: " + userType);
			return null;
		}

		return userFound;
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
		String userTypeStr = (String) staffData.get("userType");
		
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

	public boolean updateMemberInfo(User updatedUser) {
			
		return dbController.setUpdatedMemberData(updatedUser);	
	}
}

