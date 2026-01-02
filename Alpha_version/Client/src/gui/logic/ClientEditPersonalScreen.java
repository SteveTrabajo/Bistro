package gui.logic;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import logic.BistroClientGUI;

/**
 * This class represents the controller for the Client Edit Personal screen in
 * the BistroClientGUI.
 */

public class ClientEditPersonalScreen {
	// ****************************** FXML Elements ******************************
	@FXML
	private Button btnBack;
	@FXML
	private Button btnSave;
	@FXML
	private TextField txtFirstName;
	@FXML
	private TextField txtLastName;
	@FXML
	private TextField txtPhoneNumber;
	@FXML
	private TextField txtEmailAddress;
	@FXML
	private TextField txtAddress;
	@FXML
	private Label lblMemberID;
	@FXML
	private Label lblError;
	
	// ****************************** Instance Methods ******************************
	
	/**
	 * Initializes the Client Edit Personal screen.
	 */
	@FXML
	public void initialize() {
		// Load initial data
		lblMemberID.setText(String.valueOf(BistroClientGUI.client.getUserCTRL().getLoggedInUser().getUserId()));
		txtFirstName.setText(BistroClientGUI.client.getUserCTRL().getLoggedInUser().getFirstName());
		txtLastName.setText(BistroClientGUI.client.getUserCTRL().getLoggedInUser().getLastName());
		txtPhoneNumber.setText(BistroClientGUI.client.getUserCTRL().getLoggedInUser().getPhoneNumber());
		txtEmailAddress.setText(BistroClientGUI.client.getUserCTRL().getLoggedInUser().getEmail());
		txtAddress.setText(BistroClientGUI.client.getUserCTRL().getLoggedInUser().getAddress());
		lblError.setText("");
		//TODO: change input restriction to use the input check class on common folder
		// Added: Restriction for First Name - only letters allowed during typing
		txtFirstName.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("[a-zA-Zא-ת]*")) {
				txtFirstName.setText(oldValue);
			}
		});
		
		// Added: Restriction for Phone - only digits and max 10 characters
		txtPhoneNumber.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("\\d*") || newValue.length() > 10) {
				txtPhoneNumber.setText(oldValue);
			}
		});
	}
	
	/**
	 * Handles the Back button click event.
	 *
	 * @param event The event triggered by clicking the Back button.
	 */
	@FXML
	public void btnBack(ActionEvent event) {
		try {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Home");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Handles the Save button click event.
	 *
	 * @param event The event triggered by clicking the Save button.
	 */
	@FXML
	public void btnSave(ActionEvent event) {
		String firstName = txtFirstName.getText().trim();
		String lastName = txtLastName.getText().trim();
		String phoneNumber = txtPhoneNumber.getText().trim();
		String email = txtEmailAddress.getText().trim();
		String address = txtAddress.getText().trim();
		lblError.setText("");
		//TODO: change input check to use the input check class on common folder
		// Validations
		if (!firstName.matches("[a-zA-Zא-ת]+")) {
			lblError.setText("Error: First name must contain only letters");
			return;
		}
		if (!lastName.matches("[a-zA-Zא-ת]+")) {
			lblError.setText("Error: Last name must contain only letters");
			return;
		}
		if (!phoneNumber.matches("\\d{10}")) {
			lblError.setText("Error: Phone number must contain exactly 10 digits");
			return;
		}
		if (!email.contains("@")) {
			lblError.setText("Error: Email must contain @");
			return;
		}

		// 1. Get the current user object once to avoid long, repetitive lines
		entities.User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();

		// 2. Build the updated user object using local variables and the current user's data
		entities.User updatedUser = new entities.User(
		    currentUser.getUserId(),
		    phoneNumber, 
		    email, 
		    currentUser.getMemberCode(),
		    firstName, 
		    lastName,
		    address,
		    currentUser.getUserType()
		);
		if (updatedUser.equals(currentUser)) {
		    lblError.setText("No changes were made.");
		    return; // Don't send anything to the server
		}
		// 3. Send the updated user object to the server
		 BistroClientGUI.client.getUserCTRL().updateUserDetails(updatedUser);
		if (!BistroClientGUI.client.getUserCTRL().isUpdateSuccessful(currentUser)) {
			lblError.setText("Error: Failed to save details. Please try again.");
			return;
		}
		// Success
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Success");
		alert.setHeaderText(null);
		alert.setContentText("Your details have been updated successfully.");
		alert.showAndWait();
		}



	
}