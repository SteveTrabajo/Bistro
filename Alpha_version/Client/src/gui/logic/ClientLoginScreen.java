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
import common.InputCheck;
import javafx.event.Event;

public class ClientLoginScreen {
	
	@FXML
	private Button btnSignIn;
	@FXML
	private Button btnScanQR;
	@FXML
	private Button btnGuest;
	@FXML
	private Hyperlink lnkEmployee;
	@FXML
	private TextField txtUsername;
	@FXML
	private TextField txtPhoneNumber;
	@FXML
	private TextField txtEmailAddress;
	@FXML
	private Label lblError;
	
	
	@FXML
	public void btnSignIn(Event event) {
		String username = txtUsername.getText();
		String errorMessage = InputCheck.isValidUsername(username);
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED);
		} else {
			// Proceed with sign-in logic before calling the switchScreen method TODO
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error Message");
		}
		
	}
	
	@FXML
	public void btnScanQR(Event event) {
		
		
	}
	
	@FXML
	public void btnGuest(Event event) {
		String phoneNumber = txtPhoneNumber.getText();
		String emailAddress = txtEmailAddress.getText();
		String errorMessage = InputCheck.isValidGuestInfo(phoneNumber, emailAddress);
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED);
		} else {
			// Proceed with sign-in logic before calling the switchScreen method TODO
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error Message");
		}
	}

}
