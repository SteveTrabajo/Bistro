package logic.api.subjects;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import comms.Api;
import comms.Message;
import entities.Order;
import entities.User;
import enums.OrderStatus;
import enums.OrderType;
import logic.ServerLogger;
import logic.api.ServerRouter;
import logic.services.OrdersService;
import logic.services.TableService;

/**
 * API handlers related to orders.
 */
public final class ServerOrdersSubject {
	// ******************************** Constructors***********************************
    private ServerOrdersSubject() {}
    
	// ******************************** Static Methods***********************************
    
    /**
     * Registers all order related handlers.
     * @param router
     * @param logger 
     * @param ordersService
     * @param logger 
     */
    public static void register(ServerRouter router, OrdersService ordersService, TableService tableService, ServerLogger logger) {
    	
    	
		// New reservation order
		router.on("orders", "createReservation", (msg, client) -> {
			User sessionUser = (User) client.getInfo("user");
			if (sessionUser == null) {
				logger.log("[SECURITY] Unauthorized reservation attempt from " + client);
				client.sendToClient(new Message(Api.REPLY_CREATE_RESERVATION_FAIL, "Unauthorized"));
				return;
			}
			@SuppressWarnings("unchecked")
			List<Object> orderData = (ArrayList<Object>)msg.getData();
			// ignore any userId sent from client and use the session userId for security reasons
			if (orderData.size() > 0) {
				orderData.add(0, sessionUser.getUserId());
			}
			Order createdOrder= ordersService.createNewOrder(orderData, OrderType.RESERVATION);
			if (createdOrder != null) {
				client.sendToClient(new Message(Api.REPLY_CREATE_RESERVATION_OK,createdOrder));
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
					logger.log("[INFO] Client: "+ client + " confirmed non-existence of order with confirmation code: " + confirmationCode + " successfully.");
				}
		});
		
        //Send available time slots for reservation
        router.on("orders", "getAvailableHours", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String,Object> requestData = (Map<String,Object>) msg.getData();
			List<String> availableHours = ordersService.getAvailableReservationHours(requestData);
			if(availableHours != null && !availableHours.isEmpty()) {
				client.sendToClient(new Message(Api.REPLY_ORDER_AVAILABLE_HOURS_OK, availableHours));
				logger.log("[INFO] Client: "+ client + " retrieved available reservation hours successfully.");
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
        
        router.on("orders", "getOrdersByDate", (msg, client) -> {
            LocalDate date = (LocalDate) msg.getData();
            List<Order> orders = ordersService.getStaffReservations(date);
            client.sendToClient(new Message(Api.REPLY_GET_RESERVATIONS_BY_DATE_OK, orders));
            logger.log("[INFO] Sent " + orders.size() + " reservations for date: " + date + " successfully.");
        });
        
 		//Send available dates for reservation
 		router.on("orders", "getAvailableDates", (msg, client) -> {
 		    int diners = (int) msg.getData();
 		    List<LocalDate> availableDates = ordersService.getAvailableDates(diners);		    
 		    if(availableDates != null) {
 		        client.sendToClient(new Message(Api.REPLY_AVAILABLE_DATES_OK, availableDates));
 		        logger.log("[INFO] Client: " + client + " retrieved available dates successfully.");
 		    } else {
 		        client.sendToClient(new Message(Api.REPLY_AVAILABLE_DATES_FAIL, null));
 		        logger.log("[ERROR] Client: " + client + " failed to get available dates.");
 		    }
 		});
 		
 		router.on("orders", "seatCustomer", (msg, client) -> {
 	        String confirmationCode = (String) msg.getData();
 	        
 	        int tableNum = tableService.allocateTable(confirmationCode, LocalDateTime.now()); 	        
 	        if (tableNum != 0) {
 	            client.sendToClient(new Message(Api.REPLY_SEAT_CUSTOMER_OK, tableNum));
 	            logger.log("[INFO] Order " + confirmationCode + " seated at Table " + tableNum);
 	        } else {
 	            client.sendToClient(new Message(Api.REPLY_SEAT_CUSTOMER_FAIL, "No available table matches criteria."));
 	            logger.log("[WARN] Failed to seat order " + confirmationCode);
 	        }
 	    });
     		
 		router.on("orders", "cancelReservation", (msg, client) -> {
            String confirmationCode = (String) msg.getData();
            boolean success = ordersService.cancelReservation(confirmationCode);
            if (success) {
                client.sendToClient(new Message(Api.REPLY_CANCEL_RESERVATION_OK, confirmationCode));
                logger.log("[INFO] Order " + confirmationCode + " was cancelled by " + client);
            } else {
                client.sendToClient(new Message(Api.REPLY_CANCEL_RESERVATION_FAIL, "Could not find order or order is already processed."));
                logger.log("[WARN] Failed to cancel order " + confirmationCode);
            }
        });
 		
    }
}
