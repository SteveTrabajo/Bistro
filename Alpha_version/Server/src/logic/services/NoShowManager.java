package logic.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
 * NoShowManager handles detection and management of no-show reservations and waitlist orders.
 * 
 * Rules:
 * - RESERVATION (PENDING): If not shown up within 15 minutes of reservation time, mark as NO_SHOW
 * - WAITLIST (NOTIFIED): If not shown up within 15 minutes of notification time, mark as NO_SHOW
 * 
 * Runs background task every 5 minutes to check for no-shows.
 */
public class NoShowManager {
	
	//****************************** Instance variables ******************************//
	
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	private final ScheduledExecutorService scheduler;
	private final INotificationService notificationSimulator;
	
	private static final int NO_SHOW_THRESHOLD_MINUTES = 15;
	private static final int BACKGROUND_CHECK_INTERVAL_MINUTES = 5;
	
	//****************************** Constructor ******************************//
	
	public NoShowManager(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.notificationSimulator = new MockNotificationService();
	}
	
	//******************************* Public Methods *******************************//
	
	/**
	 * Starts the background task to check for no-shows.
	 * Runs every 5 minutes to detect orders that have exceeded the no-show threshold.
	 */
	public void startBackgroundTasks() {
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
	 * Stops the background task.
	 */
	public void stop() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
		}
	}
	
	//******************************* No-Show Detection Logic *******************************//
	
	/**
	 * Checks for orders that should be marked as NO_SHOW.
	 * 
	 * For RESERVATION orders (PENDING status):
	 *   - If current time > reservation time + 15 minutes, mark as NO_SHOW
	 * 
	 * For WAITLIST orders (NOTIFIED status):
	 *   - If current time > notification time + 15 minutes, mark as NO_SHOW
	 */
	private void checkForNoShows() {
		LocalDateTime now = LocalDateTime.now();
		
		// Check PENDING reservations (15 minutes past reservation time)
		checkPendingReservations(now);
		
		// Check NOTIFIED waitlist orders (15 minutes past notification time)
		checkNotifiedWaitlist(now);
	}
	
	/**
	 * Checks PENDING reservations that have passed their reservation time by 15+ minutes.
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
	 * Checks NOTIFIED waitlist orders that have been waiting for 15+ minutes.
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
	 * Marks an order as NO_SHOW and notifies the user.
	 * 
	 * @param order The order to mark as no-show
	 * @param reason The reason for the no-show
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
