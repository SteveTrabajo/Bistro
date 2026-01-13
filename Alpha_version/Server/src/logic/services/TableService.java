package logic.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import entities.Order;
import entities.Table;
import entities.User;
import enums.OrderStatus;
import enums.OrderType;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class TableService {
	  private final BistroDataBase_Controller dbController;
	    private final ServerLogger logger;
	    private final OrdersService orderService;
	    private final NotificationService notificationService;

	    // scheduler for 15-minute no-show checks
	    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


	    public TableService(BistroDataBase_Controller dbController, ServerLogger logger,OrdersService orderService, NotificationService notificationService) {
	        this.dbController = dbController;
	        this.logger = logger;
			this.orderService = orderService;
	        this.notificationService = notificationService;
	    }

	    public int allocateTable(String confirmationCode, LocalDateTime now) {
	        Order order = dbController.getOrderByConfirmationCodeInDB(confirmationCode);
	        if (order == null) {
	            logger.log("[ERROR] Allocation failed: Order not found for " + confirmationCode);
	            return -1;
	        }

	        //case order type WAITLIST not notified yet block allocation
	        if (order.getOrderType() == OrderType.WAITLIST && order.getStatus() != OrderStatus.NOTIFIED) {
	            logger.log("[WARN] Allocation blocked: WAITLIST not notified for " + confirmationCode);
	            return -1;
	        }
	        
	        //case order type RESERVATION check for no-show after 15 minutes from reservation time 
	        if (order.getOrderType() == OrderType.RESERVATION
	                && order.getStatus() != OrderStatus.SEATED
	                && order.getStatus() != OrderStatus.COMPLETED) {
	            LocalDateTime start = LocalDateTime.of(order.getOrderDate(), order.getOrderHour());
	            if (now.isAfter(start.plusMinutes(15))) { //if more than 15 minutes past reservation time
	                dbController.updateOrderStatusInDB(confirmationCode, OrderStatus.NO_SHOW);
	                logger.log("[WARN] Reservation NO_SHOW: " + confirmationCode);
	                return -1;
	            }
	        }
	        
	        int tableNum = dbController.findFreeTableForGroup(order.getDinersAmount());
	        if (tableNum == -1) {
	            logger.log("[WARN] No tables available for group size " + order.getDinersAmount());
	            return -1;
	        }
	        int diningMinutes = orderService.getReservationDurationMinutes(); 
	        boolean sessionCreated = dbController.createTableSession(order.getOrderNumber(), tableNum, diningMinutes);
	        if (!sessionCreated) {
	            logger.log("[ERROR] Failed to create session for order " + confirmationCode);
	            return -1;
	        }

	        dbController.updateOrderStatusInDB(confirmationCode, OrderStatus.SEATED);
	        logger.log("[INFO] Allocated Table " + tableNum + " to Order " + confirmationCode);
	        return tableNum;
	    }

	   /**
	    * method to be called when payment is completed for an order
	    * @param orderNumber
	    */
	    public void onPaymentCompleted(int orderNumber) {
	    	//close table session when payment is completed
	        Integer tableNum = dbController.getActiveTableNumByOrderNumber(orderNumber);
	        dbController.closeTableSessionForOrder(orderNumber);
	        //case no active table session found
	        if (tableNum == null) {
	            logger.log("[WARN] Payment completed but no active table session found for order " + orderNumber);
	            return;
	        }	       
	        onTableFreed(tableNum); // when table is freed, try to seat waitlist
	    }

	    /**
	     * Handles logic when a table is freed.
	     * @param tableNum The table number that was freed.
	     */
	    public void onTableFreed(int tableNum) {
	        int capacity = dbController.getTableCapacity(tableNum);
	        if (capacity <= 0) return;

	        Order next = dbController.getNextWaitlistThatFitsCapacity(capacity);
	        if (next == null) {
	            logger.log("[INFO] Table " + tableNum + " freed. No waitlist fits capacity=" + capacity);
	            return;
	        }

	        //change status to NOTIFIED and start the process 
	        boolean marked = dbController.updateOrderStatusInDB(next.getConfirmationCode(), OrderStatus.NOTIFIED);
	        if (!marked) return;

	        //send notification to the user
	        notificationService.notifyWaitlistUser(next);
	        
	        //schedule a task to check for no-show after 15 minutes
	        scheduler.schedule(() -> {
	            boolean noShow = dbController.updateOrderStatusInDB(next.getConfirmationCode(), OrderStatus.NO_SHOW);
	            if (noShow) {
	                logger.log("[WARN] WAITLIST NO_SHOW: " + next.getConfirmationCode());
	                onTableFreed(tableNum);//try to seat next waitlist
	            }
	        }, 15, TimeUnit.MINUTES);
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
	
	//TODO : change logic to return map of Table objects and their status
//	public HashMap<Table,String> getTableMap() {
//		HashMap<Table,String> data =dbController.getTableMap();
//		printDATA(data);
//		return data;
//	}
	

}
//End of TableService.java