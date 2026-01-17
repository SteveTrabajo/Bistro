package logic.api.subjects;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import comms.Api;
import comms.Message;
import entities.Bill;
import entities.Item;
import entities.Order;
import entities.User;
import enums.UserType;
import logic.ServerLogger;
import logic.api.ServerRouter;
import logic.services.PaymentService;
import logic.services.TableService;

public class ServerPaymentSubject {

    private ServerPaymentSubject() {
    }

    // To track payment failures per user
    private static final int MAX_FAILURES = 5;
    private static Map<Integer, Integer> userFailureMap = new ConcurrentHashMap<>();

    /**
	 * Registers payment-related routes to the server router.
	 *
	 * @param router         The server router to register routes with.
	 * @param tableService   The table service for managing table-related operations.
	 * @param logger         The server logger for logging events and errors.
	 * @param paymentService The payment service for handling payment operations.
	 */
    public static void register(ServerRouter router, TableService tableService, ServerLogger logger, PaymentService paymentService) {
        
    	// --- ROUTE: Get Bill Items List ---
    	router.on("payment",  "billItemsList", (msg, client) -> {
			User requester = (User) client.getInfo("user");
			// Client sends billId (int)
			int diners = (int) msg.getData();
				List<Item> items = paymentService.createRandomBillItems(diners);
				if (items != null) {
					client.sendToClient(new Message(Api.REPLY_BILL_ITEMS_LIST_OK, items));
				} else {
					client.sendToClient(new Message(Api.REPLY_BILL_ITEMS_LIST_FAIL, null));
				}
		});
    	
    	
    	// --- ROUTE: Complete Payment ---s
    	router.on("payment", "complete", (msg, client) -> {
    	    User requester = (User) client.getInfo("user");
    	    if (requester == null) {
    	        client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "Not logged in."));
    	        return;
    	    }

    	    // Expecting List<Item> from client
    	    @SuppressWarnings("unchecked")
    	    List<Item> itemsFromClient = (List<Item>) msg.getData();
    	    if (itemsFromClient == null || itemsFromClient.isEmpty()) {
    	        client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "Cart is empty."));
    	        return;
    	    }

    	    try {
    	        Order seatedOrder = tableService.getSeatedOrderForClient(requester.getUserId());
    	        if (seatedOrder == null) {
    	            client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "No active seated order found."));
    	            return;
    	        }

    	        int orderNumber = seatedOrder.getOrderNumber();
    	        // Retrieve bill ID associated with the order
    	        Integer billIdObj = paymentService.getBillIdByOrderNumber(orderNumber);
    	        if (billIdObj == null) {
    	            client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "No bill found for active order."));
    	            return;
    	        }
    	        int billId = billIdObj;
    	        // Calculate total amount from items
    	        double totalAmount = paymentService.calculateTotal(itemsFromClient, requester);
    	        // Simulate obtaining a card token (in real scenario, this would come from client-side payment processing)
    	        String mockCardToken = "TOK_" + requester.getUserId() + "_" + System.currentTimeMillis();
    	        // Process payment
    	        boolean paymentSuccess = paymentService.processCreditCardPayment(billId, totalAmount, mockCardToken);
    	        // Handle payment result
    	        if (paymentSuccess) {
    	            paymentService.onPaymentCompleted(orderNumber);
    	            // Reset failure count on success
    	            String successMessage =
    	                "Payment of ₪" + String.format("%.2f", totalAmount) + " completed successfully.\n" +
    	                "Bill ID: " + billId + "\n" +
    	                "Order Number: " + orderNumber + "\nThank you for dining with us!";
    	            // Send success message to client
    	            client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_OK, successMessage));
    	            userFailureMap.remove(requester.getUserId());
    	        } else {
    	            int failures = userFailureMap.getOrDefault(requester.getUserId(), 0) + 1;
    	            userFailureMap.put(requester.getUserId(), failures);

    	            if (failures >= MAX_FAILURES) {
    	                client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL,
    	                        "Payment Declined. You have failed 5 times. Welcome to the dishwashing team!"));
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
        router.on("payment", "processManually", (msg, client) -> {
        	Object[] data = (Object[]) msg.getData();
            int orderNumber = (int) data[0];
            String method = (String) data[1]; // Expecting "CASH" or "CREDIT"
            
            Integer billId = paymentService.getBillIdByOrderNumber(orderNumber);

            if (billId == null) {
                client.sendToClient(new Message(Api.REPLY_PROCESS_PAYMENT_MANUALLY_FAIL, "Bill not found for Order: " + orderNumber));
                return;
            }
            // Retrieve the bill
            Bill bill = paymentService.getBillById(billId); 
            boolean isSuccessful;
            // Process payment based on method
            if ("Credit Card".equalsIgnoreCase(method)) {
                String manualToken = "MANUAL-CARD-" + System.currentTimeMillis();
                isSuccessful = paymentService.processCreditCardPayment(billId, bill.getTotal(), manualToken);
            } else {
                isSuccessful = paymentService.processCashPayment(billId, bill.getTotal());
            }
            // Send response to client
            if (isSuccessful) {
                client.sendToClient(new Message(Api.REPLY_PROCESS_PAYMENT_MANUALLY_OK, "Manual Payment Successful"));
            } else {
                client.sendToClient(new Message(Api.REPLY_PROCESS_PAYMENT_MANUALLY_FAIL, "Manual Payment Failed"));
            }
        });

        // --- ROUTE: Load Pending Bills ---
        router.on("payment", "loadPendingBills", (msg, client) -> {
            User requester = (User) client.getInfo("user");
            if (requester.getUserType() == UserType.GUEST || requester.getUserType() == UserType.MEMBER) {
                client.sendToClient(new Message(Api.REPLY_LOAD_PENDING_BILLS_FAIL,
                        "Only employees and managers can load pending bills."));
                return; // Added return to prevent execution
            }
            // Fetch pending bills
            List<Bill> pendingBills = paymentService.getPendingBillsForUser();
            client.sendToClient(new Message(Api.REPLY_LOAD_PENDING_BILLS_OK, pendingBills));
        });
    }
}
// End of ServerPaymentSubject.java