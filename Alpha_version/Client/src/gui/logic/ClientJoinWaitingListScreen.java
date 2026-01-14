package gui.logic;


import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import entities.User;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

/**
 * ClientJoinWaitingListScreen class handles the logic for the client join
 * waiting list screen. It allows clients to select the number of diners and
 * join the waiting list.
 */
public class ClientJoinWaitingListScreen {
	
	//**************** FXML Variables ****************//
	@FXML
	private Button btnCheckAvail;
	
	@FXML
	private Button btnPlus;
	
	@FXML
	private Button btnMinus;
	
	@FXML
	private Button btnBack;
	
	@FXML
	private Label lblDinersAmount;
	
	@FXML
	private Label lblUser;
	
	@FXML
	private Label lblError;
	
	//**************** FXML Methods ****************//
	/**
	 * Initializes the screen by setting up user information and button actions.
	 */
	@FXML
	public void initialize() {
		User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
		lblUser.setText(currentUser.getUserType().name());
		lblDinersAmount.setText("1");
	}
	
	/**
	 * Handles the action when the "+" button is clicked.
	 * It increments the number of diners, ensuring it does not exceed 12.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	public void btnPlus(Event event) {
		int currentAmount = Integer.parseInt(lblDinersAmount.getText());
		if (currentAmount < 12) {
			currentAmount++;
			lblDinersAmount.setText(String.valueOf(currentAmount));
		}
	}
	
	
	/**
	 * Handles the action when the "-" button is clicked.
	 * It decrements the number of diners, ensuring it does not go below 1.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	public void btnMinus(Event event) {
		int currentAmount = Integer.parseInt(lblDinersAmount.getText());
		if (currentAmount > 1) {
			currentAmount--;
			lblDinersAmount.setText(String.valueOf(currentAmount));
		}
	}
	
	
	/**
	 * Handles the action when the "Check Availability" button is clicked.
	 * It attempts to join the waiting list with the specified number of diners.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	public void btnCheckAvail(Event event) {
		int dinersAmount = Integer.parseInt(lblDinersAmount.getText());
		BistroClientGUI.client.getWaitingListCTRL().checkWaitingListAvailability(dinersAmount);
		if(BistroClientGUI.client.getWaitingListCTRL().getcanSeatImmediately()) {
			BistroClientGUI.client.getWaitingListCTRL().setCanSeatImmediately(false);
			BistroClientGUI.switchScreen(event, "clientCheckInTableSuccesScreen", "Client Check-In Table Success error messege");
			return;
		}
		else {
			showAskJoinWaitlistDialog(BistroClientGUI.client.getWaitingListCTRL().getEstimatedWaitTimeMinutes(), dinersAmount);
			
		}
	}
	
	/**
	 * Displays a confirmation dialog asking the user if they want to join the waiting list.
	 * 
	 * @param estimatedMinutes The estimated wait time in minutes.
	 * @param dinersAmount The number of diners.
	 */
	public void showAskJoinWaitlistDialog(long estimatedMinutes, int dinersAmount) {

	    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
	    alert.setTitle("Waiting List Confirmation");
	    alert.setHeaderText(null);
	    alert.setContentText(
	        "Estimated wait time is " + estimatedMinutes + " minutes.\n" +
	        "Do you want to join the waiting list?"
	    );

	    ButtonType joinButton = new ButtonType("Join Waiting List");
	    ButtonType cancelButton = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
	    alert.getButtonTypes().setAll(joinButton, cancelButton);
	    Optional<ButtonType> result = alert.showAndWait();
	    if (result.isPresent() && result.get() == joinButton) {
	    	BistroClientGUI.client.getWaitingListCTRL().joinWaitingList(dinersAmount, estimatedMinutes);
	    }
	}


	/**
	 * Handles the action when the "Back" button is clicked.
	 * It navigates back to the client dashboard screen.
	 * 
	 * @param event The event triggered by clicking the button.
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
