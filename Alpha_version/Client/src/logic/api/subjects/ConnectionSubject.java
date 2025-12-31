package logic.api.subjects;

import javafx.application.Platform;
import logic.BistroClient;
import logic.api.ClientRouter;

public class ConnectionSubject {

    public static void register(ClientRouter router) {
        router.on("connection", "connect.ok", msg -> {
            // Release any waiting request loops
            BistroClient.awaitResponse = false;

            Platform.runLater(() ->
                System.out.println("Connection status: Displayed")
            );
        });

        router.on("connection", "disconnect.ok", msg -> {
            BistroClient.awaitResponse = false;
            // Handle disconnection logic here
        });
    }
}
