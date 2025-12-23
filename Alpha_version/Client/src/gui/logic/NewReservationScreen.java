package gui.logic;

import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

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
	
	@FXML
	public void initialize() {
		setupDinersAmountComboBox();
		setupDatePicker();
		
		
		generateTimeSlots(generateDefaultTimeSlots());
	}

	
	private void setupDatePicker() {
		datePicker.setValue(java.time.LocalDate.now());
		
		// code to prevent selecting past dates
		datePicker.setDayCellFactory(picker -> new DateCell() {
	        @Override
	        public void updateItem(LocalDate date, boolean empty) {
	            super.updateItem(date, empty);
	            setDisable(empty || date.isBefore(LocalDate.now()));
	        }
	    });
		
		//TODO - add listener to update available time slots when date changes
		datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
	        System.out.println("Date changed to: " + newDate);
	        //TODO - load available time slots from server based on selected date and diners amount
	        generateTimeSlots(generateDefaultTimeSlots());
	        });
		//TODO highlight selected time slot
		//TODO - load available time slots from server based on selected date and diners amount
	}

	
	private void setupDinersAmountComboBox() {
		for (int i = 1; i <= 12; i++) {
			dinersAmountComboBox.getItems().add(i + " People");
		}
		dinersAmountComboBox.getSelectionModel().selectFirst();		
	}
	
	
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
				selectedTimeSlot = timeSlot;
				System.out.println("Selected time slot: " + selectedTimeSlot);
				//TODO - highlight selected button
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
	
	@FXML
	void onConfirmClick(ActionEvent event) {
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
}
