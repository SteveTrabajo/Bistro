package logic.api.subjects;

import javafx.application.Platform;
import logic.BistroClientGUI;
import logic.api.ClientRouter;

public class ConnectionSubject {
	
	public static void register(ClientRouter router) {
		System.out.println("ConnectionSubject.register() CALLED");
		// Handler for connection displayed successfully
		router.on("connection", "connect.ok", msg -> {
			String status = "Displayed";
			Platform.runLater(() -> System.out.println("Connection status: " + status));
		});
	}
}
