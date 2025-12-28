package logic;

import java.util.ArrayList;
import java.util.List;

import entities.Order;
import entities.Table;

public class Table_Controller {
	//****************************** Instance variables ******************************
	private final BistroClient client;
	private List<Order> occupiedTables;
	private Order userAllocatedOrderForTable;
	private Table userAllocatedTable;
	//******************************** Constructors ***********************************//
	public Table_Controller(BistroClient client) {
		this.client = client;
		this.occupiedTables = new ArrayList<>();
		this.userAllocatedOrderForTable = null;
		this.userAllocatedTable = null;
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

	public Table getUserAllocatedTable() {
		return userAllocatedTable;
	}

	public void setUserAllocatedTable(Table userAllocatedTable) {
		this.userAllocatedTable = userAllocatedTable;
	}
	
	
	
	//******************************** Instance Methods ***********************************//
	
}
