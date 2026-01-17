package logic;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import comms.Api;
import comms.Message;
import entities.Order;
import entities.Table;
import dto.Holiday;
import dto.WeeklyHour;
import enums.OrderStatus;
import javafx.application.Platform;

/**
 * TableController is responsible for managing table-related operations in the BistroClient application.
 * It handles table statuses, user-allocated tables, and communicates with the server for table-related requests.
 */
public class TableController {
	
	//****************************** Instance variables ******************************
	private final BistroClient client;
	private HashMap<Table,String> tableStatuses;
	private Order seatdOrderDTO;
	private BiConsumer<Boolean, String> checkInListener;
	private int userAllocatedTable;
	private int tablesAmount;
	private Consumer<List<Table>> allTablesCallback;
	private Consumer<List<WeeklyHour>> weeklyHoursCallback;
	private Consumer<List<Holiday>> holidaysCallback;

	
	//******************************** Constructors ***********************************//
	public TableController(BistroClient client) {
		this.client = client;
		this.tableStatuses = new HashMap<>();
		this.seatdOrderDTO = null;
		this.userAllocatedTable = 0;
	}
	
	//******************************** Getters And Setters ***********************************//	

	/**
	 * Gets the current statuses of all tables.
	 * 
	 * @return A HashMap where the key is a Table object and the value is its status as a String.
	 */
	public HashMap<Table, String> getTableStatuses() {
		return tableStatuses;
	}
	
	/**
	 * Sets the statuses of all tables.
	 * 
	 * @param tableStatuses A HashMap where the key is a Table object and the value is its status as a String.
	 */
	public void setTableStatuses(HashMap<Table, String> tableStatuses) {
		this.tableStatuses = tableStatuses;
	}
	
	/**
	 * Gets the order allocated to the user for their table.
	 * 
	 * @return The Order object representing the user's allocated order for the table.
	 */
	public Order getUserAllocatedOrderForTable() {
		return seatdOrderDTO;
	}
	
	/**
	 * Sets the order allocated to the user for their table.
	 * 
	 * @param userAllocatedOrderForTable The Order object representing the user's allocated order for the table.
	 */
	public void setUserAllocatedOrderForTable(Order userAllocatedOrderForTable) {
		this.seatdOrderDTO = userAllocatedOrderForTable;
	}

	/**
	 * Gets the table number allocated to the user.
	 * 
	 * @return The table number allocated to the user.
	 */
	public int getUserAllocatedTable() {
		return userAllocatedTable;
	}

	/**
	 * Sets the table number allocated to the user.
	 * 
	 * @param userAllocatedTable The table number to allocate to the user.
	 */
	public void setUserAllocatedTable(int userAllocatedTable) {
		this.userAllocatedTable = userAllocatedTable;
	}
	
	/**
	 * Sets the listener for check-in results.
	 * 
	 * @param checkInListener A BiConsumer that accepts a Boolean indicating success and a String message.
	 */
	public void setCheckInListener(BiConsumer<Boolean, String> checkInListener) {
		this.checkInListener = checkInListener;
	}
	
	/**
	 * Gets the total number of tables.
	 * 
	 * @return The total number of tables.
	 */
	public int getTablesAmount() {
		return tablesAmount;
	}
	
	/**
	 * Sets the total number of tables.
	 * 
	 * @param tablesAmount The total number of tables.
	 */
	public void setTablesAmount(int tablesAmount) {
		this.tablesAmount = tablesAmount;
	}
	
	/**
	 * Sets the listener for receiving all tables.
	 * 
	 * @param callback A Consumer that accepts a List of Table objects.
	 */
	public void setTablesListener(Consumer<List<Table>> callback) {
		this.allTablesCallback = callback;
	}
	
	/**
	 * Fires the all tables callback with the provided list of tables.
	 * 
	 * @param tables A List of Table objects.
	 */
	public void fireAllTables(List<Table> tables) {
	    if (allTablesCallback != null) {
	        allTablesCallback.accept(tables);
	    }
	}
	
	//******************************** Instance Methods ***********************************//
	
	/**
	 * Clears the table controller's state, including table statuses, seated order, user allocated table, and tables amount.
	 * 
	 * @return true if the operation was successful.
	 */
	public boolean clearTableController() {
		this.tableStatuses.clear();
		this.seatdOrderDTO = null;
		this.userAllocatedTable = -1;
		this.tablesAmount = -1;
		return true;
	}
	
	/**
	 * Checks if the user has successfully checked in to a table.
	 * 
	 * @return true if the user is seated at a table, false otherwise.
	 */
	public boolean isCheckInTableSuccess() {
		return seatdOrderDTO.getStatus() == OrderStatus.SEATED;
	}

	/**
	 * Clears the current table information for the user.
	 */
	public void clearCurrentTable() {
		this.seatdOrderDTO = null;
		this.userAllocatedTable = 0;
		
	}

	/**
	 * Requests the current status of all tables from the server.
	 */
	public void requestTableStatus() {
		client.handleMessageFromClientUI(new Message(Api.ASK_TABLE_STATUS, null));
		
	}

	/**
	 * Updates the statuses of all tables.
	 * 
	 * @param tableStatuses A HashMap where the key is a Table object and the value is its status as a String.
	 */
	public void updateTableStatuses(HashMap<Table, String> tableStatuses) {
		setTableStatuses(tableStatuses);
	}

	/**
	 * Notifies the check-in result to the registered listener.
	 * 
	 * @param success A boolean indicating whether the check-in was successful.
	 * @param message A message providing additional information about the check-in result.
	 */
	public void notifyCheckInResult(boolean success, String message) {
		if (checkInListener != null) {
			checkInListener.accept(success, message);
		}
	}

	/**
	 * Fetches the table allocated to the user from the server.
	 */
	public void fetchUserAllocatedTable() {
		client.handleMessageFromClientUI(new Message(Api.ASK_USER_ALLOCATED_TABLE, null));
		
	}
	
	/**
	 * Requests all tables from the server.
	 */
	public void askAllTables() {
		client.handleMessageFromClientUI(new Message(Api.ASK_ALL_TABLES, null));
	}
	
	/**
	 * Requests to add a new table to the server.
	 * @param table
	 */
	public void askAddTable(Table table) {
		client.handleMessageFromClientUI(new Message(Api.ASK_ADD_TABLE, table));
	}
	
	/**
	 * Requests to remove a table from the server.
	 * @param tableNumber
	 */
	public void askRemoveTable(int tableNumber) {
		client.handleMessageFromClientUI(new Message(Api.ASK_REMOVE_TABLE, tableNumber));
	}
	
	/**
	 * Requests to update the number of seats for a specific table on the server.
	 * @param tableId
	 * @param newSeats
	 */
	public void askUpdateTableSeats(int tableId, int newSeats) {
		int[] data = new int[] { tableId, newSeats };
		client.handleMessageFromClientUI(new Message(Api.ASK_UPDATE_TABLE_SEATS, data));
	}
	
	/**
	 * Sets the list of all tables and triggers the callback if set.
	 * 
	 * @param tables A List of Table objects.
	 */
	public void setAllTables(List<Table> tables) {
		if (allTablesCallback != null) {
			Platform.runLater(() -> allTablesCallback.accept(tables));
		}
	}
	
	/**
	 * Requests to save the weekly hours to the server.
	 * 
	 * @param hours A List of WeeklyHour objects representing the weekly hours to be saved.
	 */
	public void askSaveWeeklyHours(List<WeeklyHour> hours) {
        client.handleMessageFromClientUI(new Message(Api.ASK_SAVE_WEEKLY_HOURS, hours));
    }
	
	/**
	 * Requests to add a new holiday to the server.
	 * 
	 * @param holiday A Holiday object representing the holiday to be added.
	 */
    public void askAddHoliday(Holiday holiday) {
        client.handleMessageFromClientUI(new Message(Api.ASK_ADD_HOLIDAY, holiday));
    }
    
    /**
	 * Requests to retrieve the list of holidays from the server.
	 */
    public void askGetHolidays() {
        client.handleMessageFromClientUI(new Message(Api.ASK_GET_HOLIDAYS, null));
    }
    
    /**
	 * Requests the order allocated to the user for their seated table from the server.
	 * @param userID The ID of the user.
	 */
	public void askUserAllocatedSeatedOrder(int userID) {
		client.handleMessageFromClientUI(new Message(Api.ASK_SEATED_ORDER, userID));
	}

	/**
	 * Sets the listener for weekly hours updates.
	 * @param callback
	 */
	public void setWeeklyHoursListener(Consumer<List<WeeklyHour>> callback) {
	    this.weeklyHoursCallback = callback;
	}

	/**
	 * Requests to retrieve the weekly hours from the server.
	 */
	public void askGetWeeklyHours() {
	    client.handleMessageFromClientUI(new Message(Api.ASK_GET_WEEKLY_HOURS, null));
	}

	/**
	 * Sets the listener for holidays updates.
	 * @param callback
	 */
	public void setHolidaysListener(Consumer<List<Holiday>> callback) {
	    this.holidaysCallback = callback;
	}

	/**
	 * Sets the list of weekly hours and triggers the callback if set.
	 * 
	 * @param hours A List of WeeklyHour objects.
	 */
	public void setWeeklyHours(List<WeeklyHour> hours) {
	    if (weeklyHoursCallback != null) {
	        Platform.runLater(() -> weeklyHoursCallback.accept(hours));
	    }
	}

	/**
	 * Sets the list of holidays and triggers the callback if set.
	 * 
	 * @param holidays A List of Holiday objects.
	 */
	public void setHolidays(List<Holiday> holidays) {
		System.out.println("TableController: Setting holidays: " + holidays);
	    if (holidaysCallback != null) {
	    	System.out.println("TableController: Calling holidays callback.");
	        Platform.runLater(() -> holidaysCallback.accept(holidays));
	    }
	}
}
// End of TableController.java