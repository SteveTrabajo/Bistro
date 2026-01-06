package gui.logic;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import logic.BistroClientGUI;
import java.util.HashMap;
import java.util.Map;
import common.InputCheck;
import comms.Api;
import javafx.event.Event;
import entities.*;
import enums.UserType;

/**
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
	private Hyperlink lnkForgotMemberID;

	@FXML
	private Hyperlink lnkEmployee;

	@FXML
	private TextField txtMemberID;

	@FXML
	private TextField txtPhoneNumber;

	@FXML
	private TextField txtEmailAddress;
	
	@FXML
	private StackPane mainPane;
	
	
	@FXML
	private StackPane modalOverlay; // Overlay pane for modals

	@FXML
	private Rectangle overlayBackground;
	
	private Parent ForgotIDModalRoot;

	@FXML
	private Label lblError;
	// ******************************** Variables ********************************

	private Map<String, Object> userLoginData; // holds received user login data
	
	private ClientForgotIDScreen forgotModalsCTRL;
	// ****************************** FXML Methods *****************************

	/**
	 * Initializes the client login screen. Sets up input validation for the member ID text field.
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
	
	/**
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
		UserType userType = UserType.GUEST;
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED);
		} else {
			String userLoginData = phoneNumber.trim() + "_" + emailAddress.trim();
			BistroClientGUI.client.getUserCTRL().signInUser(userLoginData,userType);
			if (BistroClientGUI.client.getUserCTRL().isUserLoggedInAs(userType)) {
				BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error Message");
			} else {
				BistroClientGUI.display(lblError, "Error has been accoured!", Color.RED);
				return;
			}
		}
	}

	
	/**
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
		UserType userType = UserType.MEMBER;
		String err = InputCheck.validateMemberCode6DigitsNoLeadingZero(memberCodeText);
		if (!err.isEmpty()) {
			lblError.setText(err);
			return;
		}
		BistroClientGUI.client.getUserCTRL().signInUser(memberCodeText, userType);

		if (BistroClientGUI.client.getUserCTRL().isUserLoggedInAs(userType)) {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error Message");
		} else {
			lblError.setText("Member code does not exist.");
		}
	}

	/**
	 * Handles the QR code scanning button click event. (Functionality to be
	 * implemented)
	 * 
	 * @param event The event triggered by clicking the QR code scanning button.
	 */
	@FXML
	public void btnScanQR(Event event) {
		// TODO - Implement QR code scanning functionality
	}
	
	@FXML
	public void lnkForgotMemberID(Event event) {
		if (ForgotIDModalRoot == null) {
			var url = getClass().getResource("/gui/fxml/clientForgotMemberIDModal.fxml");
			if (url == null) {
				BistroClientGUI.display(lblError, "FXML not found: /gui/fxml/clientForgotMemberIDModal.fxml",
						Color.RED);
				return;
			}

			FXMLLoader loader = new FXMLLoader(url);
			try {
				ForgotIDModalRoot = loader.load();
			} catch (Exception e) {
				e.printStackTrace();
				BistroClientGUI.display(lblError, "Unable to open Forgot Member ID screen.", Color.RED);
				return;
			}

			forgotModalsCTRL = loader.getController();
			forgotModalsCTRL.setParentCtrl(this);

			modalOverlay.getChildren().add(ForgotIDModalRoot);
		}
		mainPane.setEffect(new GaussianBlur(18));
		modalOverlay.setVisible(true);
		modalOverlay.setManaged(true);
	}

	/**
	 * Handles the employee login hyperlink click event. Switches to the employee
	 * login screen.
	 * 
	 * @param event The event triggered by clicking the employee login hyperlink.
	 */
	@FXML
	public void lnkEmployee(Event event) {
		BistroClientGUI.switchScreen(event, "employeeLoginScreen", "Employee Login Error Message");
	}
	
	
	/**
	 * Closes the forgot member ID modal dialog.
	 */
	public void closeForgotIDScreen() {
		mainPane.setEffect(null);
		modalOverlay.setVisible(false);
		modalOverlay.setManaged(false);
	}
	
}
//End of ClientLoginScreen.java