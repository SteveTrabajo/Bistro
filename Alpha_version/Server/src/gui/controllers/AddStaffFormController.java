package gui.controllers;

import common.InputCheck;
import entities.User;
import enums.UserType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import logic.BistroServer;

public class AddStaffFormController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtAddress;


    @FXML private RadioButton rbEmployee;
    @FXML private RadioButton rbManager;
    @FXML private ToggleGroup roleGroup;

    @FXML private Label lblError;
    @FXML private Label lblSuccess;

    @FXML private Button btnCreate;
    @FXML private Button btnCancel;

    private BistroServer server;

    public void setServer(BistroServer server) {
        this.server = server;
        System.out.println("[AddStaffForm] setServer called. server=" + (server != null));
    }

    @FXML
    private void initialize() {
        System.out.println("[AddStaffForm] initialize() fired");
        if (rbEmployee != null) rbEmployee.setSelected(true);
        clearMessages();
    }

    @FXML
    private void onCreate(ActionEvent event) {
        System.out.println("[AddStaffForm] onCreate() fired");
        clearMessages();

        // Hard fail if injections are broken
        if (lblError == null || lblSuccess == null) {
            System.out.println("[AddStaffForm] ERROR: lblError/lblSuccess not injected (fx:id mismatch).");
            return;
        }

        try {
            if (server == null) {
                showError("Server instance is null. setServer(...) was not called.");
                return;
            }

            String username = safeTrim(txtUsername);
            String password = safeTrim(txtPassword);
            String email = safeTrim(txtEmail);
            String phone = safeTrim(txtPhone);

            String firstName = safeTrim(txtFirstName);
            String lastName  = safeTrim(txtLastName);
            String address   = safeTrim(txtAddress);

            UserType userType = (rbManager != null && rbManager.isSelected())
                    ? UserType.MANAGER
                    : UserType.EMPLOYEE;

            String validationError = InputCheck.validateAllStaffData(
                    username, password, email, phone,
                    firstName, lastName, address
            );
            if (validationError != null && !validationError.isBlank()) {
                showError(validationError);
                return;
            }

            if (server.getDBController().employeeUsernameExists(username)) {
                showError("Username already exists: " + username);
                return;
            }

            btnCreate.setDisable(true);

            User created = server.getDBController().createEmployeeUser(
                    username, password, email, phone, userType,
                    firstName, lastName, address
            );

            if (created == null) {
                showError("Failed to create staff account. Check server logs/DB constraints.");
                return;
            }

            showSuccess("Created " + userType + " account: " + username);
            clearForm();


        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error: " + ex.getMessage());
        } finally {
            if (btnCreate != null) btnCreate.setDisable(false);
        }
    }

    @FXML
    private void onCancel(ActionEvent event) {
        System.out.println("[AddStaffForm] onCancel() fired");
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private static String safeTrim(TextInputControl c) {
        return (c == null || c.getText() == null) ? "" : c.getText().trim();
    }

    private void clearForm() {
        if (txtUsername != null) txtUsername.clear();
        if (txtPassword != null) txtPassword.clear();
        if (txtEmail != null) txtEmail.clear();
        if (txtPhone != null) txtPhone.clear();
        if (txtFirstName != null) txtFirstName.clear();
        if (txtLastName != null) txtLastName.clear();
        if (txtAddress != null) txtAddress.clear();
        if (rbEmployee != null) rbEmployee.setSelected(true);
    }


    private void clearMessages() {
        if (lblError != null) lblError.setText("");
        if (lblSuccess != null) lblSuccess.setText("");
    }

    private void showError(String msg) {
        String out = (msg == null || msg.isBlank()) ? "Invalid input (no message from validator)." : msg;
        lblError.setText(out);
        lblSuccess.setText("");
        System.out.println("[AddStaffForm] ERROR: " + out);
    }

    private void showSuccess(String msg) {
        lblSuccess.setText(msg);
        lblError.setText("");
        System.out.println("[AddStaffForm] SUCCESS: " + msg);
    }
}
