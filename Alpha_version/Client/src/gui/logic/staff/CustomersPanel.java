package gui.logic.staff;

import java.util.List;

import common.InputCheck;
import entities.UserData;
import enums.UserType;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
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

	@FXML
	public TableColumn<UserData, String> colFullName;

	@FXML
	public TableColumn<UserData, String> colEmail;

	@FXML
	public TableColumn<UserData, String> colPhone;

	@FXML
	public TableColumn<UserData, String> colMemberCode;

	@FXML
	public TableColumn<UserData, UserType> colUserType;

	// List wrappers for search and sort functionality
	private final ObservableList<UserData> masterData = FXCollections.observableArrayList();
	private FilteredList<UserData> filteredData;

	@FXML
	public void initialize() {
		setupColumns();
		setupRowListeners();
		setupSearchLogic();
		refreshdata();

	}
// Send updated user data to server
	private void setupColumns() {
		colFullName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
				cellData.getValue().getName()));
		colEmail.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
				cellData.getValue().getEmail()));
		colPhone.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
				cellData.getValue().getPhone()));
		colMemberCode.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
				cellData.getValue().getMemberCode()));
		colUserType.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(
				cellData.getValue().getUserType()));
	}
	// Set up double-click listener on table rows
	private void setupRowListeners() {
		customersTable.setRowFactory(tv -> {
			TableRow<UserData> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
					UserData clickedCustomer = row.getItem();
					handleCustomerDoubleClick(clickedCustomer);
				}
			});
			return row;
		});
	}
	// Set up search functionality
	private void setupSearchLogic() {
		// Wrap master list in a filtered list
		filteredData = new FilteredList<>(masterData, p -> true);

		//  Add listener to search field with YOUR specific fields
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

		//  Wrap in a sorted list so column sorting still works
		SortedList<UserData> sortedData = new SortedList<>(filteredData);
		sortedData.comparatorProperty().bind(customersTable.comparatorProperty());

		//  Bind the Sorted List to the Table
		customersTable.setItems(sortedData);
	}

	public void updateCustomers(List<UserData> customersData) {
		if (customersData == null)
			return;

		int total = customersData.size();
		long members = customersData.stream().filter(c -> c.getUserType() == UserType.MEMBER).count();
		int walkins = total - (int) members;
		
		// Update UI
		Platform.runLater(() -> {
			directoryTitleLabel.setText("Customer Directory (" + total + ")");
			totalCustomersLabel.setText(String.valueOf(total));
			membersLabel.setText(String.valueOf(members));
			walkinsLabel.setText(String.valueOf(walkins));
			masterData.setAll(customersData);
		});
	}
	

	public void btnRefresh() {
		refreshdata();
		searchField.clear();
	}

	private void refreshdata() {
		BistroClientGUI.client.getUserCTRL().loadCustomersData();
		if (BistroClientGUI.client.getUserCTRL().isCustomersDataLoaded()) {
			updateCustomers(BistroClientGUI.client.getUserCTRL().getCustomersData());
		}
		else {
			totalCustomersLabel.setText("0");
			membersLabel.setText("0");
			walkinsLabel.setText("0");
			masterData.clear();
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("No Data");
			alert.setHeaderText(null);
			alert.setContentText("No customer data available.");
			alert.showAndWait();
		}

	}
	private void handleCustomerDoubleClick(UserData editUser) {
	    //  Create the custom dialog
	    Dialog<UserData> dialog = new Dialog<>();
	    dialog.setTitle("Edit Customer: " + editUser.getName());
	    dialog.setHeaderText("Update details for " + editUser.getName());

	    //  Set the button types (Save and Cancel)
	    ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
	    dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

	    //  Create the form fields
	    GridPane grid = new GridPane();
	    grid.setHgap(10);
	    grid.setVgap(10);
	    grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

	    TextField nameField = new TextField(editUser.getName());
	    TextField emailField = new TextField(editUser.getEmail());
	    TextField phoneField = new TextField(editUser.getPhone());
	    
	    // Member code is usually read-only
	    TextField memberCodeField = new TextField(editUser.getMemberCode());
	    memberCodeField.setEditable(false); 
	    memberCodeField.setDisable(true);
	    
	    ComboBox<UserType> typeComboBox = new ComboBox<>();
	    typeComboBox.getItems().setAll(
	    	    java.util.Arrays.stream(UserType.values())
	    	        .filter(type -> type != UserType.MANAGER)
	    	        .collect(java.util.stream.Collectors.toList())
	    	);
	    typeComboBox.setValue(editUser.getUserType());
	    typeComboBox.setMaxWidth(Double.MAX_VALUE);

	    grid.add(new Label("Full Name:"), 0, 0);
	    grid.add(nameField, 1, 0);
	    
	    grid.add(new Label("Email:"), 0, 1);
	    grid.add(emailField, 1, 1);
	    
	    grid.add(new Label("Phone:"), 0, 2);
	    grid.add(phoneField, 1, 2);
	    
	    grid.add(new Label("Member Code:"), 0, 3);
	    grid.add(memberCodeField, 1, 3);
	    
	    grid.add(new Label("User Type:"), 0, 4);
	    grid.add(typeComboBox, 1, 4);

	    dialog.getDialogPane().setContent(grid);
	    Platform.runLater(nameField::requestFocus);
	    
	    Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
	    
	    saveButton.disableProperty().bind(
	            Bindings.createBooleanBinding(() -> 
	                nameField.getText().trim().isEmpty() ||
	                !InputCheck.validateEmail(emailField.getText()).isEmpty() ||
	                !InputCheck.validatePhoneNumber(phoneField.getText()).isEmpty(),
	                
	                nameField.textProperty(), 
	                emailField.textProperty(), 
	                phoneField.textProperty()
	            )
	        );
	    
	    // Convert the result when save is clicked
	    dialog.setResultConverter(dialogButton -> {
	        if (dialogButton == saveButtonType) {
	            // Return a new UserData object with updated fields
	            return new UserData(
	            	nameField.getText().trim(), 
	            	emailField.getText().trim(), 
	            	phoneField.getText().trim(), 
	                editUser.getMemberCode(), 
	                typeComboBox.getValue()
	            );
	        }
	        return null;
	    });
	    
	    //Show dialog and handle the result
	    dialog.showAndWait().ifPresent(updatedUser -> {
	        Node rootNode = grid.getScene().getRoot();
	        rootNode.setDisable(true);
	        rootNode.setCursor(Cursor.WAIT);

	        Thread updateThread = new Thread(() -> {
	            try {
	                // Perform the update
	                BistroClientGUI.client.getUserCTRL().updateUserDetails(updatedUser);
	                boolean success = BistroClientGUI.client.getUserCTRL().isUserUpdateSuccessful();

	                Platform.runLater(() -> {
	                    if (success) refreshdata();
	                    else new Alert(Alert.AlertType.ERROR, "Update failed on server.").showAndWait();
	                });
	            } catch (Exception e) {
	                Platform.runLater(() -> 
	                    new Alert(Alert.AlertType.ERROR, "Connection error: " + e.getMessage()).showAndWait());
	            } finally {
	                // ALWAYS unlock the UI, even if an error occurred
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
}
