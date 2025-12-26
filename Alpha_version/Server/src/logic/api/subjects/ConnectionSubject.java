package logic.api.subjects;

import comms.Api;
import comms.Message;
import logic.api.Router;
import logic.ServerLogger;

/**
 * API handlers related to client connections.
 */
public final class ConnectionSubject {

    private ConnectionSubject() {}

    /**
     * Registers connection-related handlers.
     */
    public static void register(Router router) {

        router.on("connection", "connect", (msg, client) -> {
        	ServerLogger.log("Client connected: " + client);
            client.sendToClient(new Message(Api.REPLY_CONNECTION_CONNECT_OK, null));
        });

        router.on("connection", "disconnect", (msg, client) -> {
            ServerLogger.log("Client disconnected: " + client);
            client.close();
        });
    }
}
