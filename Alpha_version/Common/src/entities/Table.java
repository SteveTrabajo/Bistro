package entities;

public class Table {
	//****************************** Instance variables ******************************/
	private int tableID;
	private int capacity;
	private boolean isAvailable;
	//******************************** Constructors ***********************************//
	public Table(int tableNumber, int capacity, boolean isAvailable) {
		this.tableID = tableNumber;
		this.capacity = capacity;
		this.isAvailable = isAvailable;
	}
	//******************************** Getters And Setters ***********************************//
	public int getTableID() {
		return tableID;
	}
	
	public void setTableID(int tableNumber) {
		this.tableID = tableNumber;
	}
	
	public int getCapacity() {
		return capacity;
	}
	
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	public boolean isAvailable() {
		return isAvailable;
	}
	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
	
	
}
