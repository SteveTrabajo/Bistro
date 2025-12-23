package logic;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import entities.User;
import ocsf.client.*;
import massages.*;

/*
 * This class represents a client that connects to a Bistro server.
 */

public class BistroClient extends AbstractClient {
	
	//****************************** Instance variables ******************************
	
	private static BistroClient clientInstance;

	public static Message messageFromServer;
	
	public static boolean awaitResponse = false;
	
	private final User_Controller userCTRL; //final to ensure there is only one instance associated with the client
	
	//******************************** Constructors ***********************************
	
	/*
	 * Constructor to initialize the BistroClient with the server's host and port
	 * 
	 * @param host The server's hostname or IP address
	 * 
	 * @param port The server's port number
	 * 
	 * @throws Exception If there is an error connecting to the server
	 */
	private BistroClient(String host, int port) throws Exception {
		super(host, port);
		try {
			openConnection(); // Attempt to open a connection
		} catch (IOException e) {
			throw new Exception("Could not connect to server at " + host + ":" + port, e);
		}
		this.userCTRL = new User_Controller(this);
	}
	
	/*
	 * Method to get the singleton instance of BistroClient.
	 * 
	 * @param host The server's hostname or IP address
	 * 
	 * @param port The server's port number
	 * 
	 * @return The singleton instance of BistroClient
	 * 
	 * @throws Exception If there is an error connecting to the server
	 */
	public static synchronized BistroClient getInstance(String host, int port) throws Exception {
		if (clientInstance == null) {
			clientInstance = new BistroClient(host, port);
		}
		return clientInstance;
	}
	
	//******************************** Instance methods ********************************
	
	/*
	 * Method to handle messages received from the server.
	 * 
	 * @param msg The message received from the server.
	 */
	@Override
	protected void handleMessageFromServer(Object msg) {
		BistroClient.messageFromServer = (Message) msg; // Update static message variable
		awaitResponse = false; // Set response status to false
		switch(messageFromServer.getId()) {
		case "REQ_TO_LOGIN_APPROVED":
			userCTRL.setLoggedInUser((User) messageFromServer.getData());
			break;
		default:
			// TODO: Handle other message types as needed
			break;
		}
	}
	
	/*
	 * Method to handle messages sent from the client UI to the server.
	 * 
	 * @param message The message to be sent to the server.
	 */
	public void handleMessageFromClientUI(Object message) {
		try {
			awaitResponse = true; // Indicate that a response is awaited
			sendToServer(message); // Send the message to the server
			// Wait for a response in a loop
			while (awaitResponse) {
				try {
					Thread.sleep(100); // Avoid busy-waiting
				} catch (InterruptedException e) {
					e.printStackTrace(); // Handle interruptions
				}
			}
		} catch (IOException e) {
			e.printStackTrace(); // Handle errors during message sending
			System.out.println("Could not send message to server: Terminating client." + e);
			System.exit(0);
		}
	}
	
	//TODO: check if method below are neccessary:
	/*
	 * Method to notify the server when the client is exiting.
	 */
	public void notifyServerOnExit() {
		try {
			sendToServer(new Message("disconnect", null));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: Could not notify server on exit." + e);
		}
		try {
			this.closeConnection();
			System.out.println("Client Disconnected from server successfully.");
		} catch (Exception e) {
			System.out.println("Error while closing connection: " + e.getMessage());
		}
	}
	
	/*
	 * Method to notify the server when the client successfully connects.
	 */
	public void notifyServerOnConnection() {
		try {
			handleMessageFromClientUI(new Message("connect", null));
			if (messageFromServer.getId().equals("connectionDisplayed")) {
				System.out.println("Connected to server and connection displayed.");
			}
		} catch (Exception e) {
			e.printStackTrace(); // Handle errors during connection notification
			System.out.println("Error: Could not notify server on connection." + e);
		}
	}

	// Server connection closed or lost handling
	@Override
    protected void connectionClosed() {
        notifyServerDisconnected("The connection to the server was closed, please exit the application.");
    }

    @Override
    protected void connectionException(Exception exception) {
        notifyServerDisconnected("A connection error occurred, please exit the application.");
    }

    private void notifyServerDisconnected(String message) {
        // This is called from the client's thread � we must switch to JavaFX thread
        Platform.runLater(() -> {
            BistroClientGUI.showServerDisconnected(message);
        });
    }
    
   /*
	* Method to terminate the client and close the connection.
	*/
	public void quit() {
		try {
			closeConnection(); // Close the connection
		} catch (IOException e) {
			e.printStackTrace(); // Handle errors during disconnection
			System.out.println("Error: Could not close connection properly." + e);
		}
		System.exit(0); // Exit the program
	}
	
	//****************************** Getters and Setters ******************************
	/*
	 * Getter for the User_Controller associated with this client.
	 * 
	 * @return The User_Controller instance.
	 */
	public User_Controller getUserCTRL() {
		return this.userCTRL;
	}

}
