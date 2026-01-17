package logic.api.subjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.InputCheck;
import comms.Api;
import comms.Message;
import entities.User;
import enums.UserType;
import logic.LoginAttemptTracker;
import logic.ServerLogger;
import logic.api.ServerRouter;
import logic.services.UserService;
import dto.UserData;
/**
 * ServerUserSubject handles user-related requests such as login, signout, member info update, and staff creation.
 */
public final class ServerUserSubject {
	
	// Private constructor to prevent instantiation
	private ServerUserSubject() {
	}
	
	/**
	 * Registers user-related request handlers to the provided router.
	 *
	 * @param router      The router to register handlers with.
	 * @param userService The user service for user operations.
	 * @param logger      The server logger for logging events.
	 */
	public static void register(ServerRouter router, UserService userService, ServerLogger logger) {

		// Request: "login.guest"
		router.on("login", "guest", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> loginData = (Map<String, Object>) msg.getData();
			User user = userService.getUserInfo(loginData);
			if (user != null) {
				logger.log("[LOGIN] GUEST login successful for username: " + user.getUsername());
				client.setInfo("user", user); // Store session user for authorization
				client.sendToClient(new Message(Api.REPLY_LOGIN_GUEST_OK, user));
			} else {
				logger.log("[Warn] GUEST login failed for username: " + String.valueOf(loginData.get("username")));
				client.sendToClient(new Message(Api.REPLY_LOGIN_GUEST_NOT_FOUND, null));
			}
		});

		// Request: "login.member"
		router.on("login", "member", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> loginData = (Map<String, Object>) msg.getData();
			User user = userService.getUserInfo(loginData);
			if (user != null) {
				logger.log("[LOGIN] MEMBER login successful for username: " + user.getUsername());
				client.setInfo("user", user); // Store session user for authorization
				client.sendToClient(new Message(Api.REPLY_LOGIN_MEMBER_OK, user));
			} else {
				logger.log("[Warn] MEMBER login failed for username: " + String.valueOf(loginData.get("username")));
				client.sendToClient(new Message(Api.REPLY_LOGIN_MEMBER_NOT_FOUND, null));
			}
		});

		// Request: "login.staff" (unified EMPLOYEE/MANAGER login)
		router.on("login", "staff", (msg, client) -> {
		    @SuppressWarnings("unchecked")
		    Map<String, Object> loginData = (Map<String, Object>) msg.getData();
		    String username = String.valueOf(loginData.get("username"));
		    // Check if account is locked
		    if (LoginAttemptTracker.isAccountLocked(username)) {
		        logger.log("[LOGIN] Account locked for STAFF: " + username);
		        client.sendToClient(new Message(Api.REPLY_LOGIN_STAFF_ACCOUNT_LOCKED, null));
		        return;
		    }

		    // Force staff lookup. DB returns the correct role (EMPLOYEE or MANAGER).
		    Map<String, Object> staffLoginData = new HashMap<>(loginData);
		    staffLoginData.put("userType", UserType.EMPLOYEE); // userService routes staff lookup for EMPLOYEE/MANAGER
		    User user = userService.getUserInfo(staffLoginData);
		    if (user != null && (user.getUserType() == UserType.EMPLOYEE || user.getUserType() == UserType.MANAGER)) {
		    	logger.log("[LOGIN] STAFF login successful for username: " + user.getUsername() + " as " + user.getUserType());
		        // Store session user for authorization (e.g., staff.create)
		        client.setInfo("user", user);
		        client.sendToClient(new Message(Api.REPLY_LOGIN_STAFF_OK, user));
		    } else {
		    	logger.log("[Warn] STAFF login failed for username: " + username);
		        client.sendToClient(new Message(Api.REPLY_LOGIN_STAFF_INVALID_CREDENTIALS, null));
		    }
		});

		// Request: "signout.guest"
		router.on("signout", "guest", (msg, client) -> {
			client.setInfo("user", null); // Clear session user
			logger.log("[INFO] Client " + client + " signed out as GUEST");
			client.sendToClient(new Message(Api.REPLY_SIGNOUT_GUEST_OK, null));
		});

		// Request: "signout.member"
		router.on("signout", "member", (msg, client) -> {
			client.setInfo("user", null); // Clear session user
			logger.log("[INFO] Client " + client + " signed out as MEMBER");
			client.sendToClient(new Message(Api.REPLY_SIGNOUT_MEMBER_OK, null));
		});

		// Request: "signout.employee"
		router.on("signout", "employee", (msg, client) -> {
			client.setInfo("user", null); // Clear session user
			logger.log("[INFO] Client " + client + " signed out as EMPLOYEE");
			client.sendToClient(new Message(Api.REPLY_SIGNOUT_EMPLOYEE_OK, null));
		});

		// Request: "signout.manager"
		router.on("signout", "manager", (msg, client) -> {
			client.setInfo("user", null); // Clear session user
			logger.log("[INFO] Client " + client + " signed out as MANAGER");
			client.sendToClient(new Message(Api.REPLY_SIGNOUT_MANAGER_OK, null));
		});

		// Request: "member.updateInfo"
		router.on("member", "updateInfo", (msg, client) -> {
			if (client.getInfo("user") == null) {
				client.sendToClient(new Message(Api.REPLY_MEMBER_UPDATE_INFO_FAILED, null));
				return;
			}
			UserData updatedUser = (UserData) msg.getData();
			boolean success = userService.updateMemberInfo(updatedUser);
			if (success) {
				logger.log("[INFO] Client " + client + " updated member info: successful");
				client.sendToClient(new Message(Api.REPLY_MEMBER_UPDATE_INFO_OK, updatedUser));
			} else {
				logger.log("[ERROR] Client " + client + " updated member info: failed");
				client.sendToClient(new Message(Api.REPLY_MEMBER_UPDATE_INFO_FAILED, null));
			}
		});
		
		router.on("user", "registerNewMember", (msg,client) -> {
		    @SuppressWarnings("unchecked")
		    List<String> newMemberData = (ArrayList<String>) msg.getData();

		    int code = userService.registerNewMember(newMemberData);

		    if (code > 0) {
		        logger.log("[INFO] New member registered successfully.");
		        client.sendToClient(new Message(Api.REPLY_REGISTER_NEW_MEMBER_OK, code));
		        return;
		    }

		    String reason;
		    switch (code) {
		        case -2:
		            reason = "This email/phone is already registered as a MEMBER.";
		            break;
		        case -3:
		            reason = "This email/phone belongs to a STAFF account (employee/manager).";
		            break;
		        case -4:
		            reason = "Phone and email exist but belong to different users (conflict).";
		            break;
		        default:
		            reason = "Member registration failed due to a server/database error.";
		            break;
		    }

		    logger.log("[ERROR] New member registration failed: " + reason);
		    client.sendToClient(new Message(Api.REPLY_REGISTER_NEW_MEMBER_FAILED, reason));
		});


		router.on("user", "forgotMemberID", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, String> requestData = (Map<String, String>) msg.getData();
			String email = requestData.get("email");
			String phoneNumber = requestData.get("phoneNumber");
			String memberID = userService.findMemberCode(email, phoneNumber);
			if (memberID != null) {
				logger.log("[INFO] Retrieved member ID for email: " + email);
				client.sendToClient(new Message(Api.REPLY_FORGOT_MEMBER_ID_OK, memberID));
			} else {
				logger.log("[WARN] Failed to retrieve member ID for email: " + email);
				client.sendToClient(new Message(Api.REPLY_FORGOT_MEMBER_ID_FAILED, null));
			}
		});
		
		router.on("staff", "recoverPassword", (msg, client) -> {
		    @SuppressWarnings("unchecked")
		    Map<String, String> requestData = (Map<String, String>) msg.getData();
		    
		    String email = requestData.get("email");
		    String phoneNumber = requestData.get("phoneNumber");
		    
		    String result = userService.recoverStaffLogin(email, phoneNumber);
		    
		    if (result != null && !result.equals("NOT_FOUND") && !result.equals("ERROR_DB")) {
		        logger.log("[INFO] Staff credentials recovered for: " + email);
		        client.sendToClient(new Message(Api.REPLY_RECOVER_STAFF_PASSWORD_OK, result));
		    } else {
		        logger.log("[WARN] Staff recovery failed (not found) for: " + email);
		        client.sendToClient(new Message(Api.REPLY_RECOVER_STAFF_PASSWORD_FAIL, null));
		    }
		});
		
	

		// Request: "staff.create"
		router.on("staff", "create", (msg, client) -> {
		    @SuppressWarnings("unchecked")
		    Map<String, Object> staffData = (Map<String, Object>) msg.getData();
		    if (staffData == null) {
		    	logger.log("[MANAGER] Invalid staff creation data received.");
		        client.sendToClient(new Message(Api.REPLY_STAFF_CREATE_INVALID_DATA, "Missing staff data"));
		        return;
		    }
		    // Authorization: Only a logged-in MANAGER (from the client) may create an EMPLOYEE.
		    User requester = (User) client.getInfo("user");
		    if (requester == null || requester.getUserType() != UserType.MANAGER) {
		    	logger.log("[MANAGER] Unauthorized staff creation attempt.");
		        client.sendToClient(new Message(Api.REPLY_STAFF_CREATE_UNAUTHORIZED, null));
		        return;
		    }
		    String username = (String) staffData.get("username");
		    String password = (String) staffData.get("password");
		    String email = (String) staffData.get("email");
		    String phoneNumber = (String) staffData.get("phoneNumber");
		    String firstName = (String) staffData.get("firstName");
		    String lastName  = (String) staffData.get("lastName");
		    String address   = (String) staffData.get("address");
		    String validationError = InputCheck.validateAllStaffData(
		            username, password, email, phoneNumber,
		            firstName, lastName, address
		    );
		    if (validationError != null) {
		    	logger.log("[MANAGER] Staff creation failed due to invalid data: " + validationError);
		        client.sendToClient(new Message(Api.REPLY_STAFF_CREATE_INVALID_DATA, validationError));
		        return;
		    }
		    if (userService.staffUsernameExists(username)) {
		    	logger.log("[MANAGER] Staff creation failed: username already exists - " + username);
		        client.sendToClient(new Message(Api.REPLY_STAFF_CREATE_USERNAME_EXISTS, null));
		        return;
		    }
		    // Enforce employee-only creation from the client.
		    staffData.put("userType", UserType.EMPLOYEE.name());
		    User newStaff = userService.createStaffAccount(staffData);
		    if (newStaff != null) {
		        logger.log("[MANAGER] New employee created: " + username + " by manager user_id=" + requester.getUserId());
		        client.sendToClient(new Message(Api.REPLY_STAFF_CREATE_OK, newStaff));
		    } else {
		        logger.log("[MANAGER] Employee creation failed for username=" + username);
		        client.sendToClient(new Message(Api.REPLY_STAFF_CREATE_FAILED, null));
		    }
		});
		
		router.on("customers", "getalldata", (msg,client)->{
			List<UserData> allCustomers = userService.getAllCustomers();
			if(allCustomers != null) {
				logger.log("[INFO] Sent all customer data to client: " + client);
				client.sendToClient(new Message(Api.REPLY_LOAD_CUSTOMERS_DATA_OK, allCustomers));
				} else {
					logger.log("[ERROR] Failed to retrieve all customer data for client: " + client);
					client.sendToClient(new Message(Api.REPLY_LOAD_CUSTOMERS_DATA_FAIL, null));
				}
		});
	}
}


