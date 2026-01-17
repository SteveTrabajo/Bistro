package logic.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import entities.Bill;
import entities.Item;
import entities.User;
import enums.EndTableSessionType;
import enums.OrderStatus;
import enums.UserType;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;
import logic.services.payment_simulator.MockPaymentGateway;
import logic.services.payment_simulator.PaymentGateway;

/**
 * Service class to handle payment processing and bill management.
 */
public class PaymentService {

	/* Dependencies */
    private final BistroDataBase_Controller dbController;
    private final ServerLogger logger;
    private final TableService tableService;
    private final PaymentGateway paymentGateway;

    /**
	 * Constructor to initialize dependencies.
	 * @param dbController The database controller for data operations.
	 * @param logger The server logger for logging events.
	 * @param tableService The table service for managing table sessions.
	 * @param paymentGateway The payment gateway for processing payments.
	 */
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
        	total += item.getPrice() * item.getQuantity();
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
        Bill bill = dbController.getBillById(billId);
        if (bill == null) {
            logger.log("[ERROR] Bill not found: " + billId);
            return false;
        }

        // Check if already paid
        if ("PAID".equalsIgnoreCase(bill.getPaymentStatus())) {
            logger.log("[INFO] Bill " + billId + " is already paid.");
            return false;
        }

        // Process payment via Payment Gateway
        String transactionId = paymentGateway.processPayment(amount, creditCardToken);

        // If payment succeeded, update bill status
        if (transactionId != null) {
            dbController.markBillAsPaid(billId, "CREDIT", transactionId);
            finalizeOrderPayment(billId);
            logger.log("[SUCCESS] Bill " + billId + " paid. Transaction Ref: " + transactionId);
            return true;
        } else {
            logger.log("[FAILURE] Payment failed for Bill " + billId);
            return false;
        }
    }


    /**
	 * Processes a cash payment for a specific bill.
	 * @param billId The ID of the bill to pay.
	 * @param amount The amount paid in cash.
	 * @return true if payment succeeded, false otherwise.
	 */
    public boolean processCashPayment(int billId, double amount) {
        Bill bill = dbController.getBillById(billId);
        if (bill == null) return false;

        // Check if already paid
        if ("PAID".equalsIgnoreCase(bill.getPaymentStatus())) {
            logger.log("[INFO] Bill " + billId + " is already paid.");
            return false;
        }
        // Mark bill as paid in the database
        dbController.markBillAsPaid(billId, "CASH", null);
        finalizeOrderPayment(billId);
        logger.log("[SUCCESS] Bill " + billId + " paid via CASH.");
        return true;
    }

    /**
	 * Finalizes the order/session associated with a paid bill.
	 * @param billId The ID of the paid bill.
	 */
    private void finalizeOrderPayment(int billId) {
        try {
            Integer orderNum = dbController.getOrderNumberByBillId(billId);
            if (orderNum != null) {
                onPaymentCompleted(orderNum);
            } else {
                logger.log("[WARN] Could not find Order Number for Bill ID: " + billId);
            }
        } catch (Exception e) {
            logger.log("[ERROR] Failed to finalize order/session for bill " + billId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    /**
	 * Retrieves the Bill ID associated with a specific order number.
	 * @param orderNumber The order number to look up.
	 * @return The Bill ID if found, null otherwise.
	 */
    public Integer getBillIdByOrderNumber(int orderNumber) {
        return dbController.getBillIdByOrderNumber(orderNumber);
    }

    /**
     * Retrieves a Bill by its ID.
     * @param billId The ID of the bill to retrieve.
     * @return The Bill object, or null if not found.
     */
    public Bill getBillById(Integer billId) {
        if (billId == null) return null;
        return dbController.getBillById(billId);
    }

    /**
	 * Retrieves all pending bills for the current user.
	 * @return A list of pending Bill objects.
	 */
    public List<Bill> getPendingBillsForUser() {
        return dbController.getPendingBillsByUserId();
    }
    
    /**
     * Handles post-payment completion tasks such as closing table sessions.
     * @param orderNumber The order number associated with the completed payment.
     * @return true if successful, false otherwise.
     */
    public boolean onPaymentCompleted(int orderNumber) {
        Integer tableNum = dbController.getActiveTableNumByOrderNumber(orderNumber);
        dbController.closeTableSessionForOrder(orderNumber, EndTableSessionType.PAID);
        dbController.updateOrderStatusByOrderNumber(orderNumber, OrderStatus.COMPLETED); 

        // If tableNum is null, it means the session was already closed.
        if (tableNum == null) {
            logger.log("[INFO] Payment completed, but table session was already closed or not found for order " + orderNumber);
            return true; 
        }          
        
        // Notify TableService to free the table
        if (tableService != null) {
            tableService.tableFreed(tableNum);
        }
        
        return true;
    }
   

	
	//***************************************** Checkout Items List for Bill ID *****************************************//
    /**
	 * Generates a random list of items for a bill based on the number of diners.
	 * @param dinersAmount The number of diners to base item quantities on.
	 * @return A list of randomly selected Item objects.
	 */
	public List<Item> createRandomBillItems(int dinersAmount) {

		// Sample menu items
	    List<Item> items = new ArrayList<>();
	    Random random = new Random();

	    if (dinersAmount <= 0) {
	        dinersAmount = 1;
	    }
	    // Define a sample menu
	    Item[] menu = {
	        new Item(1, "Classic Burger", 55.0, 0),
	        new Item(2, "Margherita Pizza", 45.0, 0),
	        new Item(3, "Caesar Salad", 38.0, 0),
	        new Item(4, "Coca Cola", 12.0, 0),
	        new Item(5, "French Fries", 18.0, 0)
	    };
	    // Randomly select items and quantities
	    for (int i = 0; i < menu.length; i++) {
	        int quantity = random.nextInt(dinersAmount + 1);
	        if (quantity > 0) {
	            Item orderedItem = new Item(
	                menu[i].getItemId(),
	                menu[i].getName(),
	                menu[i].getPrice(),
	                quantity
	            );
	            items.add(orderedItem);
	        }
	    }
	    // Ensure at least one item is ordered
	    if (items.isEmpty()) { // Ensure at least one item is ordered
	        Item fallback = menu[random.nextInt(menu.length)];
	        items.add(new Item(
	            fallback.getItemId(),
	            fallback.getName(),
	            fallback.getPrice(),
	            1
	        ));
	    }
	    return items;
	}
}
// End of PaymentService.java