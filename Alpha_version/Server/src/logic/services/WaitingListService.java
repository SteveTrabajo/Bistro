package logic.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dto.WaitListResponse;
import entities.Order;
import enums.OrderStatus;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.BistroServer;
import logic.ServerLogger;

public class WaitingListService {
	private final BistroServer server;
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	private final OrdersService ordersService;
	
	public WaitingListService(BistroServer server,BistroDataBase_Controller dbController,OrdersService ordersService, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
		this.server = server;
		this.ordersService = ordersService;
	}


	public boolean checkIfuserHasOrderForToday(int userId) {
		
		return false;
	}
	
	public int assignTableForWaitingListOrder(Order createdOrder) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean removeFromWaitingList(String confirmationCode) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isUserInWaitingList(String confirmationCode) {
		return dbController.isUserInWaitingList(confirmationCode);
	}
	
	
	public boolean CreateWaitListOrder(int dinersAmount,int userID, boolean addToWaitlist) {
		List<Object> data = new ArrayList<>();
		OrderStatus initialStatus;
		data.add(userID);
		data.add(LocalDate.now());
		data.add(dinersAmount);
		data.add(LocalTime.now());
		
		String confirmationCode = ordersService.generateConfirmationCode("W");
		data.add(confirmationCode);
		//condition to set the initial status according to seating or waitlist
		if (addToWaitlist) {
			initialStatus = OrderStatus.PENDING;
		} else {
			initialStatus = OrderStatus.SEATED;
		}
		
		return dbController.setNewOrder(data, OrderType.WAITLIST, initialStatus);
	}
	
	
	public WaitListResponse checkAvailabilityForWalkIn(int dinersAmount) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        // Define the time window: From NOW until the reservation duration ends (e.g., 2 hours)
        LocalTime walkInEndTime = now.plusMinutes(ordersService.getReservationDurationMinutes());

        // 1. Fetch all orders that might conflict with this time window:
        //    - Customers currently SEATED.
        //    - Future RESERVATIONS that start within the next 2 hours.
        List<Order> activeOrders = dbController.getActiveAndUpcomingOrders(today, now, walkInEndTime);
        
        // 2. Build the list of "loads" (diners amount) for the bin-packing algorithm
        List<Integer> currentLoad = new ArrayList<>();
        
        for (Order o : activeOrders) {
            if (o.getStatus() == OrderStatus.SEATED) {
                currentLoad.add(o.getDinersAmount());
            } else {
                LocalTime orderStart = o.getOrderHour();
                LocalTime orderEnd = orderStart.plusMinutes(ordersService.getReservationDurationMinutes());

                if (ordersService.overlaps(now, walkInEndTime, orderStart, orderEnd)) {
                    currentLoad.add(o.getDinersAmount());
                }
            }
        }
        
        // 3. Add the new walk-in group to the load list to test feasibility
        currentLoad.add(dinersAmount);

        // 4. Run the Greedy Algorithm (Bin Packing)
        if (ordersService.canAssignAllDinersToTables(currentLoad, ordersService.getTableSizes())) {
            return new WaitListResponse(true, 0, "Table is available immediately.");
        } 
        
        // 5. If no immediate table, calculate wait time
        long waitTime = calculateEstimatedWaitTime(dinersAmount);
        return new WaitListResponse(false, waitTime, "No tables available. Estimated wait: " + waitTime + " minutes.");
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
        
        // UX Polish: If table is technically free (minutes <= 0) but algorithm said no,
        // it means staff is clearing it. Give a 5-minute buffer.
        if (minutes > 0) {
            return minutes;
        } else {
            return 5;
        }
    }

}
