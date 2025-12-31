package logic;

public class Payment_Controller {
	private final BistroClient client;
	private double amountPaid;
	private enum PaymentStatus {
		PENDING,
		COMPLETED,
		FAILED
	}
	private String paymentstatus;
	private double taxRate= 0.18; // tax rate of 18%
	private double discountRate= 0.1; // discount rate of 10%
	private Object orderItemsForBilling;
	public Payment_Controller(BistroClient client) {
		this.client = client;
		this.amountPaid = 0.0;
		this.paymentstatus = PaymentStatus.PENDING.name();
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
		return amount * (1-discountRate);
	}
	public void setOrderItems(Object orderItems) {
		this.orderItemsForBilling = orderItems;
	}
	
	public boolean processPaymentCompleted() {
		return this.getPaymentStatus().equals(PaymentStatus.COMPLETED.name());
	}
	public Object getOrderItems() {
		return this.orderItemsForBilling;
	}
	public void checkpaymentSuccess(double amountPaid) {
		client.handleMessageFromClientUI(new comms.Message(comms.Api.ASK_PAYMENT_COMPLETE, amountPaid));
	}
}
