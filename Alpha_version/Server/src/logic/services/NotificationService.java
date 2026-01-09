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
import enums.OrderStatus;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class NotificationService {
	
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	private final ScheduledExecutorService scheduler;

	public NotificationService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
	}

	/**
	 * Starts the background polling task.
	 * Call this method when the Server starts (in BistroServer.java).
	 */
	public void startBackgroundTasks() {
		logger.log("[NOTIFICATIONS] Background service started. Polling every 10 minutes.");
		
		// Run checks every 15 minutes
		scheduler.scheduleAtFixedRate(() -> {
			try {
				checkPreArrivalReminders();
				checkPaymentReminders();
			} catch (Exception e) {
				logger.log("[ERROR] Notification background task failed: " + e.getMessage());
				e.printStackTrace();
			}
		}, 0, 15, TimeUnit.MINUTES);
	}
	
	/**
	 * Stops the background service (call on server shutdown).
	 */
	public void stop() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
		}
	}

	// ==================== 1. Pre-Arrival Reminder (2 Hours Before) ====================
	
	private void checkPreArrivalReminders() {
		// Target window: Reservations scheduled for roughly 2 hours from NOW
		// We look for orders between (Now + 1h 50m) and (Now + 2h 10m) to account for the 10-min polling gap
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime startWindow = now.plusMinutes(110);
		LocalDateTime endWindow = now.plusMinutes(130);
		
		// NOTE: Ask DB Teammate for: getOrdersBetweenTimes(LocalDateTime start, LocalDateTime end, OrderStatus status)
		List<Order> upcomingOrders = dbController.getOrdersBetweenTimes(startWindow, endWindow, OrderStatus.PENDING);
		
		if (upcomingOrders != null) {
			for (Order order : upcomingOrders) {
				User user = dbController.getUserById(order.getUserId()); // Need to fetch user contact info
				if (user != null) {
					String msg = "Reminder: Your reservation at Bistro is in 2 hours (" + order.getOrderHour() + ").";
					sendNotification(user, msg);
					
					// Update status so we don't spam them in the next poll
					dbController.updateOrderStatusInDB(order.getConfirmationCode(), OrderStatus.NOTIFIED);
				}
			}
		}
	}

	// ==================== 2. Payment Requirement (2 Hours After Start) ====================
	
	private void checkPaymentReminders() {
		// Target: Orders that started roughly 2 hours ago AND are still SEATED
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime startWindow = now.minusMinutes(130);
		LocalDateTime endWindow = now.minusMinutes(110); // 2 hours ago +/- buffer
		
		// NOTE: Ask DB Teammate to support searching by SEATED status in the time window
		List<Order> overstayingOrders = dbController.getOrdersBetweenTimes(startWindow, endWindow, OrderStatus.SEATED);
		
		if (overstayingOrders != null) {
			for (Order order : overstayingOrders) {
				User user = dbController.getUserById(order.getUserId());
				if (user != null) {
					String msg = "Your 2-hour dining window has ended. Please proceed to payment.";
					sendNotification(user, msg);
					
					// We don't change status here (they are still seated), 
					// but you might want a flag in DB like 'payment_reminder_sent' to avoid double sending.
				}
			}
		}
	}

	// ==================== 3. Waitlist Availability ====================
	
	/**
	 * This method is EVENT-DRIVEN. 
	 * It should be called by WaitingListService whenever a table is assigned to a user.
	 */
	public void notifyWaitlistUser(Order order) {
		User user = dbController.getUserById(order.getUserId());
		if (user != null) {
			String msg = "Good news! A table is now available for you. Please check in within 15 minutes.";
			sendNotification(user, msg);
			logger.log("[NOTIFY] Waitlist alert sent to user " + user.getUserId());
		}
	}

	// ==================== Helper: Send Logic ====================
	
	private void sendNotification(User user, String message) {
		boolean hasEmail = user.getEmail() != null && !user.getEmail().isEmpty();
		boolean hasPhone = user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty();
		
		if (hasEmail) {
			// Mock Email Sending
			logger.log("[EMAIL SENT] To: " + user.getEmail() + " | Body: " + message);
		}
		
		if (hasPhone) {
			// Mock SMS Sending
			logger.log("[SMS SENT] To: " + user.getPhoneNumber() + " | Body: " + message);
		}
		
		if (!hasEmail && !hasPhone) {
			logger.log("[WARN] Could not notify User " + user.getUserId() + " (No contact info).");
		}
	}
}