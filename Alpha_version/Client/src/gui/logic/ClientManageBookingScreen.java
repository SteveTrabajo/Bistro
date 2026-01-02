package gui.logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import entities.Order;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import java.io.IOException;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
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
	@FXML
	private Hyperlink lnkForgot;
	@FXML
	private StackPane modalOverlay;
	@FXML
	private Pane mainPane;

	private String selectedTimeSlot = null;
	private Order currentOrder = null;
	private Parent forgotCodeRoot = null;

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

		BistroClientGUI.client.getReservationCTRL().setOrderLoadedListener(this::handleOrderFound);
		BistroClientGUI.client.getReservationCTRL().askOrderDetails(code.trim());
	}

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
		datePicker.setValue(order.getOrderDate());
		dinersAmountComboBox.setValue(order.getDinersAmount() + " People");
		refreshTimeSlots();
		setEditingEnabled(true);
	}

	// --- 2. UPDATE / CANCEL LOGIC ---

	@FXML
	void btnConfirmReservation(Event event) { 
		if (currentOrder == null)
			return;

		if (selectedTimeSlot == null) {
			showAlert(Alert.AlertType.WARNING, "Selection Missing", "Please select a time slot.");
			return;
		}

		LocalDate date = datePicker.getValue();
		LocalTime time = LocalTime.parse(selectedTimeSlot);
		int diners = parseDiners(dinersAmountComboBox.getValue());

		currentOrder.setOrderDate(date);
		currentOrder.setOrderHour(time);
		currentOrder.setDinersAmount(diners);
		
		BistroClientGUI.client.getReservationCTRL().setUpdateListener(success -> {
			Platform.runLater(() -> {
				if (success) {
					showAlert(Alert.AlertType.INFORMATION, "Success", "Reservation updated successfully!");
					BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Returning to Dashboard");
				} else {
					showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update reservation. The slot might be taken.");
				}
			});
		});
		
		BistroClientGUI.client.getReservationCTRL().updateReservation(currentOrder);
	}

	@FXML
	void btnCancel(Event event) { 
		if (currentOrder == null)
			return;

		BistroClientGUI.client.getReservationCTRL().setCancelListener(success -> {
			Platform.runLater(() -> {
				if (success) {
					showAlert(Alert.AlertType.INFORMATION, "Cancelled", "Reservation has been cancelled.");
					setEditingEnabled(false);
					txtConfirmationCode.clear();
					currentOrder = null;
					timeSlotsGridPane.getChildren().clear();
				} else {
					showAlert(Alert.AlertType.ERROR, "Error", "Could not cancel the reservation. Please try again.");
				}
			});
		});
		BistroClientGUI.client.getReservationCTRL().cancelReservation(currentOrder.getConfirmationCode());
	}

	@FXML
	void btnBack(Event event) {
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

		timeSlotsGridPane.getChildren().clear();
		selectedTimeSlot = null;
		btnConfirmReservation.setDisable(true);
		
		BistroClientGUI.client.getReservationCTRL().setAvailableTimeSlotsListener(this::generateTimeSlots);
		BistroClientGUI.client.getReservationCTRL().askAvailableHours(date, diners);
	}

	private void generateTimeSlots(List<String> availableTimeSlots) {
		Platform.runLater(() -> {
			timeSlotsGridPane.getChildren().clear();
			if (availableTimeSlots == null || availableTimeSlots.isEmpty()) {
				// Optional: Show a "No slots available" label
				Label noSlotsLabel = new Label("No available time slots.");
				noSlotsLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-font-weight: bold;");
				timeSlotsGridPane.add(noSlotsLabel, 0, 0);
				GridPane.setColumnSpan(noSlotsLabel, 4);
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
		});
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

	@FXML
	private void lnkForgot(Event event) {
		if (forgotCodeRoot==null) {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/fxml/ClientForgotConfirmationCode.fxml"));
				forgotCodeRoot = loader.load();
				modalOverlay.getChildren().add(forgotCodeRoot);
			} catch (IOException e) {
				e.printStackTrace();
				showAlert(Alert.AlertType.ERROR, "Error", "Unable to open Forgot Booking Code screen.");
				return;
			}
		}
		
		modalOverlay.setVisible(true);
		modalOverlay.setManaged(true);
		
		mainPane.setEffect(new GaussianBlur(15));
	}
}

