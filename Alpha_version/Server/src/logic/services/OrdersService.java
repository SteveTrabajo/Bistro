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
 * Service class for managing orders and reservations.
 */
public class OrdersService {
	
	// ******************************** Instance variables ***********************************
	private final BistroServer server;
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	private TableService tableService;
	
	//Variables for reservation slots calculation:
	private List<Integer> tableSizes; // [2,2,4,4,6,6,8]
	private int slotStepMinutes; // 30
	private int reservationDurationMinutes;// 120 

	
	// ******************************** Constructors***********************************
	
	/**
	 * Constructor for OrdersService.
	 * 
	 * @param server The BistroServer instance.
	 * @param dbController The database controller for data access.
	 * @param logger The server logger for logging events.
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
	
	/**
	 * Sets the TableService instance.
	 * 
	 * @param tableService The TableService instance to set.
	 */
	public void setTableService(TableService tableService) {
		this.tableService = tableService;
	}
	
	/**
	 * Gets the slot step minutes.
	 * 
	 * @return The slot step minutes.
	 */
	public int getSlotStepMinutes() {
		return slotStepMinutes;
	}
	
	/**
	 * Gets the reservation duration in minutes.
	 * 
	 * @return The reservation duration in minutes.
	 */
	public int getReservationDurationMinutes() {
		return this.reservationDurationMinutes;
	}
	
	/**
	 * Gets the list of table sizes.
	 * 
	 * @return The list of table sizes.
	 */
	public List<Integer> getTableSizes(){
		return this.tableSizes;
	}
	
	/**
	 * Retrieves the list of seated reservations for a member for today.
	 * 
	 * @param userId The user ID of the member.
	 * @return A list of seated reservations for the member.
	 */
	public List<Order> getMemberSeatedReservations(int userId) {
	    return dbController.getMemberSeatedReservationsForToday(userId);
	}
	
	// ********************************Instance Methods ***********************************
	
	/**
	 * Creates a new order (reservation or waitlist) in a thread-safe manner.
	 * 
	 * @param data A list containing order details: [0]userId, [1]date, [2]dinersAmount, [3]time
	 * @param orderType The type of the order (RESERVATION or WAITLIST).
	 * @return The created Order object, or null if creation failed.
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
		// Generate unique confirmation code
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
	 * Creates an Order DTO object.
	 * 
	 * @param userId The user ID associated with the order.
	 * @param date The date of the order.
	 * @param dinersAmount The number of diners for the order.
	 * @param time The time of the order.
	 * @param confirmationCode The confirmation code for the order.
	 * @param orderType The type of the order (RESERVATION or WAITLIST).
	 * @param status The status of the order.
	 * @return The created Order object.
	 */
	public Order createOrderDto(int userId, LocalDate date, int dinersAmount, LocalTime time, String confirmationCode, OrderType orderType, OrderStatus status) {
		return new Order(userId, date, dinersAmount, time, confirmationCode, orderType, status);
	}
	
	/**
	 * Checks if a specific reservation slot is still available for the given date, time, and diners amount.
	 * @param date The date of the reservation.
	 * @param targetTime The specific time slot to check.
	 * @param diners The number of diners for the reservation.
	 * @return true if the slot is available, false otherwise.
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
	 * @param confirmationCode The confirmation code of the order.
	 * @return The Order object if found, null otherwise.
	 */
	public Order getOrderByConfirmationCode(String confirmationCode) {
		return dbController.getOrderByConfirmationCodeInDB(confirmationCode);
	}
	
	/**
	 * Checks if an order exists in the database by its confirmation code.
	 * 
	 * @param confirmationCode The confirmation code of the order.
	 * @return true if the order exists, false otherwise.
	 */
	public boolean checkOrderExists(String confirmationCode) {
		return dbController.checkOrderExistsInDB(confirmationCode);
	}
	
	/**
	 * Checks if an order belongs to a specific user and is eligible for check-in.
	 * 
	 * @param confirmationCode The confirmation code of the order.
	 * @param userId The user ID to check against.
	 * @return true if the order belongs to the user and is NOTIFIED, false otherwise.
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
	 * Gets order history for a client using their user ID.
	 * 
	 * @param userId The user's ID
	 * @return List of orders for the client
	 */
	public List<Order> getClientHistory(int userId) {
		return dbController.getOrdersByUserId(userId);
	}
	
	/**
	 * Gets order history for a member using their member code.
	 * First looks up the user by member code, then retrieves their order history.
	 * 
	 * @param memberCode The member's code
	 * @return List of orders for the member, or null if member not found
	 */
	public List<Order> getMemberHistoryByCode(int memberCode) {
		logger.log("[DEBUG] getMemberHistoryByCode called with memberCode: " + memberCode);
		User member = dbController.findMemberUserByCode(memberCode);
		if (member == null) {
			logger.log("[DEBUG] No member found with code: " + memberCode);
			return null;
		}
		// Member found, retrieve order history
		logger.log("[DEBUG] Found member: userId=" + member.getUserId() + ", name=" + member.getFirstName() + " " + member.getLastName() + ", memberCode=" + member.getMemberCode());
		List<Order> orders = dbController.getOrdersByUserId(member.getUserId());
		logger.log("[DEBUG] getOrdersByUserId(" + member.getUserId() + ") returned " + (orders == null ? "null" : orders.size() + " orders"));
		if (orders != null && !orders.isEmpty()) {
			for (Order o : orders) {
				logger.log("[DEBUG]   Order #" + o.getOrderNumber() + " - date: " + o.getOrderDate() + ", status: " + o.getStatus());
			}
		}
		return orders;
	}
	
	/**
	 * Generates a unique 6-digit code with a prefix (e.g., "R-123456").
	 * Verifies against the DB to ensure no duplicates exist.
	 * @param prefix The prefix for the code (e.g., "R" for reservations).
	 * @return A unique confirmation code.
	 * @throws RuntimeException if unable to generate a unique code after 3 attempts.
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
	 * Retrieves the allocated table number for a reservation based on its confirmation code.
	 * 
	 * @param confirmationCode The confirmation code of the reservation.
	 * @return The allocated table number, or -1 if not found.
	 */
	public int getAllocatedTableForReservation(String confirmationCode) {
		return tableService.getTableNumberByReservationConfirmationCode(confirmationCode);
	}
	
	/**
	 * Updates the status of an order in the database.
	 * 
	 * @param confirmationCode The confirmation code of the order.
	 * @param completed The new status to set for the order.
	 * @return true if the update was successful, false otherwise.
	 */
	public boolean updateOrderStatus(String confirmationCode, OrderStatus completed) {
		return dbController.updateOrderStatusInDB(confirmationCode, completed);
	}
	
	/**
	 * Scheduler method for 15 min no-show check for reservations.
	 * @param confirmationCode The confirmation code of the reservation to check.
	 * @return true if marked as NO_SHOW, false otherwise.
	 */
	public boolean checkReservationNoShow(String confirmationCode) {
		if (dbController.getOrderStatusInDB(confirmationCode) == OrderStatus.PENDING) {
			boolean noShow = dbController.updateOrderStatusInDB(confirmationCode,OrderStatus.NO_SHOW);
			// Log the no-show event
			if (noShow) {
				logger.log("[WARN] RESERVATION NO_SHOW: " + confirmationCode);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Recovers the seated code for a guest based on their email and phone number.
	 * 
	 * @param email The email of the guest.
	 * @param phone The phone number of the guest.
	 * @return The seated code if found, null otherwise.
	 */
	public String recoverGuestSeatedCode(String email, String phone) {
	    return dbController.recoverGuestSeatedCode(email, phone);
	}
	
	// ******************************** Reservation Available Time Slots Calculation Methods ***********************************
	
	/**
	 * Returns a list of available reservation hours for a given date and diners amount.
	 * 
	 * @param requestData A map containing "date" (LocalDate) and "dinersAmount" (int).
	 * @return A list of available reservation hours in "HH:mm" format.
	 */
	public List<String> getAvailableReservationHours(Map<String, Object> requestData) {
	    getTablesCapacity();

	    // Extract date and diners amount from requestData
	    LocalDate date = (LocalDate) requestData.get("date");
	    int dinersAmount = (int) requestData.get("dinersAmount");

	    // Fetch opening hours for the specified date
	    List<LocalTime> openingHours = dbController.getOpeningHoursFromDB(date);

	    // If closed (holiday closed) or missing hours - no slots
	    if (openingHours == null || openingHours.size() < 2) {
	        return new ArrayList<>();
	    }
	    // Extract opening and closing times
	    LocalTime openingTime = openingHours.get(0);
	    LocalTime closingTime = openingHours.get(1);
	    // Adjust opening time if the date is today
	    LocalTime effectiveOpeningTime = openingTime;
	    if (date.equals(LocalDate.now())) {
	        LocalTime now = LocalTime.now().plusHours(1);
	        if (now.isAfter(openingTime)) {
	            int minutes = now.getMinute();
	            int remainder = minutes % slotStepMinutes;
	            int minutesToAdd = (remainder == 0) ? 0 : (slotStepMinutes - remainder);
	            effectiveOpeningTime = now.plusMinutes(minutesToAdd).withSecond(0).withNano(0);
	        }
	    }

	    // Fetch existing reservations for the date
	    List<Order> reservationsByDate = dbController.getOrdersByDate(date);
	    return computeAvailableSlots(effectiveOpeningTime, closingTime, dinersAmount, reservationsByDate);
	}

	
	/**
	 * Retrieves the capacities of all tables and stores them in the tableSizes list.
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
	 * Computes available reservation slots within opening hours that can accommodate
	 * the new diners amount, considering existing reservations.
	 * 
	 * @param openingTime The restaurant's opening time.
	 * @param closingTime The restaurant's closing time.
	 * @param newDinersAmount The number of diners for the new reservation.
	 * @param reservationsByDate A list of existing reservations for the specified date.
	 * @return A list of available reservation slots in "HH:mm" format.
	 */
	public List<String> computeAvailableSlots(LocalTime openingTime, LocalTime closingTime, int newDinersAmount,
	        List<Order> reservationsByDate) {
		// Sanity check for table sizes
	    if (this.tableSizes == null || this.tableSizes.isEmpty()) {
	        System.err.println("ERROR: tableSizes is EMPTY! No tables to seat diners.");
	        return new ArrayList<>();
	    }
	    // Build possible time slots within opening hours
	    List<LocalTime> possibleTimeSlots = buildPossibleTimeSlots(openingTime, closingTime);
	    Map<LocalTime, List<Integer>> tablesPerTime = new HashMap<>();
	    for (LocalTime slot : possibleTimeSlots){
	        tablesPerTime.put(slot, new ArrayList<>()); 
	    }
	    // Map existing reservations to overlapping time slots
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
	    // Determine available slots for the new diners amount
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
	 * Builds a list of possible reservation time slots between opening and closing times.
	 * 
	 * @param openingTime The restaurant's opening time.
	 * @param closingTime The restaurant's closing time.
	 * @return A list of possible reservation time slots as LocalTime objects.
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
	 * 
	 * @param slotStartTime Start time of the time slot.
	 * @param slotEndTime End time of the time slot.
	 * @param orderStart Start time of the order.
	 * @param orderEnd End time of the order.
	 * @return true if the intervals overlap, false otherwise.
	 */
    public boolean overlaps(LocalTime slotStartTime, LocalTime slotEndTime, LocalTime orderStart, LocalTime orderEnd) {
        return slotStartTime.isBefore(orderEnd) && orderStart.isBefore(slotEndTime);
    }

    /**
	 * Checks if it is possible to assign all diners amounts to available tables.
	 * 
	 * @param overlappingDinersAmounts A list of diners amounts that need to be seated.
	 * @param tableSizes A list of available table sizes.
	 * @return true if all diners amounts can be assigned to tables, false otherwise.
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
			// Update table size counts:
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
	 * Converts a LocalTime object to a string in "HH:mm" format.
	 * 
	 * @param time The LocalTime object to convert.
	 * @return The formatted time string.
	 */
	public String timeToString(LocalTime time) {
        // "HH:mm"
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

	/**
	 * Gets client reservations for a specific date.
	 * @param date The date to retrieve reservations for.
	 * @return A list of client reservations for the specified date.
	 */
	public List<Order> getClientReservations(LocalDate date) {
        return dbController.getOrdersByDate(date);
    }
	
	/**
	 * Gets staff reservations for a specific date.
	 * @param date The date to retrieve reservations for.
	 * @return A list of staff reservations for the specified date.
	 */
	public List<Order> getStaffReservations(LocalDate date) {
        return dbController.getFullOrdersByDate(date);
    }
	
	// ******************************** New Method for Date Availability ***********************************
	
	/**
	 * Returns a list of dates (starting from tomorrow up to 30 days ahead)
	 * where there is at least one available time slot for the given number of diners.
	 * @param diners The number of diners.
	 * @return List of available LocalDate objects.
	 */
	public List<LocalDate> getAvailableDates(int diners) {
		getTablesCapacity(); // Refresh table sizes
		List<LocalTime> openingHours = dbController.getOpeningHoursFromDB();
		if (openingHours == null || openingHours.size() < 2) {
			return new ArrayList<>();
		}
		// Extract opening and closing times
		LocalTime open = openingHours.get(0);
		LocalTime close = openingHours.get(1);
		// Prepare result list
		List<LocalDate> resultDates = new ArrayList<>();
		LocalDate startDate = LocalDate.now().plusDays(0); // Start checking from today
		LocalDate endDate = startDate.plusDays(30); // Check for the next 30 days

		// Iterate through each day
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

		    List<LocalTime> hours = dbController.getOpeningHoursFromDB(date);

		    // Skip if closed or missing hours
		    if (hours == null || hours.size() < 2) {
		        continue;
		    }
		    // Fetch existing reservations for the date
		    List<Order> reservationsOnDate = dbController.getOrdersByDate(date);
		    List<String> slots = computeAvailableSlots(open, close, diners, reservationsOnDate);

		    if (!slots.isEmpty()) {
		        resultDates.add(date);
		    }
		}

		return resultDates;
	}

	/**
	 * Cancels a reservation if it is not already seated or completed.
	 * @param confirmationCode The confirmation code of the reservation to cancel.
	 * @return true if the reservation was successfully cancelled, false otherwise.
	 */
    public boolean cancelReservation(String confirmationCode) {
        OrderStatus currentStatus = dbController.getOrderStatusInDB(confirmationCode);
        // Confirmation code does not exist
        if (currentStatus == null) {
            return false; 
        }
        // Cannot cancel if already seated or completed
        if (currentStatus == OrderStatus.SEATED || currentStatus == OrderStatus.COMPLETED) {
            logger.log("[WARN] Attempted to cancel an active/finished order: " + confirmationCode);
            return false;
        }

        return dbController.updateOrderStatusInDB(confirmationCode, OrderStatus.CANCELLED);
    }

    /**
	 * Creates a reservation on behalf of staff for a customer (member or guest).
	 * @param data A map containing reservation details: "date" (LocalDate), "time" (LocalTime),
	 *             "diners" (Integer), "customerType" (String: "MEMBER" or "GUEST"),
	 *             "identifier" (String: member code for MEMBER, email/phone for GUEST),
	 *             "customerName" (String: optional, for GUEST).
	 * @return The created Order object, or null if creation failed.
	 */
    public Order createReservationAsStaff(Map<String, Object> data) {
        if (data == null) return null;
        LocalDate date = (LocalDate) data.get("date");
        LocalTime time = (LocalTime) data.get("time");
        Integer dinersObj = (Integer) data.get("diners");
        String customerType = (String) data.get("customerType"); 
        String identifier = (String) data.get("identifier");
        String customerName = (String) data.get("customerName");
        // Basic validation
        if (date == null || time == null || dinersObj == null || dinersObj <= 0) {
            logger.log("[WARN] createReservationAsStaff: invalid date/time/diners");
            return null;
        }
        if (customerType == null || identifier == null || identifier.isBlank()) {
            logger.log("[WARN] createReservationAsStaff: missing customerType/identifier");
            return null;
        }
        // Find or create user based on customer type
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
        // Prepare order data and create reservation
        List<Object> orderData = new ArrayList<>();
        orderData.add(user.getUserId());
        orderData.add(date);
        orderData.add(dinersObj);
        orderData.add(time);
        return createNewOrder(orderData, OrderType.RESERVATION);
    }

    /**
	 * Retrieves active reservations for a member for today.
	 * 
	 * @param userId The user ID of the member.
	 * @return A list of active reservations for the member.
	 */
    public List<Order> getMemberActiveReservations(int userId) {
        return dbController.getMemberActiveReservationsForToday(userId);
    }
    
    /**
     * Retrieves all reservation confirmation codes for a user by their user ID. 
     * @param userId The user ID of the user.
     * @return A list of reservation confirmation codes, or null if none found.
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
	 * Retrieves the earliest upcoming reservation confirmation code for a user by their user ID.
	 * @param userId The user ID of the user.
	 * @return The earliest upcoming reservation confirmation code, or null if none found.
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
			// 
			if (earliest != null) {
				return earliest.getConfirmationCode();
			}
		}
		return null;
	}
}
// End of OrdersService.java
