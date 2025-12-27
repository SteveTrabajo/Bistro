package logic.api.subjects;

import javafx.application.Platform;
import logic.BistroClientGUI;
import logic.api.ClientRouter;

public class OrderSubject {
	
	private OrderSubject() {}
	
	public static void register(ClientRouter router) {
		// Handler for new reservation creation messages
		router.on("orders.createReservation", "ok", msg -> {
			String confirmationCode = (String) msg.getData();
			Platform.runLater(() -> BistroClientGUI.client.getReservationCTRL().setConfirmationCode(confirmationCode));
		});
	}
	
}
