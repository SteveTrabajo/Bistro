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
    
    /**
	 * Constructor for StaffWaitAndRes dialog.
	 * This dialog collects customer information for either adding to the waitlist or making a reservation.
	 * The dialog dynamically adjusts its fields based on whether it's in Waitlist mode or Reservation mode.
	 * In Waitlist mode, it collects the number of diners along with customer details.
	 * In Reservation mode, it focuses on identifying the customer without asking for diners.
	 * The dialog supports two customer types: Guest and Member.
	 * Guest customers provide phone number, name, and email.
	 * Member customers provide their Member ID.
	 * Input validation is performed to ensure required fields are filled correctly.
	 * The result is returned as a Map containing the collected information.
	 * The keys in the result map are:
	 * - "customerType": "GUEST" or "MEMBER"
	 * - "identifier": Phone number for Guests, Member ID for Members
	 * - "name": Full name (only for Guests)
	 * - "email": Email address (only for Guests)
	 * - "diners": Number of diners (only in Waitlist mode)
	 * 
	 * @param isWaitlistMode true for Waitlist mode, false for Reservation mode.
	 */
    public StaffWaitAndRes(boolean isWaitlistMode) {
        this.isWaitlistMode = isWaitlistMode;
        this.setTitle(isWaitlistMode ? "Add to Waitlist" : "New Reservation");
        this.setHeaderText(isWaitlistMode ? "Enter walk-in details" : "Identify customer for booking");       
        buildUI();
    }
    
    /*
     * Builds the user interface for the dialog.
     * This method sets up the layout, controls, and event handlers.
     * It includes radio buttons for selecting customer type (Guest or Member),
     * text fields for entering customer details, and a combo box for selecting the number of diners (in Waitlist mode).
     * Input validation is performed when the user attempts to confirm their input.
     * The result converter processes the input and constructs a result map to be returned when the dialog is confirmed.
     * The layout is organized using a GridPane for a clean and structured appearance.
     * The dialog's buttons are configured to reflect the mode (Waitlist or Reservation).
     * The method also includes logic to dynamically show or hide fields based on the selected customer type.
     * @return void
     */
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
        txtName.setPromptText("Full Name (Optional)");

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
        
        grid.add(lbl2, 0, row);
        grid.add(txtName, 1, row, 2, 1); // Name or Empty
        row++;
        grid.add(lbl1, 0, row);
        grid.add(txtPhone, 1, row, 2, 1); // Phone or MemberID
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
                grid.add(txtPhone, 1, 2, 2, 1);
                grid.add(txtName, 1, 1, 2, 1);
                grid.add(txtEmail, 1, 3, 2, 1);
                txtName.setVisible(true);
                txtEmail.setVisible(true);
            }
            this.getDialogPane().getScene().getWindow().sizeToScene();
        });
        this.getDialogPane().setContent(grid);
        // Validation:
        Button btnOk = (Button) this.getDialogPane().lookupButton(confirmType);
        btnOk.addEventFilter(ActionEvent.ACTION, ae -> {
            boolean isMember = rbMember.isSelected();
            String id = txtMemberId.getText();
            String phone = txtPhone.getText();
            String email = txtEmail.getText();
            String error = InputCheck.validateWalkIn(isMember, id, phone, email);
            if (!error.isEmpty()) {
                ae.consume();
                showAlert(error);
            }
        });

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

    /*
	 * Shows an alert dialog with the given message.
	 * @param msg The message to display in the alert.
	 */
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Input");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
// end of StaffWaitAndRes.java