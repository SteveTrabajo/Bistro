package gui.logic;

import java.io.IOException;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import logic.BistroClient;
import logic.BistroClientGUI;
import common.InputCheck;

/*
 * This class represents the server connection screen controller.
 */
public class ServerConnectionFrame {
	//****************************** FXML Variables *****************************
	@FXML
	private Button btnConnect;
	
	@FXML	
	private Hyperlink lnkExit;
	
	@FXML
	private TextField ipTextField;
	
	@FXML
	private TextField portTextField;
	
	@FXML
	private Label lblError;	
	
	//****************************** FXML Methods *****************************
	
	/**
	 * Handles the connect button click event.
	 * Validates the IP address and port inputs.
	 * If valid, attempts to create a BistroClient instance and connect to the server.
	 * On successful connection, switches to the client login screen.
	 * Displays error messages for invalid inputs or connection failures.
	 * 
	 * @param event The event triggered by clicking the connect button.
	 */
	@FXML
	public void btnConnect(Event event) {
		String ip; // holds the entered IP address
		String port; // holds the entered Port
		int intPort; // holds the validated Port as an integer
		ip = ipTextField.getText(); // gets the entered IP address
		port = portTextField.getText(); // gets the entered Port
		// Validate the input fields
		String errorMessage = InputCheck.isValidPortAndIP(ip, port);
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED); // input is empty
		} else {
			intPort = Integer.parseInt(port); // converts the port to integer
			try {
				// Attempts to create a client instance and connect to the server.
				BistroClientGUI.client = BistroClient.getInstance(ip, intPort);
				System.out.println("IP Entered Successfully");
				BistroClientGUI.client.notifyServerOnConnection(); // Notify successful connection
				Platform.runLater(() -> BistroClientGUI.switchScreen(event, "clientLoginScreen", "Server Connection"));
			} catch (Exception e) {
				// Handles connection errors
				System.out.println("Error: Can't setup connection! \nThe error message: ");
				e.printStackTrace();
				BistroClientGUI.display(lblError, "Can't setup connection", Color.RED); // Displays an error message.
			}
		}
	}
	
	/**
	 * Handles the exit hyperlink click event.
	 * Safely exits the Bistro Client application.
	 * 
	 * @param event The event triggered by clicking the exit hyperlink.
	 */
	@FXML
	public void lnkExit(Event event) {
		BistroClientGUI.safeExit();
		System.out.println("Closed Bistro Client Successfully");
	}
	
	
	/**
	 * Method to start the Server Connection screen.
	 * 
	 * @param primaryStage The primary stage for the application.
	 */
	public void start(Stage primaryStage) {
		Parent root = null;
		try {
			root = FXMLLoader.load(getClass().getResource("/gui/fxml/ServerConnectionFrame.fxml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Create the scene and set up the stage
		Scene scene = new Scene(root);
		primaryStage.setTitle("Server Connection");
		primaryStage.setScene(scene);
		primaryStage.centerOnScreen();
		primaryStage.show();
		// Set the close request handler to notify the server on exit
		primaryStage.setOnCloseRequest(_ -> {
			try {
				if (BistroClientGUI.client != null) {
					BistroClientGUI.client.notifyServerOnExit();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Platform.exit();
				System.exit(0);
			}
		});
	}
}
// End of ServerConnectionFrame class