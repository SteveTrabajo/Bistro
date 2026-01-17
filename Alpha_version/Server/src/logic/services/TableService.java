package logic.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dto.Holiday;
import dto.WeeklyHour;
import entities.Order;
import entities.Table;
import enums.EndTableSessionType;
import enums.OrderStatus;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class TableService {
	  private final BistroDataBase_Controller dbController;
	    private final ServerLogger logger;
	    private final OrdersService orderService;
	    private final NotificationService notificationService;

	    //******************************** Constructor ********************************//	
	    public TableService(BistroDataBase_Controller dbController, ServerLogger logger,OrdersService orderService, NotificationService notificationService) {
	        this.dbController = dbController;
	        this.logger = logger;
			this.orderService = orderService;
	        this.notificationService = notificationService;
	    }
	    
	    //********************************Instance  Methods ********************************//
	   /**
	    * Allocates a table for the given confirmation code if possible.
	    * @param confirmationCode
	    * @param now
	    * @return
	    */
	    public int allocateTable(String confirmationCode, LocalDateTime now) {
	        Order order = dbController.getOrderByConfirmationCodeInDB(confirmationCode);
	        //case order not found
	        if (order == null) {
	            logger.log("[ERROR] Allocation failed: Order not found for " + confirmationCode);
	            return -1;
	        }
	        //case order type WAITLIST not notified yet block allocation
	        if (order.getOrderType() == OrderType.WAITLIST && order.getStatus() != OrderStatus.NOTIFIED) {
	            logger.log("[WARN] Allocation blocked: WAITLIST not notified for " + confirmationCode);
	            return -1;
	        }
	        	        
	        //find free table for the group size dinersAmount to allocate table to seat them:
	        int tableNum = dbController.findFreeTableForGroup(order.getDinersAmount());
	        if (tableNum == -1) {
	            logger.log("[WARN] No tables available for group size " + order.getDinersAmount());
	            //TODO : add to waitlist if not already in it
	            return -1;
	        }
	        //create table session for the order 
	        int diningMinutes = orderService.getReservationDurationMinutes(); 
	        boolean sessionCreated = dbController.createTableSession(order.getOrderNumber(), tableNum, diningMinutes);
	        if (!sessionCreated) {
	            logger.log("[ERROR] Failed to create session for order " + confirmationCode);
	            return -1;
	        }
	        //update order status to SEATED
	        boolean statusUpdated = dbController.updateOrderStatusInDB(confirmationCode, OrderStatus.SEATED);
	        
	        if (!statusUpdated) {
	            logger.log("[ERROR] Critical: Session created but Status Update failed for " + confirmationCode);
	            dbController.deleteActiveSession(order.getOrderNumber());
	            logger.log("[INFO] Rolled back (deleted) orphan session for order " + order.getOrderNumber());
	            return -1;
	        }
	        logger.log("[INFO] Allocated Table " + tableNum + " to Order " + confirmationCode);
	        return tableNum;
	    }

	    /**
	     * Handles logic when a table is freed.
	     * @param tableNum The table number that was freed.
	     */
	    public boolean tableFreed(int tableNum) {
	    	 int capacity = dbController.getTableCapacity(tableNum);
	    	    if (capacity <= 0) {
	    	        logger.log("[ERROR] Invalid capacity for table " + tableNum);
	    	        return false;
	    	    }

	    	    //find next waitlist that fits the table capacity
	    	    Order next = dbController.getNextFromWaitingQueueThatFits(capacity);

	    	    if (next == null) {
	    	        logger.log("[INFO] Table " + tableNum + " freed. No waitlist fits capacity=" + capacity);
	    	        return true; //no waitlist fits the table capacity
	    	    }

	    	    //if cannot notify now due to reservation constraints, skip notifying for now
	    	    if (!canNotifyWaitlistNow(next.getDinersAmount())) {
	    	        logger.log("[INFO] Cannot notify waitlist " + next.getConfirmationCode()
	    	                + " now due to reservation constraints.");
	    	        return true; //skip notifying for now 
	    	    }

	    	    //set waitlist as NOTIFIED in DB with current timestamp
	    	    boolean marked = dbController.markWaitlistAsNotified(next.getOrderNumber(), LocalDateTime.now());
	    	    if (!marked) {
	    	        logger.log("[ERROR] Failed to mark waitlist as NOTIFIED for " + next.getConfirmationCode());
	    	        return false;
	    	    }

	    	    //refresh order data
	    	    Order refreshed = dbController.getOrderByConfirmationCodeInDB(next.getConfirmationCode());

	    	    //notify waitlist user via NotificationService
	    	    notificationService.notifyWaitlistUser(refreshed);

	    	    logger.log("[INFO] NOTIFIED waitlist " + refreshed.getConfirmationCode() + " for table " + tableNum + ", capacity=" + capacity);

	    	    return true;
	    	}
	    
	    
	    /**
	     * Checks if a waitlist user can be notified now without conflicting with existing reservations.
	     * @param dinersAmount The number of diners in the waitlist order.
	     * @return true if the waitlist user can be notified now, false otherwise.
	     */
	    private boolean canNotifyWaitlistNow(int dinersAmount) {
	        LocalTime now = LocalTime.now();
	        int duration = orderService.getReservationDurationMinutes();
	        LocalTime end = now.plusMinutes(duration);

	        //get active and upcoming reservations that may conflict
	        List<Order> conflicts = dbController.getActiveAndUpcomingOrders(LocalDate.now(), now, end);

	        List<Integer> load = new ArrayList<>();

	        for (Order o : conflicts) {
	            if (o.getStatus() == OrderStatus.SEATED) {
	                load.add(o.getDinersAmount());
	            } else if (o.getOrderType() == OrderType.RESERVATION && o.getStatus() == OrderStatus.PENDING) {
	                LocalTime s = o.getOrderHour();
	                LocalTime e = s.plusMinutes(duration);
	                if (orderService.overlaps(now, end, s, e)) {
	                    load.add(o.getDinersAmount());
	                }
	            }
	        }

	        load.add(dinersAmount);

	        return orderService.canAssignAllDinersToTables(load, orderService.getTableSizes());
	    }

    
		/**
		 * Retrieves the table number associated with a given reservation confirmation code.
		 * @param confirmationCode The reservation confirmation code.
		 * @return The table number, or -1 if not found.
		 */    
		public int getTableNumberByReservationConfirmationCode(String confirmationCode) {
			return dbController.getTableNumberByConfirmationCode(confirmationCode);
		}
		
		/**
		 * Retrieves all tables from the database.
		 * @return A list of all Table entities.
		 */
		public List<Table> getAllTables() {
			return dbController.getAllTablesFromDB();
		}
	
		public HashMap<Table, String> getAllTablesMap() {
			List<Table> tables = getAllTables();
			HashMap<Table, String> tableStatusMap = new HashMap<>();
			for (Table table : tables) {
				String confirmationCode = dbController.getActiveOrderConfirmationCodeByTableNum(table.getTableID());
				tableStatusMap.put(table, confirmationCode);
			}
			return tableStatusMap;
		}
		
	
	    public boolean addNewTable(Table table) {
	        return dbController.addTable(table);
	    }

	    public boolean deleteTable(int tableId) {
	        return dbController.removeTable(tableId);
	    }
	    
	    public boolean updateTableSeats(int tableId, int newSeats) {
	        return dbController.updateTableCapacity(tableId, newSeats);
	    }

		public Order getSeatedOrderForClient(int userId) {
			return dbController.getSeatedOrderForUser(userId);
		}

}
//End of TableService.java