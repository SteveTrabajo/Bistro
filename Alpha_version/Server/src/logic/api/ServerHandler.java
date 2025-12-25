package logic.api;

import comms.Message;
import ocsf.server.ConnectionToClient;

/**
 * Handles a single server API request.
 */
@FunctionalInterface
public interface ServerHandler {

    /**
     * Executes logic for a received message.
     *
     * @param msg the received message
     * @param client the client that sent the message
     * @throws Exception if handling fails
     */
    void handle(Message msg, ConnectionToClient client) throws Exception;
}
