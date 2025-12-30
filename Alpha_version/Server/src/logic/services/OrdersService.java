package logic.services;

import java.util.List;
import java.util.Map;
import java.util.Random;

import entities.Order;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.BistroServer;
import logic.ServerLogger;

public class OrdersService {
	private final BistroServer server;
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	
	
	public OrdersService(BistroServer server,BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
		this.server = server;
	}


	public boolean createNewOrder(List<Object> data, OrderType orderType) {
		List<Object> orderData = data; //Received data contain : userId, date ,dinersAmount , time
		String confimationCode = generateConfirmationCode();
		int orderNumber = generateOrderNumber();
		orderData.add(orderNumber);
		orderData.add(confimationCode);
		orderData.add(orderType);
		orderData.add("PENDING");
		//orderData order: userId, date ,dinersAmount , time, orderNumber, confirmationCode, orderType, status
		return dbController.setNewOrderToDataBase(orderData); //DB should provide date of placing order
		
	}
	
	
	private int generateOrderNumber() {
		int num = 100000 + new Random().nextInt(900000);
		return num;
	}


	private String generateConfirmationCode() {
		int num = 100000 + new Random().nextInt(900000);
	    return "R-" + num;
	}
	
	
	public Order getOrderByConfirmationCode(String confirmationCode, OrderType reservation) {
		// TODO Auto-generated method stub
		return null;
	}


	public List<String> getAvailableReservationHours(Map<String, Object> requestData) {
		// TODO Auto-generated method stub
		return null;
	}

}
