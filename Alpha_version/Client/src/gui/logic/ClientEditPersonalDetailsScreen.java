package gui.logic;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import logic.BistroClientGUI;
import common.InputCheck;
import dto.UserData;
import entities.User;
import enums.UserType;

/**
 * This class represents the controller for the Client Edit Personal screen in
 * the BistroClientGUI.
 */

public class ClientEditPersonalDetailsScreen {
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
	private TextField txtEmail;
	@FXML
	private TextField txtAddress;
	@FXML
	private Label lblMemberID;
	@FXML
	private Label lblError;

	private User originalUser;
	// ****************************** Instance Methods ******************************

	/**
	 * Initializes the Client Edit Personal screen.
	 */
	@FXML
	public void initialize() {
		lblError.setStyle("-fx-text-fill: red;");
		lblError.setText("");
		// Load initial data
		originalUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
		lblMemberID.setText(originalUser.getMemberCode());
		txtFirstName.setText(originalUser.getFirstName());
		txtLastName.setText(originalUser.getLastName());
		txtPhoneNumber.setText(originalUser.getPhoneNumber());
		txtEmail.setText(originalUser.getEmail());
		txtAddress.setText(originalUser.getAddress());
		
		// Restriction for First Name - only English letters
		txtFirstName.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("[a-zA-Z]*")) {
				txtFirstName.setText(oldValue);
			}
		});
		
		// Restriction for Last Name - only English letters
		txtLastName.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("[a-zA-Z]*")) {
				txtLastName.setText(oldValue);
			}
		});

		// Restriction for Phone - digits only, optional + at start, dynamic length check
		txtPhoneNumber.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) return;
			// Allow 13 chars if it starts with +, otherwise 10
			int maxLength = newValue.startsWith("+") ? 13 : 10;
			
			if (!newValue.matches("^\\+?\\d*$") || newValue.length() > maxLength) {
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
		String email = txtEmail.getText().trim();
		String address = txtAddress.getText().trim();
		lblError.setText("");

		// Input validation using InputCheck methods
		String errorMessage = InputCheck.validatePersonalDetails(firstName, lastName, phoneNumber, email, address);
		if (!errorMessage.isEmpty()) {
			lblError.setText(errorMessage);
			return;
		}

	
		  boolean isChanged =
		            !email.equals(originalUser.getEmail()) ||
		            !phoneNumber.equals(originalUser.getPhoneNumber()) ||
		            !firstName.equals(originalUser.getFirstName()) ||
		            !lastName.equals(originalUser.getLastName()) ||
		            !address.equals(originalUser.getAddress());
		  
		if (!isChanged) {
			lblError.setText("No changes were made.");
			return;
		}

		UserData updatedUser = new UserData(firstName, lastName, originalUser.getMemberCode(), phoneNumber, email,
				originalUser.getUserType());
		 
		BistroClientGUI.client.getUserCTRL().updateUserDetails(updatedUser);

		if (!BistroClientGUI.client.getUserCTRL().isUpdateSuccessful(originalUser)) {
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