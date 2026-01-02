package gui.logic;



import entities.Table;
import entities.User;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;
/**
 * ClientCheckInTableSuccesScreen class handles the logic for the client check-in table success screen.
 */
public class ClientCheckInTableSuccesScreen {
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
	@FXML
	public void initialize() {
		int currentTable = BistroClientGUI.client.getTableCTRL().getUserAllocatedTable();
		String confirmationCode = BistroClientGUI.client.getReservationCTRL().getConfirmationCode();
		lblTableNum.setText(String.valueOf(currentTable));
		lblTableNumber.setText(String.valueOf(currentTable));
		lblConfirmCode.setText(confirmationCode);
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
