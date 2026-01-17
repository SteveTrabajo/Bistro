package gui.logic;

import entities.Order;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

/**
 * ClientCheckInTableSuccessScreen class handles the logic for the client check-in table success screen.
 */
public class ClientCheckInTableSuccessScreen {
	
	//****************** FXML Elements ******************//
	@FXML
	private Label lblTableNumber;
	@FXML
	private Label lblConfirmCode;
	@FXML
	private Label lblTableNum;
	@FXML
	private Button btnBack;
	
	//****************** FXML Methods ******************//
	
	/**
	 * Initializes the Client Check-In Table Success screen.
	 */
	@FXML
	public void initialize() {
		Order storedOrder = BistroClientGUI.client.getTableCTRL().getUserAllocatedOrderForTable();
		
		if (storedOrder != null) {
            int currentTable = BistroClientGUI.client.getTableCTRL().getUserAllocatedTable();
            String confirmationCode = storedOrder.getConfirmationCode();
            
            lblTableNum.setText(String.valueOf(currentTable));
            lblTableNumber.setText(String.valueOf(currentTable));
            lblConfirmCode.setText(confirmationCode);
        } else {
            lblTableNum.setText("Error");
            lblConfirmCode.setText("");
        }
	}

	/**
	 * Handles the Back button click event.
	 *
	 * @param event The event triggered by clicking the Back button.
	 */
	@FXML
	public  void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "clientDashboardScreen", "client Dashboard error messege");
	}
	
}
