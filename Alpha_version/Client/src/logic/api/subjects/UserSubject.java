package logic.api.subjects;

import entities.User;
import javafx.application.Platform;
import logic.BistroClientGUI;
import logic.api.ClientRouter;

public class UserSubject {
	
	public static void register(ClientRouter router) {
		// Handler for login approval messages
		router.on("login", "user.ok", msg -> {
			User user = (User) msg.getData();
			Platform.runLater(() -> BistroClientGUI.client.getUserCTRL().setLoggedInUser(user));
		});
		
		// Handler for login failure messages
		router.on("login", "user.notFound", msg -> {
			Platform.runLater(() -> BistroClientGUI.client.getUserCTRL().setLoggedInUser(null));
		});
	}
	
	
}
