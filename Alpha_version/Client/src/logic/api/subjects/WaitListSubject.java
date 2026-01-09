package logic.api.subjects;

import java.util.ArrayList;
import entities.Order;
import enums.OrderStatus;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.WaitingListController;
import logic.api.ClientRouter;

public class WaitListSubject {

	private WaitListSubject() {
	}

	public static void register(ClientRouter router,WaitingListController wlController) {
				//Staff: Get All Data
				router.on("waitinglist", "getAll.ok", msg -> {
		            BistroClient.awaitResponse = false;
					@SuppressWarnings
					("unchecked")
					ArrayList<Order> list = (ArrayList<Order>) msg.getData();
					wlController.setWaitingList(list);
					
				});
				router.on("waitinglist", "getAll.fail", msg -> {
		            BistroClient.awaitResponse = false;
				});

				//Client: Join Status
				router.on("waitinglist", "join.ok", msg -> {
		            BistroClient.awaitResponse = false;
					Order order = (Order) msg.getData();
					BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(order);
					wlController.setUserOnWaitingList(true);
				});

				router.on("waitinglist", "join.fail", msg -> {
		            BistroClient.awaitResponse = false;
				});

				router.on("waitinglist", "join.skipped", msg -> {
		            BistroClient.awaitResponse = false;
					Order order = (Order) msg.getData();
					BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(order);
					wlController.setCanSeatImmediately(true);
				});

				//Client/Staff: Leave Status
				router.on("waitinglist", "leave.ok", msg -> {
		            BistroClient.awaitResponse = false;
					BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(null);
					wlController.setLeaveWaitingListSuccess(true);
					wlController.setUserOnWaitingList(false);
				});

				router.on("waitinglist", "leave.fail", msg -> {
		            BistroClient.awaitResponse = false;
					wlController.setLeaveWaitingListSuccess(false);
				});

				//Check if user is in waiting list
				router.on("waitinglist", "isInWaitingList.yes", msg -> {
		            BistroClient.awaitResponse = false;
					if(BistroClientGUI.client.getReservationCTRL().getReadyUserReservation() != null) {
						BistroClientGUI.client.getReservationCTRL().getReadyUserReservation().setStatus(OrderStatus.WAITING_LIST);
					}
				});

				router.on("waitinglist", "isInWaitingList.no", msg -> {
		            BistroClient.awaitResponse = false;
					wlController.setUserOnWaitingList(false);
					BistroClientGUI.client.getReservationCTRL().setReadyUserReservation(null);
				});
				router.on("waitinglist", "isInWaitingList.fail", msg -> {
		            BistroClient.awaitResponse = false;
		            Alert alert = new Alert(Alert.AlertType.ERROR);
		            alert.setTitle("Error");
		            alert.setHeaderText("Could not verify waiting list status");
		            alert.setContentText("An error occurred while verifying your waiting list status. Please try again later.");
		            alert.showAndWait();
				});

				//Notifications
				router.on("waitinglist", "notified.ok", msg -> {
		            BistroClient.awaitResponse = false;
					Platform.runLater(() -> {
						if(BistroClientGUI.client.getReservationCTRL().getReadyUserReservation() != null) {
							BistroClientGUI.client.getReservationCTRL().getReadyUserReservation().setStatus(OrderStatus.NOTIFIED);
						}
						BistroClientGUI.switchScreen("clientDashboardScreen", "Client Dashboard error message");
					});
				});

				router.on("waitinglist", "notified.failed", msg -> {
		            BistroClient.awaitResponse = false;
					Platform.runLater(() -> {
						Alert alert = new Alert(Alert.AlertType.ERROR);
						alert.setTitle("Error");
						alert.setHeaderText("Notification Error");
						alert.setContentText("Failed to notify the system that you have arrived. Please contact the staff for assistance.");
						alert.showAndWait();
					});
				});
				
				router.on("waitinglist", "addWalkIn.ok", msg -> {
		            BistroClient.awaitResponse = false;
		            Platform.runLater(() -> {
		                Alert alert = new Alert(Alert.AlertType.INFORMATION);
		                alert.setTitle("Success");
		                alert.setHeaderText(null);
		                alert.setContentText("Walk-in party added to the list.");
		                alert.show();
		                wlController.askWaitingList();
		            });
		        });

		        router.on("waitinglist", "addWalkIn.fail", msg -> {
		            BistroClient.awaitResponse = false;
		            Platform.runLater(() -> {
		                Alert alert = new Alert(Alert.AlertType.ERROR);
		                alert.setTitle("Error");
		                alert.setContentText("Failed to add walk-in.");
		                alert.show();
		            });
		        });
		        router.on("waitinglist", "checkAvailability.ok", msg -> {
		            BistroClient.awaitResponse = false;
		            long estimatedWaitTime = (long) msg.getData();
		            wlController.setEstimatedWaitTimeMinutes(estimatedWaitTime);
		            });
		        router.on("waitinglist", "checkAvailability.fail", msg -> {
		        			            BistroClient.awaitResponse = false;
		            Platform.runLater(() -> {
		                Alert alert = new Alert(Alert.AlertType.ERROR);
		                alert.setTitle("Error");
		                alert.setContentText("Failed to check availability.");
		                alert.show();
		            });
		            
		        });
	}
}
