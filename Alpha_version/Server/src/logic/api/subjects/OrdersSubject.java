package logic.api.subjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import comms.Api;
import comms.Message;
import entities.Order;
import logic.BistroDataBase_Controller;
import logic.BistroServerGUI;
import logic.ServerLogger;
import logic.api.Router;
import logic.services.OrdersService;
import enums.OrderStatus;
import enums.OrderType;



/**
 * API handlers related to orders.
 */
public final class OrdersSubject {
	// ******************************** Constructors***********************************
    private OrdersSubject() {}
    
	// ******************************** Static Methods***********************************
    
    /**
     * Registers all order related handlers.
     * @param router
     * @param logger 
     * @param ordersService
     * @param logger 
     */
    public static void register(Router router, OrdersService ordersService, ServerLogger logger) {
    	
    	
		// New reservation order
		router.on("orders", "newReservation", (msg, client) -> {
			@SuppressWarnings("unchecked")
			List<Object> orderData = (ArrayList<Object>)msg.getData();
			boolean orderCreated= ordersService.createNewOrder(orderData, OrderType.RESERVATION);
			if (orderCreated) {
				client.sendToClient(new Message(Api.REPLY_CREATE_RESERVATION_OK, null));
				logger.log("[INFO] Client: "+ client + " created a new reservation order successfully.");
			} else {
				client.sendToClient(new Message(Api.REPLY_CREATE_RESERVATION_FAIL, null));
				logger.log("[ERROR] Client: "+ client + " failed to create a new reservation order.");
			}
		});
		
		// Send Order by confirmation code
		router.on("orders", "getOrder", (msg, client) ->{
			String confirmationCode = (String) msg.getData();
			Order order = ordersService.getOrderByConfirmationCode(confirmationCode);
			if(order != null) {
				client.sendToClient(new Message(Api.REPLY_GET_ORDER_OK, order));
				logger.log("[INFO] Client: "+ client + " retrieved order with confirmation code: " + confirmationCode + " successfully.");
			}else {
				client.sendToClient(new Message(Api.REPLY_GET_ORDER_FAIL, null));
				logger.log("[ERROR] Client: "+ client + " failed to retrieve order with confirmation code: " + confirmationCode + ".");
			}
		});
		
		//check if order exists by confirmation code
		router.on("orders", "checkOrderExists", (msg, client) ->{
			String confirmationCode = (String) msg.getData();
			boolean orderExists = ordersService.checkOrderExists(confirmationCode);
			if(orderExists) {
				client.sendToClient(new Message(Api.REPLY_ORDER_EXISTS, null));
				logger.log("[INFO] Client: "+ client + " confirmed existence of order with confirmation code: " + confirmationCode + " successfully.");
				}else {
					client.sendToClient(new Message(Api.REPLY_ORDER_NOT_EXISTS, null));
				}
		});
		
        //Send available time slots for reservation
        router.on("orders", "getAvailableHours", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String,Object> requestData = (Map<String,Object>) msg.getData();
			List<String> availableHours = ordersService.getAvailableReservationHours(requestData);
			if(availableHours != null) {
				client.sendToClient(new Message(Api.REPLY_ORDER_AVAILABLE_HOURS_OK, availableHours));
			}else {
				client.sendToClient(new Message(Api.REPLY_ORDER_AVAILABLE_HOURS_FAIL, null));
				logger.log("[ERROR] Client: "+ client + " failed to get available reservation hours.");
			}
		});
        
        //send allocated table for reservation
        router.on("orders", "getAllocatedTable", (msg, client) -> {
        	String confirmationCode = (String) msg.getData();
        	int tableNumber = ordersService.getAllocatedTableForReservation(confirmationCode);
        	Order order = ordersService.getOrderByConfirmationCode(confirmationCode);
        	Map<String, Object> responseData = new HashMap<>();
        	responseData.put("tableNumber", tableNumber);
        	responseData.put("order", order);
        	if(tableNumber != -1 && order.getOrderType() == OrderType.RESERVATION) {
				client.sendToClient(new Message(Api.REPLY_GET_ALLOCATED_TABLE_OK, responseData));
				logger.log("[INFO] Client: "+ client + " retrieved allocated table for reservation with confirmation code: " + confirmationCode + " successfully.");
			}else {
				client.sendToClient(new Message(Api.REPLY_GET_ALLOCATED_TABLE_FAIL, null));
				logger.log("[ERROR] Client: "+ client + " failed to retrieve allocated table for reservation with confirmation code: " + confirmationCode + ".");
			}
        });
        
        //Update order status on payment success
        router.on("orders", "paymentSuccess", (msg, client) -> {
        	String confirmationCode = (String) msg.getData();
			boolean paymentUpdated = ordersService.updateOrderStatus(confirmationCode, OrderStatus.COMPLETED);
			if(paymentUpdated) {
				client.sendToClient(new Message(Api.REPLY_PAYMENT_UPDATE_OK, null));
				logger.log("[INFO] Client: "+ client + " updated payment status for order with confirmation code: " + confirmationCode + " successfully.");
			}
			else {
				client.sendToClient(new Message(Api.REPLY_PAYMENT_UPDATE_FAIL, null));
				logger.log("[ERROR] Client: "+ client + " failed to update payment status for order with confirmation code: " + confirmationCode + ".");
			}
        });
    }
}
