package gui.controllers;

import java.io.IOException;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import logic.BistroServerGUI;

/*
 * Controller class for the Server Port Frame.
 * Handles user interactions for entering and validating the server port number.
 */
public class ServerPortFrameController {
	
 	public static int listeningPort = 5555; // Default port number for server listening
	@FXML
	private Button btnDone; // Done button to confirm port entry
	
	@FXML
	private Button btnExit; // Exit button to close the application
	
	@FXML
	private Label lblError; // Label to display error messages
	
	@FXML
	private TextField txtPort; // TextField for entering the port number
	
	
	/*
	 * Method to handle the Done button click event.
	 * Validates the entered port number and proceeds if correct.
	 * 
	 * @param event The event triggered by clicking the Done button.
	 */
	@FXML
	public void btnDone(Event event) {
		String port = txtPort.getText();
		int intPort;
		// Validate port number
		try {
			intPort = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			BistroServerGUI.display(lblError," Invalid port number. Please enter a valid integer.", Color.RED);
			return;
		}
		// Check port range
		if(port.trim().isEmpty()) {
			BistroServerGUI.display(lblError," Port number cannot be empty.", Color.RED);
			return;
		}
		// Check if port is within valid range
		else if(intPort < 1024 || intPort > 65535) {
			BistroServerGUI.display(lblError," Port number must be between 1024 and 65535.", Color.RED);
			return;
		}
		else { // Valid port number
			listeningPort = intPort;
			BistroServerGUI.display(lblError," Port accepted.", Color.GREEN);
			// Proceed to server console
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/fxml/ServerConsole.fxml"));
				Parent root = loader.load();
				BistroServerGUI.switchScreen(loader, root, event, "Bistro Server Console");
				} catch (IOException e) {
					e.printStackTrace();
			}
		}
	}
	
	
	/*
	 * Method to handle the Exit button click event.
	 * Closes the application.
	 */
	@FXML
	public void btnExit(Event event) {
		Stage stage = (Stage) btnExit.getScene().getWindow();
		stage.close();
	}
	
	/*
	 * Method to start the Server Port Frame.
	 * Sets up the stage and scene for the port selection interface.
	 * 
	 * @param primaryStage The primary stage for this application.
	 */
	public void start(Stage primaryStage) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/fxml/ServerPort.fxml"));
		try {
			primaryStage.setTitle("Bistro Server - Port Selection");
			primaryStage.setScene(new Scene(loader.load()));
			primaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
// End of ServerPortFrameController.java