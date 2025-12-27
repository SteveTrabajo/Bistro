package logic.api.subjects;

import entities.Order;
import javafx.application.Platform;
import logic.BistroClientGUI;
import logic.api.ClientRouter;

public class WaitListSubject {
	
	private WaitListSubject() {}
	
	public static void register(ClientRouter router) {
		// Handler for user on waiting list status messages
		router.on("waitlist.isUserOn", "result", msg -> {
			Order waitingOrder = (Order) msg.getData();
			Platform.runLater(() -> BistroClientGUI.client.getWaitingListCTRL().setCurrentWaitingOrder(waitingOrder));
		});

	}
}
