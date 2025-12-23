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

import java.util.ArrayList;

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
	private TextField txtMemberID;
	@FXML
	private TextField txtPhoneNumber;
	@FXML
	private TextField txtEmailAddress;
	@FXML
	private Label lblError;
	
	ArrayList<String> userLoginData = new ArrayList<String>();
	
	@FXML
	public void btnSignIn(Event event) {
		String id = txtMemberID.getText();
		String errorMessage = InputCheck.isValidID(id);
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED);
		}
		else {
			userLoginData.add(id);
			BistroClientGUI.client.getUserCTRL().signInUser(userLoginData);
			if() {
				BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error Message");
			} else {
				BistroClientGUI.display(lblError, "Member ID does not exist.", Color.RED);
				return;
			}
		}
	}
	
	@FXML
	public void btnScanQR(Event event) {
		//TODO - Implement QR code scanning functionality
		
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
