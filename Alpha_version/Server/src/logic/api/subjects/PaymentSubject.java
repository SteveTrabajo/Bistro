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
	private final static Map<Integer, Integer> userFailureMap = new ConcurrentHashMap<>();
	public static void register(Router router, TableService tableService, ServerLogger logger, PaymentService paymentService) {
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
				//Use instance variable 'paymentService'
				int totalAmount = paymentService.calculateTotal(itemsFromClient, requester);

				// STEP 1: Create a record in the DB as PENDING before contacting the payment provider
				paymentService.preparePendingOrder(requester, totalAmount, idempotencyKey);

				// STEP 2: Actual charge attempt
				// Use instance variable 'paymentService'
				boolean paymentSuccess = paymentService.processPayment(totalAmount, idempotencyKey);

				if (paymentSuccess) {
					try {
						// STEP 3: Successful Sync
						// Use instance variable 'paymentService'
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
			}
		});

		router.on("payment", "processmanually", (msg, client) -> {
			int orderNumber = (int) msg.getData();
			// Use instance variable 'paymentService'
			boolean isSuccessful = paymentService.processManualPayment(orderNumber);

			if (isSuccessful) {
				client.sendToClient(new Message(Api.REPLY_PROCESS_PAYMENT_MANUALLY_OK, "Manual Payment Successful"));
			} else {
				client.sendToClient(new Message(Api.REPLY_PROCESS_PAYMENT_MANUALLY_FAIL, "Manual Payment Failed"));
			}
		});

		router.on("payment", "loadpendingbills", (msg, client) -> {
			User requester = (User) client.getInfo("user");
			if (requester.getUserType() == UserType.GUEST || requester.getUserType() == UserType.MEMBER) {
				client.sendToClient(new Message(Api.REPLY_LOAD_PENDING_BILLS_FAIL,
						"Only employees and managers can load pending bills."));
				return;
			}
			List<Bill> pendingBills = paymentService.getPendingBillsForUser();
			client.sendToClient(new Message(Api.REPLY_LOAD_PENDING_BILLS_OK, pendingBills));
		});
	}
}