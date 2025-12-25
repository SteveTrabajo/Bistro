package logic.api.subjects;

import comms.Api;
import comms.Message;
import logic.api.Router;
import logic.api.ServerContext;

/**
 * API handlers related to client connections.
 */
public final class ConnectionSubject {

    private ConnectionSubject() {}

    /**
     * Registers connection-related handlers.
     */
    public static void register(Router router, ServerContext ctx) {

        router.on("connection", "connect", (msg, client) -> {
            ctx.log("Client connected: " + client);
            client.sendToClient(new Message(Api.REPLY_CONNECTION_CONNECT_OK, null));
        });

        router.on("connection", "disconnect", (msg, client) -> {
            ctx.log("Client disconnected: " + client);
            client.close();
        });
    }
}
