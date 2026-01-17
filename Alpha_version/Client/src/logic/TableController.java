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

	public HashMap<Table, String> getTableStatuses() {
		return tableStatuses;
	}
	
	public void setTableStatuses(HashMap<Table, String> tableStatuses) {
		this.tableStatuses = tableStatuses;
	}
	public Order getUserAllocatedOrderForTable() {
		return seatdOrderDTO;
	}
	
	public void setUserAllocatedOrderForTable(Order userAllocatedOrderForTable) {
		this.seatdOrderDTO = userAllocatedOrderForTable;
	}

	public int getUserAllocatedTable() {
		return userAllocatedTable;
	}

	public void setUserAllocatedTable(int userAllocatedTable) {
		this.userAllocatedTable = userAllocatedTable;
	}
	
	public void setCheckInListener(BiConsumer<Boolean, String> checkInListener) {
		this.checkInListener = checkInListener;
	}
	
	public int getTablesAmount() {
		return tablesAmount;
	}
	
	public void setTablesAmount(int tablesAmount) {
		this.tablesAmount = tablesAmount;
	}
	
	public void setTablesListener(Consumer<List<Table>> callback) {
		this.allTablesCallback = callback;
	}
	
	public void fireAllTables(List<Table> tables) {
	    if (allTablesCallback != null) {
	        allTablesCallback.accept(tables);
	    }
	}
	
	//******************************** Instance Methods ***********************************//
	
	public boolean clearTableController() {
		this.tableStatuses.clear();
		this.seatdOrderDTO = null;
		this.userAllocatedTable = -1;
		this.tablesAmount = -1;
		return true;
	}
	
	
	public boolean isCheckInTableSuccess() {
		//TODO maybe change to return seatdOrderDTO != null && seatdOrderDTO.getStatus() == OrderStatus.SEATED;
		return seatdOrderDTO.getStatus() == OrderStatus.SEATED;
	}

	public void clearCurrentTable() {
		this.seatdOrderDTO = null;
		this.userAllocatedTable = 0;
		
	}

	public void requestTableStatus() {
		client.handleMessageFromClientUI(new Message(Api.ASK_TABLE_STATUS, null));
		
	}

	public void updateTableStatuses(HashMap<Table, String> tableStatuses) {
		setTableStatuses(tableStatuses);
	}


	public void notifyCheckInResult(boolean success, String message) {
		if (checkInListener != null) {
			checkInListener.accept(success, message);
		}
	}

	public void fetchUserAllocatedTable() {
		client.handleMessageFromClientUI(new Message(Api.ASK_USER_ALLOCATED_TABLE, null));
		
	}
	
	public void askAllTables() {
		client.handleMessageFromClientUI(new Message(Api.ASK_ALL_TABLES, null));
	}
	
	public void askAddTable(Table table) {
		client.handleMessageFromClientUI(new Message(Api.ASK_ADD_TABLE, table));
	}
	
	public void askRemoveTable(int tableNumber) {
		client.handleMessageFromClientUI(new Message(Api.ASK_REMOVE_TABLE, tableNumber));
	}
	
	public void setAllTables(List<Table> tables) {
		if (allTablesCallback != null) {
			Platform.runLater(() -> allTablesCallback.accept(tables));
		}
	}
	
	public void askSaveWeeklyHours(List<WeeklyHour> hours) {
        client.handleMessageFromClientUI(new Message(Api.ASK_SAVE_WEEKLY_HOURS, hours));
    }

    public void askAddHoliday(Holiday holiday) {
        client.handleMessageFromClientUI(new Message(Api.ASK_ADD_HOLIDAY, holiday));
    }
    
    public void askGetHolidays() {
        client.handleMessageFromClientUI(new Message(Api.ASK_GET_HOLIDAYS, null));
    }
    


	public void askUserAllocatedSeatedOrder(int userID) {
		client.handleMessageFromClientUI(new Message(Api.ASK_SEATED_ORDER, userID));
		
	}

	public void setWeeklyHoursListener(Consumer<List<WeeklyHour>> callback) {
	    this.weeklyHoursCallback = callback;
	}

	public void askGetWeeklyHours() {
	    client.handleMessageFromClientUI(new Message(Api.ASK_GET_WEEKLY_HOURS, null));
	}

	public void setHolidaysListener(Consumer<List<Holiday>> callback) {
	    this.holidaysCallback = callback;
	}

	public void setWeeklyHours(List<WeeklyHour> hours) {
	    if (weeklyHoursCallback != null) {
	        Platform.runLater(() -> weeklyHoursCallback.accept(hours));
	    }
	}

	public void setHolidays(List<Holiday> holidays) {
		System.out.println("TableController: Setting holidays: " + holidays);
	    if (holidaysCallback != null) {
	    	System.out.println("TableController: Calling holidays callback.");
	        Platform.runLater(() -> holidaysCallback.accept(holidays));
	    }
	}


	
	
}
