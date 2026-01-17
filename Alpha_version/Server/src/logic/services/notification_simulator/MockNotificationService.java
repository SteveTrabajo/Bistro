package logic.services.notification_simulator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import enums.Channel;
import enums.NotificationType;

public class MockNotificationService implements INotificationService {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m"; // Success
    private static final String YELLOW = "\u001B[33m"; // Waiting/Warning
    private static final String CYAN = "\u001B[36m"; // Info
    private static final String RED = "\u001B[31m"; // Alert
    
    /**
	 * Simulates sending a notification by printing a formatted log to the console.
	 * @param recipient The recipient's contact (phone/email).
	 * @param message The message content.
	 * @param type The type of notification.
	 * @param channel The channel to send the notification through.
	 */
    @Override
    public void sendNotification(String recipient, String message, NotificationType type, Channel channel) {
    	//simulate async behavior of real notification services
        new Thread(() -> {
            try {
                System.out.println(YELLOW + "[SIMULATOR] Connecting to provider..." + RESET);
                Thread.sleep(1000); //1 sec wait to simulate connection   
                
                printLog(recipient, message, type, channel);
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Prints a formatted log of the sent notification.
     * @param recipient
     * @param message
     * @param type
     * @param channel
     */
    private void printLog(String recipient, String message, NotificationType type, Channel channel) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String prefix = (channel == Channel.SMS) ? "[SMS]" : "[EMAIL]";
        String color = getColorByType(type);

        System.out.println("--------------------------------------------------");
        System.out.println(prefix + " SENT AT " + timestamp);
        System.out.println("To: " + recipient);
        System.out.println("Type: " + type);
        System.out.println("Content: " + color + message + RESET);
        System.out.println("--------------------------------------------------");
    }

    /**
	 * Determines the color code based on notification type.
	 * @param type The type of notification.
	 * @return The ANSI color code as a string.
	 */
    private String getColorByType(NotificationType type) {
        switch (type) {
            case TABLE_READY: 
            	return GREEN;       //success 
            case CANCELLATION_NOTICE:
            	return RED; //alert
            case BILL_DETAILS: 
            	return CYAN;       //info
            default:
            	return RESET;
        }
    }
}