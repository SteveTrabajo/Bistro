package gui.logic;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

public class clientWaitingOverScreen {

	@FXML
	private Button btnFinish;
	@FXML
	private Label lblTableNum;
	
	@FXML
	public void initialize() {
		int tableNum = BistroClientGUI.client.getTableCTRL().getUserAllocatedTable();
		lblTableNum.setText("Your table number is: " + tableNum);
	}
	
	@FXML
	void btnFinish(Event event) {
	    System.out.println("Finish button clicked in clientWaitingOverScreen.");
	    BistroClientGUI.switchScreen(event, "ClientDashboardScreen", "Error returning to Client Dashboard Screen.");
	}
}
