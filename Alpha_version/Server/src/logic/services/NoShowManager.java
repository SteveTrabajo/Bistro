package logic.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import entities.Order;
import entities.User;
import enums.NotificationType;
import enums.OrderStatus;
import enums.OrderType;
import enums.Channel;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;
import logic.services.notification_simulator.INotificationService;
import logic.services.notification_simulator.MockNotificationService;

/**
 * Detects and handles no-show reservations and waitlist orders.
 * 
 * No-show rules:
 * - RESERVATION: If customer doesn't arrive within 15 minutes of reservation time, mark as NO_SHOW
 * - WAITLIST: If customer doesn't arrive within 15 minutes of being notified, mark as NO_SHOW
 * 
 * Runs a background task every 5 minutes to check for no-shows.
 */
public class NoShowManager {
	
	//****************************** Instance variables ******************************//
	
	/** Database controller for all DB operations */
	private final BistroDataBase_Controller dbController;
	
	/** Logger for tracking service activity */
	private final ServerLogger logger;
	
	/** Scheduler for background no-show checking */
	private ScheduledExecutorService scheduler;
	
	/** Mock notification service for sending no-show alerts */
	private final INotificationService notificationSimulator;
	
	/** Minutes after scheduled time before marking as no-show */
	private static final int NO_SHOW_THRESHOLD_MINUTES = 15;
	
	/** How often to check for no-shows (in minutes) */
	private static final int BACKGROUND_CHECK_INTERVAL_MINUTES = 5;
	
	//****************************** Constructor ******************************//
	
	/**
	 * Creates a new NoShowManager with required dependencies.
	 * 
	 * @param dbController database controller for DB access
	 * @param logger server logger for logging events
	 */
	public NoShowManager(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
		// commented out to allow restart after shutdown (might fix the thread issue) => moved it to startBackgroundTasks
		//this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.notificationSimulator = new MockNotificationService();
	}
	
	//******************************* Public Methods *******************************//
	
	/**
	 * Starts the background task that checks for no-shows.
	 * Runs every 5 minutes to detect overdue orders.
	 */
	public synchronized void startBackgroundTasks() {
		// moved from constructor to allow restart after shutdown (might fix the thread issue)
		if (scheduler == null || scheduler.isShutdown()) {
			scheduler = Executors.newSingleThreadScheduledExecutor();
		}
		logger.log("[NO_SHOW] Background service started. Checking every " + BACKGROUND_CHECK_INTERVAL_MINUTES + " minutes.");
		scheduler.scheduleAtFixedRate(() -> {
			try {
				checkForNoShows();
			} catch (Exception e) {
				logger.log("[ERROR] No-show check failed: " + e.getMessage());
				e.printStackTrace();
			}
		}, 0, BACKGROUND_CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
	}
	
	/**
	 * Stops the background no-show checking task.
	 * Should be called when shutting down the server.
	 */
	public synchronized void stop() {
		if (scheduler != null && !scheduler.isShutdown()) {
			// changed shutdown to shutdownNow to forcefully stop the thread (might fix the thread issue)
			scheduler.shutdownNow();
		}
	}
	
	//******************************* No-Show Detection Logic *******************************//
	
	/**
	 * Main check method - looks for both reservation and waitlist no-shows.
	 */
	private void checkForNoShows() {
		LocalDateTime now = LocalDateTime.now();
		
		// Check PENDING reservations (15 minutes past reservation time)
		checkPendingReservations(now);
		
		// Check NOTIFIED waitlist orders (15 minutes past notification time)
		checkNotifiedWaitlist(now);
	}
	
	/**
	 * Checks for PENDING reservations that are 15+ minutes past their scheduled time.
	 * 
	 * @param now current date/time
	 */
	private void checkPendingReservations(LocalDateTime now) {
		// Get all PENDING reservations for today
		LocalDate today = now.toLocalDate();
		List<Order> pendingOrders = dbController.getOrdersByDateAndStatus(today, OrderStatus.PENDING, OrderType.RESERVATION);
		
		if (pendingOrders == null || pendingOrders.isEmpty()) {
			return;
		}
		
		for (Order order : pendingOrders) {
			// Combine order date and hour to get full datetime
			LocalDateTime reservationDateTime = LocalDateTime.of(order.getOrderDate(), order.getOrderHour());
			LocalDateTime noShowDeadline = reservationDateTime.plusMinutes(NO_SHOW_THRESHOLD_MINUTES);
			
			// If current time is past the no-show deadline and not yet seated
			if (now.isAfter(noShowDeadline) && order.getStatus() != OrderStatus.SEATED && order.getStatus() != OrderStatus.NO_SHOW) {
				markAsNoShow(order, "Reservation not shown up at " + order.getOrderHour());
			}
		}
	}
	
	/**
	 * Checks for NOTIFIED waitlist orders that are 15+ minutes past notification time.
	 * 
	 * @param now current date/time
	 */
	private void checkNotifiedWaitlist(LocalDateTime now) {
		// Get all NOTIFIED waitlist orders for today
		LocalDate today = now.toLocalDate();
		List<Order> notifiedOrders = dbController.getOrdersByDateAndStatus(today, OrderStatus.NOTIFIED, OrderType.WAITLIST);
		
		if (notifiedOrders == null || notifiedOrders.isEmpty()) {
			return;
		}
		
		for (Order order : notifiedOrders) {
			// Get the notification time from database
			LocalDateTime notificationTime = dbController.getOrderNotificationTime(order.getOrderNumber());
			
			if (notificationTime != null) {
				LocalDateTime noShowDeadline = notificationTime.plusMinutes(NO_SHOW_THRESHOLD_MINUTES);
				
				// If current time is past the no-show deadline
				if (now.isAfter(noShowDeadline)) {
					markAsNoShow(order, "Waitlist not shown up within 15 minutes of notification");
				}
			}
		}
	}
	
	/**
	 * Marks an order as NO_SHOW and handles cleanup.
	 * Frees any associated table and logs the event.
	 * 
	 * @param order the order to mark as no-show
	 * @param reason explanation for logging purposes
	 */
	private void markAsNoShow(Order order, String reason) {
		boolean updated = dbController.updateOrderStatusByUserId(order.getOrderNumber(), OrderStatus.NO_SHOW);
		
		if (updated) {
			logger.log("[NO_SHOW] Order #" + order.getOrderNumber() + " marked as NO_SHOW. Reason: " + reason);
			
			// Free the table if it was a reservation
			if (order.getOrderType() == OrderType.RESERVATION) {
				dbController.freeReservationTable(order.getOrderNumber());
				logger.log("[NO_SHOW] Table freed for order #" + order.getOrderNumber());
			}
			
			// Log notification (would be sent by NotificationService in real implementation)
			logger.log("[NO_SHOW] Notification would be sent to user " + order.getUserId() + " for order #" + order.getOrderNumber());
		}
	}
}