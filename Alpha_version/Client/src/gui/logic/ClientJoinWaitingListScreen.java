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
 * ClientJoinWaitingListScreen class handles the logic for the client join
 * waiting list screen. It allows clients to select the number of diners and
 * join the waiting list.
 */
public class ClientJoinWaitingListScreen {
	//*********** FXML Elements ***********//
	@FXML
	private Button btnCheckAvail;
	@FXML
	private Button btnPlus;
	@FXML
	private Button btnMinus;
	@FXML
	private Label lblDinersAmount;
	@FXML
	private Label lblUser;
	@FXML
	private Label lblError;
	@FXML
	private Button btnBack;
	
	//***********FXML Methods***********//
	/**
	 * Initializes the screen by setting up user information and button actions.
	 */
	@FXML
	public void initialize() {
		User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
		lblUser.setText(currentUser.getUserType().name());
		lblDinersAmount.setText("1");
		btnPlus.setOnAction(e -> {
			int currentAmount = Integer.parseInt(lblDinersAmount.getText());
			if (currentAmount < 12) {
				currentAmount++;
				lblDinersAmount.setText(String.valueOf(currentAmount));
			}
		});
		btnMinus.setOnAction(e -> {
			int currentAmount = Integer.parseInt(lblDinersAmount.getText());
			if (currentAmount > 1) {
				currentAmount--;
				lblDinersAmount.setText(String.valueOf(currentAmount));
			}
		});
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
		BistroClientGUI.client.getWaitingListCTRL().joinWaitingList(dinersAmount);
		if( BistroClientGUI.client.getWaitingListCTRL().getskipWaitingListJoin()) {
			BistroClientGUI.switchScreen(event, "clientCheckInTableSuccesScreen", "client Join Waiting List messege");
			BistroClientGUI.client.getWaitingListCTRL().setskipWaitingListJoin(false);
			return;
		}
		if (BistroClientGUI.client.getWaitingListCTRL().isUserOnWaitingList()) {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "client Join Waiting List messege");
		} else {
			BistroClientGUI.display(lblError, "Error has been accoured!", Color.RED);
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
