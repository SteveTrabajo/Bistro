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
		dinersAmountComboBox.valueProperty().addListener((obs, oldV, newV) -> refreshTimeSlots());
		datePicker.valueProperty().addListener((obs, oldDate, newDate) -> refreshTimeSlots());
		datePicker.setValue(LocalDate.now());// Set default date to today
	}
	
	
	private void refreshTimeSlots() {
	    LocalDate date = datePicker.getValue();
	    if (date == null) return;
	    int diners = parseDiners(dinersAmountComboBox.getValue());
	    
	    // 1. Clear grid immediately so user knows it's refreshing
	    timeSlotsGridPane.getChildren().clear(); 
	    
	    // 2. Register the method reference (handles the callback)
	    BistroClientGUI.client.getReservationCTRL().setUIUpdateListener(this::generateTimeSlots);
	    
	    // 3. Send Request
	    BistroClientGUI.client.getReservationCTRL().askAvailableHours(date, diners);
	}

	
	private int parseDiners(String value) {
		if (value != null && value.contains(" ")) {
			String numberPart = value.split(" ")[0];
			try {
				return Integer.parseInt(numberPart);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return 2; // Default to 2
	}

	private void setupDatePicker() {
		datePicker.setDayCellFactory(picker -> new DateCell() {
	        @Override
	        public void updateItem(LocalDate date, boolean empty) {
	            super.updateItem(date, empty);
	            setDisable(empty || date.isBefore(LocalDate.now()));
	        }
	    });
	}

	private void setupDinersAmountComboBox() {
		for (int i = 1; i <= 12; i++) {
			dinersAmountComboBox.getItems().add(i + " People");
		}
		dinersAmountComboBox.getSelectionModel().select(1); // Default "2 People"
	}
	
	private void generateTimeSlots(List<String> availableTimeSlots) {
	    if (availableTimeSlots == null) {
	        availableTimeSlots = new ArrayList<>();
	    }
	    timeSlotsGridPane.getChildren().clear();
	    
	    // Handle no available time slots case:
	    if (availableTimeSlots.isEmpty()) {
	        // TODO Optional: Add a label saying "No tables available"
	        //javafx.scene.control.Label noSlotsLabel = new javafx.scene.control.Label("No availability");
	        //noSlotsLabel.getStyleClass().add("error-label"); // or any style you have
	        //timeSlotsGridPane.add(noSlotsLabel, 0, 0);
	        return; 
	    }

	    ToggleGroup timeSlotToggleGroup = new ToggleGroup();
	    int col = 0;
	    int row = 0;

	    for (String timeSlot : availableTimeSlots) {
	        ToggleButton timeSlotButton = new ToggleButton(timeSlot);
	        timeSlotButton.setToggleGroup(timeSlotToggleGroup);
	        timeSlotButton.setPrefWidth(104);
	        timeSlotButton.setPrefHeight(37);
	        timeSlotButton.getStyleClass().add("time-slot");

	        timeSlotButton.setOnAction(event -> {
	            if (timeSlotButton.isSelected()) {
	                selectedTimeSlot = timeSlot;
	                btnConfirmReservation.setDisable(false);
	            } else {
	                selectedTimeSlot = null;
	                btnConfirmReservation.setDisable(true);
	            }
	        });

	        timeSlotsGridPane.add(timeSlotButton, col, row);
	        col++;
	        if (col >= 4) { 
	            col = 0;
	            row++;
	        }
	    }
	}
	
	@FXML
	void btnConfirmReservation(Event event) {
	    LocalDate date = datePicker.getValue();
	    String dinersStr = dinersAmountComboBox.getValue();
	    int diners = parseDiners(dinersStr);
	    
	    if (selectedTimeSlot == null) {
	        Alert alert = new Alert(Alert.AlertType.WARNING);
	        alert.setContentText("Please select a time slot.");
	        alert.showAndWait();
	        return;
	    }
	    
	    BistroClientGUI.client.getReservationCTRL().createNewReservation(date, selectedTimeSlot, diners);
	    
	    // TODO maybe add a loading icon / indicator?
	    String reservationCode = BistroClientGUI.client.getReservationCTRL().getConfirmationCode();
	    
	    if(reservationCode != null && !reservationCode.isEmpty()) { 
	    	BistroClientGUI.switchScreen(event, "clientNewReservationCreatedScreen", "Reservation Confirmed");
	    } else {
	    	// Handle failure (Optional: show error alert)
	    	System.out.println("Reservation failed or waiting for server...");
	    	// For now, let's switch anyway so you can see the screen, 
	    	// but in production, you should wait for the REPLY_CREATE_RESERVATION_OK message
	    	BistroClientGUI.switchScreen(event, "clientNewReservationCreatedScreen", "Reservation Failed");
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
	
	/*
	// Temporary helper to fake data until you connect your Server
	private List<String> generateDefaultTimeSlots() {
	    List<String> times = new ArrayList<>();
	    times.add("11:00"); times.add("11:30");
	    times.add("12:00"); times.add("12:30");
	    times.add("13:00"); times.add("13:30");
	    times.add("18:00"); times.add("18:30");
	    return times;
	}*/
}
