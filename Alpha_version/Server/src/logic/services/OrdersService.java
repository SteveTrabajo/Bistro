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
	
	/*
	 * Fetches all table sizes from the database and stores them in the tableSizes list.
	 */
	public void getTablesCapacity() {
		List<Table> tables = dbController.getAllTablesFromDB();
		tableSizes.clear();
		for (Table table : tables) {
			tableSizes.add(table.getCapacity());
		}
		return;
	}
	


	/**
	 * Creates a new standard Reservation (future order).
	 * Generates a code starting with "R-".
	 */
	public synchronized boolean createNewOrder(List<Object> data, OrderType orderType) {
		// data: [0]userId, [1]date, [2]dinersAmount, [3]time, [4]Code (added later)
		
		LocalDate date = (LocalDate) data.get(1);
		int diners = (int) data.get(2);
		LocalTime time = (LocalTime) data.get(3);

		if (orderType == OrderType.RESERVATION) {
			boolean isSlotStillFree = checkSpecificSlotAvailability(date, time, diners);
			if (!isSlotStillFree) {
				System.out.println("Race Condition Avoided: Slot " + time + " was taken just before insertion.");
				return false; 
			}
		}

		// 2. Generate Code
		String confirmationCode = generateConfirmationCode("R");
		data.add(confirmationCode); 
		
		// 3. Insert
		return dbController.setNewOrder(data, orderType, OrderStatus.PENDING);
	}
	
	/*
	 * Checks if a specific reservation slot is still available for the given date, time, and diners amount.
	 * @param date The date of the reservation.
	 * @param targetTime The specific time slot to check.
	 * @param diners The number of diners for the reservation.
	 * @return true if the slot is available, false otherwise.
	 */
	private boolean checkSpecificSlotAvailability(LocalDate date, LocalTime targetTime, int diners) {
		List<Order> existingReservations = dbController.getReservationsbyDate(date);
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
		while (exists){
			// Generate random number 100000-999999
			int num = 100000 + new Random().nextInt(900000);
			code = prefix + "-" + num;
			
			// Check DB to avoid collision
			exists = dbController.checkOrderExistsInDB(code);
			if (exists) {
				System.out.println("Duplicate code generated: " + code + ". Retrying...");
			}
		}
		
		return code;
	}
	
	/**
	 * 
	 * Returns a list of available reservation hours for a given date and diners amount.
	 * 
	 * @param requestData A map containing "date" (LocalDate) and "dinersAmount" (int).
	 * @return A list of available reservation hours in "HH:mm" format.
	 */
	public List<String> getAvailableReservationHours(Map<String, Object> requestData) {
		getTablesCapacity(); // Fetch table sizes from DB
		List<LocalTime> openingHours = dbController.getOpeningHoursFromDB();
		printOpeningHours(openingHours); //TODO: Remove debug prints later
		LocalTime openingTime = openingHours.get(0);
		LocalTime closingTime = openingHours.get(1);
		LocalDate date = (LocalDate) requestData.get("date");
		int dinersAmount = (int) requestData.get("dinersAmount");
		List<Order> reservationsByDate = dbController.getReservationsbyDate(date);
		printReservationsByDate(reservationsByDate); //TODO: Remove debug prints later
		return computeAvailableSlots(openingTime, closingTime, dinersAmount, reservationsByDate);
	}
	
	
	//TODO: Remove debug prints later ---------------------------------------------
	private void printReservationsByDate(List<Order> reservationsByDate) {
		System.out.println("Reservations fetched from DB for the given date: ");
		for (Order o : reservationsByDate) {
			System.out.println( "Time: " + o.getOrderHour().toString() + ", Diners: " + o.getDinersAmount());
		}
		
	}

	private void printOpeningHours(List<LocalTime> openingHours) {
		System.out.println("Opening Hours fetched from DB: ");
		System.out.println("Opening Time: " + openingHours.get(0).toString());
		System.out.println("Closing Time: " + openingHours.get(1).toString());
		
	}
	//-----------------------------------------------------------------------

	public int getAllocatedTableForReservation(String confirmationCode) {
		
		return server.getTablesService().getTableNumberByReservationConfirmationCode(confirmationCode);
	}
	
	public boolean updateOrderStatus(String confirmationCode, OrderStatus completed) {
		return dbController.updateOrderStatusInDB(confirmationCode, completed);
	}
	
	// ****************************** Instance Private Methods ******************************
	
	
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
	    
	    //TODO: Remove debug prints later
	    System.out.println("--- DEBUG: Starting computeAvailableSlots ---");
	    System.out.println("Table Sizes Loaded: " + this.tableSizes); 
	    System.out.println("New Diners Amount: " + newDinersAmount);
	    System.out.println("Opening: " + openingTime + ", Closing: " + closingTime);
	    // ----------------------------------------------
	    
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
	        
	        //TODO: Remove debug prints later
	        System.out.println("Checking " + slot + " with groups: " + overlappingDinersAmounts);
	        // ----------------------------------------------
	        
	        
	        if (canAssignAllDinersToTables(overlappingDinersAmounts, tableSizes)) {
	            available.add(timeToString(slot));
	        } else {
	             // System.out.println("Slot " + slot + " REJECTED (Not enough tables)");
	        }
	    }
	    
	    printListbeforeReturn(available);
	    return available;
	}
	
	public void printListbeforeReturn(List<String> available) {
		System.out.println("Available slots computed: ");
		for (String s : available) {
			System.out.println(s);
		}
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
		overlappingDinersAmountsCopy.sort(Comparator.reverseOrder());
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






}
