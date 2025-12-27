package entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private int orderNumber;
    private LocalDate orderDate;
    private LocalTime orderHour;
    private int dinersAmount;
    private int confirmationCode;
    private int userId;
    private boolean orderActive;
    private boolean waitList;
    private LocalDate dateOfPlacingOrder;

    public Order() {
    }

    public Order(int orderNumber, LocalDate orderDate, LocalTime orderHour, int dinersAmount,
                 int confirmationCode, int userId, boolean orderActive, boolean waitList, LocalDate dateOfPlacingOrder) {

        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.orderHour = orderHour;
        this.dinersAmount = dinersAmount;
        this.confirmationCode = confirmationCode;
        this.userId = userId;
        this.orderActive = orderActive;
        this.waitList = waitList;
        this.dateOfPlacingOrder = dateOfPlacingOrder;
    }

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

    public int getMemberId() { return userId; }
    public void setMemberId(int memberId) { this.userId = memberId; }

    public boolean isOrderActive() { return orderActive; }
    public void setOrderActive(boolean orderActive) { this.orderActive = orderActive; }

    public boolean isWaitList() { return waitList; }
    public void setWaitList(boolean waitList) { this.waitList = waitList; }

    public LocalDate getDateOfPlacingOrder() { return dateOfPlacingOrder; }
    public void setDateOfPlacingOrder(LocalDate dateOfPlacingOrder) {
        this.dateOfPlacingOrder = dateOfPlacingOrder;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderNumber=" + orderNumber +
                ", orderDate=" + orderDate +
                ", orderHour=" + orderHour +
                ", dinersAmount=" + dinersAmount +
                ", confirmationCode=" + confirmationCode +
                ", memberId=" + userId +
                ", orderActive=" + orderActive +
                ", waitList=" + waitList +
                ", dateOfPlacingOrder=" + dateOfPlacingOrder +
                '}';
    }
}
