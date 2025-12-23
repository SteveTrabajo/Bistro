package gui.logic;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;
import java.util.HashMap;
import java.util.Map;
import common.InputCheck;
import javafx.event.Event;
import entities.*;

/*
 * This class represents the client login screen controller.
 */
public class ClientLoginScreen {

	// ****************************** FXML Variables *****************************

	@FXML
	private Button btnGuest;

	@FXML
	private Button btnSignIn;

	@FXML
	private Button btnScanQR;

	@FXML
	private Hyperlink lnkEmployee;

	@FXML
	private TextField txtMemberID;

	@FXML
	private TextField txtPhoneNumber;

	@FXML
	private TextField txtEmailAddress;

	@FXML
	private Label lblError;

	// ******************************** Variables ********************************

	private Map<String, Object> userLoginData; // holds received user login data

	// ****************************** FXML Methods *****************************

	/*
	 * Handles the guest login button click event. Validates the phone number and
	 * email address inputs. If valid, attempts to log in the guest user and switch
	 * to the client dashboard screen. Displays error messages for invalid inputs or
	 * login failures.
	 * 
	 * @param event The event triggered by clicking the guest login button.
	 */
	@FXML
	public void btnGuest(Event event) {
		String phoneNumber = txtPhoneNumber.getText();
		String emailAddress = txtEmailAddress.getText();
		String errorMessage = InputCheck.isValidGuestInfo(phoneNumber, emailAddress);
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED);
		} else {
			userLoginData = new HashMap<String, Object>();
			userLoginData.put("phoneNumber", (Object) phoneNumber);
			userLoginData.put("emailAddress", (Object) emailAddress);
			BistroClientGUI.client.getUserCTRL().signInUser(userLoginData, "REQ_TO_LOGIN", UserType.GUEST);
			if (BistroClientGUI.client.getUserCTRL().isUserLoggedIn()) {
				BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error Message");
			} else {
				BistroClientGUI.display(lblError, "Error has been accoured!", Color.RED);
				return;
			}
		}
	}

	/*
	 * Handles the member sign-in button click event. Validates the member ID input.
	 * If valid, attempts to log in the member user and switch to the client
	 * dashboard screen. Displays error messages for invalid inputs or login
	 * failures.
	 * 
	 * @param event The event triggered by clicking the member sign-in button.
	 */
	@FXML
	public void btnSignIn(Event event) {
		String id = txtMemberID.getText();
		String errorMessage = InputCheck.isValidID(id);
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED);
		} else {
			userLoginData = new HashMap<String, Object>();
			userLoginData.put("userType", (UserType.MEMBER));
			userLoginData.put("id", (Object) id);
			BistroClientGUI.client.getUserCTRL().signInUser(userLoginData, "REQ_TO_LOGIN", UserType.MEMBER);
			if (BistroClientGUI.client.getUserCTRL().isUserLoggedIn()) {
				BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error Message");
			} else {
				BistroClientGUI.display(lblError, "Member ID does not exist.", Color.RED);
				return;
			}
		}
	}

	/*
	 * Handles the QR code scanning button click event. (Functionality to be
	 * implemented)
	 * 
	 * @param event The event triggered by clicking the QR code scanning button.
	 */
	@FXML
	public void btnScanQR(Event event) {
		// TODO - Implement QR code scanning functionality
		
	}
	
	/*
	 * Handles the employee login hyperlink click event. Switches to the employee
	 * login screen.
	 * 
	 * @param event The event triggered by clicking the employee login hyperlink.
	 */
	@FXML
	public void lnkEmployee(Event event) {
		BistroClientGUI.switchScreen(event, "employeeLoginScreen", "Employee Login Error Message");
	}
}
//End of ClientLoginScreen class