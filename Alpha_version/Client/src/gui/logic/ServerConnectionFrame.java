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
	 * If valid, uses TaskRunner to attempt a connection in a background thread
	 * while showing a loading overlay.
	 * * @param event The event triggered by clicking the connect button, used to locate the root pane.
	 */
	@FXML
	public void btnConnect(Event event) {
		String ip = ipTextField.getText();
		String port = portTextField.getText();
		String errorMessage = InputCheck.isValidPortAndIP(ip, port);		
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED);
			return;
		}
		int intPort = Integer.parseInt(port);
		TaskRunner.run(event,
			() -> {
				try {
					// Attempt connection
					BistroClientGUI.client = BistroClient.getInstance(ip, intPort);
					System.out.println("IP Entered Successfully");
					BistroClientGUI.client.notifyServerOnConnection();
				} catch (Exception e) {
					System.out.println("Error: Can't setup connection!");
					e.printStackTrace();
					Platform.runLater(() -> 
						BistroClientGUI.display(lblError, "Can't setup connection", Color.RED)
					);
					// Throw runtime exception to stop TaskRunner from executing the 'onSuccess' block
					throw new RuntimeException("Connection failed", e);
				}
			}, 
			() -> {
				BistroClientGUI.switchScreen(event, "clientLoginScreen", "Server Connection");
			}
		);
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