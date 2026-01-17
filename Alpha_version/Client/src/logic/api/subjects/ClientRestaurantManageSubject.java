package logic.api.subjects;

import java.util.List;

import dto.Holiday;
import dto.WeeklyHour;
import gui.logic.staff.RestaurantManagementPanel;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.api.ClientRouter;

public class ClientRestaurantManageSubject {

	public static void register(ClientRouter router) {

		router.on("hours", "saveWeeklyHours.ok", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Success");
				alert.setHeaderText(null);
				alert.setContentText("Opening hours updated successfully.");
				alert.showAndWait();
			});
		});

		router.on("hours", "saveWeeklyHours.fail", msg -> {
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

		router.on("hours", "getWeeklyHours.ok", msg -> {
			BistroClient.awaitResponse = false;

			System.out.println("Received weekly hours from server.");
		    @SuppressWarnings("unchecked")
		    List<WeeklyHour> hours = (List<WeeklyHour>) msg.getData();
		    BistroClientGUI.client.getTableCTRL().setWeeklyHours(hours);
		    BistroClientGUI.client.getReservationCTRL().setWeeklyHours(hours);
		});
		
		router.on("hours", "getWeeklyHours.fail", msg -> {
			BistroClient.awaitResponse = false;

			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText(null);
				alert.setContentText("Failed to retrieve opening hours.");
				alert.showAndWait();
			});
		});

		router.on("hours", "getHolidays.ok", msg -> {
			BistroClient.awaitResponse = false;

			System.out.println("Received holidays from server.");
		    @SuppressWarnings("unchecked")
		    List<Holiday> holidays = (List<Holiday>) msg.getData();
		    BistroClientGUI.client.getTableCTRL().setHolidays(holidays);
		});
		
		router.on("hours", "getHolidays.fail", msg -> {
			BistroClient.awaitResponse = false;

			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText(null);
				alert.setContentText("Failed to retrieve holidays.");
				alert.showAndWait();
			});
		});

	}
}
