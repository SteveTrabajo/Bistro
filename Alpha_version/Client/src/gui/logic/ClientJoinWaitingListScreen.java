package gui.logic;

import entities.User;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

public class ClientJoinWaitingListScreen {

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

	
	
	@FXML
	public void initialize() {
	User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
	lblUser.setText(currentUser.getUserType().name());
	lblDinersAmount.setText("1");
	btnPlus.setOnAction(e -> {
		int currentAmount = Integer.parseInt(lblDinersAmount.getText());
		if(currentAmount < 12) {
			currentAmount++;
			lblDinersAmount.setText(String.valueOf(currentAmount));
		}
	});
	btnMinus.setOnAction(e -> {
		int currentAmount = Integer.parseInt(lblDinersAmount.getText());
		if(currentAmount > 1) {
			currentAmount--;
			lblDinersAmount.setText(String.valueOf(currentAmount));
		}
	});
	}
	public void btnCheckAvail(Event event) {
		int dinersAmount = Integer.parseInt(lblDinersAmount.getText());
		BistroClientGUI.client.getWaitingListCTRL().joinWaitingList(dinersAmount);
		if(BistroClientGUI.client.getWaitingListCTRL().isUserOnWaitingList()) {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "client Join Waiting List messege");
		} else {
			BistroClientGUI.display(lblError, "Error has been accoured!", Color.RED);
		}
	}
	public void btnBack(Event event) {
		try {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client back error messege");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
