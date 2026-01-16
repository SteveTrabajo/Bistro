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

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        int duration = ordersService.getReservationDurationMinutes();
        LocalTime walkInEnd = now.plusMinutes(duration);

        int freeTable = dbController.findFreeTableForGroup(dinersAmount);
        if (freeTable == -1 ) {
            long wait = calculateEstimatedWaitTime(dinersAmount);
            return new WaitListResponse(true, wait,"No table available. Estimated wait: " + wait + " minutes.");
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

    public boolean isUserInWaitingList(int userID) { 
        return dbController.isUserInWaitingList(userID);
    }

	public Order getWaitingListOrderByCode(String code) {
		return ordersService.getOrderByConfirmationCode(code);
	}

	public Order getWaitingListOrderByUserId(int userID) {
		return dbController.getWaitingListOrderByUserId(userID);
	}

    
//   /**
//	* Logic for Members using the existing getUserInfo logic in UserService
//	* @param dinersAmount Number of diners in the group.
//	* @param memberIdStr Member ID string.
//	* @return Result object containing seating or waitlist information.
//	*/
//   public Object handleMemberWalkIn(int dinersAmount, String memberIdStr) {
//       Map<String, Object> loginData = new HashMap<>();
//       loginData.put("userType", "MEMBER");
//       loginData.put("memberCode", memberIdStr);
//
//       User user = userService.getUserInfo(loginData);
//       if (user == null) return null;
//
//       return processSeatingOrWaiting(dinersAmount, user.getUserId());
//   }
//
//   /**
//    * Logic for Guests using the existing getUserInfo logic in UserService
//    * @param dinersAmount
//    * @param phone
//    * @param email
//    * @return
//    */
//   public Object handleGuestWalkIn(int dinersAmount, String phone, String email) {
//       Map<String, Object> loginData = new HashMap<>();
//       loginData.put("userType", "GUEST");
//       loginData.put("phoneNumber", phone);
//       loginData.put("email", email);
//
//       User user = userService.getUserInfo(loginData);
//       if (user == null) return null;
//
//       return processSeatingOrWaiting(dinersAmount, user.getUserId());
//   }
//
//   /**
//    * Core algorithm logic
//    */
//   /**
//    * Shared logic for seating or waitlist registration.
//    * Returns a Map with the results.
//    */
//   private Object processSeatingOrWaiting(int dinersAmount, int userID) {
//       Object availability = checkAvailabilityAndSeat(dinersAmount, userID);
//       Map<String, Object> resultMap = new HashMap<>();
//
//       if (availability instanceof Map) {
//           // --- SCENARIO A: IMMEDIATE SEATING ---
//           @SuppressWarnings("unchecked")
//           Map<String, Object> successMap = (Map<String, Object>) availability;
//           Order order = (Order) successMap.get("order");
//           int tableNum = (int) successMap.get("table");
//
//           resultMap.put("status", "SEATED");
//           resultMap.put("userID", userID);
//           resultMap.put("confirmationCode", order.getConfirmationCode());
//           resultMap.put("tableNumber", tableNum);
//           return resultMap;
//       } 
//       else if (availability instanceof WaitListResponse) {
//           // --- SCENARIO B: ADDED TO WAITLIST ---
//           WaitListResponse res = (WaitListResponse) availability;
//           String code = createWaitListOrder(dinersAmount, userID, true, (int) res.getEstimatedWaitTimeMinutes());
//           
//           if (code != null) {
//               resultMap.put("status", "WAITING");
//               resultMap.put("userID", userID);
//               resultMap.put("confirmationCode", code);
//               resultMap.put("waitTime", res.getEstimatedWaitTimeMinutes());
//               resultMap.put("message", res.getMessage());
//               return resultMap;
//           }
//       }
//       return null; // Registration failed
//   }
//   
    public List<Order> getCurrentQueue() {
        // We return a list of Order entities that are currently in the waitlist
        return dbController.getWaitingQueueFromView();
    }

}
