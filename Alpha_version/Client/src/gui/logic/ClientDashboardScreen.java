package gui.logic;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ClientDashboardScreen {
	
	private 
	
	@FXML
	private Button btnNewResvation;
	
	@FXML
	private Button btnJoinWaitingList;
	
	@FXML
	private Button btnCheckInForTable;
	
	@FXML
	private Button btnManageBooking;
	
	@FXML
	private Button btnPayBill;
	
	@FXML
	private Button btnEditPersonalDetails;
	
	@FXML
	private Button btnSignOut;
	
	@FXML
	private Label lblWelcome;
	
	@FXML
	private Label lblTopSubTitle;
	
	@FXML
	public void initialize() {
		checkSignedInUser();
	}
	
	
}
