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
	public class NotificationService{
		
	    //****************************** Instance variables ******************************//
		
	    private final BistroDataBase_Controller dbController;
	    private final ServerLogger logger; 
	    private final ScheduledExecutorService scheduler;
	    private final INotificationService notificationSimulator; 
	    
	    //****************************** Constructor ******************************//
	    
	    public NotificationService(BistroDataBase_Controller dbController, ServerLogger logger) {
	        this.dbController = dbController;
	        this.logger = logger;
	        this.scheduler = Executors.newSingleThreadScheduledExecutor();
	        this.notificationSimulator = new MockNotificationService();
	    }
	    
	    //******************************* Instance Methods *******************************//
	    //******************************* Background Tasks ******************************//
	    
	    /**
		 * Starts the background tasks for sending notifications.
		 * Polls every 15 minutes to check for reminders and notifications to send.
		 * 
		 */
	    public void startBackgroundTasks() {
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
	    
	    public void stop() {
	        if (scheduler != null && !scheduler.isShutdown()) {
	            scheduler.shutdown();
	        }
	    }
	
	    //******************************* Pre-Arrival Reminder (2 Hours Before) ******************************* 
	    
	    /**
	     * Checks for orders starting in approximately 2 hours and sends reminders.
	     * Req Source: 30
	     */
	    private void checkPreArrivalReminders() {
	        LocalDateTime now = LocalDateTime.now();
	        LocalDateTime startWindow = now.plusMinutes(110);
	        LocalDateTime endWindow = now.plusMinutes(130);
	        
	        List<Order> upcomingOrders = dbController.getOrdersBetweenTimes(startWindow, endWindow, OrderStatus.PENDING);
	        
	        if (upcomingOrders != null) {
	            for (Order order : upcomingOrders) {
	                User user = dbController.getUserById(order.getUserId());
	                if (user != null) {
	                    String msg = "Reminder: Your reservation at Bistro is in 2 hours (" + order.getOrderHour() + ").";
	                    dispatchToSimulator(user, msg, NotificationType.RESERVATION_REMINDER);
	                    
	                    // Update DB to avoid re-sending
	                    // dbController.markOrderAsNotified(...) 
	                }
	            }
	        }
	    }
	    
	    // ******************************* Payment Requirement (2 Hours After Start) *******************************
	    /**
		 * Checks for orders that have exceeded the 2-hour dining window and sends payment reminders.
		 */
	    private void checkPaymentReminders() {
	        LocalDateTime now = LocalDateTime.now();
	        LocalDateTime startWindow = now.minusMinutes(130);
	        LocalDateTime endWindow = now.minusMinutes(110); 
	        
	        List<Order> overstayingOrders = dbController.getOrdersBetweenTimes(startWindow, endWindow, OrderStatus.SEATED);
	        
	        if (overstayingOrders != null) {
	            for (Order order : overstayingOrders) {
	                User user = dbController.getUserById(order.getUserId());
	                if (user != null) {
	                    String msg = "Your 2-hour dining window has ended. Please proceed to payment.";
	                    dispatchToSimulator(user, msg, NotificationType.BILL_DETAILS);
	                }
	            }
	        }
	    }
	
	    // ******************************* Waitlist / Table Ready *******************************
	    
	    /**
	     * Notifies a waitlist user that their table is ready.
	     * @param order The order associated with the waitlist user.s
	     */
	    public void notifyWaitlistUser(Order order) {
	        User user = dbController.getUserById(order.getUserId());
	        if (user != null) {
	            String msg = "Good news! Table is ready. Please arrive within 15 minutes.";
	            dispatchToSimulator(user, msg, NotificationType.TABLE_READY);
	            logger.log("[NOTIFY] Waitlist alert sent to user " + user.getUserId());
	        }
	    }
	
	    // ******************************* Helper: Unified Dispatcher *******************************
	    
	    /**
		 * Dispatches notification via available channels (Email/SMS).
		 * @param user The recipient user.
		 * @param message The message content.
		 * @param type The type of notification.
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