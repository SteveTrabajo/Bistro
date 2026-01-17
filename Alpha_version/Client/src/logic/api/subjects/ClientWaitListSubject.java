package logic.api.subjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dto.WaitListResponse;
import entities.Order;
import entities.Table;
import enums.OrderStatus;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.TableController;
import logic.WaitingListController;
import logic.api.ClientRouter;
/**
 * ClientWaitListSubject is responsible for handling waiting list related
 * messages between the client and server.
 */
public class ClientWaitListSubject {
	/**
	 * Private constructor to prevent instantiation.
	 */
	private ClientWaitListSubject() {
	}
	/**
	 * Registers the waiting list related message handlers with the provided router.
	 *
	 * @param router          The ClientRouter to register handlers with.
	 * @param waitingListCTRL The WaitingListController to update based on messages.
	 * @param tableCTRL       The TableController to update based on messages.
	 */
	public static void register(ClientRouter router, WaitingListController waitingListCTRL, TableController tableCTRL) {
		// Staff: Get All Data
		router.on("waitinglist", "getAll.ok", msg -> {
			BistroClient.awaitResponse = false;
			@SuppressWarnings("unchecked")
			ArrayList<Order> list = (ArrayList<Order>) msg.getData();
			waitingListCTRL.setWaitingList(list);
		});
		//Handler for getAll.fail
		router.on("waitinglist", "getAll.fail", msg -> {
			BistroClient.awaitResponse = false;
		});

		// Client: Join Status
		router.on("waitinglist", "join.ok", msg -> {
			BistroClient.awaitResponse = false;
			Order order = (Order) msg.getData();
			waitingListCTRL.clearWaitingListController();
			waitingListCTRL.setOrderWaitListDTO(order);
			waitingListCTRL.setUserOnWaitingList(true);
			Platform.runLater(() -> {
				BistroClientGUI.switchScreen("clientOnListScreen", "Client On List Screen error message");
			});
		});
		//Handler for join.fail
		router.on("waitinglist", "join.fail", msg -> {
			BistroClient.awaitResponse = false;
			waitingListCTRL.clearWaitingListController();
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Failed to join waiting list");
				alert.setContentText(
						"An error occurred while trying to join the waiting list. Please try again later.");
				alert.showAndWait();
			});
		});
		// Client/Staff: Join Skipped (Seated Immediately)
		router.on("waitinglist", "join.skipped", msg -> {
			BistroClient.awaitResponse = false;
			@SuppressWarnings("unchecked")
			HashMap<String, Object> data = (HashMap<String, Object>) msg.getData();
			Order order = (Order) data.get("order");
			int table = (int) data.get("table");
			// reset waiting list state
			waitingListCTRL.clearWaitingListController();
			// set table state for seating
			tableCTRL.setUserAllocatedOrderForTable(order);
			tableCTRL.setUserAllocatedTable(table);
		});

		// Client/Staff: Leave Status
		router.on("waitinglist", "leave.ok", msg -> {
			BistroClient.awaitResponse = false;
			System.out.println("Left waiting list successfully.");
			boolean cleared = waitingListCTRL.clearWaitingListController();
			System.out.println("Clearing waiting list controller state after leaving: " + cleared);
			if (!cleared) {
				System.out.println("Warning: Failed to clear waiting list controller state after leaving.");
				return;
			}
			System.out.println("Waiting list controller state cleared.");
			waitingListCTRL.setLeaveWaitingListSuccess(true);
			System.out.println("Leave waiting list success set to true in controller.");
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Left Waiting List");
				alert.setHeaderText(null);
				alert.setContentText("You have successfully left the waiting list.");
				alert.showAndWait();
				BistroClientGUI.switchScreen("clientDashboardScreen", "Client Dashboard error message");
			});
		});
		
		// Client/Staff: Leave Status
		router.on("waitinglist","leave.staff.ok", msg -> {
			BistroClient.awaitResponse = false;
			System.out.println("Staff: Left waiting list successfully.");
		});
		
		//Handler for leave.staff.fail
		router.on("waitinglist","leave.staff.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
			 				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Failed to leave waiting list");
				alert.setContentText(
						"An error occurred while trying to leave the waiting list. Please try again later.");
				alert.showAndWait();
			});
		});
		
		//Handler for leave.fail
		router.on("waitinglist", "leave.fail", msg -> {
			BistroClient.awaitResponse = false;
			waitingListCTRL.setLeaveWaitingListSuccess(false);
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Failed to leave waiting list");
				alert.setContentText(
						"An error occurred while trying to leave the waiting list. Please try again later.");
				alert.showAndWait();
			});
		});

		// Check if user is in waiting list
		router.on("waitinglist", "isInWaitingList.yes", msg -> {
			BistroClient.awaitResponse = false;
			waitingListCTRL.setUserOnWaitingList(true);
			waitingListCTRL.setOrderWaitListDTO((Order) msg.getData());
		});

		//Handler for isInWaitingList.no
		router.on("waitinglist", "isInWaitingList.no", msg -> {
			BistroClient.awaitResponse = false;
			waitingListCTRL.setUserOnWaitingList(false);
			waitingListCTRL.setOrderWaitListDTO(null);
		});
		
		//Handler for isInWaitingList.fail
		router.on("waitinglist", "isInWaitingList.fail", msg -> {
			BistroClient.awaitResponse = false;
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Could not verify waiting list status");
			alert.setContentText("An error occurred while verifying your waiting list status. Please try again later.");
			alert.showAndWait();
		});

		// Notifications
		router.on("waitinglist", "notified.ok", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				if (waitingListCTRL.getOrderWaitListDTO() != null) {
					waitingListCTRL.getOrderWaitListDTO().setStatus(OrderStatus.NOTIFIED);
				}
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Notification Received");
				alert.setHeaderText("You have been notified!");
				alert.setContentText("Please proceed to the restaurant to be seated in table numer: " + msg.getData()
						+ ". If you do not arrive within 15 minutes, you may lose your spot.");
				alert.showAndWait();
				BistroClientGUI.switchScreen("clientDashboardScreen", "Client Dashboard error message");
			});
		});

		//Handler for notified.failed
		router.on("waitinglist", "notified.failed", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Notification Error");
				alert.setContentText(
						"Failed to notify the system that you have arrived. Please contact the staff for assistance.");
				alert.showAndWait();
			});
		});

		// Handler for addWalkIn.ok
		router.on("waitinglist", "addWalkIn.ok", msg -> {
		    BistroClient.awaitResponse = false;

		    Object payload = msg.getData();

		    Platform.runLater(() -> {
		        Alert alert = new Alert(Alert.AlertType.INFORMATION);
		        alert.setTitle("Walk-In Result");

		        StringBuilder content = new StringBuilder();

		        //server returned Map { order, table }
		        if (payload instanceof java.util.Map<?, ?>) {
		            @SuppressWarnings("unchecked")
		            java.util.Map<String, Object> m = (java.util.Map<String, Object>) payload;

		            Object orderObj = m.get("order");
		            Object tableObj = m.get("table");

		            Integer tableNum = null;
		            if (tableObj instanceof Number) {
		                tableNum = ((Number) tableObj).intValue();
		            }

		            String code = null;
		            if (orderObj instanceof Order) {
		                code = ((Order) orderObj).getConfirmationCode();
		            }

		            alert.setHeaderText("Table Assigned Immediately!");
		            content.append("Successfully registered!\n");
		            content.append("Confirmation Code: ").append(code != null ? code : "(not available)").append("\n");
		            content.append("Please proceed to Table Number: ").append(tableNum != null ? tableNum : "(unknown)");

		            alert.setContentText(content.toString());
		            alert.show();

		            waitingListCTRL.askWaitingList();
		            return; 
		        }

		        //server returned WaitListResponse
		        if (payload instanceof WaitListResponse) {
		            WaitListResponse r = (WaitListResponse) payload;

		            alert.setHeaderText("Added to Waiting List");
		            content.append("No table available right now.\n");
		            content.append("Estimated Wait Time: ").append(r.getEstimatedWaitTimeMinutes()).append(" minutes.\n");
		            content.append("Message: ").append(r.getMessage());

		            alert.setContentText(content.toString());
		            alert.show();

		            waitingListCTRL.askWaitingList();
		            return;
		        }

		        //unexpected
		        alert.setHeaderText("Unexpected Response");
		        alert.setContentText("Server returned: " + (payload == null ? "null" : payload.getClass().getName()));
		        alert.show();
		    });
		});

		// Handler for addWalkIn.fail
		router.on("waitinglist", "addWalkIn.fail", msg -> {
			BistroClient.awaitResponse = false;
			// Extract error message if the server sent one
			String errorMsg = (msg.getData() instanceof String) ? (String) msg.getData() : "Unknown error occurred.";

			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Failed to add walk-in");
				alert.setContentText(errorMsg);
				alert.show();
			});
		});

		//Handler for checkAvailability responses
		router.on("waitinglist", "checkAvailability.ok", msg -> {
			BistroClient.awaitResponse = false;
			WaitListResponse dto = (WaitListResponse) msg.getData();
			waitingListCTRL.setEstimatedWaitTimeMinutes(dto.getEstimatedWaitTimeMinutes());
		});

		//Handler for checkAvailability.skipped
		router.on("waitinglist", "checkAvailability.skipped", msg -> {
			BistroClient.awaitResponse = false;
			Map<String, Object> data = (Map<String, Object>) msg.getData();
			waitingListCTRL.setCanSeatImmediately(true);
			tableCTRL.setUserAllocatedOrderForTable((Order.class.cast(data.get("order"))));
			tableCTRL.setUserAllocatedTable((int) data.get("table"));
			BistroClientGUI.switchScreen("clientCheckInTableSuccesScreen",
					"Client Check-In Table Success error messege");
		});

		//Handler for checkAvailability.fail
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
// End of ClientWaitListSubject.java