package logic.api.subjects;

import entities.Order;
import enums.OrderStatus;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClientGUI;
import logic.api.ClientRouter;

public class WaitListSubject {

	private WaitListSubject() {
	}

	public static void register(ClientRouter router) {
		// Handler for user on waiting list status messages

		router.on("waitinglist", "isInWaitingList.yes", msg -> {
			;
			OrderStatus status = OrderStatus.WAITING_LIST;
			BistroClientGUI.client.getReservationCTRL().getReadyUserReservation().setStatus(status);
		});

		router.on("waitinglist", "isInWaitingList.no", msg -> {
		});

		router.on("waitinglist", "join.ok", msg -> {
			Order order = (Order) msg.getData();
			BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(order);
		});

		router.on("waitinglist", "join.fail", msg -> {
		});

		router.on("waitinglist", "join.skipped", msg -> {
			Order order = (Order) msg.getData();
			BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(order);
			BistroClientGUI.client.getWaitingListCTRL().setskipWaitingListJoin(true);
		});

		router.on("waitinglist", "leave.ok", msg -> {
			BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(null);
		});
		router.on("waitinglist", "leave.fail", msg -> {
		});
		router.on("waitinglist", "notified.ok", msg -> {
			OrderStatus status = OrderStatus.NOTIFIED;
			BistroClientGUI.client.getReservationCTRL().getReadyUserReservation().setStatus(status);
			BistroClientGUI.switchScreen("clientDashboardScreen", "Client Dashboard error message");
		});
		router.on("waitinglist", "notified.failed", msg -> {
			BistroClientGUI.showAlert(Alert.AlertType.ERROR, "Waitlist Notification Failed",
					"Failed to notify the restaurant that you have arrived from the waitlist. Please try again.",
					Platform::runLater);
		});
	}
}
