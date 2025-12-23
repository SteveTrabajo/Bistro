package logic;

import java.awt.event.ActionEvent;
import java.io.IOException;

import gui.logic.ServerConnectionFrame;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/*
 * This class represents the GUI application for the Bistro client.
 */

public class BistroClientGUI extends Application {
	
	//****************************** Instance variables ******************************

	public static BistroClient client; // Static client instance for server communication

	private static Stage primaryStage; // Primary stage for the application
	
	//******************************** Main Method ***********************************
	
	/*
	 * Main method to launch the JavaFX application.
	 * 
	 * @param args Command-line arguments.
	 */
	public static void main(String[] args) {
		launch(args); // Launch the JavaFX application
	}
	//******************************** Start Method **********************************
	
	/*
	 * Method to start the JavaFX application and establish a connection to the
	 * server.
	 * 
	 * @param primaryStage The primary stage for the application.
	 * 
	 * @throws Exception If there is an error during startup.
	 */

	@Override
	public void start(Stage primaryStage) throws Exception {
		BistroClientGUI.primaryStage = primaryStage;
		ServerConnectionFrame connectionFrame = new ServerConnectionFrame(); // Create server connection frame
		connectionFrame.start(primaryStage); // Start the connection frame
		primaryStage.centerOnScreen(); // Center the primary stage on the screen
	}
	
	//****************************** Static Methods ******************************
	
	public static void switchScreen(Event event, String fxmlName, String errorMessage) {
        try {
            // 1. Build the path dynamically
            String path = "/gui/fxml/" + fxmlName + ".fxml";
            
            // 2. Load the FXML
            FXMLLoader loader = new FXMLLoader(BistroClientGUI.class.getResource(path));
            Parent root = loader.load();
            
            // 3. Get the current Stage (Window) from the event that triggered this
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            
            // 4. Set the new Scene
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading screen: " + fxmlName + "\n" + errorMessage);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not load the requested screen due to unforseen circumstances, we'll look into it.");
            alert.showAndWait();
        }
    }
	
	/*
	 * Method to display an error message in a label with a specified color.
	 * 
	 * @param lbl The label to display the message.
	 * 
	 * @param message The message to display.
	 * 
	 * @param color The color of the message text.
	 */
	public static void display(Label lbl, String message, Color color) {
		lbl.setText(message); // Sets the error message in the label
		lbl.setTextFill(color); // Sets the text color for the error message
	}
	
	/**
     * Called by BistroClient when the server disconnects.
     * Blurs the entire UI, blocks interaction, and shows an exit dialog.
     */
    public static void showServerDisconnected(String message) {
        if (primaryStage == null) {
            // As a fallback, just exit.
            safeExit();
            return;
        }

        Scene scene = primaryStage.getScene();
        if (scene == null) {
            safeExit();
            return;
        }

        Parent root = scene.getRoot();

        // Disable interaction with the entire app
        root.setDisable(true);

        // Blur the UI
        root.setEffect(new GaussianBlur(15));

        // Show an alert dialog with an Exit button
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(primaryStage);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setTitle("Server Disconnected");
        alert.setHeaderText("Connection to the server was lost");
        alert.setContentText(message);

        ButtonType exitButton = new ButtonType("Exit", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(exitButton);

        // When the alert closes, exit safely
        alert.setOnHidden(_ -> safeExit());

        alert.show();
    }

    /**
     * Closes the client connection (if any) and exits the application.
     */
    public static void safeExit() {
        try {
            if (client != null && client.isConnected()) {
                client.closeConnection();
            }
        } catch (IOException e) {
            // ignore, we are exiting anyway
        }
        Platform.exit();
        System.exit(0);
    }
}
