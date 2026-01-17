package logic.api.subjects;

import gui.logic.staff.RestaurantManagementPanel;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.api.ClientRouter;

public class ClientRestaurantManageSubject {
	
	public static void register(ClientRouter router) {

		router.on("hours", "saveWeekly.ok", msg -> {
		    BistroClient.awaitResponse = false;
		    Platform.runLater(() -> {
		        Alert alert = new Alert(Alert.AlertType.INFORMATION);
		        alert.setTitle("Success");
		        alert.setHeaderText(null);
		        alert.setContentText("Opening hours updated successfully.");
		        alert.showAndWait();
		    });
		});

	    router.on("hours", "saveWeekly.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText(null);
				alert.setContentText("Failed to update opening hours.");
				alert.showAndWait();
			});
		});

	    router.on("hours", "addHoliday.ok", msg -> {
	        BistroClient.awaitResponse = false;
	        Platform.runLater(() -> {
		        Alert alert = new Alert(Alert.AlertType.INFORMATION);
		        alert.setTitle("Success");
		        alert.setHeaderText(null);
		        alert.setContentText("Opening hours updated successfully.");
		        alert.showAndWait();
		    });
	    });

	    router.on("hours", "addHoliday.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText(null);
				alert.setContentText("Failed to update opening hours.");
				alert.showAndWait();
			});
		});

	    router.on("hours", "removeHoliday.ok", msg -> {
	        BistroClient.awaitResponse = false;
	        Platform.runLater(() -> {
		        Alert alert = new Alert(Alert.AlertType.INFORMATION);
		        alert.setTitle("Success");
		        alert.setHeaderText(null);
		        alert.setContentText("Opening hours updated successfully.");
		        alert.showAndWait();
		    });
	    });

	    
	    router.on("hours", "removeHoliday.fail", msg -> {
	        BistroClient.awaitResponse = false;
	        Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText(null);
				alert.setContentText("Failed to update opening hours.");
				alert.showAndWait();
			});
	    });
	}

}
