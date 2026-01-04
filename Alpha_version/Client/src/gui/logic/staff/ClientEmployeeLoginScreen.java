package gui.logic.staff;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

	@FXML
	public void initialize() {
		txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
		txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);
	}

	public void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "clientLoginScreen", "employee back error messege");
		
	}

	@FXML
	public void btnToggleVisibility(Event event) {
		boolean show = !txtPasswordVisible.isVisible();
		txtPasswordVisible.setVisible(show);
		txtPasswordVisible.setManaged(show);
		txtPassword.setVisible(!show);
		txtPassword.setManaged(!show);
		if (show) {
						imgEyeIcon.setImage(eyeOpen);
		} else {
			imgEyeIcon.setImage(eyeClosed);
		}
		TextInputControl activeField = show ? txtPasswordVisible : txtPassword;
        updateFieldFocus(activeField);
	}

	private void updateFieldFocus(TextInputControl field) {
		field.requestFocus();
		if (field.getText() != null) {
			field.positionCaret(field.getText().length());
        }
	}
	public void btnSignIn(ActionEvent event) {
		String username = txtUserName.getText().trim();
		String password = txtPassword.getText().trim();
		BistroClientGUI.client.getUserCTRL().staffLogin(username, password);
		if (BistroClientGUI.client.getUserCTRL().isEmployeeLoginSuccess()) {
			BistroClientGUI.switchScreen(event, "clientstaffDashboardScreen", "employee login error messege");
		} else {
			if (BistroClientGUI.client.getUserCTRL().isManagerLoginSuccess()) {
				BistroClientGUI.switchScreen(event, "clientstaffDashboardScreen", "manager login error messege");
			} 
			// Handle login failure (e.g., show error message)
		}
	}
	public void btnForgotPassword(ActionEvent event) {
		BistroClientGUI.switchScreen(event, "employeeForgotPasswordScreen", "employee forgot password error messege");
	}
	

}
