package logic.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import logic.BistroDataBase_Controller;
import entities.Order;

/**
 * Repository implementation bridging CodeManager with BistroDataBase_Controller.
 * * NOTE: Write operations (insert/release) are NO-OPs by design.
 * The assumption is that the calling Controller creates an Order object 
 * with the assigned code and saves it to the DB separately.
 */
public class BistroOrderCodeRepository implements CodeRepository {

    @Override
    public List<String> loadUsedCodes() throws SQLException {
        List<Order> orders = BistroDataBase_Controller.getAllOrders();
        
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> used = new ArrayList<>(orders.size());
        for (Order o : orders) {
            // Check for valid code range before adding to prevent format errors
            int code = o.getConfirmationCode();
            if (code >= 0) {
                used.add(String.format("%05d", code));
            }
        }
        return used;
    }

    @Override
    public void insertAssignment(String code, int userId) throws SQLException {
        // Log explicitly that this is a placeholder. 
        // The CodeManager thinks the data is saved, but it relies on external Order saving.
        // Debug level logging recommended here.
    }

    @Override
    public void insertBatch(List<String> codes) throws SQLException {
        // No-Op: We don't pre-save orders in this system.
    }

    @Override
    public void releaseCodeFromDB(String code) throws SQLException {
        // No-Op: Assuming deleting/canceling the order is handled elsewhere.
        // If CodeManager releases a code, but the Order still exists in DB, 
        // the code will reappear as "used" on next server restart.
    }

    @Override
    public int getUserIdByCode(String code) throws SQLException {
        try {
            int conf = Integer.parseInt(code);
            Order o = BistroDataBase_Controller.getOrderByConfirmationCode(conf);
            if (o == null) return -1;
            return o.getMemberID();
        } catch (NumberFormatException ex) {
            System.err.println("[BistroRepo] Invalid code format for lookup: " + code);
            return -1;
        }
    }
}