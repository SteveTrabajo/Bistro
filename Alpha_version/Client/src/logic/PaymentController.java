package logic;

import java.util.List;

import comms.Api;
import comms.Message;
import entities.Bill;
import entities.Item;
import entities.Order;

public class PaymentController {
	private final BistroClient client;
	private double amountPaid;

	public enum PaymentStatus {
		PENDING, COMPLETED, FAILED
	}

	private String paymentstatus;
	private double taxRate = 0.18; // tax rate of 18%
	private double discountRate = 0.1; // discount rate of 10%
	private Object orderItemsForBilling;
	private boolean isPaymentManuallySuccessful = false;
	private List<Bill> pendingBills;
	private List<Item> billItemsList;

	public PaymentController(BistroClient client) {
		this.client = client;
		this.amountPaid = 0.0;
		this.paymentstatus = PaymentStatus.PENDING.name();
		this.orderItemsForBilling = null;
	}
	
	public boolean clearPaymentController() {
		this.amountPaid = 0.0;
		this.paymentstatus = null;
		this.orderItemsForBilling = null;
		this.isPaymentManuallySuccessful = false;
		this.pendingBills = null;
		this.billItemsList = null;
		return true;
	}

	public void setPaymentAmount(double amount) {
		this.amountPaid = amount;
	}

	public double getPaymentAmount() {
		return this.amountPaid;
	}

	public String getPaymentStatus() {
		return this.paymentstatus;
	}

	public void setPaymentStatus(String status) {
		this.paymentstatus = status;
	}

	public double calculateTax(double amount) {
		return amount * taxRate;
	}

	public double calculateDiscount(double amount) {
		return amount * (1 - discountRate);
	}

	public void setOrderItems(Object orderItems) {
		this.orderItemsForBilling = orderItems;
	}

	public Object getOrderItems() {
		return this.orderItemsForBilling;
	}
	
	public void setBillItemsList(List<Item> items) {
		this.billItemsList = items;
	}
	
	public List<Item> getBillItemsList() {
		return this.billItemsList;
	}

	public boolean processPaymentCompleted() {
		return this.getPaymentStatus().equals(PaymentStatus.COMPLETED.name());
	}

	public void checkpaymentSuccess(List<Item> randomItems) {
		client.handleMessageFromClientUI(new Message(Api.ASK_PAYMENT_COMPLETE, randomItems));
	}

	public void processPayment(int orderNumber) {
		client.handleMessageFromClientUI(new Message(Api.ASK_PROCESS_PAYMENT_MANUALLY, orderNumber));

	}

	public void setIsPaymentManuallySuccessful(boolean isSuccessful) {
		this.isPaymentManuallySuccessful = isSuccessful;

	}

	public boolean getIsPaymentManuallySuccessful() {
		return this.isPaymentManuallySuccessful;
	}

	public void loadPendingBills() {
		client.handleMessageFromClientUI(new Message(Api.ASK_LOAD_PENDING_BILLS, null));

	}

	public List<Bill> getPendingBills() {
		return this.pendingBills;
	}

	public boolean isBillsLoaded() {
		if (this.pendingBills != null) {
			return true;
		}
		return false;
	}

	public void setPendingBills(List<Bill> data) {
		this.pendingBills = data;

	}

	public void askBillItemsList(int orderId) {
		client.handleMessageFromClientUI(new Message(Api.ASK_BILL_ITEMS_LIST, orderId));
		
	}
	
	
}
