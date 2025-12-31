package logic.api.subjects;

import comms.Api;
import comms.Message;
import logic.api.Router;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

/**
 * API handlers related to client connections.
 */
public final class ConnectionSubject {
	// ******************************** Constructors***********************************
    private ConnectionSubject() {}
	// ******************************** Static Methods***********************************
    /**
     * Registers connection-related handlers.
     * @param logger 
     * @param logger 
     * @param dbController 
     */
    public static void register(Router router, ServerLogger logger) {

        router.on("connection", "connect", (msg, client) -> {
        	logger.log("[INFO] Client connected: " + client);
            client.sendToClient(new Message(Api.REPLY_CONNECTION_CONNECT_OK, null));
        });
        

        router.on("connection", "disconnect", (msg, client) -> {
        	logger.log("[INFO] Client disconnected: " + client);
            client.sendToClient(new Message(Api.REPLY_CONNECTION_DISCONNECT_OK, null));
            client.close();
        });
    }
}
//End of ConnectionSubject.java