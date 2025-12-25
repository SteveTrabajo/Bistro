package logic;

import java.util.List;

import comms.Message;
import entities.Order;
import gui.controllers.ServerConsoleController;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/*
 * BistroServer class that extends AbstractServer to handle client-server communication
 * for a Bistro application.
 */
public class BistroServer extends AbstractServer {

	private ServerConsoleController serverConsole; // Reference to the server console controller

	/*
	 * Constructor for BistroServer class
	 */
	public BistroServer(int port, ServerConsoleController serverConsoleController) {
		super(port);
		this.serverConsole = serverConsoleController;
	}

	/*
	 * Method to handle messages received from clients.
	 * 
	 * @param msg The message received from the client.
	 * 
	 * @param client The connection to the client that sent the message.
	 */
	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		if (msg instanceof Message) { // Ensure the message is of type Message
			// Cast the message to Message type:
			Message messageObj = (Message) msg;
			String messageId = messageObj.getId();
			String messageToDisplay;
			// Log the received message:
			messageToDisplay = "Message received: " + messageId + " from: " + client;
			System.out.print(messageToDisplay + "\n");
			serverConsole.displayMessageToConsole(messageToDisplay);
			// Handle different message IDs:
			try {
				switch (messageId) {

				// case client requests the full orders list.
				case "getOrdersList":
					// Retrieve all orders from the database:
					List<Order> allOrders = BistroDataBase_Controller.getAllOrders();
					client.sendToClient(new Message("ordersList", allOrders));
					// Log the action:
					messageToDisplay = client + " requested the full orders list. Total orders sent: "
							+ allOrders.size();
					serverConsole.displayMessageToConsole(messageToDisplay);
					return;

				// case client requests to update an order status.
				case "updateOrderStatus":
					// The client sends an Order object as the data field:
					Order orderToUpdate = (Order) messageObj.getData();
					// check if date is taken:
					if (!BistroDataBase_Controller.isDateAvailable(orderToUpdate.getOrderDate(),
							orderToUpdate.getConfirmationCode())) {
						client.sendToClient(new Message("dateNotAvailable", null));
						// Log the action:
						messageToDisplay = client + " attempted to update order with confirmation code: "
								+ orderToUpdate.getConfirmationCode() + " but the date is already taken.";
						serverConsole.displayMessageToConsole(messageToDisplay);
						return;
					}
					boolean updateStatus = BistroDataBase_Controller.updateOrder(orderToUpdate);
					// Send response back to client based on update status:
					if (updateStatus) {
						// Match the string your controller already expects:
						client.sendToClient(new Message("updateOrderSuccess", null));
						// Log the action:
						messageToDisplay = client + " updated order with confirmation code: "
								+ orderToUpdate.getConfirmationCode() + " Successfully.";
						serverConsole.displayMessageToConsole(messageToDisplay);
					} else {
						// Currently we only know "update failed", treat it as invalid code:
						client.sendToClient(new Message("invalidConfirmCode", null));
						// Log the action:
						messageToDisplay = client + " attempted to update order with invalid confirmation code";
						serverConsole.displayMessageToConsole(messageToDisplay);
					}
					return;

				// case client request an order by confirmation code.
				case "getOrderByConfirmationCode":
					// The client sends an integer confirmation code as the data field:
					int confirmationCode = (int) messageObj.getData();
					Order order = BistroDataBase_Controller.getOrderByConfirmationCode(confirmationCode);
					client.sendToClient(new Message("orderByConfirmationCode", order));
					// Log the action:
					messageToDisplay = client + " requested order with confirmation code: " + confirmationCode;
					serverConsole.displayMessageToConsole(messageToDisplay);
					return;

				case "connect":
					// Log the connection:
					messageToDisplay = client + " has connected.";
					serverConsole.displayMessageToConsole(messageToDisplay);
					client.sendToClient(new Message("connectionDisplayed", null));
					return;

				case "disconnect":
					// Log the disconnection:
					messageToDisplay = client + " has disconnected.";
					serverConsole.displayMessageToConsole(messageToDisplay);
					client.close(); // Close the client connection
					return;

				// Default case when client request and unknownCommand.
				default:
					client.sendToClient(new Message("unknownCommand", "Unknown command: " + messageId));
					// Log the action:
					messageToDisplay = client + " sent an unknown command: " + messageId;
					serverConsole.displayMessageToConsole(messageToDisplay);
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Method called when the server starts listening for client connections.
	 */
	protected void serverStarted() {
		System.out.println("Server started");
		serverConsole.displayMessageToConsole("Server started, listening for connections on port " + getPort());
		boolean isConnectToDB = BistroDataBase_Controller.openConnection();
		if (isConnectToDB) {
			serverConsole.displayMessageToConsole("Connected to database successfully");
		} else {
			serverConsole.displayMessageToConsole("Failed to connect to database");
		}
	}

	/*
	 * Method called when the server stops to close the database connection.
	 */
	protected void serverStopped() {
		System.out.println("Server stopped");
		serverConsole.displayMessageToConsole("Server stopped");
		BistroDataBase_Controller.closeConnection();
	}

	/*
	 * Method to receive all client connections from abstract server and display
	 * them on the server console.
	 */
	public void showAllConnections() {
		Thread[] clientList = this.getClientConnections(); // Thread array of all clients
		serverConsole.displayMessageToConsole("Number of connected clients: " + clientList.length);
		// Display each client's information
		for (Thread client : clientList) {
			serverConsole.displayMessageToConsole("Client: " + client.toString());
		}
	}
}
// End of BistroServer.java