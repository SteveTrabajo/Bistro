package logic;

import gui.controllers.ServerPortFrameController;
import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class BistroServerGUI extends Application {

	public static BistroServer server; // Static server instance for client communication

	/*
	 * Main method to launch the JavaFX application.
	 */
	public static void main(String[] args) {
		launch(args); // Launch the JavaFX application
	}

	/*
	 * Start method to initialize the primary stage of the JavaFX application.
	 * 
	 * @param primaryStage The primary stage for this application.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		ServerPortFrameController portFrame = new ServerPortFrameController();
		portFrame.start(primaryStage); // Start the port frame
	}

	/*
	 * Method to switch the current screen to a new screen.
	 * 
	 * @param loader The FXMLLoader for the new screen.
	 * 
	 * @param root The root node of the new screen.
	 * 
	 * @param event The event that triggered the screen switch.
	 * 
	 * @param string The title for the new screen.
	 */
	public static void switchScreen(FXMLLoader loader, Parent root, Event event, String string) {
		Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
		Scene newScene = new Scene(root);
		currentStage.setTitle(string);
		currentStage.setScene(newScene);
		currentStage.centerOnScreen();
		currentStage.show();
	}

	/*
	 * Method to display an error message or messages in a label with a specified
	 * color.
	 * 
	 * @param lblError The label to display the error message / message .
	 * 
	 * @param message The error message to be displayed.
	 * 
	 * @param color The color of the error message text.
	 */
	public static void display(Label lblError, String message, Color color) {
		lblError.setText(message); // Sets the error message in the label
		lblError.setTextFill(color); // Sets the text color for the error message
	}
}
// End of BistroServerGUI.java
