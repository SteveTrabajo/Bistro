package gui.logic;



import entities.Table;
import entities.User;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

public class clientCheckInTableSuccesScreen {
	@FXML
	private Label tableNumberLabel;
	@FXML
	private Label lblConfirmCode;
	@FXML
	private Label lblTableNum;
	@FXML
	private Button btnBack;
	@FXML
	public void initialize() {
		int currentTable = BistroClientGUI.client.getTableCTRL().getUserAllocatedTable();
		String confirmationCode = BistroClientGUI.client.getReservationCTRL().getConfirmationCode();
		lblTableNum.setText(String.valueOf(currentTable));
		tableNumberLabel.setText(String.valueOf(currentTable));
		lblConfirmCode.setText(confirmationCode);
	}
	@FXML
	public  void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "clientDashboardScreen.fxml", "client Dashboard error messege");
	}
	
}
