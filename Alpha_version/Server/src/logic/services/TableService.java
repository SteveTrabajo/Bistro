package logic.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entities.Order;
import entities.Table;
import entities.User;
import enums.OrderStatus;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class TableService {
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;

	public TableService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}

	public int getTableNumberByReservationConfirmationCode(String confirmationCode) {
		return dbController.getTableNumberByConfirmationCode(confirmationCode);
	}
	
	public HashMap<Table,String> getTableMap() {
		HashMap<Table,String> data =dbController.getTableMap();
		printDATA(data);
		return data;
	}
	
	private void printDATA(HashMap<Table, String> data) {
		for (Map.Entry<Table, String> entry : data.entrySet()) {
			Table table = entry.getKey();
			String code = entry.getValue();
			System.out.println("Table ID: " + table.getTableID() + ", Capacity: " + table.getCapacity() + ", Occupied Code: " + code);
		}
	}

	/**
    * Logic to handle table status after a successful payment.
    */
	//TODO: Expand this method in the future as needed to clear the table or update its status.
   public static void notifyPaymentCompletion(User user, TableService tableService, ServerLogger logger) {
       logger.log("[INFO] Payment completed for User: " + user.getUserId());
       // Future logic: You might want to auto-mark the table as "DIRTY" or "FREE" here,
       // or simply log that the session is financially settled.
   }

	public int allocateTable(String confirmationCode) {
	    Order order = dbController.getOrderByConfirmationCodeInDB(confirmationCode);
	    if (order == null) {
	        logger.log("[ERROR] Allocation failed: Order not found for " + confirmationCode);
	        return -1;
	    }
	    int tableNum = dbController.findFreeTableForGroup(order.getDinersAmount());	    
	    if (tableNum != -1) {
	        boolean sessionCreated = dbController.createTableSession(order.getOrderNumber(), tableNum);	        
	        if (sessionCreated) {
	            dbController.updateOrderStatusInDB(confirmationCode, OrderStatus.SEATED);
	            logger.log("[INFO] Allocated Table " + tableNum + " to Order " + confirmationCode);
	            return tableNum;
	        }
	    }	    
	    logger.log("[WARN] No tables available for group size " + order.getDinersAmount());
	    return -1;
	}

	public List<Table> getAllTables() {
		return dbController.getAllTablesFromDB();
	}

}
