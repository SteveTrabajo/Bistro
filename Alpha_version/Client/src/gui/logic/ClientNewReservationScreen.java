package gui.logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import entities.User;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import logic.BistroClientGUI;

/**
 * Controller class for the Client New Reservation Screen.
 * Handles user interactions for creating a new reservation.
 */
public class ClientNewReservationScreen {
	
	//***********************FXML Variables************************//
	
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
	@FXML
	private Label lblUser;
	
	private String selectedTimeSlot = null;
	
	private Map<String, Object> staffProxyData = null;
	
	//***********************FXML Methods************************//
	
	/*
	 * Initializes the New Reservation Screen by setting up the diners amount combo box and date picker.
	 */
	@FXML
    public void initialize() {
        setupDinersAmountComboBox();
        setupDatePicker();
        // Default Label Logic (Normal Client Mode)
        if (BistroClientGUI.client != null && BistroClientGUI.client.getUserCTRL().getLoggedInUser() != null) {
            User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
            if (lblUser != null) {
                lblUser.setText("Welcome, " + currentUser.getUserType().name());
            }
        }
        
        dinersAmountComboBox.valueProperty().addListener((obs, oldV, newV) -> refreshTimeSlots());
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> refreshTimeSlots());
        datePicker.setValue(LocalDate.now());
        btnConfirmReservation.setDisable(true);
    }
	
	/*
     * @param customerData Map containing "name", "identifier" (Phone/ID), and "customerType".
     */
    public void setBookingForCustomer(Map<String, Object> customerData) {
        this.staffProxyData = customerData;
        
        String name = (String) customerData.get("name");
        String id = (String) customerData.get("identifier");
        
        // UI update to show who the booking is for
        if (lblUser != null) {
            if (name != null && !name.isEmpty()) {
                lblUser.setText("Booking for: " + name);
            } else {
                lblUser.setText("Booking for Member: " + id);
            }
        }
    }
	
	
	/** 
	 * Refreshes the available time slots based on the selected date and number of diners.
	 * Sends a request to the server to fetch available hours.
	 */
	private void refreshTimeSlots() {
		// Safety Check
		if (timeSlotsGridPane == null) return;
	    LocalDate date = datePicker.getValue();
	    if (date == null) {
	    	timeSlotsGridPane.getChildren().clear();
	    	return;
	    }

	    int diners = parseDiners(dinersAmountComboBox.getValue());

	    //Clear grid immediately so user knows it's refreshing
	    timeSlotsGridPane.getChildren().clear(); 
	    selectedTimeSlot = null;
	    btnConfirmReservation.setDisable(true);

	    BistroClientGUI.client.getReservationCTRL().setUIUpdateListener((availableSlots) -> {
			Platform.runLater(() -> generateTimeSlots(availableSlots));
		});
	    BistroClientGUI.client.getReservationCTRL().askAvailableHours(date, diners);
	}

	/**
	 * Parses the number of diners from the combo box value.
	 * 
	 * @param value The combo box value (e.g., "2 People").
	 * @return The number of diners as an integer.
	 */
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
	
	/**
	 * Sets up the date picker to disable past dates.
	 */
	private void setupDatePicker() {
		datePicker.setDayCellFactory(picker -> new DateCell() {
	        @Override
	        public void updateItem(LocalDate date, boolean empty) {
	            super.updateItem(date, empty);
	            setDisable(empty || date.isBefore(LocalDate.now()) || date.isAfter(LocalDate.now().plusMonths(1)));
	        }
	    });
	}
	
	/**
	 * Method to setup diners amount combo box with values from 1 to 12.
	 */
	private void setupDinersAmountComboBox() {
		for (int i = 1; i <= 12; i++) {
			dinersAmountComboBox.getItems().add(i + " People");
		}
		dinersAmountComboBox.getSelectionModel().select(1); // Default "2 People"
	}
	
	/**
	 * Generates and displays available time slots in the grid pane.
	 * 
	 * @param availableTimeSlots A list of available time slots as strings.
	 */
	private void generateTimeSlots(List<String> availableTimeSlots) {
		if (timeSlotsGridPane == null) return;
		
	    timeSlotsGridPane.getChildren().clear(); // Clear existing buttons
	    
	    if (availableTimeSlots == null || availableTimeSlots.isEmpty()) {
			Label lblNoSlots = new Label("No time slots available for this date.");
			lblNoSlots.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
			timeSlotsGridPane.add(lblNoSlots, 0, 0);
			GridPane.setColumnSpan(lblNoSlots, 4);
			return;
		}
	    //initialize ToggleGroup and row/col counters
	    ToggleGroup timeSlotToggleGroup = new ToggleGroup();
	    int col = 0;
	    int row = 0;
	    //loop through available time slots and create buttons
	    for (String timeSlot : availableTimeSlots) {
	        ToggleButton timeSlotButton = new ToggleButton(timeSlot);
	        timeSlotButton.setToggleGroup(timeSlotToggleGroup);
	        timeSlotButton.setPrefWidth(104);
	        timeSlotButton.setPrefHeight(37);
	        timeSlotButton.getStyleClass().add("time-slot");
	        //event handler for button selection
	        timeSlotButton.setOnAction(event -> {
	            if (timeSlotButton.isSelected()) {
	                selectedTimeSlot = timeSlot;
	                btnConfirmReservation.setDisable(false);
	            } else {
	                selectedTimeSlot = null;
	                btnConfirmReservation.setDisable(true);
	            }
	        });
	        //add button to grid pane
	        timeSlotsGridPane.add(timeSlotButton, col, row);
	        col++;
	        if (col >= 4) { 
	            col = 0;
	            row++;
	        }
	    }
	}
	
	/**
	 * Handles the confirm reservation button click event.
	 * @param event
	 */
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
	    // logic to differentiate between Staff Mode and Normal Client Mode
        if (staffProxyData != null) {
            // staff mode
            String type = (String) staffProxyData.get("customerType");
            String id = (String) staffProxyData.get("identifier");
            String name = (String) staffProxyData.get("name");
            
            // Calls the specialized method in ReservationController
            BistroClientGUI.client.getReservationCTRL().createReservationAsStaff(date, LocalTime.parse(selectedTimeSlot), diners, type, id, name);
        } else {
            // normal client mode
            BistroClientGUI.client.getReservationCTRL().createNewReservation(
                date, selectedTimeSlot, diners
            );
        }
	}
	
	/*
	 * Handles the back button click event to return to the Client Dashboard Screen.
	 * 
	 * @param event The event triggered by clicking the back button.
	 */
	@FXML
	void btnBack(Event event) {
	    if (staffProxyData != null) {
	    	BistroClientGUI.switchScreen(event, "ReservationsPanel", "Error returning to Staff Reservations Panel");
	    } else {
	    	BistroClientGUI.switchScreen(event, "ClientDashboardScreen", "Error returning to Client Dashboard");
	    }
	}
	
}
