package gui.logic;

import entities.User;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

public class ClientCheckInTableScreen {
	// ****************************** FXML Variables ******************************
	@FXML
	private Button btnCheckIn;
	@FXML 
	private TextField txtConfirmCode;
	@FXML 
	private Label lblUser;
	@FXML
	private Label lblError;
	@FXML
	private Button btnBack;
	// ****************************** Instance Methods ******************************
	@FXML
	public void initialize() {
		User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
		lblUser.setText(currentUser.getUserType().name());
		int maxLength = 8;
		txtConfirmCode.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.length() > maxLength) {
				txtConfirmCode.setText(oldValue);
			}
		});
	}
	@FXML
	public void btnCheckIn(Event event) {
		String testConfirmationCode = txtConfirmCode.getText();
		BistroClientGUI.client.getReservationCTRL().CheckConfirmationCodeCorrect(testConfirmationCode);
		if(BistroClientGUI.client.getTableCTRL().isCheckInTableSuccess()) {
			BistroClientGUI.switchScreen(event, "clientCheckInTableSucces", "clientCheckIn error messege");
		} else {
			BistroClientGUI.display(lblError, "Error has been occured!", Color.RED);//TODO: add error message Label to fxml
		}
	}
	public void btnBack(Event event) {
		try {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client back error messege");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}