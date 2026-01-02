package gui.logic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PreviewScreen extends Application {

	// JavaFX VM options: (Don't forget to adjust the path to your javafx-sdk, the one below is for my PC [Julian's])
	// --module-path "F:\javafx-sdk-25.0.1\lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics
    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Define the path you think it is
        String path = "/gui/fxml/ClientManageBookingScreen.fxml";
        
        // 2. Check if it exists
        java.net.URL fileUrl = getClass().getResource(path);
        if (fileUrl == null) {
            System.err.println("CRITICAL ERROR: Could not find file at: " + path);
            System.err.println("Please check the folder name and file name exactly (Case Sensitive!)");
            return;
        }

        // 3. Load if found
        FXMLLoader loader = new FXMLLoader(fileUrl);
        Parent root = loader.load();


        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Screen Design Preview");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}