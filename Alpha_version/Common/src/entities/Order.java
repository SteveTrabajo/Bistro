package entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import enums.OrderStatus;
import enums.OrderType;

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
	
	
	
	//constructor for display order
	public Order(LocalTime orderHour, int dinersAmount)
	{
		this.orderHour = orderHour;
		this.dinersAmount = dinersAmount;
	}

	// Convenience flags
	public boolean isWaitList() {
		return orderType == OrderType.WAITLIST;
	}

	public boolean isOrderActive() {
		return status == OrderStatus.PENDING || status == OrderStatus.NOTIFIED || status == OrderStatus.SEATED;
	}

	// ---- getters/setters ----
	public int getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}

	public LocalDate getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(LocalDate orderDate) {
		this.orderDate = orderDate;
	}

	public LocalTime getOrderHour() {
		return orderHour;
	}

	public void setOrderHour(LocalTime orderHour) {
		this.orderHour = orderHour;
	}

	public int getDinersAmount() {
		return dinersAmount;
	}

	public void setDinersAmount(int dinersAmount) {
		this.dinersAmount = dinersAmount;
	}

	public String getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public LocalDateTime getDateOfPlacingOrder() {
		return dateOfPlacingOrder;
	}

	public void setDateOfPlacingOrder(LocalDateTime dateOfPlacingOrder) {
		this.dateOfPlacingOrder = dateOfPlacingOrder;
	}

	public OrderType getOrderType() {
		return orderType;
	}

	public void setOrderType(OrderType orderType) {
		this.orderType = orderType;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}
	
	public Item[] getOrderedItems() {
		return orderedItems;
	}
	
	public void setOrderedItems(Item[] orderedItems) {
		this.orderedItems = orderedItems;
	}

	public String getIdempotencyKey() {// for payment processing
		return this.idempotencyKey; //
	}
	
	public void setIdempotencyKey(String idempotencyKey) {// for payment processing
		this.idempotencyKey = idempotencyKey;
	}
	
	public int getTableId() {
		return tableID;
	}
	
	public void setTableId(int tableID) {
		this.tableID = tableID;
	}
	
	public String getUserTypeStr() {
		return userTypeStr;
	}
	
	public void setUserTypeStr(String userTypeStr) {
		this.userTypeStr = userTypeStr;
	}
}
