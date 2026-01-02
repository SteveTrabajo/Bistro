package gui.logic;

import common.InputCheck;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

/**
 * This class represents the client forgot ID modal controller.
 */	
public class ClientForgotIDModal {
	
	// ****************************** FXML Variables ******************************
	private ClientLoginScreen parentCtrl;
	
	@FXML
	private Button btnClose;
	
	@FXML
	private Button btnCancel;
	
	@FXML
	private Button btnFindMemberID;
	
	@FXML
	private TextField txtEmail;
	
	@FXML
	private TextField txtPhoneNum;
	
	@FXML
	private Label lblError;
	
	
	// ****************************** FXML Methods ******************************
	
	/**
	 * Sets the parent controller for this modal.
	 * 
	 * @param parentCtrl The parent ClientLoginScreen controller.
	 */
	public void setParentCtrl(ClientLoginScreen parentCtrl) {
		this.parentCtrl = parentCtrl;
	}
	
	/**
	 * Handles the Find Member ID button click event.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	private void btnFindMemberID(Event event) {
		String email = txtEmail.getText().trim();
		String phoneNum = txtPhoneNum.getText().trim();
		String errorMessage= "";
		errorMessage = InputCheck.isValidGuestInfo(phoneNum, email);
		if(errorMessage != "") {
			BistroClientGUI.display(lblError,errorMessage,Color.RED);
			return;
		}
		else {
			//send to server
			BistroClientGUI.client.getUserCTRL().forgotMemberID(email, phoneNum);
		}
	}
	
	/**
	 * Handles the Cancel button click event.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	private void btnCancel(Event event) {
		if (parentCtrl != null)
			parentCtrl.closeForgotIDModal();
	}
	
	/**
	 * Handles the Close button click event.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	private void btnClose(Event event) {
		if (parentCtrl != null)
			parentCtrl.closeForgotIDModal();
	}
}
// End of ClientForgotIDModal.java