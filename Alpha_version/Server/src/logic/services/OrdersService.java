package logic.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import entities.Order;
import entities.Table;
import entities.User;
import enums.OrderStatus;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.BistroServer;
import logic.ServerLogger;

/**
 * Handles all order and reservation logic for the restaurant.
 * This includes creating reservations, calculating available time slots,
 * managing order statuses, and checking for no-shows.
 * 
 * The slot availability algorithm simulates overlapping reservations
 * and uses a greedy table assignment approach to maximize seating.
 */
public class OrdersService {
	
	// ******************************** Instance variables ***********************************
	
	/** Server instance for communication */
	private final BistroServer server;
	
	/** Database controller for all DB operations */
	private final BistroDataBase_Controller dbController;
	
	/** Logger for tracking service activity */
	private final ServerLogger logger;
	
	/** Service for table management - set after construction to avoid circular dependency */
	private TableService tableService;
	
	/** List of all table capacities in the restaurant (e.g., [2,2,4,4,6,6,8]) */
	private List<Integer> tableSizes;
	
	/** Time interval between reservation slots in minutes (default: 30) */
	private int slotStepMinutes;
	
	/** How long each reservation lasts in minutes (default: 120) */
	private int reservationDurationMinutes;

	
	// ******************************** Constructors***********************************
	
	/**
	 * Creates a new OrdersService with default slot settings.
	 * Slot step is 30 minutes, reservation duration is 2 hours.
	 * 
	 * @param server the server instance
	 * @param dbController database controller for DB access
	 * @param logger server logger for logging events
	 */
	public OrdersService(BistroServer server,BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
		this.server = server;
		this.tableSizes = new ArrayList<Integer>();
		this.slotStepMinutes = 30;
		this.reservationDurationMinutes = 120;
	}
	// ******************************* Getters and Setters ***********************************
	
	public void setTableService(TableService tableService) {
		this.tableService = tableService;
	}
	
	public int getSlotStepMinutes() {
		return slotStepMinutes;
	}
	
	public int getReservationDurationMinutes() {
		return this.reservationDurationMinutes;
	}
	
	public List<Integer> getTableSizes(){
		return this.tableSizes;
	}
	
	// ********************************Instance Methods ***********************************
	
	/**
	 * Creates a new Reservation in a thread-safe manner.
	 * For reservations, it checks slot availability before insertion to avoid conflicts 
	 * while handling concurrent requests (race condition prevention).
	 * 
	 * @param data A list containing order details: [0]userId, [1]date, [2]dinersAmount, [3]time, [4]Code
	 * @param orderType The type of order (RESERVATION or WAITLIST)
	 * @return the created Order object, or null if slot was taken or creation failed
	 */
	public synchronized Order createNewOrder(List<Object> data, OrderType orderType) {
		// data: [0]userId, [1]date, [2]dinersAmount, [3]time, [4]Code
		System.out.println("Creating new order with data: " + data.toString() + " of type: " + orderType);
		int userId = (int) data.get(0);
		LocalDate date = (LocalDate) data.get(1);
		int diners = (int) data.get(2);
		LocalTime time = (LocalTime) data.get(3);
		//condition that checks to ensure reservation slot is still free before insertion and type is RESERVATION and not WAITLIST by mistake
		if (orderType == OrderType.RESERVATION) {
			boolean isSlotStillFree = checkSpecificSlotAvailability(date, time, diners);
			if (!isSlotStillFree) {
				System.out.println("Race Condition Avoided: Slot " + time + " was taken just before insertion.");
				return null; 
			}
		}
		String confirmationCode = generateConfirmationCode("R");
		data.add(confirmationCode); 
		boolean orderCreated = dbController.setNewOrder(data, orderType, OrderStatus.PENDING);
		if (orderCreated) {
			System.out.println("Order created successfully with confirmation code: " + confirmationCode);
			logger.log("[INFO] New order created: " + confirmationCode + " for userId: " + userId);
			return createOrderDto(userId, date, diners, time, confirmationCode, orderType, OrderStatus.PENDING);
		} else {
			System.out.println("Failed to create order in DB.");
			logger.log("[ERROR] Failed to create new order for userId: " + userId);
			return null;
		}
	}
	
	/**
	 * Helper method to build an Order DTO from individual fields.
	 */
	public Order createOrderDto(int userId, LocalDate date, int dinersAmount, LocalTime time, String confirmationCode, OrderType orderType, OrderStatus status) {
		return new Order(userId, date, dinersAmount, time, confirmationCode, orderType, status);
	}
	
	/**
	 * Checks if a specific reservation slot is still available.
	 * Used for last-second validation before inserting a reservation.
	 * 
	 * @param date the reservation date
	 * @param targetTime the specific time slot to check
	 * @param diners number of diners
	 * @return true if slot is still free, false if taken
	 */
	private boolean checkSpecificSlotAvailability(LocalDate date, LocalTime targetTime, int diners) {
		List<Order> existingReservations = dbController.getOrdersByDate(date);
		List<LocalTime> openingHours = dbController.getOpeningHoursFromDB();
		if (openingHours == null || openingHours.size() < 2) return false;
		LocalTime open = openingHours.get(0);
		LocalTime close = openingHours.get(1);
		List<String> availableSlots = computeAvailableSlots(open, close, diners, existingReservations);		
		String targetString = timeToString(targetTime);
		return availableSlots.contains(targetString);
	}
	
	/**
	 * Retrieves an order by its confirmation code.
	 * 
	 * @param confirmationCode the order's confirmation code
	 * @return the Order entity, or null if not found
	 */
	public Order getOrderByConfirmationCode(String confirmationCode) {
		return dbController.getOrderByConfirmationCodeInDB(confirmationCode);
	}
	
	/**
	 * Checks if an order exists in the database.
	 * 
	 * @param confirmationCode the confirmation code to check
	 * @return true if order exists
	 */
	public boolean checkOrderExists(String confirmationCode) {
		return dbController.checkOrderExistsInDB(confirmationCode);
	}
	
	/**
	 * Verifies that an order exists, belongs to the user, and is ready for check-in.
	 * Only orders with NOTIFIED status can be checked in.
	 * Logs security warnings if a user tries to check in with someone else's code.
	 * 
	 * @param confirmationCode the order's confirmation code
	 * @param userId the user attempting to check in
	 * @return true if the order is valid and belongs to this user
	 */
	public boolean checkOrderBelongsToUser(String confirmationCode, int userId) {
		Order order = dbController.getOrderByConfirmationCodeInDB(confirmationCode);
		// Confirmation code does not exist
		if (order == null) {
			return false; 
		}
		// Checking code belongs to the requesting user
		if (order.getUserId() != userId) {
			logger.log("[SECURITY] User " + userId + " tried to check-in with order " + confirmationCode + " belonging to User " + order.getUserId());
			return false; 
		}
		// Status Check: Can only check in if NOTIFIED (not pending, cancelled or completed)
		if (!(order.getStatus() == OrderStatus.NOTIFIED)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Gets all orders (history) for a specific user.
	 * 
	 * @param userId the user's ID
	 * @return list of past and current orders
	 */
	public List<Order> getClientHistory(int userId) {
		return dbController.getOrdersByUserId(userId);
	}
	
	/**
	 * Generates a unique confirmation code with a prefix (e.g., "R-123456" for reservations).
	 * Retries up to 3 times if a collision is detected in the database.
	 * 
	 * @param prefix the code prefix ("R" for reservation, "W" for waitlist)
	 * @return a unique confirmation code
	 * @throws RuntimeException if unable to generate unique code after 3 attempts
	 */
	public String generateConfirmationCode(String prefix) {
	    String code = null;
	    boolean exists = true;
	    int attempts = 0;
	    final int MAX_ATTEMPTS = 3;
	    Random random = new Random();

	    while (exists) {
	        // Increment attempt counter
	        attempts++;

	        // If we failed 3 times, throw an error to the server
	        if (attempts > MAX_ATTEMPTS) {
	            String errorMsg = "Critical Error: Failed to generate a unique confirmation code after " + MAX_ATTEMPTS + " attempts.";
	            logger.log("[ERROR] " + errorMsg);
	            throw new RuntimeException(errorMsg);
	        }
	        // Generate random number 100000-999999
	        int num = 100000 + random.nextInt(900000);
	        code = prefix + "-" + num;
	        // Check DB to avoid collision
	        exists = dbController.checkOrderExistsInDB(code);

	        if (exists) {
	            System.out.println("Duplicate code generated: " + code + ". Attempt " + attempts + " failed. Retrying...");
	        }
	    }
	    return code;
	}
	
	
	/**
	 * Gets the table number allocated to a reservation.
	 * 
	 * @param confirmationCode the reservation's confirmation code
	 * @return the table number, or -1 if not found
	 */
	public int getAllocatedTableForReservation(String confirmationCode) {
		return tableService.getTableNumberByReservationConfirmationCode(confirmationCode);
	}
	
	/**
	 * Updates the status of an order.
	 * 
	 * @param confirmationCode the order's confirmation code
	 * @param completed the new status
	 * @return true if update was successful
	 */
	public boolean updateOrderStatus(String confirmationCode, OrderStatus completed) {
		return dbController.updateOrderStatusInDB(confirmationCode, completed);
	}
	
	/**
	 * Checks if a reservation is a no-show (15 minutes past reservation time).
	 * Only marks as NO_SHOW if the order is still PENDING.
	 * 
	 * @param confirmationCode the reservation to check
	 * @return true if marked as NO_SHOW, false otherwise
	 */
	public boolean checkReservationNoShow(String confirmationCode) {
		if (dbController.getOrderStatusInDB(confirmationCode) == OrderStatus.PENDING) {
			boolean noShow = dbController.updateOrderStatusInDB(confirmationCode,OrderStatus.NO_SHOW);
			if (noShow) {
				logger.log("[WARN] RESERVATION NO_SHOW: " + confirmationCode);
				return true;
			}
		}
		return false;
	}
	
	
	
	
	// ******************************** Reservation Available Time Slots Calculation Methods ***********************************
	
	/**
	 * Returns all available reservation time slots for a given date and party size.
	 * Takes into account existing reservations and table availability.
	 * If the date is today, excludes past times and adds a 1-hour buffer.
	 * 
	 * @param requestData map with "date" (LocalDate) and "dinersAmount" (int)
	 * @return list of available times in "HH:mm" format
	 */
	public List<String> getAvailableReservationHours(Map<String, Object> requestData) {
		getTablesCapacity(); // Fetch table sizes from DB
		//Get opening hours and existing reservations from DB:
		List<LocalTime> openingHours = dbController.getOpeningHoursFromDB();
		LocalDate date = (LocalDate) requestData.get("date");
		LocalTime openingTime = openingHours.get(0);
		LocalTime closingTime = openingHours.get(1);
	    LocalTime effectiveOpeningTime = openingTime;
	    // If the requested date is today, adjust the effective opening time:
	    if (date.equals(LocalDate.now())) {
	        LocalTime now = LocalTime.now().plusHours(1); // Add 1 hour buffer
	        // If current time is after opening time, adjust effective opening time
	        if (now.isAfter(openingTime)) {
	            int minutes = now.getMinute();
	            int remainder = minutes % slotStepMinutes; 
	            int minutesToAdd = (remainder == 0) ? 0 : (slotStepMinutes - remainder);
	            effectiveOpeningTime = now.plusMinutes(minutesToAdd).withSecond(0).withNano(0);
	        }
	    }
		int dinersAmount = (int) requestData.get("dinersAmount");
		List<Order> reservationsByDate = dbController.getOrdersByDate(date);
		//Compute available slots:
		return computeAvailableSlots(effectiveOpeningTime, closingTime, dinersAmount, reservationsByDate);
	}
	
	/**
	 * Refreshes the table sizes list from the database.
	 */
	public void getTablesCapacity() {
		List<Table> tables = tableService.getAllTables();
		tableSizes.clear();
		for (Table table : tables) {
			tableSizes.add(table.getCapacity());
		}
		return;
	}
	
	
	/**
	 * Core algorithm that computes which time slots are available for a new reservation.
	 * For each possible slot, it checks if adding this reservation would exceed table capacity
	 * by simulating all overlapping reservations.
	 * 
	 * @param openingTime restaurant opening time
	 * @param closingTime restaurant closing time
	 * @param newDinersAmount party size for the new reservation
	 * @param reservationsByDate existing reservations on that date
	 * @return list of available time slots in "HH:mm" format
	 */
	public List<String> computeAvailableSlots(LocalTime openingTime, LocalTime closingTime, int newDinersAmount,
	        List<Order> reservationsByDate) {
	    if (this.tableSizes == null || this.tableSizes.isEmpty()) {
	        System.err.println("ERROR: tableSizes is EMPTY! No tables to seat diners.");
	        return new ArrayList<>();
	    }

	    List<LocalTime> possibleTimeSlots = buildPossibleTimeSlots(openingTime, closingTime);
	    Map<LocalTime, List<Integer>> tablesPerTime = new HashMap<>();
	    for (LocalTime slot : possibleTimeSlots){
	        tablesPerTime.put(slot, new ArrayList<>()); 
	    }

	    for (Order o : reservationsByDate) {
	        LocalTime orderStart = o.getOrderHour();
	        LocalTime orderEnd = orderStart.plusMinutes(reservationDurationMinutes);
	        for (LocalTime slot : possibleTimeSlots) {
	            LocalTime slotStartTime = slot;
	            LocalTime slotEndTime = slotStartTime.plusMinutes(reservationDurationMinutes);
	            if (overlaps(slotStartTime, slotEndTime, orderStart, orderEnd)) {
	                tablesPerTime.get(slot).add(o.getDinersAmount());
	            }
	        }
	    }

	    List<String> available = new ArrayList<>(); 
	    for (LocalTime slot : possibleTimeSlots) {
	        List<Integer> overlappingDinersAmounts = new ArrayList<>(tablesPerTime.get(slot));
	        overlappingDinersAmounts.add(newDinersAmount);
	        if (canAssignAllDinersToTables(overlappingDinersAmounts, tableSizes)) {
	            available.add(timeToString(slot));
	        } else {
	             System.out.println("Slot " + slot + " REJECTED (Not enough tables)");
	        }
	    }
	    return available;
	}
	
	

	/**
	 * Builds all possible time slots from opening to closing time.
	 * The last slot must allow for a full reservation duration before closing.
	 * 
	 * @param openingTime restaurant opening time
	 * @param closingTime restaurant closing time
	 * @return list of possible slot start times
	 */
	public List<LocalTime> buildPossibleTimeSlots(LocalTime openingTime, LocalTime closingTime) {
		// The last possible time slot starts at closingTime minus reservationDuration
		LocalTime lastTimeSlot = closingTime.minusMinutes(reservationDurationMinutes);
		// Build time slots from openingTime to lastTimeSlot
		List<LocalTime> slots = new ArrayList<>();
		//Explanation: for each time t from openingTime to lastTimeSlot, step by slotStepMinutes
		for (LocalTime t = openingTime; !t.isAfter(lastTimeSlot); t = t.plusMinutes(slotStepMinutes)) {
			slots.add(t);
		}
		return slots;
	}
	
	/**
	 * Checks if two time intervals overlap.
	 * Used to detect reservation conflicts.
	 * 
	 * @param slotStartTime start of first interval
	 * @param slotEndTime end of first interval
	 * @param orderStart start of second interval
	 * @param orderEnd end of second interval
	 * @return true if the intervals overlap
	 */
    public boolean overlaps(LocalTime slotStartTime, LocalTime slotEndTime, LocalTime orderStart, LocalTime orderEnd) {
        return slotStartTime.isBefore(orderEnd) && orderStart.isBefore(slotEndTime);
    }

    /**
	 * Greedy algorithm to check if all party sizes can be assigned to available tables.
	 * Sorts parties by size (largest first) and assigns each to the smallest fitting table.
	 * 
	 * @param overlappingDinersAmounts list of party sizes that need tables
	 * @param tableSizes list of available table capacities
	 * @return true if all parties can be seated, false if not enough tables
	 */
	public boolean canAssignAllDinersToTables(List<Integer> overlappingDinersAmounts, List<Integer> tableSizes) {
		// Sort diners amounts in descending order:
		List<Integer> overlappingDinersAmountsCopy = new ArrayList<>(overlappingDinersAmounts);
		overlappingDinersAmountsCopy.sort(Comparator.reverseOrder()); //TODO: refactor to alternative way without using Comparator
		// Build a TreeMap of table sizes to their counts:
		TreeMap<Integer, Integer> tableSizeCounts = new TreeMap<>();
		//loop over table sizes and count occurrences:
		for (int t : tableSizes) {
			if (tableSizeCounts.containsKey(t)) {
				tableSizeCounts.put(t, tableSizeCounts.get(t) + 1);
			} else {
				tableSizeCounts.put(t, 1);
			}
		}
		// Try to assign each diners amount to a suitable table:
		for (int i : overlappingDinersAmountsCopy) {
			Integer chosen = tableSizeCounts.ceilingKey(i); // smallest table >= p
			if (chosen == null) {
				return false;
			}
			int count = tableSizeCounts.get(chosen);
			if (count == 1) {
				tableSizeCounts.remove(chosen);
			} else {
				tableSizeCounts.put(chosen, count - 1);
			}
		}
		return true;
	}
	
	/**
	 * Formats a LocalTime as "HH:mm" string.
	 * 
	 * @param time the time to format
	 * @return formatted time string
	 */
	public String timeToString(LocalTime time) {
        // "HH:mm"
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

	/**
	 * Gets all reservations for a specific date (client view).
	 */
	public List<Order> getClientReservations(LocalDate date) {
        return dbController.getOrdersByDate(date);
    }
	
	/**
	 * Gets all reservations for a specific date with full details (staff view).
	 */
	public List<Order> getStaffReservations(LocalDate date) {
        return dbController.getFullOrdersByDate(date);
    }
	
	// ******************************** New Method for Date Availability ***********************************
	
	/**
	 * Returns all dates in the next 30 days that have at least one available slot
	 * for the given party size.
	 * 
	 * @param diners number of people in the party
	 * @return list of available dates
	 */
	public List<LocalDate> getAvailableDates(int diners) {
		getTablesCapacity(); // Refresh table sizes
		List<LocalTime> openingHours = dbController.getOpeningHoursFromDB();
		if (openingHours == null || openingHours.size() < 2) {
			return new ArrayList<>();
		}
		LocalTime open = openingHours.get(0);
		LocalTime close = openingHours.get(1);

		List<LocalDate> resultDates = new ArrayList<>();
		LocalDate startDate = LocalDate.now().plusDays(0); // Start checking from today
		LocalDate endDate = startDate.plusDays(30); // Check for the next 30 days

		// Iterate through each day
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			// Get existing reservations for this specific date
			List<Order> reservationsOnDate = dbController.getOrdersByDate(date);
			
			// Calculate available slots for this date
			List<String> slots = computeAvailableSlots(open, close, diners, reservationsOnDate);
			
			// If there is at least one slot, add the date to the result
			if (!slots.isEmpty()) {
				resultDates.add(date);
			}
		}
		return resultDates;
	}

	/**
     * Cancels a reservation if it hasn't started yet.
     * Only PENDING or NOTIFIED orders can be cancelled.
     * Orders that are SEATED or COMPLETED cannot be cancelled.
     * 
     * @param confirmationCode the reservation to cancel
     * @return true if cancellation was successful
     */
    public boolean cancelReservation(String confirmationCode) {
        OrderStatus currentStatus = dbController.getOrderStatusInDB(confirmationCode);
        
        if (currentStatus == null) {
            return false; 
        }
        
        if (currentStatus == OrderStatus.SEATED || currentStatus == OrderStatus.COMPLETED) {
            logger.log("[WARN] Attempted to cancel an active/finished order: " + confirmationCode);
            return false;
        }

        return dbController.updateOrderStatusInDB(confirmationCode, OrderStatus.CANCELLED);
    }

    /**
     * Creates a reservation on behalf of a customer (staff action).
     * Handles both member and guest customers.
     * For guests, creates a new user record if needed.
     * 
     * @param data map with date, time, diners, customerType, identifier, and customerName
     * @return the created Order, or null if creation failed
     */
    public Order createReservationAsStaff(Map<String, Object> data) {
        if (data == null) return null;
        LocalDate date = (LocalDate) data.get("date");
        LocalTime time = (LocalTime) data.get("time");
        Integer dinersObj = (Integer) data.get("diners");
        String customerType = (String) data.get("customerType"); 
        String identifier = (String) data.get("identifier");
        String customerName = (String) data.get("customerName");

        if (date == null || time == null || dinersObj == null || dinersObj <= 0) {
            logger.log("[WARN] createReservationAsStaff: invalid date/time/diners");
            return null;
        }
        if (customerType == null || identifier == null || identifier.isBlank()) {
            logger.log("[WARN] createReservationAsStaff: missing customerType/identifier");
            return null;
        }
        String typeNorm = customerType.trim().toUpperCase();
        User user = null;
        if ("MEMBER".equals(typeNorm)) {
        	int memberCode= Integer.parseInt(identifier);
        	user = dbController.findMemberUserByCode(memberCode); 
        } else {
        	user = dbController.findOrCreateGuestUser(identifier,null);
        }
        if (user == null) {
        	logger.log("[WARN] createReservationAsStaff: user not found and could not be created. type=" + typeNorm + " identifier=" + identifier);
        	return null;            
        }
        List<Object> orderData = new ArrayList<>();
        orderData.add(user.getUserId());
        orderData.add(date);
        orderData.add(dinersObj);
        orderData.add(time);
        return createNewOrder(orderData, OrderType.RESERVATION);
    }

	/**
	 * Gets all reservation confirmation codes for a user.
	 * 
	 * @param userId the user's ID
	 * @return list of confirmation codes
	 */
	public List<String> getReservationCodesByUserId(int userId) {
		List<Order> orders = dbController.getOrdersByUserId(userId);
		if (orders != null) {
			List<String> codes = new ArrayList<>();
			for (Order o : orders) {
				codes.add(o.getConfirmationCode());
			}
			return codes;
		}
		return null;
	}

	/**
	 * Gets the user's earliest upcoming reservation code.
	 * Useful for quick check-in scenarios.
	 * 
	 * @param userId the user's ID
	 * @return the earliest reservation code, or null if none found
	 */
	public String getEarlierReservationCodeByUserId(int userId) {
		List<Order> orders = dbController.getOrdersByUserId(userId);
		if (orders != null) {
			LocalDateTime now = LocalDateTime.now();
			Order earliest = null;
			for (Order o : orders) {
				LocalDateTime orderDateTime = LocalDateTime.of(o.getOrderDate(), o.getOrderHour());
				if (orderDateTime.isAfter(now)) {
					if (earliest == null || orderDateTime
							.isBefore(LocalDateTime.of(earliest.getOrderDate(), earliest.getOrderHour()))) {
						earliest = o;
					}
				}
			}
			if (earliest != null) {
				return earliest.getConfirmationCode();
			}
		}
		return null;
	}

	

}
