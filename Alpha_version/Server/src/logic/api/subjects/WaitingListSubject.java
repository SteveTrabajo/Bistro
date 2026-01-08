package logic.api.subjects;

import java.util.Map;

import comms.Api;
import comms.Message;
import entities.Order;
import entities.User;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;
import logic.api.Router;
import logic.services.WaitingListService;
/**
 * WaitingListSubject class that registers handlers related to waiting list operations.
 */
public class WaitingListSubject {
	// ******************************** Constructors***********************************
	private WaitingListSubject() {
	}
	// ******************************** Static Methods***********************************
	/**
	 * Registers handlers related to waiting list operations.
	 * 
	 * @param router       The Router instance to register handlers with.
	 * @param waitingListService The BistroDataBase_Controller instance for database operations.
	 * @param logger       The ServerLogger instance for logging.
	 */
	public static void register(Router router, WaitingListService waitingListService, ServerLogger logger) {
		// Handlers related to waiting list can be added here
		
		router.on("WaitingList","isInWaitingList", (msg, client) -> {
			String userID = (String) msg.getData();
			boolean isInWaitingList = waitingListService.isUserInWaitingList(userID);
			if (isInWaitingList) {
				logger.log("[INFO] Client: "+ client + " checked and found to be in the waiting list.");
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_IS_IN_LIST, null));
			}
			else {
				logger.log("[INFO] Client: "+ client + " checked and found NOT to be in the waiting list.");
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_IS_NOT_IN_LIST, null));
			}
		});
		
		
		
		//join to waiting list
		router.on("waitingList", "join", (msg, client) -> {
			//TODO: change logic.
			Map<String, Object> userData = (Map<String, Object>) msg.getData();
			Order createdOrder = waitingListService.addToWaitingList(userData);
			//successful joining to waiting list:
			if (createdOrder.getOrderType()==OrderType.WAITLIST) {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_JOIN_OK, createdOrder));
				logger.log("[INFO] Client: "+ client +" created a waiting list order successfully.");
			}
			//Skip the waiting list and get a reservation directly if there is place immediately available:
			else if(createdOrder.getOrderType()==OrderType.RESERVATION) {
				int tableNumber = waitingListService.assignTableForWaitingListOrder(createdOrder);
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_SKIPPED, tableNumber));
				logger.log("[INFO] Client: "+ client + " joined the waiting list and got a reservation successfully.");
			}
			//in case of failure:
			else {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_JOIN_FAIL, null));
				logger.log("[ERROR] Client: "+ client + " failed to join the waiting list.");
			}
		});
		
		//leave waiting list
		router.on("waitingList", "leave", (msg, client) -> {
			String confirmationCode = (String) msg.getData();
			boolean success = waitingListService.removeFromWaitingList(confirmationCode);
			//successful leaving the waiting list:
			if (success) {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_LEAVE_OK, null));
				logger.log("[INFO] Client: "+ client + " left the waiting list successfully.");
			//failure to leave the waiting list:
			} else {
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_LEAVE_FAIL, null));
				logger.log("[ERROR] Client: "+ client + " failed to leave the waiting list.");
			}
		});
		
	}
}
// End of WaitingListSubject class