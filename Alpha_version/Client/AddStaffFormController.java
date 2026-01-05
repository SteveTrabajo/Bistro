// COPY-PASTE READY: Add Staff Form Implementation

package logic; // Or your appropriate package

import logic.UserController;
import entities.User;
import enums.UserType;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the "Add New Employee" form.
 * Managers can only be created on the server as they are administrators.
 * This form allows adding EMPLOYEE staff members only.
 */
public class AddStaffFormController {
    
    // ==================== UI Components ====================
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button createButton;
    @FXML private Button cancelButton;
    
    // ==================== Instance Variables ====================
    private UserController userController;
    
    // ==================== Initialization ====================
    
    /**
     * Initialize the controller with reference to UserController.
     * Call this after loading the FXML.
     */
    public void initialize(UserController userController) {
        this.userController = userController;
        
        // Clear labels
        errorLabel.setText("");
        successLabel.setText("");
    }
    
    // ==================== Event Handlers ====================
    
    /**
     * Called when "Create Staff" button is clicked.
     */
    @FXML
    private void onCreateButtonClicked() {
        // Clear previous messages
        errorLabel.setText("");
        successLabel.setText("");
        
        // ===== Step 1: Get form values =====
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        // Role is always EMPLOYEE for this form
        String roleStr = "EMPLOYEE";
        
        // ===== Step 2: Client-side validation =====
        String validationError = validateFormInput(username, password, email, phone, roleStr);
        if (validationError != null) {
            showError(validationError);
            return;
        }
        
        // ===== Step 3: Convert to UserType =====
        UserType userType = UserType.valueOf(roleStr); // "EMPLOYEE" or "MANAGER"
        
        // ===== Step 4: Show loading state =====
        createButton.setDisable(true);
        createButton.setText("Creating...");
        
        // ===== Step 5: Send to server =====
        try {
            userController.clearStaffCreationStatus();
            userController.createNewStaffMember(username, password, email, phone, userType);
            
            // ===== Step 6: Check server response =====
            if (userController.isStaffCreationSuccess()) {
                showSuccess("✓ Staff member '" + username + "' created successfully!");
                clearFormFields();
                
                // Optional: Close form after 2 seconds
                // new Thread(() -> {
                //     try { Thread.sleep(2000); } catch (InterruptedException e) {}
                //     Platform.runLater(() -> closeForm());
                // }).start();
                
            } else {
                String serverError = userController.getStaffCreationErrorMessage();
                showError("✗ " + (serverError != null ? serverError : "Failed to create staff member"));
            }
        } catch (Exception e) {
            showError("✗ Communication error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // ===== Step 7: Reset button state =====
            createButton.setDisable(false);
            createButton.setText("Create");
        }
    }
    
    /**
     * Called when "Cancel" button is clicked.
     */
    @FXML
    private void onCancelButtonClicked() {
        clearFormFields();
        closeForm();
    }
    
    // ==================== Validation ====================
    
    /**
     * Validates all form input.
     * @return null if valid, error message if invalid
     */
    private String validateFormInput(String username, String password, String email, String phone, String role) {
        // Check empty fields
        if (username.isEmpty()) return "Username cannot be empty";
        if (password.isEmpty()) return "Password cannot be empty";
        if (email.isEmpty()) return "Email cannot be empty";
        if (phone.isEmpty()) return "Phone cannot be empty";
        
        // Check username length
        if (username.length() < 3) return "Username must be at least 3 characters";
        if (username.length() > 20) return "Username must not exceed 20 characters";
        
        // Check username characters (alphanumeric + dot, underscore, hyphen)
        if (!username.matches("^[a-zA-Z0-9._-]+$")) {
            return "Username can only contain letters, numbers, dots, underscores, and hyphens";
        }
        
        // Check password length
        if (password.length() < 4) return "Password must be at least 4 characters";
        
        // Check email format
        if (!email.contains("@") || !email.contains(".")) {
            return "Invalid email format";
        }
        
        // Check phone format (numbers only, 9-15 digits)
        String phoneDigitsOnly = phone.replaceAll("[^0-9]", "");
        if (phoneDigitsOnly.length() < 9 || phoneDigitsOnly.length() > 15) {
            return "Phone number must be 9-15 digits";
        }
        
        return null; // All valid
    }
    
    // ==================== UI Updates ====================
    
    /**
     * Display success message in green label.
     */
    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        errorLabel.setText("");
    }
    
    /**
     * Display error message in red label.
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        successLabel.setText("");
    }
    
    /**
     * Clear all form fields.
     */
    private void clearFormFields() {
        usernameField.clear();
        passwordField.clear();
        emailField.clear();
        phoneField.clear();
        errorLabel.setText("");
        successLabel.setText("");
    }
    
    /**
     * Close the add staff form window.
     */
    private void closeForm() {
        // Option 1: Close the parent stage
        // Stage stage = (Stage) usernameField.getScene().getWindow();
        // stage.close();
        
        // Option 2: Hide the form
        // usernameField.getScene().getRoot().setVisible(false);
        
        // Option 3: Navigate back to previous screen
        // (depending on your navigation structure)
    }
}


// ==================== FXML LAYOUT (Add Staff.fxml) ====================

/*
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.ComboBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.PasswordField?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.VBox?>

<VBox maxHeight="-Infinity" maxWidth="-Infinity" minHeight="400" minWidth="500" prefHeight="400" prefWidth="500" spacing="15" style="-fx-padding: 20;" xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1">
   
   <!-- Title -->
   <Label text="Add New Staff Member" style="-fx-font-size: 18; -fx-font-weight: bold;" />
   
   <!-- Username -->
   <HBox spacing="10">
      <Label minWidth="100" text="Username:" />
      <TextField fx:id="usernameField" promptText="john_doe" HBox.hgrow="ALWAYS" />
   </HBox>
   
   <!-- Password -->
   <HBox spacing="10">
      <Label minWidth="100" text="Password:" />
      <PasswordField fx:id="passwordField" promptText="Min 4 characters" HBox.hgrow="ALWAYS" />
   </HBox>
   
   <!-- Email -->
   <HBox spacing="10">
      <Label minWidth="100" text="Email:" />
      <TextField fx:id="emailField" promptText="john@bistro.com" HBox.hgrow="ALWAYS" />
   </HBox>
   
   <!-- Phone -->
   <HBox spacing="10">
      <Label minWidth="100" text="Phone:" />
      <TextField fx:id="phoneField" promptText="0501234567" HBox.hgrow="ALWAYS" />
   </HBox>
   
   <!-- Error Label -->
   <Label fx:id="errorLabel" text="" style="-fx-text-fill: red;" />
   
   <!-- Success Label -->
   <Label fx:id="successLabel" text="" style="-fx-text-fill: green;" />
   
   <!-- Spacer -->
   <Region VBox.vgrow="ALWAYS" />
   
   <!-- Buttons -->
   <HBox alignment="CENTER" spacing="10">
      <Button fx:id="createButton" onAction="#onCreateButtonClicked" prefWidth="100" text="Create" />
      <Button fx:id="cancelButton" onAction="#onCancelButtonClicked" prefWidth="100" text="Cancel" />
   </HBox>
</VBox>
*/
