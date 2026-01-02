package gui.logic;

import entities.User;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

/**
 * This class represents the controller for the Client Check-In Table screen in
 * the BistroClientGUI.
 */
public class ClientCheckInTableScreen {
	// ****************************** FXML Elements ******************************
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

	/**
	 * Initializes the Client Check-In Table screen.
	 */
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
	
	/**
	 * Handles the Check-In button click event.
	 *
	 * @param event The event triggered by clicking the Check-In button.
	 */
	@FXML
	public void btnCheckIn(Event event) {
		String testConfirmationCode = txtConfirmCode.getText();
		BistroClientGUI.client.getReservationCTRL().CheckConfirmationCodeCorrect(testConfirmationCode);
		if (BistroClientGUI.client.getTableCTRL().isCheckInTableSuccess()) {
			BistroClientGUI.switchScreen(event, "clientCheckInTableSucces", "clientCheckIn error messege");
		} else {
			BistroClientGUI.display(lblError, "Error has been occured!", Color.RED);																		
		}
	}
	
	/**
	 * Handles the Back button click event.
	 *
	 * @param event The event triggered by clicking the Back button.
	 */
	@FXML
	public void btnBack(Event event) {
		try {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client back error messege");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
//End of ClientCheckInTableScreen.java