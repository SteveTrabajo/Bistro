package logic.api.subjects;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import comms.Api;
import comms.Message;
import entities.User;
import entities.Item;
import logic.ServerLogger;
import logic.api.Router;
import logic.services.PaymentService;


public class PaymentSubject {
	private final Map<Integer, Integer> userFailureMap = new ConcurrentHashMap<>();
    private final Router router;
    private final PaymentService paymentService;
    private final ServerLogger logger;

    public PaymentSubject(Router router, PaymentService paymentService, ServerLogger logger) {
        this.router = router;
        this.paymentService = paymentService;
        this.logger = logger;
        registerRoutes();
    }

    private void registerRoutes() {
        router.on("payment", "complete", (msg, client) -> {
            User requester = (User) client.getInfo("user");

            // Client sends List<Item> in the message data
            List<Item> itemsFromClient = (List<Item>) msg.getData();
            if (itemsFromClient == null || itemsFromClient.isEmpty()) {
                client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "Cart is empty."));
                return;
            }

            // Create a unique key for this specific transaction attempt
            String idempotencyKey = "PAY_" + requester.getUserId() + "_" + System.currentTimeMillis();

            try {
                // Calculate the amount on the server to prevent client-side tampering
                int totalAmount = paymentService.calculateTotal(itemsFromClient,requester);
            	
                // STEP 1: Create a record in the DB as PENDING before contacting the payment provider
                paymentService.preparePendingOrder(requester, totalAmount, idempotencyKey);

                // STEP 2: Actual charge attempt
                boolean paymentSuccess = paymentService.processPayment(totalAmount, idempotencyKey);

                if (paymentSuccess) {
                    try {
                        // STEP 3: Successful Sync
                        paymentService.recordPayment(requester, totalAmount);
                        paymentService.updateOrderStatusAfterPayment(requester, totalAmount);
                        
                        client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_OK, "Success"));
                        userFailureMap.remove(requester.getUserId());
                    } catch (Exception dbEx) {
                        logger.log("[CRITICAL] Payment captured but DB update failed. Key: " + idempotencyKey);
                        client.sendToClient(new Message(Api.REPLY_PAYMENT_PENDING_VERIFICATION, "Verifying..."));
                    }
                } else {
                    // Increment the counter on every failure
                    int failures = userFailureMap.getOrDefault(requester.getUserId(), 0) + 1;
                    userFailureMap.put(requester.getUserId(), failures);

                    if (failures >= 5) {
                        // Reset counter if you want the cycle to restart, or leave it to keep sending the message
                        String dishwasherMessage = "Payment Declined. You have failed 5 times. Welcome to the dishwashing team!";
                        client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, dishwasherMessage));
                        userFailureMap.remove(requester.getUserId());
                    } else {
                        // Standard failure message for attempts 1-4
                        client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "Payment Declined"));
                    }
                }
            } catch (Exception e) {
                logger.log("[ERROR] Payment System Error: " + e.getMessage());
            }
        });
    }
}