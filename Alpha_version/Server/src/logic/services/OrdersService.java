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
	private final List<Integer> tableSizes; // [2,2,4,4,6,6,8]
	private final int slotStepMinutes; // 30
	private final int reservationDurationMinutes;// 120 

	
	// ******************************** Constructors***********************************
	public OrdersService(BistroServer server,BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
		this.server = server;
		this.tableSizes = new ArrayList<Integer>();
		this.slotStepMinutes = 30;
		this.reservationDurationMinutes = 120;
		//getTableSizes();
	}
	// ********************************Instance Methods ***********************************
	
	/*
	 * Fetches all table sizes from the database and stores them in the tableSizes list.
	 */
	/* public void getTableSizes() {
		List<Table> tables = dbController.getAllTablesFromDB();
		for (Table table : tables) {
			tableSizes.add(table.getCapacity());
		}
		return;
	}
	*/


	public boolean createNewOrder(List<Object> data, OrderType orderType) {
		List<Object> orderData = data; //Received data contain : userId, date ,dinersAmount , time
		String confimationCode = generateConfirmationCode();
		int orderNumber = generateOrderNumber();
		orderData.add(orderNumber);
		orderData.add(confimationCode);
		orderData.add(orderType);
		orderData.add("PENDING");
		//orderData order: userId, date ,dinersAmount , time, orderNumber, confirmationCode, orderType, status
		return dbController.setNewOrderToDataBase(orderData); //DB should provide date of placing order
		
	}
	
	
	private int generateOrderNumber() {
		int num = 100000 + new Random().nextInt(900000);
		return num;
	}


	private String generateConfirmationCode() {
		int num = 100000 + new Random().nextInt(900000);
	    return "R-" + num;
	}
	
	
	public Order getOrderByConfirmationCode(String confirmationCode) {
		Order order  = dbController.getOrderByConfirmationCodeInDB(confirmationCode);
		return order;
	}

	public boolean checkOrderExists(String confirmationCode) {
		boolean exists = dbController.checkOrderExistsInDB(confirmationCode);
		return exists;
	}
	
	
	/**
	 * 
	 * Returns a list of available reservation hours for a given date and diners amount.
	 * 
	 * @param requestData A map containing "date" (LocalDate) and "dinersAmount" (int).
	 * @return A list of available reservation hours in "HH:mm" format.
	 */
	public List<String> getAvailableReservationHours(Map<String, Object> requestData) {
		List<LocalTime> openingHours = dbController.getOpeningHoursFromDB();
		LocalTime openingTime = openingHours.get(0);
		LocalTime closingTime = openingHours.get(1);
		LocalDate date = (LocalDate) requestData.get("date");
		int dinersAmount = (int) requestData.get("dinersAmount");
		List<Order> reservationsByDate = dbController.getReservationsbyDate(date);
		return computeAvailableSlots(openingTime, closingTime, dinersAmount, reservationsByDate);
	}
	
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
		// Build all possible time slots within opening hours
		List<LocalTime> possibleTimeSlots = buildPossibleTimeSlots(openingTime, closingTime);
		// Map from time slot to list of diners amounts overlapping with that slot
		Map<LocalTime, List<Integer>> tablesPerTime = new HashMap<>();
		for (LocalTime slot : possibleTimeSlots){
			tablesPerTime.put(slot, new ArrayList<>()); 
		}
		//loop over existing reservations and mark overlapping time slots:
		for (Order o : reservationsByDate) {// for each existing reservation
			LocalTime orderStart = o.getOrderHour();
			LocalTime orderEnd = orderStart.plusMinutes(reservationDurationMinutes);
			// for each possible time slot
			for (LocalTime slot : possibleTimeSlots) {
				LocalTime slotStartTime = slot;
				LocalTime slotEndTime = slotStartTime.plusMinutes(reservationDurationMinutes);
				// Check overlap between reservation and time slot to avoid double-booking:
				if (overlaps(slotStartTime, slotEndTime, orderStart, orderEnd)) {
					tablesPerTime.get(slot).add(o.getDinersAmount());
				}
			}
		}
		// Check each time slot if it can contain the new diners amount:
		List<String> available = new ArrayList<>(); // available time slots
		for (LocalTime slot : possibleTimeSlots) {// for each possible time slot
			// Get overlapping diners amounts for this slot and add the new diners amount
			List<Integer> overlappingDinersAmounts = new ArrayList<>(tablesPerTime.get(slot));
			overlappingDinersAmounts.add(newDinersAmount);// add new diners amount
			// Check if all diners amounts can be assigned to available tables
			if (canAssignAllDinersToTables(overlappingDinersAmounts, tableSizes)) {
				available.add(timeToString(slot));
			}
		}
		printListbeforeReturn(available);
		return available;
	}
	
	private void printListbeforeReturn(List<String> available) {
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
	private List<LocalTime> buildPossibleTimeSlots(LocalTime openingTime, LocalTime closingTime) {
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
    private boolean overlaps(LocalTime slotStartTime, LocalTime slotEndTime, LocalTime orderStart, LocalTime orderEnd) {
        return slotStartTime.isBefore(orderEnd) && orderStart.isBefore(slotEndTime);
    }

    /**
	 * Checks if it is possible to assign all diners amounts to available tables.
	 * 
	 * @param overlappingDinersAmounts A list of diners amounts that need to be seated.
	 * @param tableSizes A list of available table sizes.
	 * @return true if all diners amounts can be assigned to tables, false otherwise.
	 */
	public static boolean canAssignAllDinersToTables(List<Integer> overlappingDinersAmounts, List<Integer> tableSizes) {
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
    private String timeToString(LocalTime time) {
        // "HH:mm"
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }






}
