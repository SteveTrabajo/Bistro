package logic.services.notification_simulator;

import enums.Channel;
import enums.NotificationType;

/**
 * NotificationService interface to simulate sending notifications via different channels.
 */
public interface INotificationService {
    void sendNotification(String recipient, String message, NotificationType type, Channel channel);
}
