package gui.logic;

import common.InputCheck;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

/**
 * Controller for the Forgot Confirmation Code modal window.
 */
public class ClientForgotConfirmCodeScreen {

    @FXML
    private TextField txtPhoneNum;
    @FXML
    private TextField txtEmail;
    @FXML
    private Label lblError;
    @FXML
    private Button btnFindCode;

    private ClientCheckInTableScreen parentController;

    @FXML
    public void initialize() {
        lblError.setText("");
        // Clear error as user types
        txtPhoneNum.textProperty().addListener((obs, old, val) -> clearError());
        txtEmail.textProperty().addListener((obs, old, val) -> clearError());
        
        Platform.runLater(() -> txtPhoneNum.requestFocus());
    }

    public void setParent(ClientCheckInTableScreen parent) {
        this.parentController = parent;
    }

    /**
     * Handles the Find Code button click.
     */
    @FXML
    public void btnFindCode(ActionEvent event) {
        String email = txtEmail.getText().trim();
        String phoneNum = txtPhoneNum.getText().trim();

        // Validation Logic
        String validationError = InputCheck.isValidGuestInfo(phoneNum, email);
        if (!validationError.isEmpty()) {
            BistroClientGUI.display(lblError, validationError, Color.RED);
            return;
        }

        // Prepare UI
        lblError.setText("Checking reservations...");
        lblError.setTextFill(Color.GRAY);
        btnFindCode.setDisable(true);

        //Set up the Consumer listener (Callback)
        // Assuming your Reservation Controller has this listener/method
        BistroClientGUI.client.getReservationCTRL().setOnConfirmationCodeRetrieveResult(result -> {
            btnFindCode.setDisable(false);

            if ("NOT_FOUND".equals(result)) {
                lblError.setTextFill(Color.RED);
                lblError.setText("No reservation found for these details.");
            } else {
                // If found, show success message via parent and close
                if (parentController != null) {
                    parentController.showSuccessMessage("Success! Your code: " + result + "\nIt has also been sent to your email.");
                    parentController.closeForgotCodeScreen();
                }
            }
        });

        // Send request to server
        BistroClientGUI.client.getReservationCTRL().retrieveConfirmationCode(email, phoneNum);
    }

    @FXML
    public void btnCancel(ActionEvent event) {
        closeScreen();
    }

    @FXML
    public void btnClose(ActionEvent event) {
        closeScreen();
    }

    private void closeScreen() {
        if (parentController != null) {
            // Clear listener on close to be safe
            BistroClientGUI.client.getReservationCTRL().setOnConfirmationCodeRetrieveResult(null);
            parentController.closeForgotCodeScreen();
        }
    }

    public void clearError() {
        lblError.setText("");
        btnFindCode.setDisable(false);
    }
}