package gui.logic.staff;

import entities.Order;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import logic.BistroClientGUI;

public class PaymentPanel {

    @FXML 
    private Label pendingTitleLabel;
    @FXML 
    private TableView<Order> billsTable;
    @FXML 
    private TableColumn<Order, String> colTable; // We need to calculate this
    @FXML 
    private TableColumn<Order, Integer> colBillId;
    @FXML 
    private TableColumn<Order, String> colCustomer; // Confirmation Code
    @FXML 
    private TableColumn<Order, String> colMember;   // User ID/Name
    @FXML 
    private TableColumn<Order, String> colAmount;   // Need price in Order or separate Bill entity
    @FXML 
    private TableColumn<Order, String> colStatus;

    @FXML 
    private Button btnRefreshList;
    @FXML 
    private Button btnPaymentReservation;

    private ObservableList<Order> pendingBills = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colBillId.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colMember.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Mocking Table Column (Since Order doesn't have tableID directly in your entity yet)
        colTable.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty("-")); 
        
        billsTable.setItems(pendingBills);
    }

    @FXML
    void btnRefreshList(ActionEvent event) {
        loadData();
    }

    private void loadData() {
        // Logic to ask server for all UNPAID orders
        // BistroClientGUI.client.getPaymentCTRL().askPendingBills();
        pendingTitleLabel.setText("Pending Reservation Bills (" + pendingBills.size() + ")");
    }

    @FXML
    void btnPaymentReservation(ActionEvent event) {
        Order selected = billsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a bill to process.");
            return;
        }
        
        // Process manual payment
        // BistroClientGUI.client.getPaymentCTRL().processStaffPayment(selected.getOrderNumber());
        showAlert("Success", "Payment processed for Order #" + selected.getOrderNumber());
        pendingBills.remove(selected);
        updateTitle();
    }
    
    private void updateTitle() {
        pendingTitleLabel.setText("Pending Reservation Bills (" + pendingBills.size() + ")");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}