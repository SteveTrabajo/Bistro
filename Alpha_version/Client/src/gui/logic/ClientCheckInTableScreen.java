package gui.logic;

import entities.User;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;
import common.InputCheck;

/**
 * This class represents the controller for the Client Check-In Table screen in
 * the BistroClientGUI.
 */
public class ClientCheckInTableScreen {
	
	// ****************************** FXML Elements ******************************
	@FXML private Button btnCheckIn;
	@FXML private Button btnBack;
	@FXML private Hyperlink lnkForgot;
	@FXML private TextField txtConfirmCode;
	@FXML private Label lblUser;
	@FXML private Label lblError;
	
	// Modal containers
	@FXML private StackPane modalOverlay;
	
	private ClientForgotConfirmCodeScreen forgotModalsCTRL;
	private Parent ForgotIDModalRoot;

	// ****************************** Instance Methods ******************************

	/**
	 * Initializes the Client Check-In Table screen.
	 */
	@FXML
	public void initialize() {
		// 1. Safe User Loading (Prevents crash if client not fully ready)
		if (BistroClientGUI.client != null && BistroClientGUI.client.getUserCTRL().getLoggedInUser() != null) {
			User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
			if (lblUser != null) {
				lblUser.setText(currentUser.getUserType().name());
			}
		}

		// 2. Length Limiter (UX)
		int maxLength = 8;
		txtConfirmCode.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.length() > maxLength) {
				txtConfirmCode.setText(oldValue);
			}
		});
	}
	
	/**
	 * Handles the Check-In button click event.
	 * Now uses ASYNC listener logic instead of immediate checking.
	 */
	@FXML
	public void btnCheckIn(Event event) {
		String code = txtConfirmCode.getText();

		// 1. Input Validation (Client Side)
		String errorMsg = InputCheck.checkConfirmationCode(code);
		if (errorMsg != null) {
			BistroClientGUI.display(lblError, errorMsg, Color.RED);
			return;
		}

		// 2. Clear previous errors
		lblError.setText(""); 
		btnCheckIn.setDisable(true); // Prevent double-clicking

		// 3. Register Listener for Server Response (Async)
		BistroClientGUI.client.getTableCTRL().setCheckInListener((isSuccess, message) -> {
			Platform.runLater(() -> {
				btnCheckIn.setDisable(false); // Re-enable button
				
				if (isSuccess) {
					BistroClientGUI.switchScreen(event, "clientCheckInTableSucces", "Error loading Success Screen");
				} else {
					BistroClientGUI.display(lblError, message, Color.RED);
				}
			});
		});

		// 4. Send Request
		BistroClientGUI.client.getReservationCTRL().CheckConfirmationCodeCorrect(code);
	}
	
	/**
	 * Handles the Back button click event.
	 */
	@FXML
	public void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "ClientDashboardScreen", "Error returning to Dashboard");
	}
	
	/**
	 * Opens the Forgot Code Modal.
	 */
	@FXML
	public void lnkForgot(Event event) {
		if (modalOverlay == null) {
			System.err.println("Error: modalOverlay is null in FXML.");
			return;
		}

		if (ForgotIDModalRoot == null) {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/fxml/ClientForgotConfirmationCode.fxml"));
			try {
				ForgotIDModalRoot = loader.load();
				forgotModalsCTRL = loader.getController();
				// Link the modal back to this parent so it can call close()
				forgotModalsCTRL.setParent(this);
			} catch (Exception e) {
				e.printStackTrace();
				BistroClientGUI.display(lblError, "Unable to open Forgot Code screen.", Color.RED);
				return;
			}
		}
		
		// Add to overlay if not already there
		if (!modalOverlay.getChildren().contains(ForgotIDModalRoot)) {
			modalOverlay.getChildren().add(ForgotIDModalRoot);
		}
		
		modalOverlay.setVisible(true);
		modalOverlay.setManaged(true);
	}
	
	/**
	 * Displays an error message on the screen.
	 */
	public void showSuccessMessage(String message) {
		BistroClientGUI.display(lblError, message, Color.GREEN);
	}
	
	/**
	 * Closes the Forgot Code modal screen.
	 * Fixed logic to correctly hide the overlay.
	 */
	public void closeForgotCodeScreen() {
		if (modalOverlay != null) {
			modalOverlay.setVisible(false);
			modalOverlay.setManaged(false);
			// Optional: Remove it to save memory or keep it to load faster next time
			// modalOverlay.getChildren().clear(); 
			// ForgotIDModalRoot = null; 
		} else {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setContentText("Unable to close the Forgot Code screen (Overlay not found).");
			alert.showAndWait();
		}
	}
}