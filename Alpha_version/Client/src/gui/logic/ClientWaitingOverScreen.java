package gui.logic;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

/**
 * Controller class for the Client Waiting Over Screen.
 * This screen informs the client that their waiting period is over
 * and provides them with their allocated table number.
 */
public class ClientWaitingOverScreen {
	
	//**************** FXML Variables ****************//
	
	@FXML
	private Button btnFinish;
	
	@FXML
	private Label lblTableNum;
	
	//**************** FXML Methods ****************//
	
	/**
	 * Initializes the screen by retrieving and displaying the user's allocated table number.
	 */
	@FXML
	public void initialize() {
		Integer tableNum = BistroClientGUI.client.getTableCTRL().getUserAllocatedTable();
		lblTableNum.setText(Integer.toString(tableNum));
	}
	
	/**
	 * Handles the action when the Finish button is clicked.
	 * Switches the screen back to the Client Dashboard Screen.
	 * 
	 * @param event The event triggered by clicking the Finish button.
	 */
	@FXML
	void btnFinish(Event event) {
	    System.out.println("Finish button clicked in clientWaitingOverScreen.");
	    BistroClientGUI.switchScreen(event, "ClientDashboardScreen", "Error returning to Client Dashboard Screen.");
	}
}
// End of ClientWaitingOverScreen.java