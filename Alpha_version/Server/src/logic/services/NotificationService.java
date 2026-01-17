package logic.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import entities.Order;
import entities.User;
import enums.Channel;
import enums.NotificationType;
import enums.OrderStatus;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;
import logic.services.notification_simulator.INotificationService;
import logic.services.notification_simulator.MockNotificationService;

/**
 * Handles sending notifications to users via Email and SMS.
 * Runs background tasks for pre-arrival reminders (2 hours before reservation)
 * and payment reminders (when dining time exceeds 2 hours).
 * 
 * Currently uses a mock notification service for testing.
 */
public class NotificationService {

	/** Database controller for all DB operations */
    private final BistroDataBase_Controller dbController;
    
    /** Logger for tracking service activity */
    private final ServerLogger logger;
    
    /** Scheduler for background notification polling (runs every 15 minutes) */
    private ScheduledExecutorService scheduler;
    
    /** Mock notification service for testing (swap for real service in production) */
    private final INotificationService notificationSimulator;
    
    /** Flag to track if background tasks are running */
    private volatile boolean started = false;

    /**
     * Creates a new NotificationService with required dependencies.
     * 
     * @param dbController database controller for DB access
     * @param logger server logger for logging events
     */
    public NotificationService(BistroDataBase_Controller dbController, ServerLogger logger) {
        this.dbController = dbController;
        this.logger = logger;
        // commented out to allow restart after shutdown (might fix the thread issue) => moved it to startBackgroundTasks
        //this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.notificationSimulator = new MockNotificationService();
    }


    /**
     * Starts the background notification polling.
     * Runs every 15 minutes to check for reminders that need to be sent.
     */
    public synchronized void startBackgroundTasks() {
        if (started) return;
        // moved from constructor to allow restart after shutdown (might fix the thread issue)
        if (scheduler == null || scheduler.isShutdown()) {
			scheduler = Executors.newSingleThreadScheduledExecutor();
		}
        started = true;
        logger.log("[NOTIFICATIONS] Background service started. Polling every 15 minutes.");
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
     * Stops the background notification tasks.
     * Should be called when shutting down the server.
     */
    public synchronized void stop() {
        if (!started) return;
        started = false;

        if (scheduler != null && !scheduler.isShutdown()) {
        	// changed shutdown to shutdownNow to forcefully stop the thread (might fix the thread issue)
            scheduler.shutdownNow();
        }
    }


    /**
     * Checks for reservations starting in about 2 hours and sends reminders.
     * Uses notified_at column to avoid sending duplicate reminders.
     */
    private void checkPreArrivalReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startWindow = now.plusMinutes(110);
        LocalDateTime endWindow   = now.plusMinutes(130);

        // returns only RESERVATION + PENDING + notified_at IS NULL within window
        List<Order> upcomingOrders = dbController.getReservationsBetweenTimes(startWindow, endWindow, OrderStatus.PENDING);

        if (upcomingOrders == null || upcomingOrders.isEmpty()) return;

        for (Order order : upcomingOrders) {
            User user = dbController.getUserById(order.getUserId());
            if (user == null) continue;

            String msg = "Reminder: Your reservation at Bistro is in 2 hours (" + order.getOrderHour() + ").";
            dispatchToSimulator(user, msg, NotificationType.RESERVATION_REMINDER);

            //lock it so we won't re-send
            boolean marked = dbController.markReservationReminderSent(order.getOrderNumber(), now);
            if (!marked) {
                logger.log("[WARN] Could not mark reservation reminder as sent for order #" + order.getOrderNumber());
            }
        }
    }

    /**
     * Checks for customers who have been seated for over 2 hours
     * and sends payment reminders.
     */
    private void checkPaymentReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startWindow = now.minusMinutes(130);
        LocalDateTime endWindow   = now.minusMinutes(110);

        List<Order> overstayingOrders = dbController.getSeatedOrdersBetweenTimes(startWindow, endWindow);

        if (overstayingOrders == null || overstayingOrders.isEmpty()) return;

        for (Order order : overstayingOrders) {
            User user = dbController.getUserById(order.getUserId());
            if (user == null) continue;

            String msg = "Your 2-hour dining window has ended. Please proceed to payment.";
            dispatchToSimulator(user, msg, NotificationType.BILL_DETAILS);
        }
    }

    /**
     * Notifies a waitlist customer that their table is ready.
     * Called by TableService when a table becomes available.
     * The customer has 15 minutes to arrive or they'll be marked as no-show.
     * 
     * @param order the waitlist order to notify
     */
    public void notifyWaitlistUser(Order order) {
        User user = dbController.getUserById(order.getUserId());
        if (user != null) {
            String msg = "Good news! Table is ready. Please arrive within 15 minutes.";
            dispatchToSimulator(user, msg, NotificationType.TABLE_READY);
            dbController.updateOrderStatusByUserId(order.getUserId(), OrderStatus.NOTIFIED);
            logger.log("[NOTIFY] Waitlist alert sent to user " + user.getUserId());
        }
    }

    /**
     * Sends a notification to the user via email and/or SMS.
     * Uses whatever contact info is available.
     * 
     * @param user the user to notify
     * @param message the notification message
     * @param type the type of notification
     */
    private void dispatchToSimulator(User user, String message, NotificationType type) {
        boolean hasEmail = user.getEmail() != null && !user.getEmail().isEmpty();
        boolean hasPhone = user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty();

        if (hasEmail) {
            notificationSimulator.sendNotification(user.getEmail(), message, type, Channel.EMAIL);
        }
        if (hasPhone) {
            notificationSimulator.sendNotification(user.getPhoneNumber(), message, type, Channel.SMS);
        }
    }
}