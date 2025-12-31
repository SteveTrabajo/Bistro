package gui.logic;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;
import java.util.HashMap;
import java.util.Map;
import common.InputCheck;
import comms.Api;
import javafx.event.Event;
import entities.*;
import enums.UserType;

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
	public void initialize() {
		txtMemberID.setTextFormatter(new TextFormatter<String>(change -> {
			String newText = change.getControlNewText();

			// allow empty so backspace/delete works
			if (newText.isEmpty())
				return change;

			// digits only, up to 6
			if (!InputCheck.isDigitsUpTo(newText, 6))
				return null;

			// enforce "no leading 0" while typing
			if (newText.length() >= 1 && newText.charAt(0) == '0')
				return null;

			return change;
		}));
	}

	@FXML
	public void btnGuest(Event event) {
		String phoneNumber = txtPhoneNumber.getText();
		String emailAddress = txtEmailAddress.getText();
		String errorMessage = InputCheck.isValidGuestInfo(phoneNumber, emailAddress);
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED);
		} else {
			userLoginData = new HashMap<String, Object>();
			userLoginData.put("userType", (UserType.GUEST));
			userLoginData.put("phoneNumber", (Object) phoneNumber);
			userLoginData.put("email", (Object) emailAddress);
			BistroClientGUI.client.getUserCTRL().signInUser(userLoginData);
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
		String memberCodeText = txtMemberID.getText();

		String err = InputCheck.validateMemberCode6DigitsNoLeadingZero(memberCodeText);
		if (!err.isEmpty()) {
			lblError.setText(err);
			return;
		}

		int memberCode = Integer.parseInt(memberCodeText.trim());

		HashMap<String, Object> userLoginData = new HashMap<>();
		userLoginData.put("userType", UserType.MEMBER);
		userLoginData.put("memberCode", memberCode);

		BistroClientGUI.client.getUserCTRL().signInUser(userLoginData);

		if (BistroClientGUI.client.getUserCTRL().isUserLoggedIn()) {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error Message");
		} else {
			lblError.setText("Member code does not exist.");
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