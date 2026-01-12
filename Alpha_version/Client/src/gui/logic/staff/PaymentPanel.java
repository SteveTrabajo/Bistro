package gui.logic.staff;

import java.util.List;
import java.util.stream.Collectors;

import entities.Bill;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import logic.BistroClientGUI;

public class PaymentPanel {

    @FXML
    private Label lblPendingBills, lblTodayRevenue, lblAvgBill, pendingTitleLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnRefreshList, btnPaymentReservation;
    @FXML
    private TableView<Bill> billsTable;
    @FXML
    private TableColumn<Bill, String> colTable, colCustomer, colMember, colCreated, colTotal;
    @FXML
    private TableColumn<Bill, Integer> colBillId;

    private final ObservableList<Bill> masterData = FXCollections.observableArrayList();
    private FilteredList<Bill> filteredData;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchLogic();
        refreshData();
    }

    private void setupTableColumns() {
        colBillId.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        
        colMember.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getUserType() != null ? cell.getValue().getUserType().toString() : "GUEST"));

        colCreated.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getDate() != null ? cell.getValue().getDate().toString() : "N/A"));

        colTotal.setCellValueFactory(cell -> 
            new SimpleStringProperty(String.format("₪%.2f", cell.getValue().getTotal())));

        colTable.setCellValueFactory(cell -> 
            new SimpleStringProperty("Table " + cell.getValue().getTableId()));

        billsTable.setPlaceholder(new Label("No occupied tables with pending bills."));
    }

    private void setupSearchLogic() {
        filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(bill -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String filter = newValue.toLowerCase();
                return String.valueOf(bill.getOrderNumber()).contains(filter)
                        || (bill.getConfirmationCode() != null && bill.getConfirmationCode().toLowerCase().contains(filter))
                        || String.valueOf(bill.getTableId()).contains(filter);
            });
        });

        SortedList<Bill> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(billsTable.comparatorProperty());
        billsTable.setItems(sortedData);
    }

    private void refreshData() {
        BistroClientGUI.client.getPaymentCTRL().loadPendingBills();
        BistroClientGUI.client.getTableCTRL().requestTableStatus();

        if (BistroClientGUI.client.getPaymentCTRL().isBillsLoaded()) {
            updateUI(BistroClientGUI.client.getPaymentCTRL().getPendingBills());
        } else {
            clearStats();
        }
    }

    public void updateUI(List<Bill> allBills) {
        if (allBills == null) return;

        List<Bill> activeBills = allBills.stream()
                .filter(bill -> isTableOccupied(bill.getTableId()))
                .collect(Collectors.toList());

        int count = activeBills.size();
        double totalRevenue = activeBills.stream().mapToDouble(Bill::getTotal).sum();
        double avg = count > 0 ? totalRevenue / count : 0;

        Platform.runLater(() -> {
            masterData.setAll(activeBills);
            lblPendingBills.setText(String.valueOf(count));
            lblTodayRevenue.setText(String.format("₪%.2f", totalRevenue));
            lblAvgBill.setText(String.format("₪%.2f", avg));
            pendingTitleLabel.setText("Active Table Bills (" + count + ")");
        });
    }

    @FXML
    void btnPaymentReservation(Event event) {
        Bill selectedBill = billsTable.getSelectionModel().getSelectedItem();
        if (selectedBill == null) {
            showAlert("Selection Required", "Please select a bill to process payment.", Alert.AlertType.WARNING);
            return;
        }
        processManualPayment(selectedBill);
    }

    private void processManualPayment(Bill bill) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Finalize Payment - Order #" + bill.getOrderNumber());
        dialog.setHeaderText("Processing payment for Order: " + bill.getConfirmationCode());

        ButtonType payButtonType = new ButtonType("Complete Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(payButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tableField = new TextField("Table " + bill.getTableId());
        tableField.setEditable(false); 
        tableField.setDisable(true);
        tableField.setMaxWidth(Double.MAX_VALUE);
        
        TextField requiredAmountField = new TextField(String.format("%.2f", bill.getTotal()));
        requiredAmountField.setEditable(false); 
        requiredAmountField.setDisable(true);
        requiredAmountField.setMaxWidth(Double.MAX_VALUE);
        
        TextField actualPaymentField = new TextField(String.format("%.2f", bill.getTotal()));
        actualPaymentField.setPromptText("Enter amount paid");
        actualPaymentField.setMaxWidth(Double.MAX_VALUE);
        
        ComboBox<String> paymentMethod = new ComboBox<>();
        paymentMethod.getItems().addAll("Credit Card", "Cash", "Member Balance");
        paymentMethod.setValue("Credit Card");
        paymentMethod.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Table Number:"), 0, 0);
        grid.add(tableField, 1, 0);
        grid.add(new Label("Required Total (₪):"), 0, 1);
        grid.add(requiredAmountField, 1, 1);
        grid.add(new Label("Amount Paid (₪):"), 0, 2);
        grid.add(actualPaymentField, 1, 2);
        grid.add(new Label("Payment Method:"), 0, 3);
        grid.add(paymentMethod, 1, 3);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(paymentMethod::requestFocus);

        Node payButton = dialog.getDialogPane().lookupButton(payButtonType);
        payButton.setDisable(true);
        Runnable validateInput = () -> {
            try {
                String method = paymentMethod.getValue();
                String amountText = actualPaymentField.getText();
                
                if (amountText == null || amountText.isEmpty() || method == null) {
                    payButton.setDisable(true);
                    return;
                }

                double enteredAmount = Double.parseDouble(amountText);
                boolean isAmountValid = enteredAmount >= bill.getTotal();
                
                // Button is enabled ONLY if amount is valid AND method is selected
                payButton.setDisable(!isAmountValid);
                
                // Visual feedback
                actualPaymentField.setStyle(!isAmountValid ? "-fx-border-color: red; -fx-text-fill: red;" : "");
                
            } catch (NumberFormatException e) {
                payButton.setDisable(true);
                actualPaymentField.setStyle("-fx-border-color: red;");
            }
        };

        dialog.setResultConverter(btn -> (btn == payButtonType) ? paymentMethod.getValue() : null);

        dialog.showAndWait().ifPresent(method -> {
            Parent rootNode = billsTable.getScene().getRoot();
            rootNode.setDisable(true);
            billsTable.setCursor(Cursor.WAIT);

            Thread updateThread = new Thread(() -> {
                try {
                    // Assuming your controller handles the 'Wait' internally 
                    BistroClientGUI.client.getPaymentCTRL().processPayment(bill.getOrderNumber());
                    boolean success = BistroClientGUI.client.getPaymentCTRL().getIsPaymentManuallySuccessful();

                    Platform.runLater(() -> {
                        if (success) {
                            showAlert("Success", "Payment processed via " + method, Alert.AlertType.INFORMATION);
                            refreshData();
                        } else {
                            showAlert("Error", "Server rejected the payment.", Alert.AlertType.ERROR);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Connection Error", e.getMessage(), Alert.AlertType.ERROR));
                } finally {
                    Platform.runLater(() -> {
                        rootNode.setDisable(false);
                        rootNode.setCursor(Cursor.DEFAULT);
                    });
                }
            });
            updateThread.setDaemon(true);
            updateThread.start();
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private boolean isTableOccupied(int tableId) {
        if (BistroClientGUI.client.getTableCTRL().getTableStatuses() == null) return false;
        return BistroClientGUI.client.getTableCTRL().getTableStatuses().keySet().stream()
                .anyMatch(t -> t.getTableID() == tableId && t.isOccupiedNow());
    }

    private void clearStats() {
        Platform.runLater(() -> {
            masterData.clear();
            lblPendingBills.setText("0");
            lblTodayRevenue.setText("₪0.00");
            lblAvgBill.setText("₪0.00");
            pendingTitleLabel.setText("Active Table Bills (0)");
        });
    }

    @FXML 
    void btnRefreshList(Event event) { refreshData(); searchField.clear(); }
}