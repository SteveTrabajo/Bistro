package logic;

import java.util.List;
import comms.Api;
import comms.Message;
import entities.Bill;
import entities.Item;
import enums.PaymentStatus;

/**
 * PaymentController handles payment processing, tax and discount calculations,
 * and manages payment status for orders in the bistro system.
 */
public class PaymentController {
	
	/***************************Instance Variables***************************/
	
	private final BistroClient client;
	private double amountPaid;
	private String paymentstatus;
	private double taxRate = 0.18; // tax rate of 18%
	private double discountRate = 0.1; // discount rate of 10%
	private boolean isPaymentManuallySuccessful = false;
	private List<Bill> pendingBills;
	private List<Item> billItemsList;
	
	/***************************Constructor***************************/
	
	/**
	 * Constructs a PaymentController with the specified BistroClient.
	 * 
	 * @param client the BistroClient instance for communication
	 */
	public PaymentController(BistroClient client) {
		this.client = client;
		this.amountPaid = 0.0;
		this.paymentstatus = PaymentStatus.PENDING.name();
	}
	
	/*************************** Getters & Setters ***************************/
	public boolean clearPaymentController() {
		this.amountPaid = 0.0;
		this.paymentstatus = null;
		this.isPaymentManuallySuccessful = false;
		this.pendingBills = null;
		this.billItemsList = null;
		return true;
	}
	
	/**
	 * Sets the payment amount.
	 * @param amount
	 */
	public void setPaymentAmount(double amount) {
		this.amountPaid = amount;
	}

	/**
	 * Gets the payment status.
	 * @return
	 */
	public String getPaymentStatus() {
		return this.paymentstatus;
	}
	
	/**
	 * Sets the payment status.
	 * @param status
	 */
	public void setPaymentStatus(String status) {
		this.paymentstatus = status;
	}

	/**
	 * Calculates the tax for a given amount.
	 * @param amount
	 * @return
	 */
	public double calculateTax(double amount) {
		return amount * taxRate;
	}

	/**
	 * Calculates the discount for a given amount including tax.
	 * @param amount
	 * @return
	 */
	public double calculateDiscount(double amount) {
		return (amount +  calculateTax(amount)) * discountRate;
	}
	
	/**
	 * Sets the bill items list.
	 * @param items
	 */
	public void setBillItemsList(List<Item> items) {
		this.billItemsList = items;
	}
	
	/**
	 * Gets the bill items list.
	 * @return
	 */
	public List<Item> getBillItemsList() {
		return this.billItemsList;
	}
	
	/*************************** Methods ***************************/
	
	/**
	 * Sets whether the payment was manually successful.
	 * @param isSuccessful
	 */
	public void setIsPaymentManuallySuccessful(boolean isSuccessful) {
		this.isPaymentManuallySuccessful = isSuccessful;

	}

	/**
	 * Gets whether the payment was manually successful.
	 * @return
	 */
	public boolean getIsPaymentManuallySuccessful() {
		return this.isPaymentManuallySuccessful;
	}

	/**
	 * Gets the list of pending bills.
	 * @return
	 */
	public List<Bill> getPendingBills() {
		return this.pendingBills;
	}

	/**
	 * Checks if pending bills are loaded.
	 * @return
	 */
	public boolean isBillsLoaded() {
		if (this.pendingBills != null) {
			return true;
		}
		return false;
	}
	
	/**
	 * Sets the list of pending bills.
	 * @param data
	 */
	public void setPendingBills(List<Bill> data) {
		this.pendingBills = data;

	}
	
	/**
	 * Processes payment completion status.
	 * @return
	 */
	public boolean processPaymentCompleted() {
		return this.getPaymentStatus().equals(PaymentStatus.COMPLETED.name());
	}
	
	/**
	 * Checks payment success by sending a message to the client UI.
	 * @param randomItems
	 */
	public void checkpaymentSuccess(List<Item> randomItems) {
		client.handleMessageFromClientUI(new Message(Api.ASK_PAYMENT_COMPLETE, randomItems));
	}
	
	/**
	 * Processes payment manually by sending a message to the client UI.
	 * @param orderNumber
	 * @param paymentMethod
	 */
	public void processPayment(int orderNumber, String paymentMethod) {
		Object[] payload = new Object[] { orderNumber, paymentMethod };
		client.handleMessageFromClientUI(new Message(Api.ASK_PROCESS_PAYMENT_MANUALLY, payload));
	}
	
	/**
	 * Loads pending bills by sending a message to the client UI.
	 */
	public void loadPendingBills() {
		client.handleMessageFromClientUI(new Message(Api.ASK_LOAD_PENDING_BILLS, null));
		
	}

	/**
	 * Asks for the bill items list for a given order ID by sending a message to the client UI.
	 * @param orderId
	 */
	public void askBillItemsList(int orderId) {
		client.handleMessageFromClientUI(new Message(Api.ASK_BILL_ITEMS_LIST, orderId));
		
	}
}
//End of PaymentController.java