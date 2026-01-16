package logic.api.subjects;

import java.util.List;
import java.util.Map;

import comms.Api;
import comms.Message;
import dto.WaitListResponse;
import entities.Order;
import entities.User;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;
import logic.api.ServerRouter;
import logic.services.WaitingListService;

/**
 * ServerWaitingListSubject class that registers handlers related to waiting list
 * operations.
 */
public class ServerWaitingListSubject {
	
	// ******************************** Constructors ***********************************
	
	private ServerWaitingListSubject() {}
	
	// ******************************** Static Methods***********************************
	/**
	 * Registers handlers related to waiting list operations.
	 * 
	 * @param router             The ServerRouter instance to register handlers with.
	 * @param waitingListService The BistroDataBase_Controller instance for database
	 *                           operations.
	 * @param logger             The ServerLogger instance for logging.
	 */
	public static void register(ServerRouter router, WaitingListService waitingListService, ServerLogger logger) {
		// 1. Check if user is in waiting list
		router.on("waitinglist", "isInWaitingList", (msg, client) -> {
			int userID = (int) msg.getData();
			boolean isInWaitingList = waitingListService.isUserInWaitingList(userID);
			if (isInWaitingList) {
				Order waitListOrder = waitingListService.getWaitingListOrderByUserId(userID);
				if (waitListOrder != null && waitListOrder.getOrderType() == OrderType.WAITLIST) {
					logger.log("[INFO] User ID: " + userID + " has waitlist order: " + waitListOrder.getConfirmationCode());
					client.sendToClient(new Message(Api.REPLY_WAITING_LIST_IS_IN_LIST, waitListOrder));
				}
			} else {
				logger.log("[INFO] Client: " + client + " is NOT in waiting list.");
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_IS_NOT_IN_LIST, false));
			}
		});

		// 2. Check Availability AND Seat if possible (Optimistic Scheduling)
		// Client sends: int (dinersAmount)
		// Server uses: client.getInfo("user") to get contact info
		router.on("waitinglist", "checkAvailability", (msg, client) -> {
			// TODO : add option to staff to add users to waitlist
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
				logger.log("[INFO] Immediate Seating Success. Order: "
						+ ((Order) resMap.get("order")).getConfirmationCode());
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_CHECK_AVALIBILTY_SKIPPED_TO_SEAT, resMap));

			} else if (result instanceof WaitListResponse) {
				// FULL: Must Wait
				WaitListResponse resp = (WaitListResponse) result;
				logger.log("[INFO] Table full. Wait time: " + resp.getEstimatedWaitTimeMinutes());
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_CHECK_AVAILABILITY_OK, resp));
			}
		});

		// 3. Add to Waitlist (User accepted the wait time)
		// This handles the explicit request to join the queue
		router.on("waitinglist", "join", (msg, client) -> {
			// Expecting a Map or Object, but we can extract User ID from session too!
			// Assuming for simplicity the client sends specific data,
			// OR we can use the session user again:

			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) msg.getData();
			int dinersAmount = (int)data.get("diners");
			Integer waitTime = (Integer)data.get("waitTime");
			System.out.println("Join waitlist requested: diners=" + dinersAmount + ", waitTime=" + waitTime);
			User currentUser = (User) client.getInfo("user");

			// Create the waitlist order
			String code = waitingListService.createWaitListOrder(dinersAmount, currentUser.getUserId(), true, waitTime);
			if (code != null) {
				Order waitListOrder= waitingListService.getWaitingListOrderByCode(code);
				if (waitListOrder != null) {
					logger.log("[INFO] Added to waitlist. Code: " + code);
					client.sendToClient(new Message(Api.REPLY_WAITING_LIST_JOIN_OK, waitListOrder));
				}
			} else {
				logger.log("[ERROR] Failed to join waiting list for client: " + client);
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_JOIN_FAIL, null));
			}
		});

		// 4. Leave Waiting List
		router.on("waitinglist", "leave", (msg, client) -> {
			String confirmationCode = (String) msg.getData();
			boolean success = waitingListService.removeFromWaitingList(confirmationCode);
			if (success) {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_LEAVE_OK, null));
			} else {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_LEAVE_FAIL, null));
			}
		});
		
		
		router.on("waitinglist", "getAll", (msg, client) -> {
			List<Order> waitingList = waitingListService.getCurrentQueue();
			if (waitingList != null) {
				logger.log("[INFO] Sent current waiting list to client: " + client);
				client.sendToClient(new Message(Api.REPLY_GET_WAITING_LIST_OK, waitingList));
			} else {
				logger.log("[ERROR] Failed to retrieve waiting list for client: " + client);
				client.sendToClient(new Message(Api.REPLY_GET_WAITING_LIST_FAIL, null));
			}
		});
		
//		router.on("waitinglist", "addWalkIn", (msg, client) -> {
//		    try {
//		        @SuppressWarnings("unchecked")
//		        Map<String, Object> data = (Map<String, Object>) msg.getData();
//		        
//		        int dinersAmount = ((Number) data.get("diners")).intValue();
//		        String type = (String) data.get("type");
//		        
//		        Object response = null;
//		        if ("MEMBER".equals(type)) {
//		            response = waitingListService.handleMemberWalkIn(dinersAmount, (String) data.get("memberId"));
//		        } else if ("GUEST".equals(type)) {
//		            response = waitingListService.handleGuestWalkIn(dinersAmount, (String) data.get("phone"), (String) data.get("email"));
//		        }
//
//		        if (response != null) {
//		            // Could be Order or WaitListResponse
//		            client.sendToClient(new Message(Api.REPLY_WAITING_LIST_ADD_WALKIN_OK, response));
//		        } else {
//		            client.sendToClient(new Message(Api.REPLY_WAITING_LIST_ADD_WALKIN_FAIL, "User registration failed"));
//		        }
//		    } catch (Exception e) {
//		        logger.log("[ERROR] addWalkIn critical failure: " + e.getMessage());
//		        client.sendToClient(new Message(Api.REPLY_WAITING_LIST_ADD_WALKIN_FAIL, "Internal Server Error"));
//		    }
//		});
	}
}
// End of ServerWaitingListSubject class