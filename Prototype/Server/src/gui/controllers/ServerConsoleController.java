package gui.controllers;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import logic.BistroServer;
import logic.BistroServerGUI;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;

/*
 * Controller class for the Server Console Frame.
 * Handles user interactions for starting/stopping the server,
 * sending commands, and displaying console logs.
 */
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

	// Map-based command handlers
	private final Map<String, Consumer<Event>> commandHandlers = new HashMap<>();

	// Initialize is called by JavaFX after FXML injection
	@FXML
	public void initialize() {
		// Populate the command handlers map
		commandHandlers.put("/start", e -> btnStart(e));
		commandHandlers.put("/stop", e -> btnStop(e));
		commandHandlers.put("/clear", e -> btnClear(e));
		commandHandlers.put("/connections", e -> {
			if (BistroServerGUI.server == null || !BistroServerGUI.server.isListening()) {
				displayMessageToConsole("Server is not running. Please start the server first.");
			} else {
				BistroServerGUI.server.showAllConnections();
			}
		});
		commandHandlers.put("/help", e -> displayMessageToConsole("Available commands:\n" 
				+ "/start - Start the server\n"
				+ "/stop - Stop the server\n"
				+ "/clear - Clear the console log\n"
				+ "/connections - Show all active client connections\n"
				+ "/help - Show this help message"));
	}

	/*
	 * Method to handle the Start button click event. Starts the Bistro server and
	 * begins listening for client connections.
	 * 
	 * @param event The event that triggered the button click.
	 */
	@FXML
	public void btnStart(Event event) {
		// Check if the server is already running
		if (BistroServerGUI.server != null && BistroServerGUI.server.isListening()) {
			displayMessageToConsole(
					"Server is already running and listening on port " + ServerPortFrameController.listeningPort);
		} else {
			displayMessageToConsole("Starting server...");
			// Start the server in a separate thread to avoid blocking the javafx UI thread:
			try {
				// create a new server singleton instance:
				BistroServerGUI.server = new BistroServer(ServerPortFrameController.listeningPort, this);
			} catch (Exception e) {
				e.printStackTrace();
				displayMessageToConsole("Error starting server: " + e.getMessage());
			}
			try {
				BistroServerGUI.server.listen();
			} catch (Exception e) {
				e.printStackTrace();
				displayMessageToConsole("Error: Could not listen on port " + ServerPortFrameController.listeningPort);
			}
		}
	}

	/*
	 * Method to handle the Stop button click event. Stops the Bistro server and
	 * disconnects all clients.
	 * 
	 * @param event The event that triggered the button click.
	 */
	@FXML
	public void btnStop(Event event) {
		// Check if the server is running:
		if (BistroServerGUI.server == null || !BistroServerGUI.server.isListening()) {
			displayMessageToConsole("Server is not running. Please start the server first.");
		} else {
			displayMessageToConsole("Stopping server...");
			try {
				BistroServerGUI.server.close();
			} catch (Exception e) {
				e.printStackTrace();
				displayMessageToConsole("Error stopping server: " + e.getMessage() + "\n");
			}
		}
	}

	/*
	 * Method to handle the Clear button click event. Clears the console log area.
	 * 
	 * @param event The event that triggered the button click.
	 */
	@FXML
	public void btnClear(Event event) {
		txtLog.clear();
	}

	/*
	 * Method to handle the Send button click event. Processes commands entered in
	 * the command input field.
	 * 
	 * @param event The event that triggered the button click.
	 */
	@FXML
	public void btnSend(Event event) {
		String cmdRaw = txtCommand.getText();
		if (cmdRaw == null) {
			cmdRaw = "";
		}

		String cmd = cmdRaw.trim().toLowerCase();

		// Look up the handler in the map and execute it if present
		Consumer<Event> handler = commandHandlers.get(cmd);
		if (handler != null) {
			handler.accept(event);
		} else if (cmd.isEmpty()) {
			displayMessageToConsole("No command entered. Type /help for a list of available commands.");
		} else {
			displayMessageToConsole("Unknown command: " + cmdRaw + ". Type /help for a list of available commands.");
		}

		txtCommand.clear();
	}

	/*
	 * Method to display a message in the console log area.
	 * 
	 * @param message The message to be displayed.
	 */
	public void displayMessageToConsole(String message) {
		if (Platform.isFxApplicationThread()) {
			txtLog.appendText(">" + message + "\n");
		} else {
			Platform.runLater(() -> txtLog.appendText(">" + message + "\n"));
		}
	}
}
//End of ServerConsoleController.java