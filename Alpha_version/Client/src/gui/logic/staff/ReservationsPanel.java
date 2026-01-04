package gui.logic.staff;

import entities.Order;
import enums.OrderStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import logic.BistroClientGUI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationsPanel {

    @FXML 
    private DatePicker dateFilter;
    @FXML 
    private TextField txtSearch;
    @FXML 
    private Button btnRefresh;
    @FXML 
    private Button btnMarkSeated;
    @FXML 
    private Button btnCancelRes;
    @FXML 
    private Button btnNewReservation;

    @FXML 
    private TableView<Order> reservationsTable;
    
    // Columns
    @FXML 
    private TableColumn<Order, LocalDate> colDate;
    @FXML 
    private TableColumn<Order, LocalTime> colTime;
    @FXML 
    private TableColumn<Order, Integer> colOrderId;
    @FXML 
    private TableColumn<Order, Integer> colCustomerType; 
    @FXML 
    private TableColumn<Order, String> colConfirm;
    @FXML 
    private TableColumn<Order, Integer> colDiners;
    @FXML 
    private TableColumn<Order, Void> colTable; 
    @FXML 
    private TableColumn<Order, OrderStatus> colStatus;

    private ObservableList<Order> masterData = FXCollections.observableArrayList();
    private FilteredList<Order> filteredData;

    @FXML
    public void initialize() {
        setupColumns();

        filteredData = new FilteredList<>(masterData, p -> true);
        
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(order -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (String.valueOf(order.getOrderNumber()).contains(lowerCaseFilter)) return true;
                if (order.getConfirmationCode() != null && order.getConfirmationCode().toLowerCase().contains(lowerCaseFilter)) return true;
                if (String.valueOf(order.getUserId()).contains(lowerCaseFilter)) return true;

                return false; 
            });
        });

        SortedList<Order> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(reservationsTable.comparatorProperty());
        reservationsTable.setItems(sortedData);

        dateFilter.setValue(LocalDate.now());
        
        loadData();
    }

    private void setupColumns() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        
        colTime.setCellValueFactory(new PropertyValueFactory<>("orderHour"));
        colTime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        });

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colConfirm.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colDiners.setCellValueFactory(new PropertyValueFactory<>("dinersAmount"));

        // TODO change into our actual logic: 4. Customer Type (Logic: If UserID > 0 it's a member, else Guest. Adjust based on your real logic)
        colCustomerType.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colCustomerType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer userId, boolean empty) {
                super.updateItem(userId, empty);
                if (empty || userId == null) {
                    setText(null);
                } else {
                	// TODO change into our actual logic: 
                    // MOCK LOGIC: You can adjust this threshold or check a specific field
                    if (userId > 5000) setText("Member"); 
                    else setText("Guest");
                }
            }
        });

        colTable.setCellFactory(col -> new TableCell<>() {
            @Override 
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    Order order = getTableView().getItems().get(getIndex());
                    if (order.getStatus() == OrderStatus.SEATED) {
                    	// TODO change into our actual logic: 
                        // In reality, you'd get order.getTableId(), but Order entity is missing it.
                        // Showing dummy data for visual confirmation
                        setText("T-" + (order.getOrderNumber() % 20 + 1)); 
                    } else {
                        setText("-");
                    }
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override 
            protected void updateItem(OrderStatus item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-seated", "status-pending", "status-cancelled", "status-completed");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    switch (item) {
                        case SEATED: getStyleClass().add("status-seated"); break;
                        case PENDING: getStyleClass().add("status-pending"); break;
                        case CANCELLED: getStyleClass().add("status-cancelled"); break;
                        case COMPLETED: getStyleClass().add("status-completed"); break;
                    }
                }
            }
        });
    }

    @FXML
    void btnNewReservation(ActionEvent event) {
        BistroClientGUI.switchScreen(event, "clientNewReservationScreen", "Error opening New Reservation");
    }

    @FXML
    void onDateChanged(ActionEvent event) { loadData(); }

    @FXML
    void btnRefresh(ActionEvent event) { loadData(); }

    private void loadData() {
        LocalDate date = dateFilter.getValue();
        if (date == null) return;
        
        if (BistroClientGUI.client == null) {
            System.out.println("DEBUG: Preview Mode");
            loadDummyData(); 
            return; 
        }

        BistroClientGUI.client.getReservationCTRL().setAllReservationsListener(this::updateTable);
        BistroClientGUI.client.getReservationCTRL().askReservationsByDate(date);
    }
    
    private void updateTable(List<Order> orders) {
        Platform.runLater(() -> {
            masterData.clear();
            if (orders != null) {
                masterData.addAll(orders);
            }
        });
    }

    private void loadDummyData() {
        masterData.clear();
        Order o1 = new Order(101, LocalDate.now(), LocalTime.of(18, 30), 4, "RES-998", 6001, null, OrderStatus.PENDING, LocalDate.now());
        Order o2 = new Order(102, LocalDate.now(), LocalTime.of(19, 00), 2, "RES-112", 205, null, OrderStatus.SEATED, LocalDate.now());
        masterData.addAll(o1, o2);
    }
    
    @FXML
    void btnMarkSeated(ActionEvent event) {
        Order selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select a reservation."); return; }
        if (BistroClientGUI.client != null) {
            selected.setStatus(OrderStatus.SEATED);
            BistroClientGUI.client.getReservationCTRL().updateReservation(selected);
        }
        reservationsTable.refresh();
    }

    @FXML
    void btnCancelRes(ActionEvent event) {
        Order selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select a reservation."); return; }
        if (BistroClientGUI.client != null) {
            BistroClientGUI.client.getReservationCTRL().cancelReservation(selected.getConfirmationCode()); 
        }
        showAlert("Cancelled", "Cancellation request sent.");
        loadData();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}