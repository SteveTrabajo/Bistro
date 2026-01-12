package gui.logic;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import java.io.IOException;

/**
 * Utility class to handle background tasks and loading screen management.
 * Provides a generic way to run blocking tasks without freezing the UI.
 */
public class TaskRunner {

    private static Parent loadingView;

    /**
     * Loads the FXML for the loading screen. Singleton pattern.
     *
     * @return The loaded Parent node of the loading view.
     */
    private static Parent getLoadingView() {
        if (loadingView == null) {
            try {
                // Path to your FXML based on your project structure
                String fxmlPath = "/gui/fxml/LoadingView.fxml";
                
                java.net.URL resourceUrl = TaskRunner.class.getResource(fxmlPath);
                if (resourceUrl == null) {
                    throw new IllegalStateException("FXML file NOT found at: " + fxmlPath);
                }

                FXMLLoader loader = new FXMLLoader(resourceUrl);
                loadingView = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
                return new StackPane(); // Fallback to avoid crash
            }
        }
        return loadingView;
    }

    /**
     * Executes a background task automatically finding the root StackPane from the Event.
     * Best used when the UI root is a StackPane (standard for overlaying loading screens).
     *
     * @param event          The UI event (e.g., button click) used to find the active scene.
     * @param backgroundTask The logic to run in a separate thread.
     * @param onSuccess      The logic to run on the UI thread after success.
     */
    public static void run(Event event, Runnable backgroundTask, Runnable onSuccess) {
        // 1. Find the source Node (the button clicked)
        Node source = (Node) event.getSource();
        
        // 2. Get the Scene and then the Root container
        Scene scene = source.getScene();
        Parent root = scene.getRoot();

        // 3. Check if we can overlay the loading screen
        if (root instanceof StackPane) {
            run((StackPane) root, backgroundTask, onSuccess);
        } else {
            // If the root isn't a StackPane, we can't do a pretty overlay, 
            // but we still run the thread to avoid freezing.
            System.err.println("Warning: Root is not a StackPane. Loading overlay skipped.");
            new Thread(() -> {
                try {
                    backgroundTask.run();
                    Platform.runLater(onSuccess);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Executes a background task while displaying a loading overlay on a specific StackPane.
     *
     * @param rootPane       The StackPane where the loading overlay will be attached.
     * @param backgroundTask The logic to run in a separate thread (e.g., database queries).
     * @param onSuccess      The logic to run on the UI thread after the background task completes.
     */
    public static void run(StackPane rootPane, Runnable backgroundTask, Runnable onSuccess) {
        
        // 1. Prepare loading view
        Parent loading = getLoadingView();
        
        // Add to UI if not already there
        if (!rootPane.getChildren().contains(loading)) {
            rootPane.getChildren().add(loading);
        }
        
        // Block interactions
        loading.setMouseTransparent(false);
        loading.toFront(); // Ensure it's on top

        // 2. Run Background Thread
        new Thread(() -> {
            try {
                // Execute heavy logic
                backgroundTask.run();

                // 3. Back to UI Thread
                Platform.runLater(() -> {
                    rootPane.getChildren().remove(loading);
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                // Always remove loading screen on error
                Platform.runLater(() -> rootPane.getChildren().remove(loading));
                // Optional: Re-throw if you want to catch it higher up, 
                // but usually better to handle UI error display here or pass an onError callback.
            }
        }).start();
    }
}