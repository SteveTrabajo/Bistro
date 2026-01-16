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

    private static final int MAX_FAILURES = 5;
    private static Map<Integer, Integer> userFailureMap = new ConcurrentHashMap<>();

    public static void register(ServerRouter router, TableService tableService, ServerLogger logger, PaymentService paymentService) {
        
    	
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
    	
    	
    	
    	router.on("payment", "complete", (msg, client) -> {
    	    User requester = (User) client.getInfo("user");
    	    if (requester == null) {
    	        client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "Not logged in."));
    	        return;
    	    }

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

    	        Integer billIdObj = paymentService.getBillIdByOrderNumber(orderNumber);
    	        if (billIdObj == null) {
    	            client.sendToClient(new Message(Api.REPLY_PAYMENT_COMPLETE_FAIL, "No bill found for active order."));
    	            return;
    	        }
    	        int billId = billIdObj;

    	        double totalAmount = paymentService.calculateTotal(itemsFromClient, requester);

    	        String mockCardToken = "TOK_" + requester.getUserId() + "_" + System.currentTimeMillis();

    	        boolean paymentSuccess = paymentService.processCreditCardPayment(billId, totalAmount, mockCardToken);

    	        if (paymentSuccess) {
    	            paymentService.onPaymentCompleted(orderNumber);

    	            String successMessage =
    	                "Payment of ₪" + String.format("%.2f", totalAmount) + " completed successfully.\n" +
    	                "Bill ID: " + billId + "\n" +
    	                "Order Number: " + orderNumber + "\nThank you for dining with us!";

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
        router.on("payment", "loadPendingBills", (msg, client) -> {
            User requester = (User) client.getInfo("user");
            if (requester.getUserType() == UserType.GUEST || requester.getUserType() == UserType.MEMBER) {
                client.sendToClient(new Message(Api.REPLY_LOAD_PENDING_BILLS_FAIL,
                        "Only employees and managers can load pending bills."));
                return; // Added return to prevent execution
            }
            List<Bill> pendingBills = paymentService.getPendingBillsForUser();
            client.sendToClient(new Message(Api.REPLY_LOAD_PENDING_BILLS_OK, pendingBills));
        });
    }
}