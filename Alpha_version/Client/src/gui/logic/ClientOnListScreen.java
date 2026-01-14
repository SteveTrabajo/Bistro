package gui.logic;

import java.util.ArrayList;
import java.util.Optional;

import entities.Order;
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

	@FXML
    public void initialize() {
        String code = retrieveConfirmationCode();
        if (code != null) {
            lblConfirmCode.setText(code);
        } else {
            lblConfirmCode.setText("Error");
        }
    }

    /**
     * Helper method to find the confirmation code using standard loops.
     * @return The confirmation code String, or null if not found.
     */
    private String retrieveConfirmationCode() {
        // Check for a ready reservation specific to this session
        Order activeOrder = BistroClientGUI.client.getWaitingListCTRL().getOrderWaitListDTO();
        
        if (activeOrder != null && activeOrder.getConfirmationCode() != null) {
            return activeOrder.getConfirmationCode();
        }

        // Check the Waiting List
        ArrayList<Order> waitingList = BistroClientGUI.client.getWaitingListCTRL().getWaitingList();
        
        // Guard clause: if list is missing or empty, stop here
        if (waitingList == null || waitingList.isEmpty()) {
            return null;
        }

        int currentUserId = BistroClientGUI.client.getUserCTRL().getLoggedInUser().getUserId();

        // Iterate through the list to find the matching user ID
        for (Order order : waitingList) {
            if (order.getUserId() == currentUserId) {
                return order.getConfirmationCode();
            }
        }

        return null;
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
		TaskRunner.run(event,()->{
			BistroClientGUI.client.getWaitingListCTRL().leaveWaitingList();
		},()->{
		if (BistroClientGUI.client.getWaitingListCTRL().isLeaveWaitingListSuccess()) {
			BistroClientGUI.client.getWaitingListCTRL().clearWaitingListController();
			BistroClientGUI.switchScreen(event, "clientDashboardScreen","Error returning to dashboard after leaving waiting list.");
		} else
			BistroClientGUI.display(lblError, "Error leaving the waiting list. Please try again.", null);
		});
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