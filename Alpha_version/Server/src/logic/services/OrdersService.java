package logic.services;

import java.time.LocalDate;
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
import enums.OrderStatus;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.BistroServer;
import logic.ServerLogger;

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
	 * For reservations, it checks slot availability before insertion to avoid conflicts while handling concurrent requests.
	 * @param data A list containing order details: [0]userId, [1]date, [2]dinersAmount, [3]time, [4]Code
	 * @param orderType The type of order (RESERVATION or WAITLIST).
	 * @return true if the order was created successfully, false otherwise.
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
	
	public Order getOrderByConfirmationCode(String confirmationCode) {
		return dbController.getOrderByConfirmationCodeInDB(confirmationCode);
	}

	public boolean checkOrderExists(String confirmationCode) {
		return dbController.checkOrderExistsInDB(confirmationCode);
	}
	
	/**
	 * Generates a unique 6-digit code with a prefix (e.g., "R-123456").
	 * Verifies against the DB to ensure no duplicates exist.
	 * @param prefix The prefix for the code (e.g., "R" for reservations).
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
	
	
	//TODO : move to TableService?
	/**
	 * 
	 * Retrieves the allocated table number for a reservation based on its confirmation code.
	 * 
	 * @param confirmationCode The confirmation code of the reservation.
	 * @return The allocated table number, or -1 if not found.
	 */
	public int getAllocatedTableForReservation(String confirmationCode) {
		return tableService.getTableNumberByReservationConfirmationCode(confirmationCode);
	}
	
	/**
	 * 
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
			if (noShow) {
				logger.log("[WARN] RESERVATION NO_SHOW: " + confirmationCode);
				return true;
			}
		}
		return false;
	}
	
	
	
	
	// ******************************** Reservation Available Time Slots Calculation Methods ***********************************
	
	/**
	 * 
	 * Returns a list of available reservation hours for a given date and diners amount.
	 * 
	 * @param requestData A map containing "date" (LocalDate) and "dinersAmount" (int).
	 * @return A list of available reservation hours in "HH:mm" format.
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
	
	/*
	 * Fetches all table sizes from the database and stores them in the tableSizes list.
	 */
	public void getTablesCapacity() {
		List<Table> tables = tableService.getAllTables();
		tableSizes.clear();
		for (Table table : tables) {
			tableSizes.add(table.getCapacity());
		}
		return;
	}
	
	
	//TODO: add more comments to the methods below ---------------------------------------------
	/**
	 * 
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
	 * 
	 * Builds all possible time slots within opening hours that can accommodate
	 * a full planning window.
	 * 
	 * @param openingTime The restaurant's opening time.
	 * @param closingTime The restaurant's closing time.
	 * @return A list of possible time slots.
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
	 * 
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
	 * 
	 * Converts a LocalTime object to a string in "HH:mm" format.
	 * 
	 * @param time
	 * @return
	 */
	public String timeToString(LocalTime time) {
        // "HH:mm"
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

	public List<Order> getClientReservations(LocalDate date) {
        return dbController.getOrdersByDate(date);
    }
	
	public List<Order> getStaffReservations(LocalDate date) {
        return dbController.getFullOrdersByDate(date);
    }
	
	// ******************************** New Method for Date Availability ***********************************
	
		/**
		 * Returns a list of dates (starting from tomorrow up to 30 days ahead)
		 * where there is at least one available time slot for the given number of diners.
		 * * @param diners The number of diners.
		 * @return List of available LocalDate objects.
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
			LocalDate startDate = LocalDate.now().plusDays(1); // Start checking from tomorrow
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


}
