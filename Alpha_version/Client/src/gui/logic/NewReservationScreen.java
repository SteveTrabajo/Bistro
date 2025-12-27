package gui.logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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
import enums.*;

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
		setupDinersAmountComboBox(); //
		setupDatePicker();
		// Start with default time slots for local date and 1 diner:
		//TODO to check again
		dinersAmountComboBox.valueProperty().addListener((obs, oldV, newV) -> refreshTimeSlots());
		datePicker.valueProperty().addListener((obs, oldDate, newDate) -> refreshTimeSlots());
		datePicker.setValue(LocalDate.now());// Set default date to today
	}
	
	//TODO to change
	private void refreshTimeSlots() {
		LocalDate date = datePicker.getValue() , currentDate = LocalDate.now();
		int diners = parseDiners(dinersAmountComboBox.getValue());
		List<String> slots;
		BistroClientGUI.client.getReservationCTRL().askReservationsByDate(date); //ask server for reservations on that date
		//condition to check if the selected date is today
		if (date.isEqual(currentDate)) {
			
		}
		//TODO to change
		slots = BistroClientGUI.client.getReservationCTRL().receiveAvailableTimeSlots(LocalTime.now(), diners, date.isEqual(currentDate));
		selectedTimeSlot = null;
		btnConfirmReservation.setDisable(true);
		generateTimeSlots(slots);
	}

	
	//TODO to change
	private int parseDiners(String value) {
		if (value != null && value.contains(" ")) {
			String numberPart = value.split(" ")[0];
			try {
				return Integer.parseInt(numberPart);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return 1; // Default to 1 if parsing fails
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
		// change date for time slots:
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
		// Populate combo box with options from 1 to 12
		for (int i = 1; i <= 12; i++) {
			dinersAmountComboBox.getItems().add(i + " People");
		}
		dinersAmountComboBox.getSelectionModel().selectFirst(); // Select default value to 1 Person
		//when diners amount changes refresh time slots
		dinersAmountComboBox.valueProperty().addListener((obs, oldV, newV) -> {
		    System.out.println("Diners amount changed to: " + newV);
		    refreshTimeSlots();
		});
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
			//Ensure only one can be selected at a time
			timeSlotButton.setToggleGroup(timeSlotToggleGroup);
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
			// Add to grid
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
	    BistroClientGUI.client.getReservationCTRL().createNewReservation(date, selectedTimeSlot, parseDiners(diners));
	    if(!BistroClientGUI.client.getReservationCTRL().getConfirmationCode().isEmpty()) {
	    	System.out.println("Booking confirmed for: " + date + " at " + selectedTimeSlot + " (" + diners + ")");
		    BistroClientGUI.switchScreen(event, "clientNewReservationCreatedScreen", "Error loading Reservation Confirmation Screen.");
	    } 
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
