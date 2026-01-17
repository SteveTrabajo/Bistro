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
/**
 * ClientRestaurantManageSubject is responsible for handling restaurant management-related messages
 * received from the server and updating the client GUI accordingly.
 */
public class ClientRestaurantManageSubject {

	/**
	 * Registers restaurant management-related message handlers with the provided ClientRouter.
	 *
	 * @param router The ClientRouter to register message handlers with.
	 */
	public static void register(ClientRouter router) {
		
		// Handler for successful saving of weekly hours
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

		// Handler for failed saving of weekly hours
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

		// Handler for successful addition of a holiday
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

		// Handler for failed addition of a holiday
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

		// Handler for successful removal of a holiday
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

		// Handler for failed removal of a holiday
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

		// Handler for successful retrieval of weekly hours
		router.on("hours", "getWeeklyHours.ok", msg -> {
			BistroClient.awaitResponse = false;

			System.out.println("Received weekly hours from server.");
		    @SuppressWarnings("unchecked")
		    List<WeeklyHour> hours = (List<WeeklyHour>) msg.getData();
		    BistroClientGUI.client.getTableCTRL().setWeeklyHours(hours);
		    BistroClientGUI.client.getReservationCTRL().setWeeklyHours(hours);
		});
		
		// Handler for failed retrieval of weekly hours
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

		// Handler for successful retrieval of holidays
		router.on("hours", "getHolidays.ok", msg -> {
			BistroClient.awaitResponse = false;

			System.out.println("Received holidays from server.");
		    @SuppressWarnings("unchecked")
		    List<Holiday> holidays = (List<Holiday>) msg.getData();
		    BistroClientGUI.client.getTableCTRL().setHolidays(holidays);
		});
		
		// Handler for failed retrieval of holidays
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
// End of ClientRestaurantManageSubject.java