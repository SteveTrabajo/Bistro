package gui.logic.staff;

import java.util.HashMap;
import java.util.Map;

import common.InputCheck;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

public class StaffWaitAndRes extends Dialog<Map<String, Object>> {
    
    private final boolean isWaitlistMode; // TRUE = Waitlist (ask diners), FALSE = Reservation (identity only)
    
    public StaffWaitAndRes(boolean isWaitlistMode) {
        this.isWaitlistMode = isWaitlistMode;
        this.setTitle(isWaitlistMode ? "Add to Waitlist" : "New Reservation");
        this.setHeaderText(isWaitlistMode ? "Enter walk-in details" : "Identify customer for booking");
        
        buildUI();
    }
    
    private void buildUI() {
        ButtonType confirmType = new ButtonType(isWaitlistMode ? "Add" : "Next", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // --- Controls ---
        ToggleGroup group = new ToggleGroup();
        RadioButton rbGuest = new RadioButton("Guest");
        rbGuest.setToggleGroup(group);
        rbGuest.setSelected(true);
        RadioButton rbMember = new RadioButton("Member");
        rbMember.setToggleGroup(group);

        TextField txtPhone = new TextField();
        txtPhone.setPromptText("Phone Number");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email (Optional)");
        TextField txtName  = new TextField();
        txtName.setPromptText("Full Name");

        TextField txtMemberId = new TextField();
        txtMemberId.setPromptText("Member ID");

        // Diners (Only used if isWaitlistMode is true)
        ComboBox<Integer> cmbDiners = new ComboBox<>();
        for (int i = 1; i <= 12; i++) cmbDiners.getItems().add(i);
        cmbDiners.setValue(2);

        // --- Layout ---
        int row = 0;

        grid.add(new Label("Customer Type:"), 0, row);
        grid.add(rbGuest, 1, row);
        grid.add(rbMember, 2, row);
        row++;

        // Dynamic fields placeholders
        Label lbl1 = new Label("Phone:");
        Label lbl2 = new Label("Name:"); 
        Label lbl3 = new Label("Email:");

        grid.add(lbl1, 0, row);
        grid.add(txtPhone, 1, row, 2, 1); // Phone or MemberID
        row++;

        grid.add(lbl2, 0, row);
        grid.add(txtName, 1, row, 2, 1); // Name or Empty
        row++;
        
        grid.add(lbl3, 0, row);
        grid.add(txtEmail, 1, row, 2, 1); // Email or Empty
        row++;

        // Only show Diners selector for Waitlist. 
        // For Reservations, the next screen handles it.
        if (isWaitlistMode) {
            grid.add(new Label("Diners:"), 0, row);
            grid.add(cmbDiners, 1, row);
        }

        // --- Toggle Logic ---
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            grid.getChildren().removeAll(txtPhone, txtName, txtEmail, txtMemberId);
            
            if (rbMember.isSelected()) {
                lbl1.setText("Member ID:");
                lbl2.setText(""); 
                lbl3.setText("");
                grid.add(txtMemberId, 1, 1, 2, 1);
                // Hide others
                txtName.setVisible(false);
                txtEmail.setVisible(false);
            } else {
                lbl1.setText("Phone:");
                lbl2.setText("Name:");
                lbl3.setText("Email:");
                grid.add(txtPhone, 1, 1, 2, 1);
                grid.add(txtName, 1, 2, 2, 1);
                grid.add(txtEmail, 1, 3, 2, 1);
                txtName.setVisible(true);
                txtEmail.setVisible(true);
            }
            this.getDialogPane().getScene().getWindow().sizeToScene();
        });

        this.getDialogPane().setContent(grid);

        // --- Validation ---
        Button btnOk = (Button) this.getDialogPane().lookupButton(confirmType);
        btnOk.addEventFilter(ActionEvent.ACTION, ae -> {
            boolean isMember = rbMember.isSelected();
            String id = txtMemberId.getText();
            String phone = txtPhone.getText();
            String email = txtEmail.getText();
            
            // USE INPUTCHECK
            String error = InputCheck.validateWalkIn(isMember, id, phone, email);
            if (!error.isEmpty()) {
                ae.consume();
                showAlert(error);
            }
        });

        // --- Result Converter ---
        this.setResultConverter(dialogButton -> {
            if (dialogButton == confirmType) {
                Map<String, Object> req = new HashMap<>();
                
                if (rbMember.isSelected()) {
                    req.put("customerType", "MEMBER");
                    req.put("identifier", txtMemberId.getText().trim());
                } else {
                    req.put("customerType", "GUEST");
                    req.put("identifier", txtPhone.getText().trim());
                    req.put("name", txtName.getText().trim());
                    req.put("email", txtEmail.getText().trim());
                }
                
                // Only pass diners if we asked for it
                if (isWaitlistMode) {
                    req.put("diners", cmbDiners.getValue());
                }
                
                return req;
            }
            return null;
        });
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Input");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}