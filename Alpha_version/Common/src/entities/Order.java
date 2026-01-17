package entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import enums.OrderStatus;
import enums.OrderType;

/*
 * Represents an order with its details.
 */
public class Order implements Serializable {
	private static final long serialVersionUID = 1L;

	private int orderNumber;
	private int userId;
	private LocalDate orderDate; // NULL for waitlist
	private LocalTime orderHour; // NULL for waitlist
	private int dinersAmount;
	private String confirmationCode;
	private Item[] orderedItems=null;
	private String idempotencyKey;
	private int tableID; // temp var, doesnt get saved into DB
	private LocalDateTime dateOfPlacingOrder;

	private String userTypeStr; // temp var, doesnt get saved into DB
	private OrderType orderType; // RESERVATION / WAITLIST
	private OrderStatus status; // PENDING / ...
	// ---- constructors ----		

	public Order() {} // default constructor
	
	/*
	 * Creates an Order instance with detailed information.
	 * @param orderNumber         the order number
	 * @param orderDate           the date of the order
	 * @param orderHour           the hour of the order
	 * @param dinersAmount        the number of diners
	 * @param confirmationCode    the confirmation code
	 * @param userId              the user ID
	 * @param orderType           the type of order
	 * @param status              the status of the order
	 * @param dateOfPlacingOrder  the date and time when the order was placed
	 */
	public Order(int orderNumber, LocalDate orderDate, LocalTime orderHour, int dinersAmount, String confirmationCode,
			int userId, OrderType orderType, OrderStatus status, LocalDateTime dateOfPlacingOrder) {

		this.orderNumber = orderNumber;
		this.orderDate = orderDate;
		this.orderHour = orderHour;
		this.dinersAmount = dinersAmount;
		this.confirmationCode = confirmationCode;
		this.userId = userId;
		this.orderType = orderType;
		this.status = status;
		this.dateOfPlacingOrder = dateOfPlacingOrder;
	}
	
	/*
	 * Creates an Order instance with essential information.
	 * @param userId            the user ID
	 * @param date              the date of the order
	 * @param dinersAmount     the number of diners
	 * @param time              the hour of the order
	 * @param confirmationCode  the confirmation code
	 * @param orderType         the type of order
	 * @param status            the status of the order
	 */
	public Order(int userId, LocalDate date, int dinersAmount, LocalTime time, String confirmationCode,
			OrderType orderType, OrderStatus status) {
		this.userId = userId;
		this.orderDate = date;
		this.dinersAmount = dinersAmount;
		this.orderHour = time;
		this.confirmationCode = confirmationCode;
		this.orderType = orderType;
		this.status = status;
	}
	
	/*
	 * Creates an Order instance with minimal information.
	 * @param orderHour      the hour of the order
	 * @param dinersAmount   the number of diners
	 */
	public Order(LocalTime orderHour, int dinersAmount)
	{
		this.orderHour = orderHour;
		this.dinersAmount = dinersAmount;
	}

	// Convenience flags
	/*
	 * Checks if the order is a reservation.
	 * @return true if the order type is RESERVATION, false otherwise
	 */
	public boolean isWaitList() {
		return orderType == OrderType.WAITLIST;
	}

	/*
	 * Checks if the order is active.
	 * @return true if the order status is PENDING, NOTIFIED, or SEATED; false otherwise
	 */
	public boolean isOrderActive() {
		return status == OrderStatus.PENDING || status == OrderStatus.NOTIFIED || status == OrderStatus.SEATED;
	}

	// ---- getters/setters ----
	/*
	 * Gets the order number.
	 * @return the order number
	 */
	public int getOrderNumber() {
		return orderNumber;
	}

	/*
	 * Sets the order number.
	 * @param orderNumber the order number to set
	 */
	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}

	/*
	 * Gets the order date.
	 * @return the order date
	 */
	public LocalDate getOrderDate() {
		return orderDate;
	}

	/*
	 * Sets the order date.
	 * @param orderDate the order date to set
	 */
	public void setOrderDate(LocalDate orderDate) {
		this.orderDate = orderDate;
	}

	/*
	 * Gets the order hour.
	 * @return the order hour
	 */
	public LocalTime getOrderHour() {
		return orderHour;
	}

	/*
	 * Sets the order hour.
	 * @param orderHour the order hour to set
	 */
	public void setOrderHour(LocalTime orderHour) {
		this.orderHour = orderHour;
	}

	/*
	 * Gets the number of diners.
	 * @return the number of diners
	 */
	public int getDinersAmount() {
		return dinersAmount;
	}

	/*
	 * Sets the number of diners.
	 * @param dinersAmount the number of diners to set
	 */
	public void setDinersAmount(int dinersAmount) {
		this.dinersAmount = dinersAmount;
	}

	/*
	 * Gets the confirmation code.
	 * @return the confirmation code
	 */
	public String getConfirmationCode() {
		return confirmationCode;
	}

	/*
	 * Sets the confirmation code.
	 * @param confirmationCode the confirmation code to set
	 */
	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	/*
	 * Gets the user ID.
	 * @return the user ID
	 */
	public int getUserId() {
		return userId;
	}

	/*
	 * Sets the user ID.
	 * @param userId the user ID to set
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/*
	 * Gets the date and time when the order was placed.
	 * @return the date and time of placing the order
	 */
	public LocalDateTime getDateOfPlacingOrder() {
		return dateOfPlacingOrder;
	}

	/*
	 * Sets the date and time when the order was placed.
	 * @param dateOfPlacingOrder the date and time to set
	 */
	public void setDateOfPlacingOrder(LocalDateTime dateOfPlacingOrder) {
		this.dateOfPlacingOrder = dateOfPlacingOrder;
	}

	/*
	 * Gets the order type.
	 * @return the order type
	 */
	public OrderType getOrderType() {
		return orderType;
	}

	/*
	 * Sets the order type.
	 * @param orderType the order type to set
	 */
	public void setOrderType(OrderType orderType) {
		this.orderType = orderType;
	}

	/*
	 * Gets the order status.
	 * @return the order status
	 */
	public OrderStatus getStatus() {
		return status;
	}

	/*
	 * Sets the order status.
	 * @param status the order status to set
	 */
	public void setStatus(OrderStatus status) {
		this.status = status;
	}
	
	/*
	 * Gets the ordered items.
	 * @return the ordered items
	 */
	public Item[] getOrderedItems() {
		return orderedItems;
	}
	
	/*
	 * Sets the ordered items.
	 * @param orderedItems the ordered items to set
	 */
	public void setOrderedItems(Item[] orderedItems) {
		this.orderedItems = orderedItems;
	}

	/*
	 * Gets the idempotency key.
	 * @return the idempotency key
	 */
	public String getIdempotencyKey() {// for payment processing
		return this.idempotencyKey; //
	}
	
	/*
	 * Sets the idempotency key.
	 * @param idempotencyKey the idempotency key to set
	 */
	public void setIdempotencyKey(String idempotencyKey) {// for payment processing
		this.idempotencyKey = idempotencyKey;
	}
	
	/*
	 * Gets the table identifier.
	 * @return the table identifier
	 */
	public int getTableId() {
		return tableID;
	}
	
	/*
	 * Sets the table identifier.
	 * @param tableID the table identifier to set
	 */
	public void setTableId(int tableID) {
		this.tableID = tableID;
	}
	
	/*
	 * Gets the user type string.
	 * @return the user type string
	 */
	public String getUserTypeStr() {
		return userTypeStr;
	}
	
	/*
	 * Sets the user type string.
	 * @param userTypeStr the user type string to set
	 */
	public void setUserTypeStr(String userTypeStr) {
		this.userTypeStr = userTypeStr;
	}
}
// End of Order.java