package gui.logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import entities.Order;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import logic.BistroClientGUI;

public class ClientManageBookingScreen {
	// ****************************** FXML Variables ******************************
	@FXML
	private Button btnCheck;
	@FXML
	private Button btnBack;
	@FXML
	private Button btnCancel;
	@FXML
	private DatePicker datePicker;
	@FXML
	private ComboBox<String> dinersAmountComboBox;
	@FXML
	private GridPane timeSlotsGridPane;
	@FXML
	private TextField txtConfirmationCode;
	@FXML
	private Button btnConfirmReservation;

	private String selectedTimeSlot = null;
	private Order currentOrder = null;

	// *************************** FXML Methods ****************************

	/**
	 * Initializes the Client Manage Booking screen.
	 */
	@FXML
	public void initialize() {
		setupDinersAmountComboBox();
		setupDatePicker();

		setEditingEnabled(false);
	}

	@FXML
	void btnCheck(Event event) {
		String code = txtConfirmationCode.getText();
		if (code == null || code.trim().isEmpty()) {
			showAlert(Alert.AlertType.WARNING, "Input Error", "Please enter a confirmation code.");
			return;
		}

		// 1. Register a callback to handle the server response asynchronously
		// This ensures we wait for the data before trying to display it
		BistroClientGUI.client.getReservationCTRL().setOrderLoadedListener(this::handleOrderFound);

		// 2. Send request to server
		BistroClientGUI.client.getReservationCTRL().askOrderDetails(code.trim());
	}

	// Callback method: This runs when the server replies with the order
	private void handleOrderFound(Order order) {
		Platform.runLater(() -> {
			if (order == null) {
				showAlert(Alert.AlertType.ERROR, "Not Found", "No order found with that confirmation code.");
				setEditingEnabled(false);
			} else {
				currentOrder = order;
				loadOrderIntoUI(order);
			}
		});
	}

	private void loadOrderIntoUI(Order order) {
		// Populate fields with order data
		datePicker.setValue(order.getOrderDate());
		dinersAmountComboBox.setValue(order.getDinersAmount() + " People");

		// Fetch valid slots for this date/size
		refreshTimeSlots();

		// Try to visually select the order's existing time
		String timeStr = order.getOrderHour().toString();
		// (Simple formatting fix if needed, e.g., "17:00:00" -> "17:00")
		if (timeStr.length() > 5)
			timeStr = timeStr.substring(0, 5);

		selectTimeSlotInGrid(timeStr);

		setEditingEnabled(true);
	}

	// --- 2. UPDATE / CANCEL LOGIC ---

	@FXML
	void onConfirm(Event event) { // Matches FXML onAction="#onConfirm"
		if (currentOrder == null)
			return;

		if (selectedTimeSlot == null) {
			showAlert(Alert.AlertType.WARNING, "Selection Missing", "Please select a time slot.");
			return;
		}

		LocalDate date = datePicker.getValue();
		int diners = parseDiners(dinersAmountComboBox.getValue());

		// Call controller to update (You will need to implement updateReservation in
		// Reservation_Controller)
		// BistroClientGUI.client.getReservationCTRL().updateReservation(currentOrder.getOrderNumber(),
		// date, selectedTimeSlot, diners);

		showAlert(Alert.AlertType.INFORMATION, "Success", "Reservation updated successfully!");
		BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Returning to Dashboard");
	}

	@FXML
	void btnCancel(Event event) { // Matches FXML onAction="#btnCancel" (check FXML method name)
		if (currentOrder == null)
			return;

		// Call controller to cancel
		// BistroClientGUI.client.getReservationCTRL().cancelReservation(currentOrder.getOrderNumber());

		showAlert(Alert.AlertType.INFORMATION, "Cancelled", "Reservation has been cancelled.");
		setEditingEnabled(false);
		txtConfirmationCode.clear();
		currentOrder = null;
		timeSlotsGridPane.getChildren().clear();
	}

	@FXML
	void btnBack(Event event) { // Matches FXML onAction="#onBack"
		BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Error returning to dashboard");
	}

	// --- 3. HELPER METHODS ---

	private void setEditingEnabled(boolean enable) {
		datePicker.setDisable(!enable);
		dinersAmountComboBox.setDisable(!enable);
		timeSlotsGridPane.setDisable(!enable);
		btnConfirmReservation.setDisable(!enable);
		btnCancel.setDisable(!enable);
	}

	private void refreshTimeSlots() {
		LocalDate date = datePicker.getValue();
		if (date == null)
			return;
		int diners = parseDiners(dinersAmountComboBox.getValue());

		// In a real scenario, you would use the async callback here too (like in
		// NewReservationScreen)
		// For now, I'll use the default generator so the grid isn't empty
		generateTimeSlots(generateDefaultTimeSlots());
	}

	private void generateTimeSlots(List<String> availableTimeSlots) {
		timeSlotsGridPane.getChildren().clear();
		ToggleGroup timeSlotToggleGroup = new ToggleGroup();
		int col = 0;
		int row = 0;

		LocalDate selectedDate = datePicker.getValue();
		LocalDate today = LocalDate.now();
		LocalTime currentTime = LocalTime.now();
		LocalTime thresholdTime = currentTime.plusHours(1);
		
		for (String timeSlot : availableTimeSlots) {
			// Filter out past or too-soon time slots if the selected date is today
			if (selectedDate != null && selectedDate.equals(today)) {
				try {
					LocalTime slotTime = LocalTime.parse(timeSlot);
					if (slotTime.isBefore(thresholdTime)) {
						continue; // Skip this time slot
					}
				} catch (Exception e) {
					// If parsing fails, include the slot anyway
				}
			}
			
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
			if (col >= 3) {
				col = 0;
				row++;
			}
		}
	}

	private void selectTimeSlotInGrid(String timeToSelect) {
		for (javafx.scene.Node node : timeSlotsGridPane.getChildren()) {
			if (node instanceof ToggleButton) {
				ToggleButton btn = (ToggleButton) node;
				if (btn.getText().equals(timeToSelect)) {
					btn.setSelected(true);
					selectedTimeSlot = timeToSelect;
					return;
				}
			}
		}
	}

	private void showAlert(Alert.AlertType type, String title, String content) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	// --- SETUP & PARSING ---

	private void setupDatePicker() {
		datePicker.setDayCellFactory(picker -> new DateCell() {
			@Override
			public void updateItem(LocalDate date, boolean empty) {
				super.updateItem(date, empty);
				LocalDate today = LocalDate.now();
				LocalDate maxDate = today.plusMonths(1);
				setDisable(empty || date.isBefore(today) || date.isAfter(maxDate));
			}
		});
		datePicker.valueProperty().addListener((obs, old, newVal) -> refreshTimeSlots());
	}

	private void setupDinersAmountComboBox() {
		for (int i = 1; i <= 12; i++) {
			dinersAmountComboBox.getItems().add(i + " People");
		}
		dinersAmountComboBox.getSelectionModel().selectFirst();
		dinersAmountComboBox.valueProperty().addListener((obs, old, newVal) -> refreshTimeSlots());
	}

	private int parseDiners(String value) {
		if (value != null && value.contains(" ")) {
			try {
				return Integer.parseInt(value.split(" ")[0]);
			} catch (Exception e) {
				return 2;
			}
		}
		return 2;
	}

	// Temporary helper until server is connected for slots
	private List<String> generateDefaultTimeSlots() {
		List<String> times = new ArrayList<>();
		times.add("18:00");
		times.add("18:30");
		times.add("19:00");
		return times;
	}

}