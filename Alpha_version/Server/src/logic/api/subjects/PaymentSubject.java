package logic.api.subjects;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import comms.Api;
import comms.Message;
import entities.Bill;
import entities.Item;
import entities.User;
import enums.UserType;
import logic.ServerLogger;
import logic.api.Router;
import logic.services.PaymentService;
import logic.services.TableService;

public class PaymentSubject {

    private PaymentSubject() {
    }

    private static final int MAX_FAILURES = 5;
    private static Map<Integer, Integer> userFailureMap = new ConcurrentHashMap<>();

    public static void register(Router router, TableService tableService, ServerLogger logger, PaymentService paymentService) {
        
        // --- ROUTE: Credit Card Payment ---
        router.on("payment", "complete", (msg, client) -> {
            User requester = (User) client.getInfo("user");

            // 1. Validate Client Input (List<Item>) - UNCHANGED
            List<Item> itemsFromClient = (List<Item>) msg.getData();
            if (itemsFromClient == null || itemsFromClient.isEmpty()) {
                client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "Cart is empty."));
                return;
            }

            try {
                // 2. Calculate Total (Now returns double)
                double totalAmount = paymentService.calculateTotal(itemsFromClient, requester);

                // 3. Find the Active Bill ID for this User
                // Since the client doesn't send the Bill ID, we find the pending bill associated with this user.
                List<Bill> pendingBills = paymentService.getPendingBillsForUser(requester.getUserId());
                if (pendingBills == null || pendingBills.isEmpty()) {
                    client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "No active bill found for user."));
                    return;
                }
                // Assuming the user has one active session/bill
                int billId = pendingBills.get(0).getBillID();

                // 4. Generate a Mock Token
                // Since the client UI doesn't send a card token yet, we generate a placeholder.
                // In a real app, this token would come from msg.getData().
                String mockCardToken = "TOK_" + requester.getUserId() + "_" + System.currentTimeMillis();

                // 5. Process Payment via Gateway
                boolean paymentSuccess = paymentService.processCreditCardPayment(billId, totalAmount, mockCardToken);

                if (paymentSuccess) {
                    client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_OK, "Success"));
                    userFailureMap.remove(requester.getUserId());
                    
                    // Notify Table Service
                    //TableService.notifyPaymentCompletion(requester, tableService, logger);
                } else {
                    // Handle Failures
                    int failures = userFailureMap.getOrDefault(requester.getUserId(), 0) + 1;
                    userFailureMap.put(requester.getUserId(), failures);

                    if (failures >= MAX_FAILURES) {
                        String dishwasherMessage = "Payment Declined. You have failed 5 times. Welcome to the dishwashing team!";
                        client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, dishwasherMessage));
                        userFailureMap.remove(requester.getUserId());
                    } else {
                        client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "Payment Declined"));
                    }
                }
            } catch (Exception e) {
                logger.log("[ERROR] Payment System Error: " + e.getMessage());
                client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "System Error"));
            }
        });

        // --- ROUTE: Manual (Cash) Payment ---
        router.on("payment", "processmanually", (msg, client) -> {
            // Client sends orderNumber (int) - UNCHANGED
            int orderNumber = (int) msg.getData();
            
            // We need to find the billId associated with this orderNumber
            // Assumption: PaymentService or DBController has a lookup method. 
            // If not, you must add 'getBillIdByOrderNumber' to your service/controller.
            Integer billId = paymentService.getBillIdByOrderNumber(orderNumber);

            if (billId == null) {
                client.sendToClient(new Message(Api.REPLY_PROCESS_PAYMENT_MANUALLY_FAIL, "Bill not found for Order: " + orderNumber));
                return;
            }

            // Calculate amount (Or fetch from bill). For Cash, we often assume full payment.
            // We retrieve the bill to get the total.
            Bill bill = paymentService.getBillById(billId); 
            
            boolean isSuccessful = paymentService.processCashPayment(billId, bill.getTotal());

            if (isSuccessful) {
                client.sendToClient(new Message(Api.REPLY_PROCESS_PAYMENT_MANUALLY_OK, "Manual Payment Successful"));
            } else {
                client.sendToClient(new Message(Api.REPLY_PROCESS_PAYMENT_MANUALLY_FAIL, "Manual Payment Failed"));
            }
        });

        // --- ROUTE: Load Pending Bills ---
        router.on("payment", "loadpendingbills", (msg, client) -> {
            User requester = (User) client.getInfo("user");
            if (requester.getUserType() == UserType.GUEST || requester.getUserType() == UserType.MEMBER) {
                client.sendToClient(new Message(Api.REPLY_LOAD_PENDING_BILLS_FAIL,
                        "Only employees and managers can load pending bills."));
                return; // Added return to prevent execution
            }
            
            // Note: You might need to add a method to get ALL pending bills for staff, 
            // currently this method in your service gets bills for a specific user ID.
            List<Bill> pendingBills = paymentService.getPendingBillsForUser(requester.getUserId());
            client.sendToClient(new Message(Api.REPLY_LOAD_PENDING_BILLS_OK, pendingBills));
        });
    }
}