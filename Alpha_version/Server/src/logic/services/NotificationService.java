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
 * NotificationService handles sending notifications to users via Email and SMS.
 * It includes background tasks for pre-arrival reminders and payment reminders.
 */
public class NotificationService {

    private final BistroDataBase_Controller dbController;
    private final ServerLogger logger;
    private final ScheduledExecutorService scheduler;
    private final INotificationService notificationSimulator;
    private volatile boolean started = false;

    public NotificationService(BistroDataBase_Controller dbController, ServerLogger logger) {
        this.dbController = dbController;
        this.logger = logger;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.notificationSimulator = new MockNotificationService();
    }


    public synchronized void startBackgroundTasks() {
        if (started) return;
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

    public synchronized void stop() {
        if (!started) return;
        started = false;

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }


    /**
     * Checks for RESERVATION orders starting in approximately 2 hours and sends reminders.
     * Uses notified_at to avoid re-sending.
     * Req Source: 30
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
     * Payment reminders (2 hours after start).
     * NOTE: right now this can repeat every 15 minutes unless you add a dedicated DB flag/column.
     * If you want, we can also add bill_reminded_at later.
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
     * Waitlist/Table Ready notification (handled when table frees).
     */
    public void notifyWaitlistUser(Order order) {
        User user = dbController.getUserById(order.getUserId());
        if (user != null) {
            String msg = "Good news! Table is ready. Please arrive within 15 minutes.";
            dispatchToSimulator(user, msg, NotificationType.TABLE_READY);
            logger.log("[NOTIFY] Waitlist alert sent to user " + user.getUserId());
        }
    }

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
