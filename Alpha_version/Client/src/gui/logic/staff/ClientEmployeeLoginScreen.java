package gui.logic.staff;

import java.util.HashMap;
import java.util.Map;
import enums.UserType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

public class ClientEmployeeLoginScreen {
	@FXML
	private Button btnBack;
	@FXML
	private Button btnSignIn;
	@FXML
	private Button btnForgotPassword;
	@FXML
	private TextField txtUserName;
	@FXML
	private PasswordField txtPassword;
	@FXML
	private TextField txtPasswordVisible;
	@FXML
	private Button btnToggleVisibility;
	@FXML
	private ImageView imgEyeIcon;
	private final Image eyeOpen = new Image(getClass().getResourceAsStream("/resources/icons/eye-open.png"));
	private final Image eyeClosed = new Image(getClass().getResourceAsStream("/resources/icons/eye-closed.png"));

	/** Initializes the controller class. This method is automatically called
	 * after the fxml file has been loaded.
	 */
	@FXML
	public void initialize() {
		txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
	}

	/**
	 * Navigates back to the Client Employee Login Screen.
	 * @param event
	 */
	@FXML
	public void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "clientLoginScreen", "employee back error messege");

	}

	/**
	 * Toggles the visibility of the password field between masked and unmasked.
	 * @param event
	 */
	@FXML
	public void btnToggleVisibility(Event event) {
		boolean show = !txtPasswordVisible.isVisible();
		txtPasswordVisible.setVisible(show);
		txtPasswordVisible.setManaged(show);
		txtPassword.setVisible(!show);
		txtPassword.setManaged(!show);
		if (show) {
			imgEyeIcon.setImage(eyeClosed);
		} else {
			imgEyeIcon.setImage(eyeOpen);
		}
		TextInputControl activeField = show ? txtPasswordVisible : txtPassword;
		updateFieldFocus(activeField);
	}

	/**
	 *  Updates the focus to the specified text input control and positions the caret at the end of the text.
	 */
	private void updateFieldFocus(TextInputControl field) {
		field.requestFocus();
		if (field.getText() != null) {
			field.positionCaret(field.getText().length());
		}
	}

	/**
	 * Handles the sign-in process for an employee.
	 * @param event
	 */
	@FXML
	public void btnSignIn(Event event) {
	    String username = txtUserName.getText().trim();
	    String password = txtPassword.getText().trim();
	    Map<String, Object> loginData = new HashMap<>();
	    loginData.put("userType", UserType.EMPLOYEE);
	    loginData.put("username", username);
	    loginData.put("password", password);
	    BistroClientGUI.client.getUserCTRL().signInUser(loginData);
	    if(BistroClientGUI.client.getUserCTRL().getLoggedInUser() != null) {
	    	BistroClientGUI.switchScreen(event, "staff/clientStaffDashboardScreen", "Failed to load staff dashboard");
	    }
	}

	/**
     * Opens a dialog to recover staff password using Email/Phone.
     * @param event
     */
	@FXML
    public void btnForgotPassword(Event event) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Staff Credential Recovery");
        dialog.setHeaderText("Please enter your registered details.");
        ButtonType searchButtonType = new ButtonType("Find Credentials", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(searchButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));
        TextField emailField = new TextField();
        emailField.setPromptText("Enter Email");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Enter Phone Number");
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(300);
        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(statusLabel, 0, 2, 2, 1);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> emailField.requestFocus());
        final Button btSearch = (Button) dialog.getDialogPane().lookupButton(searchButtonType);
        // Use event filter to prevent the dialog from closing immediately when clicked
        btSearch.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume(); // Stop the dialog from closing automatically

            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();

            if (email.isEmpty() && phone.isEmpty()) {
                statusLabel.setTextFill(Color.RED);
                statusLabel.setText("Please fill at least one field.");
                return;
            }

            statusLabel.setTextFill(Color.BLUE);
            statusLabel.setText("Checking records...");
            btSearch.setDisable(true); // Prevent double-clicks
            BistroClientGUI.client.getUserCTRL().setOnStaffCredentialsListener(rawResult -> {
                Platform.runLater(() -> {
                    btSearch.setDisable(false);
                    
                    // Remove nulls and hidden characters
                    String cleanResult = (rawResult == null) ? "" : rawResult.replaceAll("\\p{Cntrl}", "").trim();

                    if ("NOT_FOUND".equals(cleanResult) || cleanResult.isEmpty()) {
                        statusLabel.setTextFill(Color.RED);
                        statusLabel.setText("Information does not belong to any staff member.");
                    } else {
                        String[] parts = cleanResult.split(":"); 
                        if (parts.length >= 2) {
                            // Update UI to show credentials
                            grid.getChildren().clear();
                            Label lblSuccess = new Label("Credentials Found:");
                            lblSuccess.setTextFill(Color.GREEN);
                            lblSuccess.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                            grid.add(lblSuccess, 0, 0, 2, 1);
                            grid.add(new Label("Username:"), 0, 1);
                            TextField txtUser = new TextField(parts[0]);
                            txtUser.setEditable(false);
                            grid.add(txtUser, 1, 1);
                            grid.add(new Label("Password:"), 0, 2);
                            TextField txtPass = new TextField(parts[1]);
                            txtPass.setEditable(false);
                            grid.add(txtPass, 1, 2);
                            // Remove "Search" button, keep only "Close"
                            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
                            dialog.setHeaderText("Identity Verified");
                        } else {
                            statusLabel.setTextFill(Color.RED);
                            statusLabel.setText("Error: Invalid data received from server.");
                        }
                    }
                });
            });
            BistroClientGUI.client.getUserCTRL().recoverStaffPassword(email, phone);
        });
        dialog.showAndWait();
    }

}
