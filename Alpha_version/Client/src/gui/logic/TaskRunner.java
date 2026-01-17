package gui.logic;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.net.URL;

/**
 * Utility class to handle background tasks and loading screen management.
 * Provides a generic way to run blocking tasks without freezing the UI.
 */
public class TaskRunner {
	
	//********************** Instance Variables ************************
	
    private static Parent loadingView;
 
    //********************** Instance Methods ************************
    
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
                
                URL resourceUrl = TaskRunner.class.getResource(fxmlPath);
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
     * Executes a background task while displaying a loading overlay on the root container of the event source.
     * @param event ,The event triggered by a UI action.
     * @param backgroundTask The logic to run in a separate thread.
     * @param onSuccess The logic to run on the UI thread after the background task completes.
     */
    public static void run(Event event, Runnable backgroundTask, Runnable onSuccess) {
        //Find the source Node
        Node source = (Node) event.getSource();
        //Get the Scene and then the Root container
        Scene scene = source.getScene();
        Parent root = scene.getRoot();
        //Check if we can overlay the loading screen
        if (root instanceof StackPane) {
            run((StackPane) root, backgroundTask, onSuccess);
        }
       else if (root instanceof BorderPane) {
            	run((BorderPane) root, backgroundTask, onSuccess);
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
     * @param rootPane, The StackPane where the loading overlay will be attached.
     * @param backgroundTask, The logic to run in a separate thread.
     * @param onSuccess, The logic to run on the UI thread after the background task completes.
     */
    public static void run(StackPane rootPane, Runnable backgroundTask, Runnable onSuccess) {
        //Prepare loading view
        Parent loading = getLoadingView();
        // Add to UI if not already there
        if (!rootPane.getChildren().contains(loading)) {
            rootPane.getChildren().add(loading);
        }
        // Block interactions
        loading.setMouseTransparent(false);
        loading.toFront(); // Ensure it's on top
        //Run Background Thread
        new Thread(() -> {
            try {
                // Execute heavy logic
                backgroundTask.run();
                //Back to UI Thread
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
            }
        }).start();
    }
    
    /**
     * Executes a background task while displaying a loading overlay on a specific StackPane.
     * 
     *@param rootPane, The BorderPane where the loading overlay will be attached.
     * @param backgroundTask, The logic to run in a separate thread.
     * @param onSuccess, The logic to run on the UI thread after the background task completes.
     */
    public static void run(BorderPane rootPane, Runnable backgroundTask, Runnable onSuccess) {
        //Prepare loading view
        Parent loading = getLoadingView();
        // Add to UI if not already there
        if (!rootPane.getChildren().contains(loading)) {
            rootPane.getChildren().add(loading);
        }
        // Block interactions
        loading.setMouseTransparent(false);
        loading.toFront(); // Ensure it's on top
        //Run Background Thread
        new Thread(() -> {
            try {
                // Execute heavy logic
                backgroundTask.run();
                //Back to UI Thread
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
            }
        }).start();
    } 
}
//End TaskRunner.java