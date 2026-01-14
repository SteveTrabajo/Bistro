package gui.logic.staff;

import entities.Order;
import enums.OrderStatus;
import gui.logic.ClientNewReservationScreen;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import logic.BistroClientGUI;

import java.io.IOException;
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

    //TODO change into our actual logic: remove mock items and implement real logic
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
    	StaffWaitAndRes dialog = new StaffWaitAndRes(false);
        dialog.showAndWait().ifPresent(customerData -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/fxml/ClientNewReservationScreen.fxml")
                );
                Parent root = loader.load();
                
                ClientNewReservationScreen controller = loader.getController();
                controller.setBookingForCustomer(customerData);
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
                
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Could not load reservation screen.");
            }
        });
    }

    @FXML
    void onDateChanged(ActionEvent event) { 
    	loadData(); 
	}

    @FXML
    void btnRefresh(ActionEvent event) { 
    	loadData(); 
	}

    private void loadData() {
        LocalDate date = dateFilter.getValue();
        if (date == null) return;
        
        if (BistroClientGUI.client == null) {
            System.out.println("DEBUG: Preview Mode");
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

    
    @FXML
    void btnMarkSeated(ActionEvent event) {
        Order selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { 
        	showAlert("No Selection", "Please select a reservation."); 
        	return; 
        } 
        if (selected.getStatus() == OrderStatus.CANCELLED || selected.getStatus() == OrderStatus.COMPLETED) {
            showAlert("Invalid Action", "This reservation is already inactive.");
            return;
        }
        if (selected.getStatus() == OrderStatus.SEATED) {
            showAlert("Invalid Action", "This customer is already seated at a table");
            return;
       }
        
        if (BistroClientGUI.client != null) {
            //selected.setStatus(OrderStatus.SEATED);
            BistroClientGUI.client.getReservationCTRL().seatCustomer(selected.getConfirmationCode());
        }
        reservationsTable.refresh();
    }

    
    @FXML
    void btnCancelRes(ActionEvent event) {
        Order selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { 
        	showAlert("No Selection", "Please select a reservation."); 
        	return; 
    	}
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Are you sure you want to cancel reservation " + selected.getConfirmationCode() + "?", 
                ButtonType.YES, ButtonType.NO);
                
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES && BistroClientGUI.client != null) {
                    BistroClientGUI.client.getReservationCTRL().cancelReservation(selected.getConfirmationCode());
                    showAlert("Cancelled", "Cancellation request sent.");
                }
            });

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