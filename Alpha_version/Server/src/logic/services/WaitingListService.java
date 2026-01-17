package logic.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dto.WaitListResponse;
import entities.Order;
import entities.Table;
import entities.User;
import enums.OrderStatus;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.BistroServer;
import logic.ServerLogger;

/**
 * Handles all waiting list operations for walk-in customers. This service
 * manages the queue when no tables are immediately available, calculates wait
 * times, and coordinates seating when tables free up.
 * 
 * @author Bistro Team
 */
public class WaitingListService {

	/** Database controller for all DB operations */
	private final BistroDataBase_Controller dbController;

	/** Logger for tracking service activity */
	private final ServerLogger logger;

	/** Service for order-related operations */
	private final OrdersService ordersService;

	/** Service for table management */
	private final TableService tableService;

	/** Service for user operations */
	private final UserService userService;

	/**
	 * Creates a new WaitingListService with all required dependencies.
	 * 
	 * @param dbController  database controller instance
	 * @param logger        server logger for logging events
	 * @param ordersService service for managing orders
	 * @param tableService  service for managing tables
	 * @param userService   service for user operations
	 */
	public WaitingListService(BistroDataBase_Controller dbController, ServerLogger logger, OrdersService ordersService,
			TableService tableService, UserService userService) {
		this.dbController = dbController;
		this.logger = logger;
		this.ordersService = ordersService;
		this.tableService = tableService;
		this.userService = userService;
	}

	/**
	 * Creates a new waitlist order in the database. Generates a confirmation code
	 * starting with 'W' for waitlist orders. If the user is being added to the
	 * queue, sets the estimated wait time. If seating immediately, marks the order
	 * as NOTIFIED.
	 * 
	 * @param dinersAmount       number of people in the party
	 * @param userID             the user's ID
	 * @param addToWaitlist      true if adding to queue, false if seating right
	 *                           away
	 * @param calculatedWaitTime estimated wait in minutes (only used if
	 *                           addToWaitlist is true)
	 * @return confirmation code if successful, null if creation failed
	 */
	public String createWaitListOrder(int dinersAmount, int userID, boolean addToWaitlist, int calculatedWaitTime) {
		// Prepare data for new order
		List<Object> data = new ArrayList<>();
		data.add(userID);
		data.add(null);
		data.add(dinersAmount);
		data.add(null);
		String confirmationCode = ordersService.generateConfirmationCode("W");
		data.add(confirmationCode);
		// create the order in the DB
		boolean success = dbController.setNewOrder(data, OrderType.WAITLIST, OrderStatus.PENDING);
		// If adding to waitlist, we need to set the wait time
		if (success) {
			if (addToWaitlist) {
				// The trigger inserted NULL for time, so we must update it manually
				dbController.enqueueWaitingList(confirmationCode, calculatedWaitTime);
			} else {
				dbController.updateOrderStatusByConfirmCode(confirmationCode, OrderStatus.NOTIFIED);
			}
			return confirmationCode; // Successfully created
		}
		return null; // Failed to create
	}

	/**
	 * Checks if a table is available for a walk-in group and attempts to seat them.
	 * First looks for a free table that fits the group size. Then checks if seating
	 * them now would interfere with upcoming reservations. Returns either an Order
	 * (if seated) or a WaitListResponse (if they need to wait).
	 * 
	 * @param dinersAmount number of people in the party
	 * @param userID       the user's ID
	 * @return Order object with table number if seated, or WaitListResponse if
	 *         restaurant is full
	 */
	public Object checkAvailabilityAndSeat(int dinersAmount, int userID) {

		LocalDate today = LocalDate.now();
		LocalTime now = LocalTime.now();
		int duration = ordersService.getReservationDurationMinutes();
		LocalTime walkInEnd = now.plusMinutes(duration);

		int freeTable = dbController.findFreeTableForGroup(dinersAmount);
		if (freeTable == -1) {
			long wait = calculateEstimatedWaitTime(dinersAmount);
			return new WaitListResponse(true, wait, "No table available. Estimated wait: " + wait + " minutes.");
		}

		boolean safe = canSeatWalkInWithoutHurtingReservations(today, now, walkInEnd, dinersAmount);

		if (!safe) {
			long wait = calculateEstimatedWaitTime(dinersAmount);
			return new WaitListResponse(true, wait,
					"Seating now may affect reservations. Estimated wait: " + wait + " minutes.");
		}

		String code = createWaitListOrder(dinersAmount, userID, false, 0);
		int tableNum = tableService.allocateTable(code, LocalDateTime.now());

		Order order = ordersService.getOrderByConfirmationCode(code);

		Map<String, Object> res = new HashMap<>();
		res.put("order", order);
		res.put("table", tableNum);
		return res;
	}

	/**
	 * Checks if seating a walk-in group now would cause problems for upcoming
	 * reservations. Looks at currently seated orders and pending reservations that
	 * overlap with the walk-in window.
	 * 
	 * @param date         the date to check
	 * @param now          current time
	 * @param walkInEnd    when the walk-in's session would end
	 * @param dinersAmount size of the walk-in group
	 * @return true if safe to seat, false if it would conflict with reservations
	 */
	private boolean canSeatWalkInWithoutHurtingReservations(LocalDate date, LocalTime now, LocalTime walkInEnd,
			int dinersAmount) {

		List<Order> conflicts = dbController.getActiveAndUpcomingOrders(date, now, walkInEnd);

		List<Integer> load = new ArrayList<>();

		for (Order o : conflicts) {
			if (o.getStatus() == OrderStatus.SEATED) {
				load.add(o.getDinersAmount());
			} else if (o.getOrderType() == OrderType.RESERVATION && o.getStatus() == OrderStatus.PENDING) {

				LocalTime start = o.getOrderHour();
				LocalTime end = start.plusMinutes(ordersService.getReservationDurationMinutes());

				if (ordersService.overlaps(now, walkInEnd, start, end)) {
					load.add(o.getDinersAmount());
				}
			}
		}

		load.add(dinersAmount);

		List<Integer> freeTableSizes = new ArrayList<>();
		for (Table t : tableService.getAllTables()) {
			if (!t.isOccupiedNow()) {
				freeTableSizes.add(t.getCapacity());
			}
		}

		return ordersService.canAssignAllDinersToTables(load, freeTableSizes);
	}

	/**
	 * Estimates how long a group will need to wait for a table. Queries the
	 * database for the earliest time a suitable table will be free. Returns a
	 * 5-minute buffer if tables appear free but aren't ready yet (being cleaned).
	 * Falls back to 60 minutes if no data is available.
	 * 
	 * @param dinersAmount number of people needing a table
	 * @return estimated wait time in minutes
	 */
	private long calculateEstimatedWaitTime(int dinersAmount) {
		// Query DB for the earliest 'expected_end_at' of a suitable table
		LocalTime earliestFreeTime = dbController.getEarliestExpectedEndTime(dinersAmount);
		if (earliestFreeTime == null) {
			return 60; // Fallback: Default wait if data is missing
		}
		LocalTime now = LocalTime.now();
		long minutes = Duration.between(now, earliestFreeTime).toMinutes();
		// If table is technically free (minutes <= 0) but algorithm said no,
		// it means staff is clearing it. Give a 5-minute buffer.
		if (minutes > 0) {
			return minutes;
		} else {
			return 5;
		}
	}

	/**
	 * Removes a user from the waiting list by cancelling their order. The database
	 * trigger handles cleanup automatically when status changes to CANCELLED.
	 * 
	 * @param confirmationCode the order's confirmation code
	 * @return true if successfully removed, false otherwise
	 */
	public boolean removeFromWaitingList(String confirmationCode) {
		// Setting to CANCELLED triggers the SQL cleanup automatically
		return dbController.removeFromWaitingList(confirmationCode);
	}

	/**
	 * Checks if a user is currently in the waiting list.
	 * 
	 * @param userID the user's ID to check
	 * @return true if user has an active waitlist entry
	 */
	public boolean isUserInWaitingList(int userID) {
		return dbController.isUserInWaitingList(userID);
	}

	/**
	 * Retrieves a waitlist order by its confirmation code.
	 * 
	 * @param code the confirmation code
	 * @return the Order object, or null if not found
	 */
	public Order getWaitingListOrderByCode(String code) {
		return ordersService.getOrderByConfirmationCode(code);
	}

	/**
	 * Retrieves a user's current waitlist order.
	 * 
	 * @param userID the user's ID
	 * @return the Order object, or null if user isn't in the queue
	 */
	public Order getWaitingListOrderByUserId(int userID) {
		return dbController.getWaitingListOrderByUserId(userID);
	}

	/**
	 * Gets all orders currently in the waiting queue. Pulls data from the waiting
	 * queue database view.
	 * 
	 * @return list of orders in the queue, ordered by position
	 */
	public List<Order> getCurrentQueue() {
		// We return a list of Order entities that are currently in the waitlist
		return dbController.getWaitingQueueFromView();
	}

}