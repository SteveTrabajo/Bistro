package logic;

import java.util.List;

import comms.Api;
import comms.Message;
import entities.Order;
import gui.controllers.ServerConsoleController;
import logic.api.Router;
import logic.api.subjects.ConnectionSubject;
import logic.api.subjects.OrdersSubject;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/*
 * BistroServer class that extends AbstractServer to handle client-server communication
 * for a Bistro application.
 */
public class BistroServer extends AbstractServer {

	private final Router router = new Router();
	
	/*
	 * Constructor for BistroServer class
	 */
	public BistroServer(int port, ServerConsoleController serverConsoleController) {
		super(port);
		ServerLogger.setConsole(serverConsoleController);
		
		// Register API subjects
        OrdersSubject.register(router);
        ConnectionSubject.register(router);
	}

	/*
	 * Method to handle messages received from clients.
	 * 
	 * @param msg The message received from the client.
	 * 
	 * @param client The connection to the client that sent the message.
	 */
	@Override
	protected void handleMessageFromClient(Object obj, ConnectionToClient client) {
        if (!(obj instanceof Message)) {
            return;
        }

        Message msg = (Message) obj;
        ServerLogger.log("Received message: " + msg.getId() + " from " + client);

        try {
            boolean handled = router.dispatch(msg, client);

            if (!handled) {
                client.sendToClient(
                        new Message(Api.REPLY_UNKNOWN_COMMAND, msg.getId())
                );
                ServerLogger.log("Unknown command: " + msg.getId());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	/*
	 * Method called when the server starts listening for client connections.
	 */
	protected void serverStarted() {
		ServerLogger.log("Server started, listening for connections on port " + getPort());
		boolean isConnectToDB = BistroDataBase_Controller.openConnection();
		if (isConnectToDB) {
			ServerLogger.log("Connected to database successfully");
		} else {
			ServerLogger.log("Failed to connect to database");
		}
	}

	/*
	 * Method called when the server stops to close the database connection.
	 */
	protected void serverStopped() {
		ServerLogger.log("Server stopped");
		BistroDataBase_Controller.closeConnection();
	}

	/*
	 * Method to receive all client connections from abstract server and display
	 * them on the server console.
	 */
	public void showAllConnections() {
		Thread[] clientList = this.getClientConnections(); // Thread array of all clients
		ServerLogger.log("Number of connected clients: " + clientList.length);
		// Display each client's information
		for (Thread client : clientList) {
			ServerLogger.log("Client: " + client.toString());
		}
	}
}
// End of BistroServer.java