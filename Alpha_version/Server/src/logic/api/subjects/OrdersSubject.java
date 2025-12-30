package logic.api.subjects;

import java.util.ArrayList;
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
		router.on("orders", "getOrderConfirmationCode", (msg, client) ->{
			String confirmationCode = (String) msg.getData();
			Order order = ordersService.getOrderByConfirmationCode(confirmationCode, OrderType.RESERVATION);
			if(order != null) {
				client.sendToClient(new Message(Api.REPLY_GET_ORDER_OK, order));
				logger.log("[INFO] Client: "+ client + " retrieved order with confirmation code: " + confirmationCode + " successfully.");
			}else {
				client.sendToClient(new Message(Api.REPLY_GET_ORDER_FAIL, null));
				logger.log("[ERROR] Client: "+ client + " failed to retrieve order with confirmation code: " + confirmationCode + ".");
			}
		});
		
		
    	
        //Send available time slots for reservation
        router.on("orders", "getAvailableHours", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String,Object> requestData = (Map<String,Object>) msg.getData();
			List<String> availableHours = ordersService.getAvailableReservationHours(requestData);
			if(availableHours != null) {
				client.sendToClient(new Message(Api.REPLY_ORDER_AVAIL_HOURS_OK, availableHours));
			}else {
				client.sendToClient(new Message(Api.REPLY_ORDER_AVAIL_HOURS_FAIL, null));
				logger.log("[ERROR] Client: "+ client + " failed to get available reservation hours.");
			}
		});
    }
}
