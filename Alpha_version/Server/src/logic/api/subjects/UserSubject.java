package logic.api.subjects;

import java.util.HashMap;
import java.util.Map;

import comms.Api;
import comms.Message;
import entities.User;
import enums.UserType;
import logic.LoginAttemptTracker;
import logic.ServerLogger;
import logic.api.Router;
import logic.services.UserService;
import dto.UserData;

public final class UserSubject {

	private UserSubject() {
	}

	public static void register(Router router, UserService userService, ServerLogger logger) {

		// Request: "login.guest"
		router.on("login", "guest", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> loginData = (Map<String, Object>) msg.getData();
			User user = userService.getUserInfo(loginData);
			if (user != null) {
				client.sendToClient(new Message(Api.REPLY_LOGIN_GUEST_OK, user));
			} else {
				client.sendToClient(new Message(Api.REPLY_LOGIN_GUEST_NOT_FOUND, null));
			}
		});

		// Request: "login.member"
		router.on("login", "member", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> loginData = (Map<String, Object>) msg.getData();
			User user = userService.getUserInfo(loginData);
			if (user != null) {
				client.sendToClient(new Message(Api.REPLY_LOGIN_MEMBER_OK, user));
			} else {
				client.sendToClient(new Message(Api.REPLY_LOGIN_MEMBER_NOT_FOUND, null));
			}
		});

		// Request: "login.employee"
		router.on("login", "employee", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> loginData = (Map<String, Object>) msg.getData();
			String username = String.valueOf(loginData.get("username"));
			
			// Check if account is locked
			if (LoginAttemptTracker.isAccountLocked(username)) {
				logger.log("[LOGIN] Account locked for EMPLOYEE: " + username);
				client.sendToClient(new Message(Api.REPLY_LOGIN_EMPLOYEE_ACCOUNT_LOCKED, null));
				return;
			}
			
			User user = userService.getUserInfo(loginData);
			if (user != null) {
				client.sendToClient(new Message(Api.REPLY_LOGIN_EMPLOYEE_OK, user));
			} else {
				client.sendToClient(new Message(Api.REPLY_LOGIN_EMPLOYEE_INVALID_CREDENTIALS, null));
			}
		});

		// Request: "login.manager"
		router.on("login", "manager", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> loginData = (Map<String, Object>) msg.getData();
			String username = String.valueOf(loginData.get("username"));
			
			// Check if account is locked
			if (LoginAttemptTracker.isAccountLocked(username)) {
				logger.log("[LOGIN] Account locked for MANAGER: " + username);
				client.sendToClient(new Message(Api.REPLY_LOGIN_MANAGER_ACCOUNT_LOCKED, null));
				return;
			}
			
			User user = userService.getUserInfo(loginData);
			if (user != null) {
				client.sendToClient(new Message(Api.REPLY_LOGIN_MANAGER_OK, user));
			} else {
				client.sendToClient(new Message(Api.REPLY_LOGIN_MANAGER_INVALID_CREDENTIALS, null));
			}
		});

		// Request: "signout.guest"
		router.on("signout", "guest", (msg, client) -> {
			logger.log("[INFO] Client " + client + " signed out as GUEST");
			client.sendToClient(new Message(Api.REPLY_SIGNOUT_GUEST_OK, null));
		});

		// Request: "signout.member"
		router.on("signout", "member", (msg, client) -> {
			logger.log("[INFO] Client " + client + " signed out as MEMBER");
			client.sendToClient(new Message(Api.REPLY_SIGNOUT_MEMBER_OK, null));
		});

		// Request: "signout.employee"
		router.on("signout", "employee", (msg, client) -> {
			logger.log("[INFO] Client " + client + " signed out as EMPLOYEE");
			client.sendToClient(new Message(Api.REPLY_SIGNOUT_EMPLOYEE_OK, null));
		});

		// Request: "signout.manager"
		router.on("signout", "manager", (msg, client) -> {
			logger.log("[INFO] Client " + client + " signed out as MANAGER");
			client.sendToClient(new Message(Api.REPLY_SIGNOUT_MANAGER_OK, null));
		});

		// Request: "member.updateInfo"
		router.on("member", "updateInfo", (msg, client) -> {
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

		// Request: "staff.create"
		router.on("staff", "create", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> staffData = (Map<String, Object>) msg.getData();
			
			User newStaff = userService.createStaffAccount(staffData);
			if (newStaff != null) {
				logger.log("[ADMIN] New staff account created: " + staffData.get("username"));
				client.sendToClient(new Message(Api.REPLY_STAFF_CREATE_OK, newStaff));
			} else {
				logger.log("[ADMIN] Staff creation failed: " + staffData.get("username"));
				client.sendToClient(new Message(Api.REPLY_STAFF_CREATE_INVALID_DATA, null));
			}
		});
	}
}


