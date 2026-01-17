package entities;

import java.io.Serializable;

/*
 * Represents a table in a restaurant with its details.
 */
public class Table implements Serializable{
	 private static final long serialVersionUID = 1L;

	// ****************************** Instance variables ******************************
	private int tableID;
	private int capacity;
	private boolean occupiedNow;

	// ****************************** Constructors ******************************
	/*
	 * Creates a Table instance.
	 * @param tableID      the table ID
	 * @param capacity     the seating capacity of the table
	 * @param occupiedNow  whether the table is currently occupied
	 */
	public Table(int tableID, int capacity, boolean occupiedNow) {
		this.tableID = tableID;
		this.capacity = capacity;
		this.occupiedNow = occupiedNow;
	}
	
	/*
	 * Creates a Table instance with default occupiedNow as false.
	 * @param tableID      the table ID
	 * @param capacity     the seating capacity of the table
	 */
	public Table(int tableID, int capacity) {
		this.tableID = tableID;
		this.capacity = capacity;
		this.occupiedNow = false;
	}

	// ****************************** Getters / Setters ******************************
	/*
	 * Gets the table ID.
	 * @return the table ID
	 */
	public int getTableID() {
		return tableID;
	}

	/*
	 * Sets the table ID.
	 * @param tableID the table ID
	 */
	public void setTableID(int tableID) {
		this.tableID = tableID;
	}

	/*
	 * Gets the seating capacity of the table.
	 * @return the seating capacity
	 */
	public int getCapacity() {
		return capacity;
	}

	/*
	 * Sets the seating capacity of the table.
	 * @param capacity the seating capacity
	 */
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

	/*
	 * Sets whether the table is currently occupied.
	 * @param occupiedNow true if the table is occupied, false otherwise
	 */
	public void setOccupiedNow(boolean occupiedNow) {
		this.occupiedNow = occupiedNow;
	}
	
	/*
	 * Gets the table number.
	 * @return the table number
	 */
	public void setTableNumber(int tableNumber) {
		this.tableID = tableNumber;
	}
}
// end of Table.java