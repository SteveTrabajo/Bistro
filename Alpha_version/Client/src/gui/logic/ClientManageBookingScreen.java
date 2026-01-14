package gui.logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import entities.Order;
import enums.OrderStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import logic.BistroClientGUI;

public class ClientManageBookingScreen {

    // ****************************** FXML Variables ******************************
    @FXML 
    private Button btnBack;
    @FXML 
    private Button btnNewReservation;
    @FXML 
    private Button btnCancelRes;
    @FXML 
    private Button btnRefresh;
    
    @FXML 
    private DatePicker dateFilter;
    
    @FXML 
    private TableView<Order> reservationsTable;
    @FXML 
    private TableColumn<Order, LocalDate> colDate;
    @FXML 
    private TableColumn<Order, LocalTime> colTime;
    @FXML 
    private TableColumn<Order, String> colConfirm;
    @FXML 
    private TableColumn<Order, Integer> colDiners;
    @FXML 
    private TableColumn<Order, String> colStatus;
    @FXML 
    private TableColumn<Order, Integer> colTable;

    private ObservableList<Order> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ****************************** Initialization ******************************
    @FXML
    public void initialize() {
        setupTableColumns();
        // Default filter: Today
        dateFilter.setValue(LocalDate.now());
        // Listener for date picker to filter the table
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterTable(newVal));

        loadData();
    }

    private void setupTableColumns() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("orderHour"));
        colConfirm.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colDiners.setCellValueFactory(new PropertyValueFactory<>("dinersAmount"));
        colTable.setCellValueFactory(new PropertyValueFactory<>("tableId"));

        // Date Formatter: dd/mm/yyyy
        colDate.setCellFactory(column -> new TableCell<Order, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(dateFormatter.format(date));
                }
            }
        });

        colStatus.setCellValueFactory(cellData -> {
            if (cellData.getValue().getStatus() != null) {
                return new SimpleStringProperty(cellData.getValue().getStatus().toString());
            }
            return new SimpleStringProperty("");
        });
        
        // Hide table value if 0
        colTable.setCellFactory(column -> new TableCell<Order, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                }
            }
        });
    }

    /**
     * Helper to force table sort by Date and then Time.
     */
    private void applyDefaultSort() {
        reservationsTable.getSortOrder().clear();
        reservationsTable.getSortOrder().add(colDate);
        reservationsTable.getSortOrder().add(colTime);
        colDate.setSortType(TableColumn.SortType.ASCENDING);
        colTime.setSortType(TableColumn.SortType.ASCENDING);
        reservationsTable.sort();
    }

    private void loadData() {
        if (BistroClientGUI.client != null) {
            BistroClientGUI.client.getReservationCTRL().setAllReservationsListener(this::updateTable);
            BistroClientGUI.client.getReservationCTRL().askClientOrderHistory();
        }
    }

    private void updateTable(List<Order> orders) {
        Platform.runLater(() -> {
            masterData.clear();
            if (orders != null) {
                masterData.addAll(orders);
            }
            filterTable(dateFilter.getValue());
        });
    }

    private void filterTable(LocalDate fromDate) {
        if (fromDate == null) {
            reservationsTable.setItems(masterData);
        } else {
            List<Order> filtered = masterData.stream()
                    .filter(o -> o.getOrderDate() != null && !o.getOrderDate().isBefore(fromDate))
                    .collect(Collectors.toList());
            reservationsTable.setItems(FXCollections.observableArrayList(filtered));
        }
        
        applyDefaultSort();
    }

    // ****************************** Actions ******************************

    @FXML
    void btnNewReservation(ActionEvent event) {
        BistroClientGUI.switchScreen(event, "clientNewReservationScreen", "New Reservation");
    }

    @FXML
    void btnCancelRes(ActionEvent event) {
        Order selected = reservationsTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showAlert("No Selection", "Please select a reservation to cancel.");
            return;
        }
        
        if (selected.getStatus() == OrderStatus.COMPLETED || selected.getStatus() == OrderStatus.CANCELLED) {
            showAlert("Invalid Action", "You cannot cancel a past or already cancelled reservation.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to cancel reservation " + selected.getConfirmationCode() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            if (BistroClientGUI.client != null) {
                BistroClientGUI.client.getReservationCTRL().cancelReservation(selected.getConfirmationCode());
                selected.setStatus(OrderStatus.CANCELLED);
                reservationsTable.refresh();
                showAlert("Success", "Cancellation request sent.");
            }
        }
    }

    @FXML
    void btnRefresh(ActionEvent event) {
        loadData();
    }

    @FXML
    public void btnBack(Event event) {
        BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Dashboard");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}