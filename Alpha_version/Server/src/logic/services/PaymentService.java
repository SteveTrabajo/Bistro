package logic.services;

import java.util.List;
import entities.Bill;
import entities.Item;
import entities.User;
import enums.EndTableSessionType;
import enums.UserType;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;
import logic.services.payment_simulator.MockPaymentGateway;
import logic.services.payment_simulator.PaymentGateway;

public class PaymentService {

    private final BistroDataBase_Controller dbController;
    private final ServerLogger logger;
    private final TableService tableService;
    private final PaymentGateway paymentGateway;

    public PaymentService(BistroDataBase_Controller dbController, ServerLogger logger,TableService tableService) {
        this.dbController = dbController;
        this.logger = logger;
        // Initialize with the Mock gateway for now. 
        // In the future, this can be swapped for a RealPaymentGateway.
        this.paymentGateway = new MockPaymentGateway();
        this.tableService = tableService;
    }

    /**
     * Calculates the total bill amount including tax and discounts.
     * @param items The list of items ordered.
     * @param requester The user requesting the bill (for member discounts).
     * @return The final total amount as a double.
     */
    public double calculateTotal(List<Item> items, User requester) {
        double total = 0.0;
        for (Item item : items) {
            total += item.getPrice();
        }

        // Apply 10% discount for MEMBERS
        if (requester.getUserType() == UserType.MEMBER) {
            total = total * 0.90; 
        }

        // Add 18% Tax/VAT
        return total * 1.18;
    }

    /**
     * Processes a credit card payment for a specific bill.
     * @param billId The ID of the bill to pay.
     * @param amount The amount to charge.
     * @param creditCardToken The dummy token (or real token in the future).
     * @return true if payment succeeded, false otherwise.
     */
    public boolean processCreditCardPayment(int billId, double amount, String creditCardToken) {
        // 1. Validate the Bill exists and is UNPAID
        Bill bill = dbController.getBillById(billId);
        if (bill == null) {
            logger.log("[ERROR] Bill not found: " + billId);
            return false;
        }
        
        // Assuming your Bill entity has a method getPaymentStatus() returning a String or Enum
        if ("PAID".equals(bill.getPaymentStatus().toString())) {
            logger.log("[INFO] Bill " + billId + " is already paid.");
            return false;
        }

        // 2. Call the External Payment Gateway (Future-Proofing step)
        String transactionId = paymentGateway.processPayment(amount, creditCardToken);

        if (transactionId != null) {
            // 3. Success: Update Database with the Transaction ID
            dbController.markBillAsPaid(billId, "CREDIT", transactionId);
            logger.log("[SUCCESS] Bill " + billId + " paid. Transaction Ref: " + transactionId);
            return true;
        } else {
            logger.log("[FAILURE] Payment failed for Bill " + billId);
            return false;
        }
    }

    /**
     * Processes a manual payment (Cash).
     */
    public boolean processCashPayment(int billId, double amount) {
        Bill bill = dbController.getBillById(billId);
        if (bill == null) return false;

        // Mark as paid with 'CASH' and no external transaction ID
        dbController.markBillAsPaid(billId, "CASH", null);
        logger.log("[SUCCESS] Bill " + billId + " paid via CASH.");
        return true;
    }
    
    /**
     * Finds the bill ID linked to a specific Order Number.
     */
    public Integer getBillIdByOrderNumber(int orderNumber) {
        return dbController.getBillIdByOrderNumber(orderNumber);
    }

    /**
     * Retrieves the full Bill object by its ID.
     */
    public Bill getBillById(Integer billId) {
        if (billId == null) return null;
        return dbController.getBillById(billId);
    }

    /**
     * Gets all UNPAID bills associated with a specific user.
     */
    public List<Bill> getPendingBillsForUser(int userId) {
        return dbController.getPendingBillsByUserId(userId);
    }

    
    /**
	    * method to be called when payment is completed for an order
	    * @param orderNumber
	    */
	    public boolean onPaymentCompleted(int orderNumber) {
	    	//close table session when payment is completed
	        Integer tableNum = dbController.getActiveTableNumByOrderNumber(orderNumber);
	        dbController.closeTableSessionForOrder(orderNumber, EndTableSessionType.PAID);
	        //case no active table session found
	        if (tableNum == null) {
	            logger.log("[WARN] Payment completed but no active table session found for order " + orderNumber);
	            return false;
	        }	       
	        return tableService.tableFreed(tableNum); //when table is freed, try to seat WAITLIST/RESERVATION order if possible
	    }

	public List<Item> getBillItemsList(int orderNumber, User requester) {
		return dbController.getBillItemsList(orderNumber, requester);
	}
}