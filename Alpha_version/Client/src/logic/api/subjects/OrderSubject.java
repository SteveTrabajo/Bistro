package logic.api.subjects;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.api.ClientRouter;
import java.util.List;

import entities.Order;

public class OrderSubject {
	
	private OrderSubject() {}
	
	public static void register(ClientRouter router) {
		// Handler for new reservation creation messages
		router.on("orders", "createReservation.ok", msg -> {
            BistroClient.awaitResponse = false;
			String confirmationCode = (String) msg.getData();
			Platform.runLater(() -> BistroClientGUI.client.getReservationCTRL().setConfirmationCode(confirmationCode));
		});
		router.on("orders","createReservation.fail", msg -> {
			BistroClient.awaitResponse = false;
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Reservation Failed");
			alert.setHeaderText("Could not create reservation");
			alert.setContentText("An error occurred while creating your reservation. Please try again later.");
		});
		// This tells the router: "When the server sends 'getAvailableHours.ok', update the controller."
		router.on("orders", "getAvailableHours.ok", (msg) -> {
            BistroClient.awaitResponse = false;
            @SuppressWarnings("unchecked")
            List<String> slots = (List<String>) msg.getData();
            Platform.runLater(() -> BistroClientGUI.client.getReservationCTRL().setAvailableTimeSlots(slots));  
        });
		
		router.on("orders", "getAvailableHours.fail", (msg) -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Could not retrieve available hours");
				alert.setContentText("An error occurred while fetching available time slots. Please try again later.");
				alert.showAndWait();
			});
		});
		router.on("orders", "order.exists", msg -> {
            BistroClient.awaitResponse = false;
			Order order = (Order) msg.getData();
			BistroClientGUI.client.getTableCTRL().setUserAllocatedOrderForTable(order);
			BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(null);
		});
		router.on("orders", "order.notExists", msg -> {
			BistroClient.awaitResponse = false;
		});	
	}
	
}
