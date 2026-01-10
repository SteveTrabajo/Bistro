package logic.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
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
	private final TableService tableService;
	
	public WaitingListService(BistroServer server,BistroDataBase_Controller dbController,OrdersService ordersService,TableService tableService, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
		this.server = server;
		this.ordersService = ordersService;
		this.tableService = tableService;
	}

	/**
     * Creates a new order.
     * Logic: 
     * - If addToWaitlist is TRUE: Status is PENDING -> SQL Trigger adds to 'waiting_list' -> We UPDATE the time.
     * - If addToWaitlist is FALSE: Status is SEATED -> SQL Trigger ignores it -> We manually seat the customer.
     * @return The generated confirmation code if successful, null otherwise.
     */
    public String createWaitListOrder(int dinersAmount, int userID, boolean addToWaitlist, int calculatedWaitTime) {
        List<Object> data = new ArrayList<>();
        
        data.add(userID);
        data.add(LocalDate.now());
        data.add(dinersAmount);
        data.add(LocalTime.now());
        
        String confirmationCode = ordersService.generateConfirmationCode("W");
        data.add(confirmationCode);
        
        // Determine Status: PENDING (for Waitlist) or SEATED (for Immediate)
        OrderStatus initialStatus = addToWaitlist ? OrderStatus.PENDING : OrderStatus.SEATED;
        
        // 1. Insert into Orders Table (Trigger fires automatically if PENDING)
        boolean success = dbController.setNewOrder(data, OrderType.WAITLIST, initialStatus);
        
        if (success) {
            if (addToWaitlist) {
                // SCENARIO: WAITING LIST
                // The trigger inserted NULL for time, so we must update it manually
                dbController.updateWaitTimeForOrder(confirmationCode, calculatedWaitTime);
            } else {
                // SCENARIO: IMMEDIATE SEATING
                // Allocate a physical table and seat the customer
                int tableNum = tableService.allocateTable(dinersAmount);
                if (tableNum >= 0) {
                     Order newOrder = dbController.getOrderByConfirmationCodeInDB(confirmationCode);
                     dbController.seatCustomerInDB(newOrder.getOrderNumber(), tableNum);
                }
            }
            return confirmationCode;
        }
        
        return null; // Failed to create
    }

    /**
     * Checks availability and Attempts to Seat Immediately.
     * @return Order object (if seated) OR WaitListResponse object (if full).
     */
    public Object checkAvailabilityAndSeat(int dinersAmount, int userID) {
        
        LocalTime now = LocalTime.now();
        int duration = ordersService.getReservationDurationMinutes();
        LocalTime walkInEndTime = now.plusMinutes(duration);
      
        // 1. Retrieve conflicts
        List<Order> ordersConflicts = dbController.getOrdersThatCouldConflictFromNow(LocalDate.now(), now, walkInEndTime);
        List<Integer> currentLoad = new ArrayList<>();
        
        // 2. Calculate Load
        for (Order o : ordersConflicts) {
            if (o.getStatus() == OrderStatus.SEATED) {
                currentLoad.add(o.getDinersAmount());
            }
            else if (o.getOrderType() == OrderType.RESERVATION && o.getStatus() == OrderStatus.PENDING) {
                LocalTime resStartTime = o.getOrderHour();
                LocalTime resEndTime = resStartTime.plusMinutes(duration);
                if (ordersService.overlaps(now, walkInEndTime, resStartTime, resEndTime)) {
                    currentLoad.add(o.getDinersAmount());
                }
            }
        }
        currentLoad.add(dinersAmount);
        
        // 3. Logic Check
        boolean canSeat = ordersService.canAssignAllDinersToTables(currentLoad, ordersService.getTableSizes());
        
        // --- SCENARIO A: IMMEDIATE SEATING (Success) ---
        if (canSeat) {
        	String code = createWaitListOrder(dinersAmount, userID, false, 0);
            int tableNum = tableService.allocateTable(code);
            Order newOrder = ordersService.getOrderByConfirmationCode(code);
            Map<String,Object> data = new HashMap();
            data.put("order", newOrder);
            data.put("table", tableNum);
            return data;
        }
        
        // --- SCENARIO B: NO TABLE (Failure/Wait) ---
        long waitTime = calculateEstimatedWaitTime(dinersAmount);
        String msg = "No table available. Estimated wait: " + waitTime + " min.";
        return new WaitListResponse(false, waitTime, msg);
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
    
    public int assignTableForWaitingListOrder(Order createdOrder) {
        // Just updates status. The Trigger handles removal from waitlist if needed (though NOTIFIED usually stays in list).
        boolean success = dbController.updateOrderStatusInDB(createdOrder.getConfirmationCode(), OrderStatus.NOTIFIED);
        if (success) {
            server.getNotificationService().notifyWaitlistUser(createdOrder);
            return 1;
        }
        return 0;
    }

    public boolean removeFromWaitingList(String confirmationCode) {
        // Setting to CANCELLED triggers the SQL cleanup automatically
        return dbController.updateOrderStatusInDB(confirmationCode, OrderStatus.CANCELLED);
    }

    public boolean isUserInWaitingList(String confirmationCode) { 
        return dbController.isUserInWaitingList(confirmationCode);
    }
    
    // [NEW METHOD] - Optional: Use the View to get the live queue
    /*
    public List<WaitListEntry> getCurrentQueue() {
         return dbController.getWaitingQueueFromView();
    }
    */

}
