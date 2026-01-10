package logic.services.payment_simulator;
/**
 * PaymentGateway interface to simulate interaction with an external payment provider.
 */
public interface PaymentGateway {
	/**
     * Process a payment request.
     * @param amount The amount to charge.
     * @param paymentToken The credit card token or payment details.
     * @return The transaction ID if successful, or null if failed.
     */
    String processPayment(double amount, String paymentToken);
}
