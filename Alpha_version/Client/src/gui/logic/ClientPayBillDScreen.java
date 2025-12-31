package gui.logic;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import logic.BistroClientGUI;

public class ClientPayBillDScreen {
@FXML
private Button btnBack;
@FXML
private Button lblUserStat;
@FXML
private Button btnVerify;
@FXML
private TextField txtConfirmCode;
@FXML
public void btnBack(javafx.event.Event event) {
	BistroClientGUI.switchScreen(event, "clientDashboardScreen", "client Dashboard error messege");
	}
@FXML
public void btnVerify(javafx.event.Event event) {
	String checkConfirmCode = txtConfirmCode.getText();
	String correctConfirmCode =BistroClientGUI.client.getTableCTRL().getUserAllocatedOrderForTable().getConfirmationCode();
	if(checkConfirmCode.equals(correctConfirmCode)) {
		BistroClientGUI.switchScreen(event, "clientPayBillScreen", "client Payment c Screen error messege");
	} else {
		//BistroClientGUI.display("Invalid Confirmation Code", "The confirmation code you entered is invalid. Please try again.");
	}
	}
@FXML
public void initialize() {
	User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
	lblUserStat.setText(currentUser.getUserType().name());
	}
}

