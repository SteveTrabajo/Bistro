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
	public boolean processPaymentCompleted() {
		return this.getPaymentStatus().equals(PaymentStatus.COMPLETED.name());
	}
}
