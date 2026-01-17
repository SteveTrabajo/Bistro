package logic;

import javafx.application.Platform;
import java.io.IOException;
import comms.*;
import logic.api.*;
import logic.api.subjects.*;
import ocsf.client.*;

/*
 * This class represents a client that connects to a Bistro server.
 */

public class BistroClient extends AbstractClient {
	
	//****************************** Instance variables ******************************
	
	private static BistroClient clientInstance;
	
	private final ClientRouter router;
	
	private final UserController userCTRL;
	
	private final ReservationController reservationCTRL;
	
	private final WaitingListController waitingListCTRL;
	
	private final TableController tableCTRL;
	
	private final PaymentController paymentCTRL;
	
	private final MonthlyReportsController monthlyReportsCTRL;
	
	public static Message messageFromServer;
	
	public static boolean awaitResponse = false;
	
	
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
		this.router = new ClientRouter();
		this.userCTRL = new UserController(this);
		this.reservationCTRL = new ReservationController(this);
		this.waitingListCTRL = new WaitingListController(this);
		this.tableCTRL = new TableController(this);
		this.paymentCTRL = new PaymentController(this);
		this.monthlyReportsCTRL = new MonthlyReportsController(this);
		registerHandlers(); // Register message handlers
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
	
	//****************************** Getters and Setters ******************************
		/**
		 * Getter for the User_Controller associated with this client.
		 * 
		 * @return The User_Controller instance.
		 */
		public UserController getUserCTRL() {
			return this.userCTRL;
		}
		
		/**
		 * Getter for the Reservation_Controller associated with this client.
		 * 
		 * @return The Reservation_Controller instance.
		 */
		public ReservationController getReservationCTRL() {
			return this.reservationCTRL;
		}
		
		/**
		 * Getter for the WaitingList_Controller associated with this client.
		 * 
		 * @return The WaitingList_Controller instance.
		 */
		public WaitingListController getWaitingListCTRL() {
			return this.waitingListCTRL;
		}
		
		/**
		 * Getter for the Table_Controller associated with this client.
		 * 
		 * @return The Table_Controller instance.
		 */
		public TableController getTableCTRL() {
			return this.tableCTRL;
		}
		
		/**
		 * Getter for the Payment_Controller associated with this client.
		 * 
		 * @return The Payment_Controller instance.
		 */
		public PaymentController getPaymentCTRL() {
			return this.paymentCTRL;
		}
		
		/**
		 * Getter for the MonthlyReports_Controller associated with this client.
		 * 
		 * @return The MonthlyReports_Controller instance.
		 */
		public MonthlyReportsController getMonthlyReportsCTRL() {
			return this.monthlyReportsCTRL;
		}
		
	//******************************** Instance methods ********************************
		
	/**
	 * Method to register message handlers for different message types.
	 */
	private void registerHandlers() {
		// Register API subjects
		ClientUserSubject.register(router, this.userCTRL);
		ClientOrderSubject.register(router);
		ClientMonthlyReportsSubject.register(router);
		ClientWaitListSubject.register(router,this.waitingListCTRL, this.tableCTRL);
		ClientTablesSubject.register(router);
		ClientConnectionSubject.register(router);
		ClientPaymentSubject.register(router);
		ClientSystemSubject.register(router);
		ClientRestaurantManageSubject.register(router);
	}

	/**
	 * Method to handle messages received from the server.
	 * 
	 * @param msg The message received from the server.
	 */
	@Override
	protected void handleMessageFromServer(Object msg) {
		if(msg instanceof Message) {
			try {
				boolean handled = router.dispatch((Message) msg);
				if (!handled) {
					System.out.println("No handler found for message ID: " + ((Message) msg).getId());
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error handling message from server: " + e.getMessage());
			}
		}
	}
	
	/**
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
	
	/**
	 * Method to notify the server when the client is exiting.
	 */
	public void notifyServerOnExit() {
		try {
			sendToServer(new Message(Api.ASK_CONNECTION_DISCONNECT, null));
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
	
	/**
	 * Method to notify the server when the client successfully connects.
	 */
	public void notifyServerOnConnection() {
		handleMessageFromClientUI(new Message(Api.ASK_CONNECTION_CONNECT, null));	
	}

	/**
	 * Handle connection closure by notifying the GUI.
	 */
	@Override
    protected void connectionClosed() {
        notifyServerDisconnected("The connection to the server was closed, please exit the application.");
    }
	
	/**
	 * Handle connection exceptions by notifying the GUI.
	 * 
	 * @param exception The exception that occurred.
	 */
    @Override
    protected void connectionException(Exception exception) {
        notifyServerDisconnected("A connection error occurred, please exit the application.");
    }
    
    /**
	 * Notify the GUI that the server has disconnected.
	 * 
	 * @param message The message to display to the user.
	 */
    private void notifyServerDisconnected(String message) {
        // This is called from the client's thread � we must switch to JavaFX thread
        Platform.runLater(() -> {
            BistroClientGUI.showServerDisconnected(message);
        });
    }
    
   /**
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

   
}
// End of BistroClient.java