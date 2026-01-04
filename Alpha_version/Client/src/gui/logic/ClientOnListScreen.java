package gui.logic;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

/**
 * Controller for the Client On List Screen. This screen displays the
 * confirmation code for clients on the waiting list and allows them to leave
 * the waiting list.
 */
public class ClientOnListScreen {
	// ************* FXML Variables *************//

	@FXML
	private Button btnBack;
	
	@FXML
	private Button btnLeave;
	
	@FXML
	private Label lblConfirmCode;
	
	@FXML
	private Label lblError;

	// ************* FXML Methods *************//
	
	/**
	 * Initializes the screen by retrieving and displaying the confirmation code
	 * for the logged-in user from the waiting list.
	 */
	//TODO: refactor logic of the method
	@FXML
	public void initialize() {
		int currentUserId = BistroClientGUI.client.getUserCTRL().getLoggedInUser().getUserId();
		var waitingListByDate = BistroClientGUI.client.getWaitingListCTRL().getWaitingList();
		if (waitingListByDate != null) {
			for (var timeMap : waitingListByDate.values()) {
				for (var order : timeMap.values()) {
					if (order.getUserId() == currentUserId) {
						lblConfirmCode.setText(order.getConfirmationCode());
						return;
					}
				}
			}
		}
		// If no order was found for this user, show nothing
		lblConfirmCode.setText("ERROR.");
	}
	
	/**
	 * Handles the action of leaving the waiting list when the "Leave" button is
	 * clicked. If successful, navigates back to the client dashboard screen;
	 * otherwise, displays an error message.
	 * 
	 * @param event The event triggered by clicking the "Leave" button.
	 */
	@FXML
	public void btnLeave(Event event) {
		BistroClientGUI.client.getWaitingListCTRL().leaveWaitingList();
		if (BistroClientGUI.client.getWaitingListCTRL().isLeaveWaitingListSuccess()) {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen",
					"Error returning to dashboard after leaving waiting list.");
		} else
			BistroClientGUI.display(lblError, "Error leaving the waiting list. Please try again.", null);
	}
	
	/**
	 * Handles the action of returning to the client dashboard screen when the
	 * "Back" button is clicked.
	 * 
	 * @param event The event triggered by clicking the "Back" button.
	 */
	@FXML
	public void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Error returning to dashboard");
	}
}