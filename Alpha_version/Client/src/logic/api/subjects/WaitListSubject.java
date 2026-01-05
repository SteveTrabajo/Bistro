package logic.api.subjects;

import java.util.ArrayList;
import entities.Order;
import enums.OrderStatus;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClientGUI;
import logic.WaitingListController;
import logic.api.ClientRouter;

public class WaitListSubject {

	private WaitListSubject() {
	}

	public static void register(ClientRouter router) {
		WaitingListController wlController = BistroClientGUI.client.getWaitingListCTRL();

				//Staff: Get All Data
				router.on("waitinglist", "getAll.ok", msg -> {
					@SuppressWarnings
					("unchecked")
					ArrayList<Order> list = (ArrayList<Order>) msg.getData();
					wlController.setWaitingList(list);
				});

				//Client: Join Status
				router.on("waitinglist", "join.ok", msg -> {
					Order order = (Order) msg.getData();
					BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(order);
					wlController.setUserOnWaitingList(true);
				});

				router.on("waitinglist", "join.fail", msg -> {
					wlController.setUserOnWaitingList(false);
				});

				router.on("waitinglist", "join.skipped", msg -> {
					Order order = (Order) msg.getData();
					BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(order);
					wlController.setskipWaitingListJoin(true);
				});

				//Client/Staff: Leave Status
				router.on("waitinglist", "leave.ok", msg -> {
					BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(null);
					wlController.setLeaveWaitingListSuccess(true);
					wlController.setUserOnWaitingList(false);
				});

				router.on("waitinglist", "leave.fail", msg -> {
					wlController.setLeaveWaitingListSuccess(false);
				});

				//Check if user is in waiting list
				router.on("waitinglist", "isInWaitingList.yes", msg -> {
					if(BistroClientGUI.client.getReservationCTRL().getReadyUserReservation() != null) {
						BistroClientGUI.client.getReservationCTRL().getReadyUserReservation().setStatus(OrderStatus.WAITING_LIST);
					}
				});

				router.on("waitinglist", "isInWaitingList.no", msg -> {
					wlController.setUserOnWaitingList(false);
					BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(null);
				});

				//Notifications
				router.on("waitinglist", "notified.ok", msg -> {
					Platform.runLater(() -> {
						if(BistroClientGUI.client.getReservationCTRL().getReadyUserReservation() != null) {
							BistroClientGUI.client.getReservationCTRL().getReadyUserReservation().setStatus(OrderStatus.NOTIFIED);
						}
						BistroClientGUI.switchScreen("clientDashboardScreen", "Client Dashboard error message");
					});
				});

				router.on("waitinglist", "notified.failed", msg -> {
					Platform.runLater(() -> {
						Alert alert = new Alert(Alert.AlertType.ERROR);
						alert.setTitle("Error");
						alert.setHeaderText("Notification Error");
						alert.setContentText("Failed to notify the system that you have arrived. Please contact the staff for assistance.");
						alert.showAndWait();
					});
				});
	}
}
