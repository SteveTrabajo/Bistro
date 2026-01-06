package gui.logic;

import entities.User;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
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
	private Button btnBack;
	
	@FXML
	private Hyperlink lnkForgot;
	
	@FXML
	private TextField txtConfirmCode;
	@FXML
	private Label lblUser;
	@FXML
	private Label lblError;
	
	private StackPane currentScreen;
	@FXML
	private StackPane ScreenContainer;
	
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
	
	@FXML
	public void lnkForgot(Event event) {
		//TODO: Open Forgot Confirmation Code modal
		return;
	}
	
	
	/**
	 * Displays an error message on the screen.
	 *
	 * @param message The error message to display.
	 */
	public void showSuccessMessage(String message) {
        BistroClientGUI.display(lblError, message, Color.GREEN);
    }
	
	
	
	/**
	 * Closes the Forgot Code modal screen.
	 */
	public void closeForgotCodeScreen() {
        // Logic to remove the modal from the screen
        if (ScreenContainer != null && currentScreen != null) {
            ScreenContainer.getChildren().remove(currentScreen);
            currentScreen = null;
        } else {
            System.out.println("Modal closed.");
        }
    }
}
//End of ClientCheckInTableScreen.java