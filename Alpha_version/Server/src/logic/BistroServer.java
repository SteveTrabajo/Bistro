package logic;

import java.util.List;
import java.util.concurrent.*;

import comms.Api;
import comms.Message;
import entities.MonthlyReport;
import entities.Order;
import gui.controllers.ServerConsoleController;
import logic.api.Router;
import logic.api.subjects.ConnectionSubject;
import logic.api.subjects.OrdersSubject;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import logic.api.subjects.*;
import logic.services.*;

/**
 * BistroServer class that extends AbstractServer to handle client-server
 * communication for a Bistro application.
 */
public class BistroServer extends AbstractServer {

	// ****************************** Instance variables******************************
	// Singleton instance
	private static BistroServer serverInstance;
	// Database controller
	private final BistroDataBase_Controller dbController;
	// Router for API message handling
	private final Router router;
	// Server logger for logging events
	private final ServerLogger logger;
	//Services to handle the server algorithms:
	private final OrdersService ordersService;
	private final TableService tableService;
	private final WaitingListService waitingListService;
	private final NotificationService notificationService;
	private final UserService userService;
	private final ReportsService reportService;
	private final PaymentService paymentService;
	private final NoShowManager noShowManager;
	// Scheduler for background tasks:
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	// ******************************** Constructors***********************************

	/**
	 * Constructor for BistroServer class
	 * 
	 * @param port                    The port number for the server to listen on.
	 * @param serverConsoleController The controller for the server console UI.
	 */
	private BistroServer(int port, ServerConsoleController serverConsoleController) {
		super(port);
		this.dbController = BistroDataBase_Controller.getInstance();
		this.router = new Router();
		this.logger = new ServerLogger(serverConsoleController);
		this.dbController.setLogger(this.logger);
		// Initialize services:
		this.userService = new UserService(this.dbController, this.logger);
		this.ordersService = new OrdersService(this, this.dbController, null, this.logger);
		this.reportService = new ReportsService(this.dbController, this.logger);
		this.notificationService = new NotificationService(this.dbController, this.logger);
		this.tableService = new TableService(this.dbController, this.logger, this.ordersService, this.notificationService);
		this.paymentService = new PaymentService(this.dbController, this.logger, this.tableService);
		this.waitingListService = new WaitingListService(this,this.dbController,this.ordersService,this.tableService, this.logger, this.userService);
		this.noShowManager = new NoShowManager(this.dbController, this.logger);
		// Register API subjects
		registerHandlers(this.router, this.dbController, this.logger);
	}

	/**
	 * Static method to get the singleton instance of BistroServer.
	 * 
	 * @param port The port number for the server to listen on.
	 * 
	 * @param serverConsoleController The controller for the server console UI.
	 * 
	 * @return The singleton instance of BistroServer.
	 */
	public static synchronized BistroServer getInstance(int port, ServerConsoleController serverConsoleController) {
		if (serverInstance == null) {
			serverInstance = new BistroServer(port, serverConsoleController);
		}
		return serverInstance;
	}

	// *************************************Getters and Setters***************************************

	/**
	 * Getter for the BistroDataBase_Controller associated with this server.
	 * 
	 * @return The BistroDataBase_Controller instance.
	 */
	public BistroDataBase_Controller getDBController() {
		return this.dbController;
	}

	/**
	 * Getter for the ServerLogger associated with this server.
	 * 
	 * @return The ServerLogger instance.
	 */
	public ServerLogger getLogger() {
		return this.logger;
	}

	// ****************************** Overridden methods from AbstractServer ******************************
	
	/**
	 * Method to handle messages received from clients.
	 * 
	 * @param msg The message received from the client.
	 * 
	 * @param client The connection to the client that sent the message.
	 */
	@Override
	protected void handleMessageFromClient(Object obj, ConnectionToClient client) {
		if (!(obj instanceof Message)) { // Validate message type
			return;
		}
		Message msg = (Message) obj;
		logger.log("Received message: " + msg.getId() + " from " + client);
		try { // Dispatch message to appropriate handler
			boolean handled = router.dispatch(msg, client);
			if (!handled) { // Unknown command
				client.sendToClient(new Message(Api.REPLY_UNKNOWN_COMMAND, msg.getId()));
				logger.log("Unknown command: " + msg.getId());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * Method called when the server starts listening for client connections.
	 */
	protected void serverStarted() {
		logger.log("Server started, listening for connections on port " + getPort());
		boolean isConnectToDB = dbController.openConnection();
		if (isConnectToDB) {
			logger.log("Connected to database successfully");
			notificationService.startBackgroundTasks(); // Start notification background tasks
			noShowManager.startBackgroundTasks(); // Start no-show detection background tasks
		} else {
			logger.log("Failed to connect to database");
		}
	}

	/**
	 * Method called when the server stops to close the database connection.
	 */
	protected void serverStopped() {
		logger.log("Server stopped");
		notificationService.stop(); // Stop notification background tasks
		noShowManager.stop(); // Stop no-show detection background tasks
		dbController.closeConnection();
	}

	/**
	 * Method to receive all client connections from abstract server and display
	 * them on the server console.
	 */
	public void showAllConnections() {
		Thread[] clientList = this.getClientConnections(); // Thread array of all clients
		logger.log("Number of connected clients: " + clientList.length);
		// Display each client's information
		for (Thread client : clientList) {
			logger.log("Client: " + client.toString());
		}
	}

	// ****************************** Instance methods ******************************
	
	/**
	 * Registers API subjects and their handlers with the router.
	 * 
	 * @param router       The Router instance to register handlers with.
	 * 
	 * @param dbController The database controller for handling data operations.
	 * 
	 * @param logger       The server logger for logging events.
	 */
	private void registerHandlers(Router router, BistroDataBase_Controller dbController, ServerLogger logger) {
		// Register API subjects
		ConnectionSubject.register(router, logger);
		UserSubject.register(router,userService, logger);
		OrdersSubject.register(router, ordersService, logger);
		WaitingListSubject.register(router, waitingListService, logger);
		TablesSubject.register(router, tableService, logger);
		ReportsSubject.register(router, reportService, logger);
	}
	
	// ******************************** Getters for Services ********************************
	public OrdersService getOrdersService() {
		return this.ordersService;
	}

	public TableService getTablesService() {
		return this.tableService;
	}
	
	public NotificationService getNotificationService() {
		return this.notificationService;
	}

}
// End of BistroServer.java