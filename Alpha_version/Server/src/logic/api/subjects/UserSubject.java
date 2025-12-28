package logic.api.subjects;

import java.util.Map;

import comms.Api;
import comms.Message;
import entities.User;
import logic.BistroDataBase_Controller;
import logic.api.Router;

public final class UserSubject {

	private UserSubject() {
	}

	public static void register(Router router) {

		// Request: "login.user"
		router.on("login", "user", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> loginData = (Map<String, Object>) msg.getData();

			User user = BistroDataBase_Controller.getUserInfo(loginData);
			if (user != null) {
				client.sendToClient(new Message(Api.REPLY_LOGIN_USER_OK, user));
			} else {
				client.sendToClient(new Message(Api.REPLY_LOGIN_USER_NOT_FOUND, null));
			}
		});

	}
}
