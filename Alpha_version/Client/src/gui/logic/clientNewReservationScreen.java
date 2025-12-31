package gui.logic;

import java.time.LocalTime;
import java.util.List;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import logic.BistroClientGUI;

public class clientNewReservationScreen {
@FXML
private Label lblUser;
@FXML
private DatePicker datePicker;
@FXML
private ComboBox<String> dinersAmountComboBox;
@FXML
private GridPane timeSlotsGridPane;
@FXML
private Button btnBack;
@FXML
private Button btnConfirmReservation;

@FXML
public void initialize() {
	User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
	lblUser.setText(currentUser.getUserType().name());
	datePicker.setValue(java.time.LocalDate.now());
	
	// Populate dinersAmountComboBox
	for (int i = 1; i <= 12; i++) {
		dinersAmountComboBox.getItems().add(String.valueOf(i)+" People");
	}
	dinersAmountComboBox.getSelectionModel().selectFirst();

	// Disable the confirm button initially
	btnConfirmReservation.setDisable(true);

	// Add listeners to fetch time slots dynamically
	datePicker.valueProperty().addListener((obs, oldDate, newDate) -> fetchAndPopulateTimeSlots());
	dinersAmountComboBox.valueProperty().addListener((obs, oldValue, newValue) -> fetchAndPopulateTimeSlots());
}

private void fetchAndPopulateTimeSlots() {
	if (datePicker.getValue() != null && dinersAmountComboBox.getValue() != null) {
		BistroClientGUI.client.getReservationCTRL().askAvailableHours(
			datePicker.getValue(),
			Integer.parseInt(dinersAmountComboBox.getValue().split(" ")[0])
		);
		// Fetch available time slots from the server
		List<String> timeSlots = BistroClientGUI.client.getReservationCTRL().getAvailableTimeSlots();

		// Clear the GridPane
		timeSlotsGridPane.getChildren().clear();

		// Populate the GridPane with time slots using a ToggleGroup for exclusive selection
		ToggleGroup timeSlotGroup = new ToggleGroup();
		int row = 0;
		int col = 0;
		for (String timeSlot : timeSlots) {
			ToggleButton toggleButton = new ToggleButton(timeSlot);
			toggleButton.setToggleGroup(timeSlotGroup);
			timeSlotsGridPane.add(toggleButton, col, row);
			col++;
			if (col > 3) {
				col = 0;
				row++;
			}
		}

		// Enable confirm when a selection exists
		timeSlotGroup.selectedToggleProperty().addListener((obs, oldToggle, selectedToggle) -> {
			btnConfirmReservation.setDisable(selectedToggle == null);
		});

		// Select the first time slot by default
		if (!timeSlots.isEmpty()) {
			ToggleButton first = (ToggleButton) timeSlotsGridPane.getChildren().get(0);
			timeSlotGroup.selectToggle(first);
			btnConfirmReservation.setDisable(false);
		}
	}
}