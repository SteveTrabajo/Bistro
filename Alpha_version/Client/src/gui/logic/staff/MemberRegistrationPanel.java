package gui.logic.staff;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import logic.BistroClientGUI;
import logic.QRCodeGenerator;

import java.nio.file.Path;
import java.util.ArrayList;

import common.InputCheck;

public class MemberRegistrationPanel {

    @FXML private TextField txtfirstName;
    @FXML private TextField txtlastName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtAddress;
    @FXML private Button btnRegister;

    // store the last submitted values so the async callback can use them
    private String lastFirstName;
    private String lastLastName;

    @FXML
    private void initialize() {
        // Hook async callbacks ONCE when panel loads
        BistroClientGUI.client.getUserCTRL().setOnRegisterNewMemberOkListener(memberCode -> {
            // Re-enable button
            if (btnRegister != null) btnRegister.setDisable(false);

            showInfo("Registration Successful",
                    "New member has been registered successfully."
                            + "\nMember Code: " + memberCode
                            + "\nA QR code has been generated for the member.");

            // Generate QR code from memberCode + name
            String fullName = (lastFirstName == null ? "" : lastFirstName)
                    + "_" + (lastLastName == null ? "" : lastLastName);

            Path qrPath = QRCodeGenerator.generateAndSaveQRCode(String.valueOf(memberCode), fullName);
            if (qrPath != null) {
                System.out.println("QR saved: " + qrPath.toAbsolutePath());
            }

            clearForm();
        });

        BistroClientGUI.client.getUserCTRL().setOnRegisterNewMemberFailListener(reason -> {
            if (btnRegister != null) btnRegister.setDisable(false);
            showError("Registration Failed", reason);
        });
    }

    @FXML
    public void btnRegister(Event event) {
        String firstName = txtfirstName.getText().trim();
        String lastName  = txtlastName.getText().trim();
        String email     = txtEmail.getText().trim();
        String phone     = txtPhone.getText().trim();
        String address   = txtAddress.getText().trim();

        String errorMessage = InputCheck.validatePersonalDetails(firstName, lastName, phone, email, address);
        if (!errorMessage.isEmpty()) {
            showError("Invalid Input", errorMessage);
            return;
        }

        // store for the async success callback (QR filename)
        lastFirstName = firstName;
        lastLastName = lastName;

        ArrayList<String> newMemberData = new ArrayList<>();
        newMemberData.add(firstName);
        newMemberData.add(lastName);
        newMemberData.add(email);
        newMemberData.add(phone);
        newMemberData.add(address);

        if (btnRegister != null) btnRegister.setDisable(true);
        BistroClientGUI.client.getUserCTRL().RegisterNewMember(newMemberData);
    }

    public void clearForm() {
        txtfirstName.clear();
        txtlastName.clear();
        txtEmail.clear();
        txtPhone.clear();
        txtAddress.clear();
    }

    public void showError(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    public void showInfo(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
