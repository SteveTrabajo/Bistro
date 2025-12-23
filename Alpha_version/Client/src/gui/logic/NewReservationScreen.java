package gui.logic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import logic.BistroClientGUI;

public class NewReservationScreen {
	
	@FXML
	private DatePicker datePicker;
	@FXML
	private ComboBox<String> dinersAmountComboBox;
	@FXML
	private GridPane timeSlotsGridPane;
	@FXML
	private Button btnConfirmReservation;
	@FXML
	private Button btnBack;
	
	private String selectedTimeSlot = null;
	
	/*
	 * Initializes the New Reservation Screen by setting up the diners amount combo box and date picker.
	 */
	@FXML
	public void initialize() {
		setupDinersAmountComboBox();
		setupDatePicker();
	}

	//TODO connect to server to get real available time slots and make sure the timeslots are updated based on the selected date and fit the database
	/*
	 * Sets up the date picker to prevent selecting past dates and handles date changes.
	 */
	private void setupDatePicker() {
		// code to prevent selecting past dates
		datePicker.setDayCellFactory(picker -> new DateCell() {
	        @Override
	        public void updateItem(LocalDate date, boolean empty) {
	            super.updateItem(date, empty);
	            setDisable(empty || date.isBefore(LocalDate.now()));
	        }
	    });
		
		datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
	        System.out.println("Date changed to: " + newDate);
	        generateTimeSlots(generateDefaultTimeSlots());
	        });
		
		datePicker.setValue(LocalDate.now());
	}

	/*
	 * Sets up the diners amount combo box with options from 1 to 12 people.
	 */
	private void setupDinersAmountComboBox() {
		for (int i = 1; i <= 12; i++) {
			dinersAmountComboBox.getItems().add(i + " People");
		}
		dinersAmountComboBox.getSelectionModel().selectFirst();		
	}
	
	/*
	 * Generates time slot buttons based on the provided list of available time slots.
	 * 
	 * @param availableTimeSlots List of available time slots to generate buttons for.
	 */
	private void generateTimeSlots(List<String> availableTimeSlots) {
		timeSlotsGridPane.getChildren().clear();
		
		ToggleGroup timeSlotToggleGroup = new ToggleGroup();
		int col = 0;
		int row = 0;
		for (String timeSlot : availableTimeSlots) {
			
			ToggleButton timeSlotButton = new ToggleButton(timeSlot);
			timeSlotButton.setToggleGroup(timeSlotToggleGroup);
			// can set this in CSS later TODO
			timeSlotButton.setPrefWidth(104); 
			timeSlotButton.setPrefHeight(37);
			
			// Handle click
			timeSlotButton.setOnAction(event -> {
			    // Check if the button is currently ON or OFF
			    if (timeSlotButton.isSelected()) {
			        selectedTimeSlot = timeSlot;
			    } else {
			        selectedTimeSlot = null; // Clear the selection if they uncheck it
			    }
			    System.out.println("Selected time slot: " + selectedTimeSlot);
			});
			timeSlotsGridPane.add(timeSlotButton, col, row);
			col++;
			if (col >= 4) { // 4 columns per row
				col = 0;
				row++;
			}
		}
	}
	
	// Temporary helper to fake data until you connect your Server
	private List<String> generateDefaultTimeSlots() {
	    List<String> times = new ArrayList<>();
	    times.add("11:00"); times.add("11:30");
	    times.add("12:00"); times.add("12:30");
	    times.add("13:00"); times.add("13:30");
	    times.add("18:00"); times.add("18:30");
	    return times;
	}
	
	/*
	 * Handles the confirm button click event to finalize the reservation.
	 * 
	 * @param event The event triggered by clicking the confirm button.
	 */
	@FXML
	void onConfirmClick(Event event) {
	    LocalDate date = datePicker.getValue();
	    String diners = dinersAmountComboBox.getValue();
	    
	    if (selectedTimeSlot == null) {
	        // Show Error Alert
	        Alert alert = new Alert(Alert.AlertType.WARNING);
	        alert.setContentText("Please select a time slot.");
	        alert.showAndWait();
	        return;
	    }

	    System.out.println("Booking confirmed for: " + date + " at " + selectedTimeSlot + " (" + diners + ")");
	    //TODO Send data to server and db
	}
	
	/*
	 * Handles the back button click event to return to the Client Dashboard Screen.
	 * 
	 * @param event The event triggered by clicking the back button.
	 */
	@FXML
	void btnBack(Event event) {
	    System.out.println("Back button clicked.");
	    BistroClientGUI.switchScreen(event, "ClientDashboardScreen", "Error returning to Client Dashboard Screen.");
	}
}
