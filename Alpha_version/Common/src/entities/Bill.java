package entities;

import java.io.Serializable;
import java.sql.Time;

import enums.BillType;
import enums.UserType;

public class Bill implements Serializable {
    private static final long serialVersionUID = 1L;

    private int tableId;
    private int orderNumber;
    private String confirmationCode;
    private UserType userType;
    private Time date;
    private double total;
    private BillType billType;

    // Constructor
    public Bill(int tableId, int orderNumber, String confirmationCode, UserType userType, Time date, double total) {
        this.tableId = tableId;
        this.orderNumber = orderNumber;
        this.confirmationCode = confirmationCode;
        this.userType = userType;
        this.date = date;
        this.total = total;
    }

    // Getters
    public int getTableId() { return tableId; }
    public int getOrderNumber() { return orderNumber; }
    public String getConfirmationCode() { return confirmationCode; }
    public UserType getUserType() { return userType; }
    public Time getDate() { return date; }
    public double getTotal() { return total; }
    public BillType getBillType() { return billType; }

    // Setters
    public void setTableId(int tableId) { this.tableId = tableId; }
    public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public void setDate(Time date) { this.date = date; }
    public void setTotal(double total) { this.total = total; }
    public void setBillType(BillType billType) { this.billType = billType; }


}
