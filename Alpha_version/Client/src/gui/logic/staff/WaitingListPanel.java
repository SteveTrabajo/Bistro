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
		// In reality: get from WaitingListController
		updateQueueTitle();
		updateStats();
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
			if (order.getStatus() == OrderStatus.PENDING) {
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
		StaffWaitAndRes dialog = new StaffWaitAndRes(true);
        dialog.showAndWait().ifPresent(data -> {
            if (BistroClientGUI.client != null) {
                // The map contains: "diners", "customerType", "identifier", etc.
                BistroClientGUI.client.getWaitingListCTRL().addWalkIn(data);
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