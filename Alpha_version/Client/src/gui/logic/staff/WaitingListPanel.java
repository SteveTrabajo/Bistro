package gui.logic.staff;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import entities.*;
import enums.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
    private TableColumn<Order, String> colName;  // We might need to fetch User name separately or stick to ID
    @FXML 
    private TableColumn<Order, String> colMember; // Type
    @FXML 
    private TableColumn<Order, Integer> colParty; // Diners
    @FXML 
    private TableColumn<Order, LocalTime> colJoined; // Time
    @FXML
    private TableColumn<Order, OrderStatus> colStatus; // Status

    private 
    ObservableList<Order> waitingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
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
                    // Format nicely to remove seconds/nanoseconds
                    setText(item.format(DateTimeFormatter.ofPattern("HH:mm")));
                }
            }
        });
        
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<Order, OrderStatus>() {
            @Override
            protected void updateItem(OrderStatus item, boolean empty) {
                super.updateItem(item, empty);
                // Clear previous styles
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
            // In a real app, you'd check cellData.getValue().getUserId() against cached users
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
        // Request update from server
        // BistroClientGUI.client.getWaitingListCTRL().askWaitingList(); 
        
        // For now, load dummy data to verify UI
        waitingList.clear();
        
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
        waitingList.add(new Order(101, today, now.minusMinutes(45), 2, "WL-202", 5002, 
                OrderType.WAITLIST, OrderStatus.WAITING_LIST, today));

        // 2. A Guest notified to enter (30 mins ago)
        waitingList.add(new Order(102, today, now.minusMinutes(30), 4, "WL-305", 5003, 
                OrderType.WAITLIST, OrderStatus.NOTIFIED, today));

        // 3. Large party waiting (15 mins ago)
        waitingList.add(new Order(103, today, now.minusMinutes(15), 6, "WL-410", 5004, 
                OrderType.WAITLIST, OrderStatus.WAITING_LIST, today));

        // 4. Couple just arrived (5 mins ago)
        waitingList.add(new Order(104, today, now.minusMinutes(5), 2, "WL-550", 5005, 
                OrderType.WAITLIST, OrderStatus.WAITING_LIST, today));

        // 5. Another notified guest (2 mins ago)
        waitingList.add(new Order(105, today, now.minusMinutes(2), 3, "WL-600", 5001, 
                OrderType.WAITLIST, OrderStatus.NOTIFIED, today));
    }

    private void updateQueueTitle() {
        lblQueueTitleLabel.setText("Current Queue (" + waitingList.size() + ")");
        lblTotalInQueueLabel.setText(String.valueOf(waitingList.size()));
    }

    @FXML
    void btnAddToWaitlist(ActionEvent event) {
        // Open a dialog to add someone manually (Optional feature)
        showAlert("Manual Add", "Functionality to add walk-in to waitlist.");
    }
    
    // Called by Logic Controller when server sends update
    public void updateListFromServer(Map<String, Order> newMap) {
        Platform.runLater(() -> {
            waitingList.clear();
            waitingList.addAll(newMap.values());
            updateQueueTitle();
            updateStats();
        });
    }
    
    @FXML
    private void btnRemoveFromWaitlist(ActionEvent event) {
		Order selectedOrder = waitingTable.getSelectionModel().getSelectedItem();
		if (selectedOrder != null) {
			// Send request to server to remove from waitlist
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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}