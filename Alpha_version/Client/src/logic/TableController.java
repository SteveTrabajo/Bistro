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
	private Order userAllocatedOrderForTable;
	private int userAllocatedTable;
	private BiConsumer<Boolean, String> checkInListener;
	
	//******************************** Constructors ***********************************//
	public TableController(BistroClient client) {
		this.client = client;
		this.tableStatuses = new HashMap<>();
		this.userAllocatedOrderForTable = null;
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
		return userAllocatedOrderForTable;
	}
	
	public void setUserAllocatedOrderForTable(Order userAllocatedOrderForTable) {
		this.userAllocatedOrderForTable = userAllocatedOrderForTable;
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
	
	//******************************** Instance Methods ***********************************//
	public boolean isCheckInTableSuccess() {
		return userAllocatedOrderForTable.getStatus() == OrderStatus.SEATED;
	}

	public void clearCurrentTable() {
		this.userAllocatedOrderForTable = null;
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
}
