package logic.services;

import java.util.List;
import entities.Order;
import entities.User;
import entities.Item;
import enums.OrderStatus;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class PaymentService {

    private final BistroDataBase_Controller dbController;
    private final ServerLogger logger;

    public PaymentService(BistroDataBase_Controller dbController, ServerLogger logger) {
        this.dbController = dbController;
        this.logger = logger;
    }

    /**
     * Calculates total sum of items. 
     * Security Note: Ideally, fetch prices from DB using item IDs.
     * @param requester 
     */
    public int calculateTotal(List<Item> items, User requester) {
        int total = 0;
        int taxRate = 18; // 18% tax
        int discount = 0;
        if (requester.getUserType().toString().equals("MEMBER")) {
			discount = 10; // 10% discount for members
		}
        for (Item item : items) {
            total += item.getPrice();
        }
        total -= (total * discount) / 100; // Applying discount
        total += (total * taxRate) / 100; // Adding tax
        return total;
    }

    public boolean processPayment(int amount, String idempotencyKey) {
        // Placeholder for external API call (e.g., Stripe/PayPal)
        logger.log("[PAYMENT] Processing " + amount + " with key: " + idempotencyKey);
        return true; 
    }

    public void updateOrderStatus(User user, OrderStatus status) {
        dbController.updateOrderStatus(user.getUserId(), status);
    }

    public void recordPayment(User user, int amount) {
        dbController.saveTransactionRecord(user.getUserId(), amount);
    }

    public void updateOrderStatusAfterPayment(User user, int amount) {
        dbController.updateOrderStatus(user.getUserId(), OrderStatus.COMPLETED);
    }

    /**
     * Recovery logic for orders stuck in PENDING status.
     */
    public void reconcilePendingPayments() {
        List<Order> stuckOrders = dbController.getOrdersByStatus(OrderStatus.PENDING);

        for (Order order : stuckOrders) {
            // In a real app, you would verify the 'idempotencyKey' with your payment provider
            boolean wasActuallyPaid = checkIfTransactionExistsWithProvider(order.getIdempotencyKey());

            if (wasActuallyPaid) {
                dbController.saveTransactionRecord(order.getUserId(), order.getAmount());
                dbController.updateOrderStatus(order.getUserId(), OrderStatus.COMPLETED);
                logger.log("[RECOVERY] Resolved stuck order: " + order.getOrderNumber());
            } else {
                dbController.updateOrderStatus(order.getUserId(), OrderStatus.FAILED);
                logger.log("[RECOVERY] Cancelled invalid pending order: " + order.getOrderNumber());
            }
        }
    }

    private boolean checkIfTransactionExistsWithProvider(String idempotencyKey) {
        // Placeholder: Connect to API to check if this key was processed
        return false; 
    }

    public void sendReceipt(User user) {
        logger.log("Receipt sent to: " + user.getEmail());
    }

	public void preparePendingOrder(User requester, int totalAmount, String idempotencyKey) {
		
	}
}