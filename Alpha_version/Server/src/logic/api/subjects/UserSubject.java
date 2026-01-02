package logic.api.subjects;

import java.util.Map;

import comms.Api;
import comms.Message;
import entities.User;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;
import logic.api.Router;
import logic.services.UserService;

public final class UserSubject {

	private UserSubject() {
	}

	public static void register(Router router, UserService userService,  ServerLogger logger) {

		// Request: "login.user"
		router.on("login", "user", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> loginData = (Map<String, Object>) msg.getData();

			User user = userService.getUserInfo(loginData);
			if (user != null) {
				client.sendToClient(new Message(Api.REPLY_LOGIN_USER_OK, user));
			} else {
				client.sendToClient(new Message(Api.REPLY_LOGIN_USER_NOT_FOUND, null));
			}
		});
		
		// Request: "signout.user"
		router.on("signout", "user", (msg, client) -> {
			logger.log("[INFO] Client " + client + " requested to sign out.");
			client.sendToClient(new Message(Api.REPLY_SIGNOUT_USER_OK, null));
		});
		
		
		//Request: "Member.updateInfo"
		router.on("member", "updateInfo", (msg, client) -> {
			User updatedUser = (User) msg.getData();
			boolean success = userService.updateMebmerInfo(updatedUser);
			if (success) {
				logger.log("[INFO] Client " + client + " requested to update user info: successful.");
				client.sendToClient(new Message(Api.REPLY_MEMBER_UPDATE_INFO_OK, null));
				
			} else {
				logger.log("[ERROR] Client " + client + " requested to update user info: failed.");	
				client.sendToClient(new Message(Api.REPLY_MEMBER_UPDATE_INFO_FAILED, null));
			}
		});

	}
}
