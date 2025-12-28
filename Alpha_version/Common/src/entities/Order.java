package entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

import enums.OrderStatus;
import enums.OrderType;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private int orderNumber;
    private LocalDate orderDate;          // NULL for waitlist
    private LocalTime orderHour;          // NULL for waitlist
    private int dinersAmount;
    private int confirmationCode;

    // This field existed in your old schema as member_id.
    // Now it’s a user_id for both guests and members.
    private int userId;

    private LocalDate dateOfPlacingOrder;

    private OrderType orderType;          // RESERVATION / WAITLIST
    private OrderStatus status;           // PENDING / ...

    public Order() {}

    public Order(int orderNumber,
                 LocalDate orderDate,
                 LocalTime orderHour,
                 int dinersAmount,
                 int confirmationCode,
                 int userId,
                 OrderType orderType,
                 OrderStatus status,
                 LocalDate dateOfPlacingOrder) {

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

    // Convenience flags
    public boolean isWaitList() {
        return orderType == OrderType.WAITLIST;
    }

    public boolean isOrderActive() {
        return status == OrderStatus.PENDING
            || status == OrderStatus.NOTIFIED
            || status == OrderStatus.SEATED;
    }

    // ---- getters/setters ----
    public int getOrderNumber() { return orderNumber; }
    public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public LocalTime getOrderHour() { return orderHour; }
    public void setOrderHour(LocalTime orderHour) { this.orderHour = orderHour; }

    public int getDinersAmount() { return dinersAmount; }
    public void setDinersAmount(int dinersAmount) { this.dinersAmount = dinersAmount; }

    public int getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(int confirmationCode) { this.confirmationCode = confirmationCode; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getDateOfPlacingOrder() { return dateOfPlacingOrder; }
    public void setDateOfPlacingOrder(LocalDate dateOfPlacingOrder) { this.dateOfPlacingOrder = dateOfPlacingOrder; }

    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
