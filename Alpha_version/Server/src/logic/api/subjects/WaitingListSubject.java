package logic.api.subjects;

import java.util.Map;

import comms.Api;
import comms.Message;
import dto.WaitListResponse;
import entities.Order;
import entities.User;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;
import logic.api.Router;
import logic.services.WaitingListService;

/**
 * WaitingListSubject class that registers handlers related to waiting list
 * operations.
 */
public class WaitingListSubject {
	// ********************************
	// Constructors***********************************
	private WaitingListSubject() {
	}

	// ******************************** Static
	// Methods***********************************
	/**
	 * Registers handlers related to waiting list operations.
	 * 
	 * @param router             The Router instance to register handlers with.
	 * @param waitingListService The BistroDataBase_Controller instance for database
	 *                           operations.
	 * @param logger             The ServerLogger instance for logging.
	 */
	public static void register(Router router, WaitingListService waitingListService, ServerLogger logger) {
		// 1. Check if user is in waiting list
		router.on("WaitingList", "isInWaitingList", (msg, client) -> {
			String confirmationCode = (String) msg.getData();
			boolean isInWaitingList = waitingListService.isUserInWaitingList(confirmationCode);

			if (isInWaitingList) {
				logger.log("[INFO] Client: " + client + " is in waiting list.");
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_IS_IN_LIST, true));
			} else {
				logger.log("[INFO] Client: " + client + " is NOT in waiting list.");
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_IS_NOT_IN_LIST, false));
			}
		});

		// 2. Check Availability AND Seat if possible (Optimistic Scheduling)
		// Client sends: int (dinersAmount)
		// Server uses: client.getInfo("user") to get contact info
		router.on("waitingList", "checkAvailability", (msg, client) -> {
			//TODO : add option to staff to add users to waitlist
			// A. Get Data from Message
			int dinersAmount = (int) msg.getData();

			// B. Get User from Session (The crucial change)
			User currentUser = (User) client.getInfo("user");

			if (currentUser == null) {
				logger.log("[ERROR] Check Availability failed: No user found in session.");
				return; // Or send an error message
			}

			int userID = currentUser.getUserId();

			// C. Call Service
			Object result = waitingListService.checkAvailabilityAndSeat(dinersAmount, userID);

			// D. Handle Result (Order vs WaitListResponse)
			if (result instanceof Map) {
				// SUCCESS: Seated Immediately
				Map<String, Object> resMap = (Map<String, Object>) result;
				logger.log("[INFO] Immediate Seating Success. Order: " + ((Order)resMap.get("order")).getConfirmationCode());
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_SKIPPED, resMap));

			} else if (result instanceof WaitListResponse) {
				// FULL: Must Wait
				WaitListResponse resp = (WaitListResponse) result;
				logger.log("[INFO] Table full. Wait time: " + resp.getEstimatedWaitTimeMinutes());
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_CHECK_AVAILABILITY_OK, resp));
			}
		});

		// 3. Add to Waitlist (User accepted the wait time)
		// This handles the explicit request to join the queue
		router.on("waitingList", "addToWaitList", (msg, client) -> {
			// Expecting a Map or Object, but we can extract User ID from session too!
			// Assuming for simplicity the client sends specific data,
			// OR we can use the session user again:

			@SuppressWarnings("unchecked")
			java.util.Map<String, Object> data = (java.util.Map<String, Object>) msg.getData();
			int dinersAmount = (int) data.get("diners");
			int waitTime = (int) data.get("waitTime");

			User currentUser = (User) client.getInfo("user");

			// Create the waitlist order
			String code = waitingListService.createWaitListOrder(dinersAmount, currentUser.getUserId(), true, waitTime);

			if (code != null) {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_JOIN_OK, code));
				logger.log("[INFO] Added to waitlist. Code: " + code);
			} else {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_JOIN_FAIL, null));
			}
		});

		// 4. Leave Waiting List
		router.on("waitingList", "leave", (msg, client) -> {
			String confirmationCode = (String) msg.getData();
			boolean success = waitingListService.removeFromWaitingList(confirmationCode);

			if (success) {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_LEAVE_OK, null));
			} else {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_LEAVE_FAIL, null));
			}
		});
	}
}
// End of WaitingListSubject class