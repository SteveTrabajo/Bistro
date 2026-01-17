package gui.logic;

import common.InputCheck;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

/**
 * Controller for the "Find Your Member ID" Screen.
 */
public class ClientForgotIDScreen {

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

    // ****************************** Initialize ******************************

    @FXML
    public void initialize() {
        // Clear error label on start
        lblError.setText("");

        // Add listeners to clear error messages and validate as the user types
        txtEmail.textProperty().addListener((obs, old, newValue) -> clearError());
        txtPhoneNum.textProperty().addListener((obs, old, newValue) -> clearError());
        
        // Focus the phone number field by default when opened
        Platform.runLater(() -> txtPhoneNum.requestFocus());
    }

    // ****************************** FXML Methods ******************************

    public void setParentCtrl(ClientLoginScreen parentCtrl) {
        this.parentCtrl = parentCtrl;
    }

    /**
     * Logic for the "Find Member ID" button.
     */
    @FXML
    private void btnFindMemberID(Event event) {
        String email = txtEmail.getText().trim();
        String phoneNum = txtPhoneNum.getText().trim();
        // Client-Side Validation: Ensure at least one field is filled and formats are correct
        String validationError = InputCheck.isValidGuestInfo(phoneNum, email);
        if (!validationError.isEmpty()) {
            BistroClientGUI.display(lblError, validationError, Color.RED);
            return;
        }

        //  Prepare UI for the wait
        lblError.setText("Searching for member...");
        lblError.setTextFill(Color.GRAY);
        btnFindMemberID.setDisable(true); // Prevent spam clicks

        // Set up the Consumer listener to handle the server response
        BistroClientGUI.client.getUserCTRL().setOnMemberIDFoundListener(result -> {
            // Re-enable the button regardless of the result
            btnFindMemberID.setDisable(false);

            if ("NOT_FOUND".equals(result)) {
                lblError.setTextFill(Color.RED);
                lblError.setText("The provided information does not match any member. You're more than welcome to approach our staff and register as a new member!");
            } else {
                lblError.setTextFill(Color.GREEN);
                lblError.setText("Your Member ID is: " + result);
                
                txtEmail.clear();
                txtPhoneNum.clear();
            }
        });

        //Send the request to the server
        BistroClientGUI.client.getUserCTRL().forgotMemberID(email, phoneNum);
    }

    @FXML
    private void btnCancel(Event event) {
        closeScreen();
    }

    @FXML
    private void btnClose(Event event) {
        closeScreen();
    }

    // ****************************** Helper Methods ******************************

    private void clearError() {
        if (!lblError.getText().isEmpty()) {
            lblError.setText("");
        }
        btnFindMemberID.setDisable(false);
    }

    private void closeScreen() {
        if (parentCtrl != null) {
            // Clear the listener so the server response doesn't trigger on a closed window
            BistroClientGUI.client.getUserCTRL().setOnMemberIDFoundListener(null);
            parentCtrl.closeForgotIDScreen();
        }
    }
}