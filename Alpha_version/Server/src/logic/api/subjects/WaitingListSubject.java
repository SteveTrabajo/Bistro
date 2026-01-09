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
			int userID = (int) msg.getData();
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
		router.on("waitingList", "checkAvailability", (msg, client) -> {
			int dinersAmount = (int) msg.getData();
			WaitListResponse response = waitingListService.checkAvailabilityForWalkIn(dinersAmount);
			if(response.isCanSeatImmediately()) {
				//can be seated immediately
				//TODO:implement notification
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_SKIPPED, response));
				logger.log("[INFO] Client: "+ client + " can be seated immediately for diners amount: " + dinersAmount);
			} else {
				//added to waiting list
				client.sendToClient(new Message(Api.REPLY_WAITING_LIST_CHECK_AVAILABILITY_OK, response));
				logger.log("[INFO] Client: "+ client + " added to waiting list for diners amount: " + dinersAmount);
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