package logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

import comms.Api;
import comms.Message;
import entities.Order;
import entities.Table;
import enums.OrderStatus;

public class TableController {
	//****************************** Instance variables ******************************
	private final BistroClient client;
	private HashMap<Table,String> tableStatuses;
	private Order seatdOrderDTO;
	private BiConsumer<Boolean, String> checkInListener;
	private int userAllocatedTable;
	private int tablesAmount;
	
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
	
	//******************************** Instance Methods ***********************************//
	public boolean isCheckInTableSuccess() {
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
}
