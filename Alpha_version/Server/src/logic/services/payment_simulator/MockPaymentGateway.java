package logic.services.payment_simulator;

/**
 * A mock implementation for development and testing.
 * Simulates a successful payment response.
 */
public class MockPaymentGateway implements PaymentGateway {
	
	/**
	 * Simulates processing a payment request.
	 * @param amount The amount to charge.
	 * @param paymentToken The credit card token or payment details.
	 * @return A fake transaction ID.
	 */
    @Override
    public String processPayment(double amount, String paymentToken) {
        // Simulate processing time
        System.out.println("[MockGateway] Processing payment of $" + amount + " with token: " + paymentToken);
        
        // Return a fake transaction ID
        return "MOCK-TRANS-" + System.currentTimeMillis(); 
    }
}
