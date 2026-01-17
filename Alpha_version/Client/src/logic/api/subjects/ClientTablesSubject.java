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

public class ClientTablesSubject {

	public static void register(ClientRouter router) {

		router.on("tables", "getStatus.ok", msg -> {
            BistroClient.awaitResponse = false;
            Platform.runLater(() -> {
			@SuppressWarnings("unchecked")
			HashMap<Table, String> tableStatuses = (HashMap<Table, String>) msg.getData();
			BistroClientGUI.client.getTableCTRL().updateTableStatuses(tableStatuses);
            });
		});
		
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
		
		router.on("tables", "getAll.ok", msg -> {
		    BistroClient.awaitResponse = false;
		    @SuppressWarnings("unchecked")
		    List<Table> tables = (List<Table>) msg.getData();
		    Platform.runLater(() -> {
		        BistroClientGUI.client.getTableCTRL().fireAllTables(tables);
		    });
		});

		
		router.on("tables", "getUserAllocatedTable.ok", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
			Integer tableNumber = (Integer) msg.getData();
			BistroClientGUI.client.getTableCTRL().setUserAllocatedTable(tableNumber);
			});
		});
		
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
		
		router.on("tables", "askSeatedOrder.ok", msg -> {
			BistroClient.awaitResponse = false;
			Order dto = (Order) msg.getData();
			Platform.runLater(() -> {
				BistroClientGUI.client.getTableCTRL().setUserAllocatedOrderForTable(dto);
			});
		});
		
		router.on("tables", "askSeatedOrder.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				BistroClientGUI.client.getTableCTRL().setUserAllocatedOrderForTable(null);
			});
		});
		
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
		
	}

}
