package gui.logic.staff;

import java.util.List;

import entities.UserData;
import enums.UserType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
    public void initialize() {
        // Load customer data from UserCTRL
        BistroClientGUI.client.getUserCTRL().loadCustomersData();

        if (BistroClientGUI.client.getUserCTRL().isCustomersDataLoaded()) {
            List<UserData> customersData = BistroClientGUI.client.getUserCTRL().getCustomersData();

            int totalCustomers = customersData.size();
            long membersCount = customersData.stream()
                    .filter(c -> c.getUserType() == UserType.MEMBER)
                    .count();
            int walkinsCount = totalCustomers - (int) membersCount;

            // Update UI safely
            Platform.runLater(() -> {
                totalCustomersLabel.setText(String.valueOf(totalCustomers));
                membersLabel.setText(String.valueOf(membersCount));
                walkinsLabel.setText(String.valueOf(walkinsCount));
                customersTable.setItems(FXCollections.observableArrayList(customersData));
            });
        }
    }

    // This method can be called from the network listener when new data arrives
    public void updateCustomers(List<UserData> customersData) {
        int totalCustomers = customersData.size();
        long membersCount = customersData.stream()
                .filter(c -> c.getUserType() == UserType.MEMBER)
                .count();
        int walkinsCount = totalCustomers - (int) membersCount;

        Platform.runLater(() -> {
            totalCustomersLabel.setText(String.valueOf(totalCustomers));
            membersLabel.setText(String.valueOf(membersCount));
            walkinsLabel.setText(String.valueOf(walkinsCount));
            customersTable.setItems(FXCollections.observableArrayList(customersData));
        });
    }
}
