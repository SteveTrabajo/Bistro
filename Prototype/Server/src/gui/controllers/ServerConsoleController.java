package gui.controllers;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import logic.BistroServer;
import logic.BistroServerGUI;

public class ServerConsoleController {
	
	@FXML
	private Button btnStart; // Start server button
	
	@FXML
	private Button btnStop; // Stop server button
	
	@FXML
	private Button btnClear; // Clear console button
	
	@FXML
	private Button btnSend; // Send command button
	
	@FXML
	private TextArea txtLog; // Console log area
	
	@FXML
	private TextField txtCommand; // Command input field
	
	
	/*
	 * Method to handle the Start button click event.
	 * Starts the Bistro server and begins listening for client connections.
	 * 
	 * @param event The event that triggered the button click.
	 */
	@FXML
	public void btnStart(Event event) {
		if(BistroServerGUI.server != null && BistroServerGUI.server.isListening()) {
			displayMessageToConsole("Server is already running and listening on port " + ServerPortFrameController.listeningPort);
			return;
		}
		displayMessageToConsole("Starting server...");
		try {
			BistroServerGUI.server = new BistroServer(ServerPortFrameController.listeningPort,this);
		} catch (Exception e) {
			e.printStackTrace();
			displayMessageToConsole("Error starting server: " + e.getMessage());
		}
		Thread startServerThread = new Thread(new Runnable() {
			public void run() {
				try {
					BistroServerGUI.server.listen();
				} catch (Exception e) {
					e.printStackTrace();
					Platform.runLater(() -> displayMessageToConsole("Error: Could not listen on port " + ServerPortFrameController.listeningPort));
				}
			}
		});
		startServerThread.start();
	}
	
	/*
	 * Method to handle the Stop button click event. Stops the Bistro server and
	 * disconnects all clients.
	 * 
	 * @param event The event that triggered the button click.
	 */
	@FXML
	public void btnStop(Event event) {
		displayMessageToConsole("Stopping server...");
		// Stop the server in a separate thread to avoid blocking the UI
		Thread stopServerThread = new Thread(new Runnable() {
			public void run() {
				try {
					BistroServerGUI.server.close();
				} catch (Exception e) {
					e.printStackTrace();
					Platform.runLater(() -> displayMessageToConsole("Error stopping server: " + e.getMessage() + "\n"));
				}
			}
		});
		stopServerThread.start();
	}
	

	/*
	 * Method to handle the Clear button click event.
	 * Clears the console log area.
	 * 
	 * @param event The event that triggered the button click.
	 */
	@FXML
	public void btnClear(Event event) {
		txtLog.clear();
	}
	
	/*
	 * Method to handle the Send button click event.
	 * Processes commands entered in the command input field.
	 * 
	 * @param event The event that triggered the button click.
	 */
	@FXML
	public void btnSend(Event event) {
		String cmd= txtCommand.getText();
		if(BistroServerGUI.server == null && BistroServerGUI.server.isListening()) {
			displayMessageToConsole("Server is not running. Please start the server first.");
		}
		switch(cmd.trim().toLowerCase()) {
		case "/start":
			btnStart(event);
			break;
		case "/stop":
			btnStop(event);
			break;
		case "/clear":
			btnClear(event);
			break;
		case "/connections":
			BistroServerGUI.server.showAllConnections();
			break;
		case "/help":
			displayMessageToConsole("Available commands:\n/start - Start the server\n/stop - Stop the server\n/clear - Clear the console log\n/connections - Show all active client connections\n/help - Show this help message");
			break;
		case "":
			displayMessageToConsole("No command entered. Type /help for a list of available commands.");
			break;
		default:
			displayMessageToConsole("Unknown command: " + cmd + ". Type /help for a list of available commands.");
			break;
		}	
	}
	
	/*
	 * Method to display a message in the console log area.
	 * 
	 * @param message The message to be displayed.
	 */
	public void displayMessageToConsole(String message) {
		txtLog.appendText(">"+ message + "\n");
	}
}
