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
import enums.UserType;
import logic.ServerLogger;
import logic.api.ServerRouter;
import logic.services.OrdersService;
import logic.services.TableService;

/**
 * API handlers related to orders.
 */
public final class ServerOrdersSubject {
	// ********************************
	// Constructors***********************************
	private ServerOrdersSubject() {
	}

	// ******************************** Static
	// Methods***********************************

	/**
	 * Registers all order related handlers.
	 * 
	 * @param router
	 * @param logger
	 * @param ordersService
	 * @param logger
	 */
	public static void register(ServerRouter router, OrdersService ordersService, TableService tableService,
			ServerLogger logger) {

		// New reservation order
		router.on("orders", "createReservation", (msg, client) -> {
			User sessionUser = (User) client.getInfo("user");
			if (sessionUser == null) {
				logger.log("[SECURITY] Unauthorized reservation attempt from " + client);
				client.sendToClient(new Message(Api.REPLY_CREATE_RESERVATION_FAIL, "Unauthorized"));
				return;
			}
			@SuppressWarnings("unchecked")
			List<Object> orderData = (ArrayList<Object>) msg.getData();
			// ignore any userId sent from client and use the session userId for security
			// reasons
			if (orderData.size() > 0) {
				orderData.add(0, sessionUser.getUserId());
			}
			Order createdOrder = ordersService.createNewOrder(orderData, OrderType.RESERVATION);
			if (createdOrder != null) {
				client.sendToClient(new Message(Api.REPLY_CREATE_RESERVATION_OK, createdOrder));
				logger.log("[INFO] Client: " + client + " created a new reservation order successfully.");
			} else {
				client.sendToClient(new Message(Api.REPLY_CREATE_RESERVATION_FAIL, null));
				logger.log("[ERROR] Client: " + client + " failed to create a new reservation order.");
			}
		});

		router.on("orders", "createReservation.asStaff", (msg, client) -> {
			Map<String, Object> data = (Map<String, Object>) msg.getData();
			Order createdOrder = ordersService.createReservationAsStaff(data);
			if (createdOrder != null) {
				client.sendToClient(new Message(Api.REPLY_CREATE_RESERVATION_AS_STAFF_OK, createdOrder));
				logger.log("[INFO] Client: " + client + " created a new reservation order as staff successfully.");
			} else {
				client.sendToClient(new Message(Api.REPLY_CREATE_RESERVATION_AS_STAFF_FAIL, null));
				logger.log("[ERROR] Client: " + client + " failed to create a new reservation order as staff.");
			}
		});

		// Send Order by confirmation code
		router.on("orders", "getOrder", (msg, client) -> {
			String confirmationCode = (String) msg.getData();
			Order order = ordersService.getOrderByConfirmationCode(confirmationCode);
			if (order != null) {
				client.sendToClient(new Message(Api.REPLY_GET_ORDER_OK, order));
				logger.log("[INFO] Client: " + client + " retrieved order with confirmation code: " + confirmationCode
						+ " successfully.");
			} else {
				client.sendToClient(new Message(Api.REPLY_GET_ORDER_FAIL, null));
				logger.log("[ERROR] Client: " + client + " failed to retrieve order with confirmation code: "
						+ confirmationCode + ".");
			}
		});

		// check if order exists and belongs to user by confirmation code and user data
		router.on("orders", "checkOrderExists", (msg, client) -> {
			String confirmationCode = (String) msg.getData();
			User sessionUser = (User) client.getInfo("user");

			if (sessionUser == null) {
				logger.log("[SECURITY] Unauthorized order existence check attempt from " + client);
				client.sendToClient(new Message(Api.REPLY_ORDER_NOT_EXISTS, null));
				return;
			}

			boolean isValid = ordersService.checkOrderBelongsToUser(confirmationCode, sessionUser.getUserId());
			if (isValid) {
				client.sendToClient(new Message(Api.REPLY_ORDER_EXISTS, confirmationCode));
				logger.log("[INFO] Client: " + client + " confirmed valid check-in for code: " + confirmationCode);
			} else {
				client.sendToClient(new Message(Api.REPLY_ORDER_NOT_EXISTS, null));
				logger.log("[INFO] Client: " + client + " attempted invalid check-in for code: " + confirmationCode);
			}
		});

		// Send client order history
		router.on("orders", "getClientHistory", (msg, client) -> {
			User sessionUser = (User) client.getInfo("user");
			if (sessionUser != null) {
				List<Order> history = ordersService.getClientHistory(sessionUser.getUserId());
				client.sendToClient(new Message(Api.REPLY_CLIENT_ORDER_HISTORY_OK, history));
				logger.log("[INFO] Sent order history to client " + client);
			} else {
				client.sendToClient(new Message(Api.REPLY_CLIENT_ORDER_HISTORY_OK, new ArrayList<>()));
			}
		});

		// Request: Get all active reservations for the logged-in member (for Check-In)
		router.on("orders", "getMemberActiveReservations", (msg, client) -> {
			User sessionUser = (User) client.getInfo("user");

			// Security Check
			if (sessionUser == null) {
				logger.log("[SECURITY] Unauthorized active reservations request from " + client);
				client.sendToClient(new Message(Api.REPLY_MEMBER_ACTIVE_RESERVATIONS_FAIL, null));
				return;
			}

			// Get Data from Service
			List<Order> activeOrders = ordersService.getMemberActiveReservations(sessionUser.getUserId());

			if (activeOrders != null) {
				// Even if empty, we send OK so the UI knows the search finished successfully
				client.sendToClient(new Message(Api.REPLY_MEMBER_ACTIVE_RESERVATIONS_OK, activeOrders));
				logger.log("[INFO] Sent " + activeOrders.size() + " active reservations to member "
						+ sessionUser.getUsername());
			} else {
				client.sendToClient(new Message(Api.REPLY_MEMBER_ACTIVE_RESERVATIONS_FAIL, null));
				logger.log("[ERROR] Failed to retrieve active reservations for member " + sessionUser.getUsername());
			}
		});

		// Send member history by member code (staff only)
		router.on("orders", "getMemberHistory", (msg, client) -> {
			User sessionUser = (User) client.getInfo("user");
			if (sessionUser == null || (sessionUser.getUserType() != UserType.EMPLOYEE
					&& sessionUser.getUserType() != UserType.MANAGER)) {
				client.sendToClient(new Message(Api.REPLY_GET_MEMBER_HISTORY_FAIL, "Unauthorized"));
				logger.log("[SECURITY] Unauthorized member history access attempt from " + client);
				return;
			}

			int memberCode = (int) msg.getData();
			logger.log("[DEBUG] Looking up member history for member code: " + memberCode);

			List<Order> history = ordersService.getMemberHistoryByCode(memberCode);
			logger.log(
					"[DEBUG] Member history lookup result: " + (history == null ? "null" : history.size() + " orders"));

			if (history != null) {
				client.sendToClient(new Message(Api.REPLY_GET_MEMBER_HISTORY_OK, history));
				logger.log("[INFO] Staff " + sessionUser.getUserId() + " retrieved history for member code "
						+ memberCode + " (" + history.size() + " orders)");
			} else {
				client.sendToClient(new Message(Api.REPLY_GET_MEMBER_HISTORY_FAIL, "Member not found"));
				logger.log("[WARN] Member not found with code " + memberCode);
			}
		});

		// Send available time slots for reservation
		router.on("orders", "getAvailableHours", (msg, client) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> requestData = (Map<String, Object>) msg.getData();
			List<String> availableHours = ordersService.getAvailableReservationHours(requestData);
			if (availableHours != null && !availableHours.isEmpty()) {
				client.sendToClient(new Message(Api.REPLY_ORDER_AVAILABLE_HOURS_OK, availableHours));
				logger.log("[INFO] Client: " + client + " retrieved available reservation hours successfully.");
			} else {
				client.sendToClient(new Message(Api.REPLY_ORDER_AVAILABLE_HOURS_FAIL, null));
				logger.log("[ERROR] Client: " + client + " failed to get available reservation hours.");
			}
		});

		// send allocated table for reservation
		router.on("orders", "getAllocatedTable", (msg, client) -> {
			String confirmationCode = (String) msg.getData();
			int tableNumber = ordersService.getAllocatedTableForReservation(confirmationCode);
			Order order = ordersService.getOrderByConfirmationCode(confirmationCode);
			Map<String, Object> responseData = new HashMap<>();
			responseData.put("tableNumber", tableNumber);
			responseData.put("order", order);
			if (tableNumber != -1 && order.getOrderType() == OrderType.RESERVATION) {
				client.sendToClient(new Message(Api.REPLY_GET_ALLOCATED_TABLE_OK, responseData));
				logger.log("[INFO] Client: " + client
						+ " retrieved allocated table for reservation with confirmation code: " + confirmationCode
						+ " successfully.");
			} else {
				client.sendToClient(new Message(Api.REPLY_GET_ALLOCATED_TABLE_FAIL, null));
				logger.log("[ERROR] Client: " + client
						+ " failed to retrieve allocated table for reservation with confirmation code: "
						+ confirmationCode + ".");
			}
		});

		// Update order status on payment success
		router.on("orders", "paymentSuccess", (msg, client) -> {
			String confirmationCode = (String) msg.getData();
			boolean paymentUpdated = ordersService.updateOrderStatus(confirmationCode, OrderStatus.COMPLETED);
			if (paymentUpdated) {
				client.sendToClient(new Message(Api.REPLY_PAYMENT_UPDATE_OK, null));
				logger.log("[INFO] Client: " + client + " updated payment status for order with confirmation code: "
						+ confirmationCode + " successfully.");
			} else {
				client.sendToClient(new Message(Api.REPLY_PAYMENT_UPDATE_FAIL, null));
				logger.log("[ERROR] Client: " + client
						+ " failed to update payment status for order with confirmation code: " + confirmationCode
						+ ".");
			}
		});

		router.on("orders", "getOrdersByDate", (msg, client) -> {
			LocalDate date = (LocalDate) msg.getData();
			List<Order> orders = ordersService.getStaffReservations(date);
			client.sendToClient(new Message(Api.REPLY_GET_RESERVATIONS_BY_DATE_OK, orders));
			logger.log("[INFO] Sent " + orders.size() + " reservations for date: " + date + " successfully.");
		});

		// Send available dates for reservation
		router.on("orders", "getAvailableDates", (msg, client) -> {
			int diners = (int) msg.getData();
			List<LocalDate> availableDates = ordersService.getAvailableDates(diners);
			if (availableDates != null) {
				client.sendToClient(new Message(Api.REPLY_AVAILABLE_DATES_OK, availableDates));
				logger.log("[INFO] Client: " + client + " retrieved available dates successfully.");
			} else {
				client.sendToClient(new Message(Api.REPLY_AVAILABLE_DATES_FAIL, null));
				logger.log("[ERROR] Client: " + client + " failed to get available dates.");
			}
		});

		router.on("orders", "seatCustomer", (msg, client) -> {
			String confirmationCode = (String) msg.getData();
			User sessionUser = (User) client.getInfo("user");

			if (sessionUser == null) {
				client.sendToClient(new Message(Api.REPLY_SEAT_CUSTOMER_FAIL, "Unauthorized"));
				logger.log("[SECURITY] Unauthorized seat customer attempt from " + client);
				return;
			}

			Order order = ordersService.getOrderByConfirmationCode(confirmationCode);
			if (order == null) {
				client.sendToClient(new Message(Api.REPLY_SEAT_CUSTOMER_FAIL, "Reservation not found."));
				logger.log("[WARN] Attempted to seat non-existent order " + confirmationCode);
				return;
			}

			boolean isStaff = (sessionUser.getUserType() == UserType.EMPLOYEE
					|| sessionUser.getUserType() == UserType.MANAGER);
			boolean isOrderOwner = order.getUserId() == sessionUser.getUserId();

			if (isStaff) {
				if (order.getStatus() == OrderStatus.SEATED || order.getStatus() == OrderStatus.CANCELLED
						|| order.getStatus() == OrderStatus.COMPLETED) {
					client.sendToClient(
							new Message(Api.REPLY_SEAT_CUSTOMER_FAIL, "Order is already " + order.getStatus()));
					return;
				}

			} else if (isOrderOwner) {
				if (order.getStatus() != OrderStatus.NOTIFIED) {
					client.sendToClient(new Message(Api.REPLY_SEAT_CUSTOMER_FAIL,
							"Reservation not ready for self-check-in. Please wait for notification or see host."));
					return;
				}

			} else {
				client.sendToClient(new Message(Api.REPLY_SEAT_CUSTOMER_FAIL,
						"Access Denied: This reservation does not belong to you."));
				return;
			}

			int tableNum = tableService.allocateTable(confirmationCode, LocalDateTime.now());
			if (tableNum > 0) {
				client.sendToClient(new Message(Api.REPLY_SEAT_CUSTOMER_OK, tableNum));
				logger.log("[INFO] Order " + confirmationCode + " seated at Table " + tableNum + " by "
						+ sessionUser.getUserType());
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
				client.sendToClient(new Message(Api.REPLY_CANCEL_RESERVATION_FAIL,
						"Could not find order or order is already processed."));
				logger.log("[WARN] Failed to cancel order " + confirmationCode);
			}
		});
		router.on("reservation", "forgotConfirmationCode", (msg, client) -> {
			User sessionUser = (User) client.getInfo("user");
			if (sessionUser == null) {
				logger.log("[SECURITY] Unauthorized forgot confirmation code attempt from " + client);
				client.sendToClient(new Message(Api.REPLY_FORGOT_CONFIRMATION_CODE_FAILED, "Unauthorized"));
				return;
			}
			String code = ordersService.getEarlierReservationCodeByUserId(sessionUser.getUserId());
			if (code != null && !code.isEmpty()) {
				client.sendToClient(new Message(Api.REPLY_FORGOT_CONFIRMATION_CODE_OK, code));
				logger.log("[INFO] Client: " + client + " retrieved reservation confirmation codes successfully.");
			} else {
				client.sendToClient(
						new Message(Api.REPLY_FORGOT_CONFIRMATION_CODE_FAILED, "No reservation codes found."));
				logger.log("[ERROR] Client: " + client + " failed to retrieve reservation confirmation codes.");
			}
		});
		
		router.on("orders", "getMemberSeatedReservations", (msg, client) -> {
		    User sessionUser = (User) client.getInfo("user");
		    if (sessionUser == null) {
		        client.sendToClient(new Message(Api.REPLY_MEMBER_SEATED_RESERVATIONS_FAIL, null));
		        return;
		    }
		    // Call DB Controller directly or via Service
		    // Assuming you add getMemberSeatedReservationsForToday to OrdersService as well
		    List<Order> orders = ordersService.getMemberSeatedReservations(sessionUser.getUserId());
		    
		    if (orders != null) {
		        client.sendToClient(new Message(Api.REPLY_MEMBER_SEATED_RESERVATIONS_OK, orders));
		    } else {
		        client.sendToClient(new Message(Api.REPLY_MEMBER_SEATED_RESERVATIONS_FAIL, null));
		    }
		});

		// Handler for Guest Seated Code
		router.on("orders", "recoverGuestSeatedCode", (msg, client) -> {
		    @SuppressWarnings("unchecked")
		    Map<String, String> data = (Map<String, String>) msg.getData();
		    String email = data.get("email");
		    String phone = data.get("phone");
		    
		    // Call DB Controller directly or via Service
		    String code = ordersService.recoverGuestSeatedCode(email, phone);
		    
		    if (code != null && !code.equals("NOT_FOUND")) {
		        client.sendToClient(new Message(Api.REPLY_GUEST_SEATED_CODE_OK, code));
		    } else {
		        client.sendToClient(new Message(Api.REPLY_GUEST_SEATED_CODE_FAIL, null));
		    }
		});
	}
}
