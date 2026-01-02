package gui.logic;

import com.bistro.app.controllers.ClientCheckInTableScreen;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller for the Forgot Confirmation Code modal window.
 */
public class ClientForgotConfirmCodeModals {
	@FXML
	private TextField txtPhoneOrEmail;
	@FXML
	private Label lblError;
	
	private ClientCheckInTableScreen parentController;
	
	/**
	 * Sets the parent controller reference.
	 * 
	 * @param parent The parent ClientCheckInTableScreen controller.
	 */
	public void setParent(ClientCheckInTableScreen parent) {
		this.parentController = parent;
	}
	
	/**
	 * Handles the Cancel button click - closes the modal.
	 * 
	 * @param event The action event.
	 */
	@FXML
	public void btnCancel(ActionEvent event) {
		if (parentController != null) {
			parentController.closeForgotCodeModal();
		}
	}
	
	/**
	 * Handles the Retrieve button click - validates and sends request.
	 * 
	 * @param event The action event.
	 */
	@FXML
	public void btnRetrieve(ActionEvent event) {
		String phoneOrEmail = txtPhoneOrEmail.getText().trim();
		
		if (phoneOrEmail.isEmpty()) {
			lblError.setText("Please enter a phone number or email address");
			return;
		}
		
		// TODO: Add validation for phone/email format
		// TODO: Send request to server
		// BistroClientGUI.client.getReservationCTRL().retrieveConfirmationCode(phoneOrEmail);
		
		// For now, show success message and close modal
		if (parentController != null) {
			parentController.showSuccessMessage("If a reservation exists with this phone/email,\nthe confirmation code has been sent to you.");
			parentController.closeForgotCodeModal();
		}
	}
	
	/**
	 * Clears the error message.
	 */
	public void clearError() {
		lblError.setText("");
	}
}
