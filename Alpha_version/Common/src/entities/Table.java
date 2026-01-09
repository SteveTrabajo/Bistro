package entities;

public class Table {

	// ****************************** Instance variables ******************************
	private int tableID;
	private int capacity;
	private boolean occupiedNow;

	// ****************************** Constructors ******************************
	public Table(int tableID, int capacity, boolean occupiedNow) {
		this.tableID = tableID;
		this.capacity = capacity;
		this.occupiedNow = occupiedNow;
	}
	
	public Table(int tableID, int capacity) {
		this.tableID = tableID;
		this.capacity = capacity;
		this.occupiedNow = false;
	}

	// ****************************** Getters / Setters ******************************
	public int getTableID() {
		return tableID;
	}

	public void setTableID(int tableID) {
		this.tableID = tableID;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	/**
	 * Indicates whether the table is currently occupied,
	 * NOT used for reservation availability calculations.
	 */
	public boolean isOccupiedNow() {
		return occupiedNow;
	}

	public void setOccupiedNow(boolean occupiedNow) {
		this.occupiedNow = occupiedNow;
	}
	public void setTableNumber(int tableNumber) {
		this.tableID = tableNumber;
	}
}
