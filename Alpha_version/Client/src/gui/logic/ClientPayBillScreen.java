package gui.logic;

import entities.User;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

public class ClientPayBillScreen {
	@FXML
	private Button btnBack;
	@FXML
	private Button btnVerify;
	@FXML
	private Hyperlink lnkForgot;
	@FXML
	private TextField txtConfirmCode;
	
	@FXML
	private Label lblUserStat;
	
	@FXML
	private Label lblError;

	

	@FXML
	public void initialize() {
		User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
		lblUserStat.setText(currentUser.getUserType().name());
	}
	
	@FXML
	public void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "clientDashboardScreen", "client Dashboard error messege");
	}
	
	@FXML	
	public void lnkForgot(Event event) {
		//TODO: implement forgot confirmation code functionality
		return;
	}

	@FXML
	public void btnVerify(Event event) {
		String checkConfirmCode = txtConfirmCode.getText();
		String correctConfirmCode = BistroClientGUI.client.getTableCTRL().getUserAllocatedOrderForTable().getConfirmationCode();
		if (checkConfirmCode.equals(correctConfirmCode)) {
			BistroClientGUI.switchScreen(event, "clientCheckoutScreen", "cannot load client checkout screen");
		} else {
			BistroClientGUI.display(lblError,"Invalid Confirmation Code", Color.RED);
		}
	}

}
