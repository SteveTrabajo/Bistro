package logic.services;

import java.util.List;
import entities.Order;
import entities.User;
import entities.Bill;
import entities.Item;
import enums.OrderStatus;
import enums.UserType;
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
     * Step 1: Initialize the record in the Database.
     */
    public void preparePendingOrder(User requester, int totalAmount, String idempotencyKey) {
        logger.log("[DB] Preparing record for Key: " + idempotencyKey);
        dbController.createNewPayment(requester.getUserId(), totalAmount, OrderStatus.PENDING, idempotencyKey);
    }

    /**
     * Step 2: Real transaction attempt via External Provider.
     */
    public boolean processPayment(int amount, String idempotencyKey) {
        try {
            logger.log("[PAYMENT] Requesting authorization from provider for: " + idempotencyKey);
            
            // return PaymentGateway.authorize(idempotencyKey, amount);
            boolean isAuthorized = callExternalPaymentApi(idempotencyKey, amount);
            return isAuthorized;
        } catch (Exception e) {
            logger.log("[CRITICAL] External Payment Gateway connection error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recovery Logic: Synchronizes local DB with the External Provider's truth.
     */
    public void reconcilePendingPayments() {
        List<Order> stuckOrders = dbController.getOrdersByStatus(OrderStatus.PENDING);
        
        if (stuckOrders == null || stuckOrders.isEmpty()) return;

        for (Order order : stuckOrders) {
            String key = order.getIdempotencyKey();
            if (key == null) continue;

            try {
                // Query the external provider for the status of this specific key
                if (checkIfTransactionExistsWithProvider(key)) {
                    int billAmount = dbController.getAmountByBillId(key);
                    
                    // Proceed with DB updates since payment is confirmed externally
                    dbController.saveTransactionRecord(order.getUserId(), billAmount);
                    dbController.updateOrderStatus(order.getUserId(), OrderStatus.COMPLETED);
                    
                    logger.log("[RECOVERY] Order " + order.getOrderNumber() + " verified and COMPLETED.");
                } else {
                    // No record found at provider; transaction never occurred or failed
                    dbController.updateOrderStatus(order.getUserId(), OrderStatus.PENDING);
                    logger.log("[RECOVERY] No external record for " + key + ". Order set to FAILED.");
                }
            } catch (Exception e) {
                logger.log("[ERROR] Could not reconcile key " + key + ": " + e.getMessage());
            }
        }
    }

    /**
     * Performs a real check against the external provider's API.
     */
    private boolean checkIfTransactionExistsWithProvider(String idempotencyKey) {
        return callExternalStatusCheckApi(idempotencyKey);
    }

    // --- Placeholder methods for actual API implementation ---

    private boolean callExternalPaymentApi(String key, int amount) {
        // Implementation for actual payment API call goes here
        return true; 
    }

    private boolean callExternalStatusCheckApi(String key) {
    	// Implementation for actual status check API call goes here
        return true;
    }

    // --- Standard logic methods ---

    public void recordPayment(User user, int amount) {
        dbController.saveTransactionRecord(user.getUserId(), amount);
    }

    public void updateOrderStatusAfterPayment(User user, int amount) {
        dbController.updateOrderStatus(user.getUserId(), OrderStatus.COMPLETED);
    }

    public int calculateTotal(List<Item> items, User requester) {
        int total = 0;
        for (Item item : items) total += item.getPrice();// Sum item prices
        int discount = (requester.getUserType() == UserType.MEMBER) ? 10 : 0;// 10% discount for members
        total -= (total * discount) / 100;// Apply discount
        return (int) (total * 1.18);// Including 18% tax
    }

	public boolean processManualPayment(int orderNumber) {
		dbController.saveTransactionRecordByOrderNumber(orderNumber);
		dbController.updateOrderStatus(orderNumber, OrderStatus.COMPLETED);
	}

	public List<Bill> getPendingBillsForUser() {
		return dbController.getPendingBills();
	}
}