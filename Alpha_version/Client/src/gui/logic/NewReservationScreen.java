package gui.logic;

import java.time.LocalDate;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
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
		//TODO highlight selected time slot
		//TODO - load available time slots from server based on selected date and diners amount
	}

	
	private void setupDinersAmountComboBox() {
		for (int i = 1; i <= 12; i++) {
			dinersAmountComboBox.getItems().add(i + " People");
		}
		dinersAmountComboBox.getSelectionModel().selectFirst();		
	}
	
	
	
	

}
