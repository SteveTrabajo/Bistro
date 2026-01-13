package logic.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import entities.Order;
import entities.Table;
import entities.User;
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
	        dbController.updateOrderStatusInDB(confirmationCode, OrderStatus.SEATED);
	        logger.log("[INFO] Allocated Table " + tableNum + " to Order " + confirmationCode);
	        return tableNum;
	    }

	   /**
	    * method to be called when payment is completed for an order
	    * @param orderNumber
	    */
	    public boolean onPaymentCompleted(int orderNumber) {
	    	//close table session when payment is completed
	        Integer tableNum = dbController.getActiveTableNumByOrderNumber(orderNumber);
	        dbController.closeTableSessionForOrder(orderNumber, EndTableSessionType.PAID);
	        //case no active table session found
	        if (tableNum == null) {
	            logger.log("[WARN] Payment completed but no active table session found for order " + orderNumber);
	            return false;
	        }	       
	        return afterTableFreed(tableNum); //when table is freed, try to seat WAITLIST/RESERVATION order if possible
	    }

	    /**
	     * Handles logic when a table is freed.
	     * @param tableNum The table number that was freed.
	     */
	    public boolean afterTableFreed(int tableNum) {
	    	 int capacity = dbController.getTableCapacity(tableNum);
	    	    if (capacity <= 0) {
	    	        logger.log("[ERROR] Invalid capacity for table " + tableNum);
	    	        return false;
	    	    }

	    	    // הבא בתור שמתאים לקיבולת (PENDING waitlist)
	    	    Order next = dbController.getNextFromWaitingQueueThatFits(capacity);

	    	    if (next == null) {
	    	        logger.log("[INFO] Table " + tableNum + " freed. No waitlist fits capacity=" + capacity);
	    	        return true; // שולחן התפנה בהצלחה, פשוט אין מי להודיע
	    	    }

	    	    // עדיפות להזמנות עתידיות (Reservation Guard)
	    	    if (!canNotifyWaitlistNow(next.getDinersAmount())) {
	    	        logger.log("[INFO] Cannot notify waitlist " + next.getConfirmationCode()
	    	                + " now due to reservation constraints.");
	    	        return true; // השולחן פנוי, פשוט לא מודיעים כרגע
	    	    }

	    	    // ✅ קריטי: מעדכנים גם NOTIFIED וגם notified_at (כדי ש-NoShowManager יעבוד)
	    	    boolean marked = dbController.markWaitlistAsNotified(next.getOrderNumber(), LocalDateTime.now());
	    	    if (!marked) {
	    	        logger.log("[ERROR] Failed to mark waitlist as NOTIFIED for " + next.getConfirmationCode());
	    	        return false;
	    	    }

	    	    // למשוך הזמנה מחדש אחרי העדכון
	    	    Order refreshed = dbController.getOrderByConfirmationCodeInDB(next.getConfirmationCode());

	    	    // שליחה (Email+SMS) דרך NotificationService של החבר
	    	    notificationService.notifyWaitlistUser(refreshed);

	    	    logger.log("[INFO] NOTIFIED waitlist " + refreshed.getConfirmationCode()
	    	            + " for table " + tableNum + ", capacity=" + capacity);

	    	    // ❌ לא עושים schedule פה — NoShowManager של החבר יטפל ב-15 דקות
	    	    return true;
	    	}
	    
	    private boolean canNotifyWaitlistNow(int dinersAmount) {
	        LocalTime now = LocalTime.now();
	        int duration = orderService.getReservationDurationMinutes();
	        LocalTime end = now.plusMinutes(duration);

	        // מביא הזמנות פעילות + הזמנות עתידיות שעלולות להתנגש
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
	
	//TODO : change logic to return map of Table objects and their status
//	public HashMap<Table,String> getTableMap() {
//		HashMap<Table,String> data =dbController.getTableMap();
//		printDATA(data);
//		return data;
//	}
	

}
//End of TableService.java