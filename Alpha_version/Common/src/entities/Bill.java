package entities;

import java.io.Serializable;
import java.sql.Timestamp; 
import enums.BillType;
import enums.UserType;

public class Bill implements Serializable {

	private static final long serialVersionUID = 1L;

    private int billID;            
    private String transactionId;   
    private String paymentStatus;   
    private int tableId;
    private int orderNumber;
    private String confirmationCode;
    private UserType userType;
    private Timestamp date;         
    private double total;
    private BillType billType;

    /*
	 * Creates a Bill instance with detailed information.
	 * @param billID            the bill identifier
	 * @param tableId          the table identifier
	 * @param orderNumber      the order number
	 * @param confirmationCode the confirmation code
	 * @param userType         the type of user
	 * @param date             the date of the bill
	 * @param total            the total amount
	 * @param paymentStatus    the payment status
	 */
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

    /*
     * Creates a Bill instance with essential information.
     * @param billID         the bill identifier
     * @param total          the total amount
     * @param paymentStatus  the payment status
     * @param transactionId  the transaction identifier
     */
    public Bill(int billID, double total, String paymentStatus, String transactionId) {
        this.billID = billID;
        this.total = total;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
    }

    // --- Getters and Setters ---

    /*
     * Gets the bill identifier.
     * @return the billID
     */
	public int getBillID() {
		return billID;
	}

	/*
	 * Sets the bill identifier.
	 * @param billID the billID to set
	 */
	public void setBillID(int billID) {
		this.billID = billID;
	}

	/*
	 * Gets the transaction identifier.
	 * @return the transactionId
	 */
	public String getTransactionId() {
		return transactionId;
	}

	/*
	 * Sets the transaction identifier.
	 * @param transactionId the transactionId to set
	 */
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	/*
	 * Gets the payment status.
	 * @return the paymentStatus
	 */
	public String getPaymentStatus() {
		return paymentStatus;
	}

	/*
	 * Sets the payment status.
	 * @param paymentStatus the paymentStatus to set
	 */
	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	/*
	 * Gets the table identifier.
	 * @return the tableId
	 */
	public int getTableId() {
		return tableId;
	}

	/*
	 * Sets the table identifier.
	 * @param tableId the tableId to set
	 */
	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

	/*
	 * Gets the order number.
	 * @return the orderNumber
	 */
	public int getOrderNumber() {
		return orderNumber;
	}

	/*
	 * Sets the order number.
	 * @param orderNumber the orderNumber to set
	 */
	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}

	/*
	 * Gets the confirmation code.
	 * @return the confirmationCode
	 */
	public String getConfirmationCode() {
		return confirmationCode;
	}

	/*
	 * Sets the confirmation code.
	 * @param confirmationCode the confirmationCode to set
	 */
	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	/*
	 * Gets the user type.
	 * @return the userType
	 */
	public UserType getUserType() {
		return userType;
	}

	/*
	 * Sets the user type.
	 * @param userType the userType to set
	 */
	public void setUserType(UserType userType) {
		this.userType = userType;
	}

	/*
	 * Gets the date.
	 * @return the date
	 */
    public Timestamp getDate() { 
    	return date; 
    }
    
    /*
     * Sets the date.
     * @param date the date to set
     */
    public void setDate(Timestamp date) { 
    	this.date = date; 
    }
    
	/*
	 * Gets the total amount.
	 * @return the total
	 */
    public double getTotal() { 
    	return total; 
    }
    
    /*
     * Sets the total amount.
     * @param total the total to set
     */
    public void setTotal(double total) { 
    	this.total = total; 
    }

    /*
     * Gets the bill type.
     * @return the billType
     */
    public BillType getBillType() { 
    	return billType; 
    }
    
    /*
	 * Sets the bill type.
	 * @param billType the billType to set
	 */
    public void setBillType(BillType billType) { 
    	this.billType = billType; 
    }
}
// end of Bill.java