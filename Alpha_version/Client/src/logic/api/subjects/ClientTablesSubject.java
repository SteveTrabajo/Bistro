package logic.api.subjects;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import entities.Order;
import entities.Table;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.api.ClientRouter;
/**
 * ClientTablesSubject is responsible for handling table-related messages
 * received from the server and updating the client GUI accordingly.
 */
public class ClientTablesSubject {
	/**
	 * Registers message handlers for table-related events.
	 *
	 * @param router The ClientRouter to register the handlers with.
	 */
	public static void register(ClientRouter router) {
		// Handler for successful retrieval of table statuses
		router.on("tables", "getStatus.ok", msg -> {
            BistroClient.awaitResponse = false;
            Platform.runLater(() -> {
			@SuppressWarnings("unchecked")
			HashMap<Table, String> tableStatuses = (HashMap<Table, String>) msg.getData();
			BistroClientGUI.client.getTableCTRL().updateTableStatuses(tableStatuses);
            });
		});
		// Handler for failed retrieval of table statuses
		router.on("tables", "getStatus.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Could not retrieve table statuses");
			alert.setContentText("An error occurred while fetching table statuses. Please try again later.");
			alert.showAndWait();
			});
			
		});
		// Handler for successful retrieval of all tables
		router.on("tables", "getAll.ok", msg -> {
		    BistroClient.awaitResponse = false;
		    @SuppressWarnings("unchecked")
		    List<Table> tables = (List<Table>) msg.getData();
		    Platform.runLater(() -> {
		        BistroClientGUI.client.getTableCTRL().fireAllTables(tables);
		    });
		});

		// Handler for failed retrieval of all tables
		router.on("tables", "getUserAllocatedTable.ok", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
			Integer tableNumber = (Integer) msg.getData();
			BistroClientGUI.client.getTableCTRL().setUserAllocatedTable(tableNumber);
			});
		});
		// Handler for failed retrieval of user allocated table
		router.on("tables", "getUserAllocatedTable.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Could not retrieve allocated table");
			alert.setContentText("An error occurred while fetching your allocated table. Please try again later.");
			alert.showAndWait();
			});
		});
		// Handler for successful retrieval of seated order
		router.on("tables", "askSeatedOrder.ok", msg -> {
			BistroClient.awaitResponse = false;
			Order dto = (Order) msg.getData();
			Platform.runLater(() -> {
				BistroClientGUI.client.getTableCTRL().setUserAllocatedOrderForTable(dto);
			});
		});
		// Handler for failed retrieval of seated order
		router.on("tables", "askSeatedOrder.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				BistroClientGUI.client.getTableCTRL().setUserAllocatedOrderForTable(null);
			});
		});
		// Handler for successful table addition
		router.on("tables", "remove.ok", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				@SuppressWarnings("unchecked")
				List<Table> tables = (List<Table>) msg.getData();
				BistroClientGUI.client.getTableCTRL().fireAllTables(tables);
				Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Success");
				alert.setHeaderText("Table removed successfully");
				alert.setContentText("The table has been removed successfully.");
				alert.showAndWait();
				});
			});
		});
		// Handler for failed table addition
		router.on("tables", "remove.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Could not remove table");
			alert.setContentText("An error occurred while removing the table. Please try again later.");
			alert.showAndWait();
			});
		});
		// Handler for successful table removal
		router.on("tables", "updateSeats.ok", msg -> {
			BistroClient.awaitResponse = false;
			@SuppressWarnings("unchecked")
			List<Table> tables = (List<Table>) msg.getData();
			Platform.runLater(() -> {
				BistroClientGUI.client.getTableCTRL().fireAllTables(tables);
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Success");
				alert.setHeaderText("Table updated successfully");
				alert.setContentText("The table capacity has been updated.");
				alert.showAndWait();
			});
		});
		// Handler for failed table removal
		router.on("tables", "updateSeats.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Could not update table");
				alert.setContentText("An error occurred while updating the table. Please try again later.");
				alert.showAndWait();
			});
		});	
	}
}
// End of ClientTablesSubject.java