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

public class WaitingListService {
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	private final OrdersService ordersService;
	private final TableService tableService;
	private final UserService userService;
	
	/**
	 * Constructor for WaitingListService.
	 * @param dbController Database controller for DB operations.
	 * @param logger Server logger for logging events.
	 * @param ordersService Service for managing orders.
	 * @param tableService Service for managing tables.
	 * @param userService Service for managing users.
	 */
	public WaitingListService(BistroDataBase_Controller dbController,ServerLogger logger,
			OrdersService ordersService,TableService tableService, UserService userService) {
		this.dbController = dbController;
		this.logger = logger;
		this.ordersService = ordersService;
		this.tableService = tableService;
		this.userService = userService;
	}

	/**
	 * Creates a WaitList Order in the database.
	 * @param dinersAmount Number of diners in the group.
	 * @param userID ID of the user making the request.
	 * @param addToWaitlist True if adding to waitlist, false for immediate seating.
	 * @param calculatedWaitTime Estimated wait time in minutes (if adding to waitlist).
	 * @return Confirmation code of the created order, or null if creation failed.
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
        //create the order in the DB
        boolean success = dbController.setNewOrder(data, OrderType.WAITLIST, OrderStatus.PENDING);
        // If adding to waitlist, we need to set the wait time
		if (success) {
			if (addToWaitlist) {
				// The trigger inserted NULL for time, so we must update it manually
				dbController.enqueueWaitingList(confirmationCode, calculatedWaitTime);
			}else {
				dbController.updateOrderStatusByConfirmCode(confirmationCode, OrderStatus.NOTIFIED);
			}
			return confirmationCode; // Successfully created
		}
		return null; // Failed to create
    }
    

    /**
     * Checks availability and Attempts to Seat Immediately.
     * @param dinersAmount Number of diners in the group.
     * @param userID ID of the user making the request.
     * @return Order object (if seated) OR WaitListResponse object (if full).
     */
    public Object checkAvailabilityAndSeat(int dinersAmount, int userID) {
    	
    	// Check if user exists
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        int duration = ordersService.getReservationDurationMinutes();
        LocalTime walkInEnd = now.plusMinutes(duration);
        // Check for free table
        int freeTable = dbController.findFreeTableForGroup(dinersAmount);
        if (freeTable == -1 ) {
            long wait = calculateEstimatedWaitTime(dinersAmount);
            return new WaitListResponse(true, wait,"No table available. Estimated wait: " + wait + " minutes.");
        }
        // Check if seating now would hurt reservations
        boolean safe = canSeatWalkInWithoutHurtingReservations(today, now, walkInEnd, dinersAmount);
        // If not safe, suggest waitlist
        if (!safe) {
            long wait = calculateEstimatedWaitTime(dinersAmount);
            return new WaitListResponse(true, wait,
                    "Seating now may affect reservations. Estimated wait: " + wait + " minutes.");
        }
		// Proceed to seat immediately
        String code = createWaitListOrder(dinersAmount, userID, false, 0);
        int tableNum = tableService.allocateTable(code, LocalDateTime.now());

        Order order = ordersService.getOrderByConfirmationCode(code);
        // Return both order and table number
        Map<String, Object> res = new HashMap<>();
        res.put("order", order);
        res.put("table", tableNum);
        return res;
    }

    /**
     * Checks if seating a walk-in group now would interfere with upcoming reservations.
     * @param date Current date.
     * @param now Current time.
     * @param walkInEnd End time of the walk-in seating.
     * @param dinersAmount Number of diners in the walk-in group.
     * @return True if seating is safe, false otherwise.
     */
	private boolean canSeatWalkInWithoutHurtingReservations(LocalDate date, LocalTime now, LocalTime walkInEnd,
			int dinersAmount) {
		// Get all active and upcoming orders for today
		List<Order> conflicts = dbController.getActiveAndUpcomingOrders(date, now, walkInEnd);
		// Calculate current load including the walk-in group
		List<Integer> load = new ArrayList<>();
		// Add diners from conflicting orders
		for (Order o : conflicts) {
			if (o.getStatus() == OrderStatus.SEATED) {
				load.add(o.getDinersAmount());
			} else if (o.getOrderType() == OrderType.RESERVATION && o.getStatus() == OrderStatus.PENDING) {
				// Check for time overlap
				LocalTime start = o.getOrderHour();
				LocalTime end = start.plusMinutes(ordersService.getReservationDurationMinutes());
				// If overlaps, add to load
				if (ordersService.overlaps(now, walkInEnd, start, end)) {
					load.add(o.getDinersAmount());
				}
			}
		}
		// Add the walk-in group
		load.add(dinersAmount);
		// Get sizes of all free tables
		List<Integer> freeTableSizes = new ArrayList<>();
		for (Table t : tableService.getAllTables()) {
			if (!t.isOccupiedNow()) {
				freeTableSizes.add(t.getCapacity());
			}
		}
		// Check if all diners can be assigned to tables
		return ordersService.canAssignAllDinersToTables(load, freeTableSizes);
	}

    /**
     * Calculates the estimated wait time for a walk-in group based on the earliest
     * @param dinersAmount The number of diners in the walk-in group.
     * @return Estimated wait time in minutes.
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
	 * Removes a user from the waiting list by updating their order status to CANCELLED.
	 * @param confirmationCode The confirmation code of the waitlist order.
	 * @return True if the operation was successful, false otherwise.
	 */
    public boolean removeFromWaitingList(String confirmationCode) {
        // Setting to CANCELLED triggers the SQL cleanup automatically
        return dbController.removeFromWaitingList(confirmationCode);
    }
    
    /**
	 * Checks if a user is currently in the waiting list.
	 * @param userID The ID of the user to check.
	 * @return True if the user is in the waiting list, false otherwise.
	 */
    public boolean isUserInWaitingList(int userID) { 
        return dbController.isUserInWaitingList(userID);
    }

    /**
	 * Retrieves a waiting list order by its confirmation code.
	 * @param code The confirmation code of the order.
	 * @return The Order object if found, null otherwise.
	 */
	public Order getWaitingListOrderByCode(String code) {
		return ordersService.getOrderByConfirmationCode(code);
	}

	/**
	 * Retrieves a waiting list order by user ID.
	 * @param userID The ID of the user.
	 * @return The Order object if found, null otherwise.
	 */
	public Order getWaitingListOrderByUserId(int userID) {
		return dbController.getWaitingListOrderByUserId(userID);
	}
	
	/**
	 * Retrieves the current waiting queue as a list of Order entities.
	 * @return List of Order objects in the waiting queue.
	 */
    public List<Order> getCurrentQueue() {
        // We return a list of Order entities that are currently in the waitlist
        return dbController.getWaitingQueueFromView();
    }

}
