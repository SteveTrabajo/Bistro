package logic.services;

import java.util.Map;

import entities.Order;
import logic.BistroDataBase_Controller;
import logic.BistroServer;
import logic.ServerLogger;

public class WaitingListService {
	private final BistroServer server;
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	
	public WaitingListService(BistroServer server,BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
		this.server = server;
	}

	public Order addToWaitingList(Map<String, Object> userData) {
		int userId = (int) userData.get("userId");
		int dinersAmount = (int) userData.get("dinersAmount");
		server.getOrdersService().createNewOrderForWaitingList(userId, dinersAmount);
		
	}

	public int assignTableForWaitingListOrder(Order createdOrder) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean removeFromWaitingList(String confirmationCode) {
		// TODO Auto-generated method stub
		return false;
	}

}
