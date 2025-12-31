package gui.logic;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

public class ClientOnListScreen {
	@FXML
	private Button btnBack;
	@FXML
	private Label lblConfirmCode;
	@FXML
	private Button btnLeave;
	@FXML
	public void btnBack(Event event) { 
	    BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Error returning to dashboard");
		}
	@FXML
	public void initialize() {
	    // 1. Get the logged-in user's ID to compare
	    int currentUserId = BistroClientGUI.client.getUserCTRL().getLoggedInUser().getUserId();
	    
	    // 2. Get the full waiting list
	    var waitingListByDate = BistroClientGUI.client.getWaitingListCTRL().getWaitingList();
	    
	    if (waitingListByDate != null) {
	        // 3. Iterate through all dates
	        for (var timeMap : waitingListByDate.values()) {
	            // 4. Iterate through all orders in each date
	            for (var order : timeMap.values()) {
	                
	                // 5. CHECK: Does this order belong to the current user?
	                if (order.getUserId() == currentUserId) {
	                    // Success - display only the code and stop searching
	                    lblConfirmCode.setText(order.getConfirmationCode());
	                    return;
	                }
	            }
	        }
	    }
	    
	    // If no order was found for this user, show nothing
	    lblConfirmCode.setText("ERROR.");
	}
	@FXML
	public void btnLeave(Event event) {
	    BistroClientGUI.client.getWaitingListCTRL().leaveWaitingList();
	    BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Error returning to dashboard after leaving waiting list.");
	}
}