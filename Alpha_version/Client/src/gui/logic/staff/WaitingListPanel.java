package gui.logic.staff;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import common.InputCheck;
import entities.*;
import enums.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import logic.BistroClientGUI;

public class WaitingListPanel {

	@FXML
	private TextField txtSearchField;
	@FXML
	private Label lblQueueTitleLabel;
	@FXML
	private Label lblTotalInQueueLabel;
	@FXML
	private Label lblTotalWaitingLabel;
	@FXML
	private Label lblLongestWaitLabel;
	@FXML
	private Label lblTotalNotifiedLabel;
	@FXML
	private Button btnRemoveFromWaitlist;
	@FXML
	private Button btnAddToWaitlist;
	@FXML
	private Button btnRefresh;

	@FXML
	private TableView<Order> waitingTable;
	@FXML
	private TableColumn<Order, String> colQueue; // Confirmation Code
	@FXML
	private TableColumn<Order, String> colName; // We might need to fetch User name separately or stick to ID
	@FXML
	private TableColumn<Order, String> colMember; // Type
	@FXML
	private TableColumn<Order, Integer> colParty; // Diners
	@FXML
	private TableColumn<Order, LocalTime> colJoined; // Time
	@FXML
	private TableColumn<Order, OrderStatus> colStatus; // Status

	private ObservableList<Order> waitingList = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		setupTable();
		if (BistroClientGUI.client != null) {
			BistroClientGUI.client.getWaitingListCTRL().setGuiController(this);
		}
		loadData();
	}

	private void setupTable() {
		colQueue.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
		colParty.setCellValueFactory(new PropertyValueFactory<>("dinersAmount"));

		colJoined.setCellValueFactory(new PropertyValueFactory<>("orderHour"));
		colJoined.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(LocalTime item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(item.format(DateTimeFormatter.ofPattern("HH:mm")));
				}
			}
		});

		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
		colStatus.setCellFactory(column -> new TableCell<Order, OrderStatus>() {
			@Override
			protected void updateItem(OrderStatus item, boolean empty) {
				super.updateItem(item, empty);
				getStyleClass().removeAll("wl-chip-waiting", "wl-chip-called");

				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					setText(item.toString());

					// Apply CSS based on status
					if (item == OrderStatus.NOTIFIED) {
						getStyleClass().add("wl-chip-called"); // Orange/Red style
					} else {
						getStyleClass().add("wl-chip-waiting"); // Yellow/Amber style
					}
				}
			}
		});

		// Custom Factory for Member Type (Simulated based on ID for now)
		colMember.setCellValueFactory(cellData -> {
			// In a real app, you'd check cellData.getValue().getUserId() against cached
			// users
			return new SimpleStringProperty("Guest");
		});

		// Custom Factory for Name (Simulated)
		colName.setCellValueFactory(cellData -> {
			return new SimpleStringProperty("Customer " + cellData.getValue().getUserId());
		});

		waitingTable.setItems(waitingList);
	}

	@FXML
	void btnRefresh(ActionEvent event) {
		loadData();
	}

	private void loadData() {
		waitingList.clear();
		// TODO Request update from server (comment out dummy data when real data is
		// used)
		// if (BistroClientGUI.client != null) {
		// BistroClientGUI.client.getWaitingListCTRL().askWaitingList();
		// }

		// dummy data for testing
		loadDummyData();

		// In reality: get from WaitingListController
		updateQueueTitle();
		updateStats();
	}

	// Helper method to generate 5 fake entries
	private void loadDummyData() {
		LocalDate today = LocalDate.now();
		LocalTime now = LocalTime.now();

		// 1. A Member waiting for a while (45 mins ago)
		waitingList.add(new Order(101, today, now.minusMinutes(45), 2, "WL-202", 5002, OrderType.WAITLIST,
				OrderStatus.WAITING_LIST, today));

		// 2. A Guest notified to enter (30 mins ago)
		waitingList.add(new Order(102, today, now.minusMinutes(30), 4, "WL-305", 5003, OrderType.WAITLIST,
				OrderStatus.NOTIFIED, today));

		// 3. Large party waiting (15 mins ago)
		waitingList.add(new Order(103, today, now.minusMinutes(15), 6, "WL-410", 5004, OrderType.WAITLIST,
				OrderStatus.WAITING_LIST, today));

		// 4. Couple just arrived (5 mins ago)
		waitingList.add(new Order(104, today, now.minusMinutes(5), 2, "WL-550", 5005, OrderType.WAITLIST,
				OrderStatus.WAITING_LIST, today));

		// 5. Another notified guest (2 mins ago)
		waitingList.add(new Order(105, today, now.minusMinutes(2), 3, "WL-600", 5001, OrderType.WAITLIST,
				OrderStatus.NOTIFIED, today));
	}

	private void updateQueueTitle() {
		lblQueueTitleLabel.setText("Current Queue (" + waitingList.size() + ")");
		lblTotalInQueueLabel.setText(String.valueOf(waitingList.size()));
	}

	// Called by Logic Controller when server sends update
	public void updateListFromServer(List<Order> newList) {
		Platform.runLater(() -> {
			waitingList.clear();
			waitingList.addAll(newList);
			updateQueueTitle();
			updateStats();
		});
	}

	@FXML
	private void btnRemoveFromWaitlist(ActionEvent event) {
		Order selectedOrder = waitingTable.getSelectionModel().getSelectedItem();
		if (selectedOrder != null) {
			// TODO uncomment next line when backend is ready
			// BistroClientGUI.client.getWaitingListCTRL().removeFromWaitingList(selectedOrder.getConfirmationCode());
			showAlert("Remove from Waitlist", "Requested removal of order: " + selectedOrder.getConfirmationCode());
		} else {
			showAlert("No Selection", "Please select an order to remove from the waitlist.");
		}
	}

	private void updateStats() {
		int waitingCount = 0;
		int notifiedCount = 0;

		// Loop through the current list to count statuses
		for (Order order : waitingList) {
			if (order.getStatus() == OrderStatus.WAITING_LIST) {
				waitingCount++;
			} else if (order.getStatus() == OrderStatus.NOTIFIED) {
				notifiedCount++;
			}
		}

		// Update the Labels (Check for null to prevent crashes if ID is missing)
		if (lblTotalWaitingLabel != null) {
			lblTotalWaitingLabel.setText(String.valueOf(waitingCount));
		}

		if (lblTotalNotifiedLabel != null) {
			lblTotalNotifiedLabel.setText(String.valueOf(notifiedCount));
		}
	}

	@FXML
	void btnAddToWaitlist(ActionEvent event) {
		// 1. Create the Dialog
		Dialog<Map<String, Object>> dialog = new Dialog<>();
		dialog.setTitle("Add to Waitlist");
		dialog.setHeaderText("New Walk-in Entry");

		// 2. Set the button types (Add and Cancel)
		ButtonType loginButtonType = new ButtonType("Add to List", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

		// 3. Create the layout
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		// 4. Create Controls

		// Type Selection
		ToggleGroup group = new ToggleGroup();
		RadioButton rbGuest = new RadioButton("Guest");
		rbGuest.setToggleGroup(group);
		rbGuest.setSelected(true); // Default
		RadioButton rbMember = new RadioButton("Member");
		rbMember.setToggleGroup(group);

		// Guest Fields
		TextField txtPhone = new TextField();
		txtPhone.setPromptText("Phone Number");
		TextField txtEmail = new TextField();
		txtEmail.setPromptText("Email (Optional)");

		// Member Fields
		TextField txtMemberId = new TextField();
		txtMemberId.setPromptText("Member ID / Code");

		// Diners ComboBox (1-12)
		ComboBox<Integer> cmbDiners = new ComboBox<>();
		for (int i = 1; i <= 12; i++)
			cmbDiners.getItems().add(i);
		cmbDiners.setValue(2); // Default

		// 5. Layout Logic (Dynamic Switching)
		grid.add(new Label("Customer Type:"), 0, 0);
		grid.add(rbGuest, 1, 0);
		grid.add(rbMember, 2, 0);

		Label lblField1 = new Label("Phone:");
		Label lblField2 = new Label("Email:");

		grid.add(lblField1, 0, 1);
		grid.add(txtPhone, 1, 1, 2, 1); // Span 2 cols

		grid.add(lblField2, 0, 2);
		grid.add(txtEmail, 1, 2, 2, 1);

		grid.add(new Label("Diners:"), 0, 3);
		grid.add(cmbDiners, 1, 3);

		// Listener to swap fields based on selection
		group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
			grid.getChildren().removeAll(txtPhone, txtEmail, txtMemberId, lblField1, lblField2);

			if (rbMember.isSelected()) {
				lblField1.setText("Member ID:");
				lblField2.setText(""); // Hide second label
				grid.add(lblField1, 0, 1);
				grid.add(txtMemberId, 1, 1, 2, 1);
			} else {
				lblField1.setText("Phone:");
				lblField2.setText("Email:");
				grid.add(lblField1, 0, 1);
				grid.add(txtPhone, 1, 1, 2, 1);
				grid.add(lblField2, 0, 2);
				grid.add(txtEmail, 1, 2, 2, 1);
			}
		});

		dialog.getDialogPane().setContent(grid);

		// 6. Convert result to Map when "Add" is clicked
		Button btnOk = (Button) dialog.getDialogPane().lookupButton(loginButtonType);

		btnOk.addEventFilter(ActionEvent.ACTION, ae -> {
			// A. Gather data
			boolean isMember = rbMember.isSelected();
			String memId = txtMemberId.getText();
			String phone = txtPhone.getText();
			String email = txtEmail.getText();

			// B. Use your shared InputCheck class
			String errorMsg = InputCheck.validateWalkIn(isMember, memId, phone, email);

			// C. Check result
			if (!errorMsg.isEmpty()) {
				// CONSUME the event -> Prevents the dialog from closing
				ae.consume();
				showAlert("Invalid Input", errorMsg);
			}
			// If errorMsg is empty, we do nothing, allowing the dialog to close normally
		});

		// 7. Convert result to Map (Only happens if validation passed)
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == loginButtonType) {
				Map<String, Object> req = new java.util.HashMap<>();
				req.put("diners", cmbDiners.getValue());

				if (rbMember.isSelected()) {
					req.put("type", "MEMBER");
					req.put("memberId", txtMemberId.getText().trim());
				} else {
					req.put("type", "GUEST");
					req.put("phone", txtPhone.getText().trim());
					req.put("email", txtEmail.getText().trim());
				}
				return req;
			}
			return null;
		});

		// 8. Show and Send
		dialog.showAndWait().ifPresent(requestMap -> {
			if (BistroClientGUI.client != null) {
				BistroClientGUI.client.getWaitingListCTRL().addWalkIn(requestMap);
			}
		});
	}

	private void showAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}