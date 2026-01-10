package entities;

import java.io.Serializable;
import java.sql.Timestamp; // Changed from Time to Timestamp to match SQL DATETIME
import enums.BillType;
import enums.UserType;

public class Bill implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- New/Updated Fields for Database Mapping ---
    private int billID;             // Matches SQL 'billID' (Primary Key)
    private String transactionId;   // Matches SQL 'transaction_id'
    private String paymentStatus;   // Matches SQL 'payment_status' ('PAID'/'UNPAID')
    
    // --- Existing Fields ---
    private int tableId;
    private int orderNumber;
    private String confirmationCode;
    private UserType userType;
    private Timestamp date;         // Changed to Timestamp
    private double total;
    private BillType billType;

    // --- Constructor 1: Full Constructor (Useful for UI/Creation) ---
    public Bill(int billID, int tableId, int orderNumber, String confirmationCode, 
                UserType userType, Timestamp date, double total, String paymentStatus) {
        this.billID = billID;
        this.tableId = tableId;
        this.orderNumber = orderNumber;
        this.confirmationCode = confirmationCode;
        this.userType = userType;
        this.date = date;
        this.total = total;
        this.paymentStatus = paymentStatus;
    }

    // --- Constructor 2: Simple Constructor for DB Controller (Minimal Data) ---
    // This matches the 'getBillById' method in your Controller
    public Bill(int billID, double total, String paymentStatus, String transactionId) {
        this.billID = billID;
        this.total = total;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
    }

    // --- Getters and Setters ---

    public int getBillID() { return billID; }
    public void setBillID(int billID) { this.billID = billID; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }

    public int getOrderNumber() { return orderNumber; }
    public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }

    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }

    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public BillType getBillType() { return billType; }
    public void setBillType(BillType billType) { this.billType = billType; }
}