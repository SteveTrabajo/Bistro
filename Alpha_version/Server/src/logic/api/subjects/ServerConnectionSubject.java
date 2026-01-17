package logic.api.subjects;

import comms.Api;
import comms.Message;
import logic.api.ServerRouter;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

/**
 * API handlers related to client connections.
 */
public final class ServerConnectionSubject {
	// ******************************** Constructors***********************************
    private ServerConnectionSubject() {}
	// ******************************** Static Methods***********************************
    /**
     * Registers connection-related handlers.
     * @param logger 
     * @param logger 
     * @param dbController 
     */
    public static void register(ServerRouter router, ServerLogger logger) {

    	// Handle client connection
        router.on("connection", "connect", (msg, client) -> {
        	logger.log("[INFO] Client connected: " + client);
            client.sendToClient(new Message(Api.REPLY_CONNECTION_CONNECT_OK, null));
        });
        
		// Handle client disconnection
        router.on("connection", "disconnect", (msg, client) -> {
        	logger.log("[INFO] Client disconnected: " + client);
            client.sendToClient(new Message(Api.REPLY_CONNECTION_DISCONNECT_OK, null));
            client.close();
        });
    }
}
//End of ServerConnectionSubject.java