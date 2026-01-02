package logic;

import java.util.ArrayList;
import java.util.List;

import comms.Api;
import comms.Message;
import entities.Order;
import entities.Table;

public class TableController {
	//****************************** Instance variables ******************************
	private final BistroClient client;
	private List<Order> occupiedTables;
	private Order userAllocatedOrderForTable;
	private int userAllocatedTable;
	//******************************** Constructors ***********************************//
	public TableController(BistroClient client) {
		this.client = client;
		this.occupiedTables = new ArrayList<>();
		this.userAllocatedOrderForTable = null;
		this.userAllocatedTable = 0;
	}
	
	//******************************** Getters And Setters ***********************************//	
	public List<Order> getOccupiedTables() {
		return occupiedTables;
	}
	
	public void setOccupiedTables(List<Order> occupiedTables) {
		this.occupiedTables = occupiedTables;
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
	
	
	
	//******************************** Instance Methods ***********************************//
	public boolean isCheckInTableSuccess() {
		return userAllocatedOrderForTable.getStatus() == enums.OrderStatus.SEATED;
	}


}
