package gui.logic.staff;

import java.util.List;
import entities.UserData;
import enums.UserType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import logic.BistroClientGUI;

public class CustomersPanel {

    @FXML
    public Label totalCustomersLabel;
    
    @FXML
    public Label membersLabel;
    
    @FXML
    public Label walkinsLabel;
    
    @FXML
    public TextField searchField;
    
    @FXML
    public Button btnRefresh;
    
    @FXML
    public Label directoryTitleLabel;
    
    @FXML
    public TableView<UserData> customersTable;

    // List wrappers for search and sort functionality
    private final ObservableList<UserData> masterData = FXCollections.observableArrayList();
    private FilteredList<UserData> filteredData;

    @FXML
    public void initialize() {
        setupSearchLogic();
        
        // Load initial data from controller
        BistroClientGUI.client.getUserCTRL().loadCustomersData();
        if (BistroClientGUI.client.getUserCTRL().isCustomersDataLoaded()) {
            updateCustomers(BistroClientGUI.client.getUserCTRL().getCustomersData());
        }
    }

    private void setupSearchLogic() {
        // 1. Wrap master list in a filtered list
        filteredData = new FilteredList<>(masterData, p -> true);

        // 2. Add listener to search field with YOUR specific fields
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                // If filter text is empty, display all users
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();
                
                // Check Full Name
                if (user.getName() != null && user.getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                
                // Check Email
                if (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                // Check Phone
                if (user.getPhone() != null && user.getPhone().contains(lowerCaseFilter)) {
                    return true;
                }

                // Check Member Code
                if (user.getMemberCode() != null && user.getMemberCode().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false; // Does not match
            });
        });

        // 3. Wrap in a sorted list so column sorting still works
        SortedList<UserData> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(customersTable.comparatorProperty());

        // 4. Bind the Sorted List to the Table
        customersTable.setItems(sortedData);
    }

    public void updateCustomers(List<UserData> customersData) {
        if (customersData == null) return;

        int total = customersData.size();
        long members = customersData.stream()
                .filter(c -> c.getUserType() == UserType.MEMBER)
                .count();
        int walkins = total - (int) members;

        // Update UI
        Platform.runLater(() -> {
            totalCustomersLabel.setText(String.valueOf(total));
            membersLabel.setText(String.valueOf(members));
            walkinsLabel.setText(String.valueOf(walkins));
            masterData.setAll(customersData);
        });
    }
}
