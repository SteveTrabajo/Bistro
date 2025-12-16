package logic.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import logic.BistroDataBase_Controller;
import entities.Order;

/**
 * Lightweight repository implementation that reads used confirmation codes from
 * the existing orders table via `BistroDataBase_Controller`.
 *
 * Per project request, write operations are intentionally implemented as no-ops
 * (they log or throw if desired) because the user will manage DB writes.
 */
public class BistroOrderCodeRepository implements CodeRepository {

    @Override
    public List<String> loadUsedCodes() throws SQLException {
        List<String> used = new ArrayList<>();
        List<Order> orders = BistroDataBase_Controller.getAllOrders();
        if (orders == null) return used;
        for (Order o : orders) {
            // store as zero-padded 5-digit string to match manager formatting
            used.add(String.format("%05d", o.getConfirmationCode()));
        }
        return used;
    }

    @Override
    public void insertAssignment(String code, int userId) throws SQLException {
        // Intentionally a no-op. The caller requested no DB writes from this component.
        System.out.println("[BistroOrderCodeRepository] insertAssignment called for code=" + code + " userId=" + userId + " (no-op)");
    }

    @Override
    public void insertBatch(List<String> codes) throws SQLException {
        // Intentionally a no-op.
        System.out.println("[BistroOrderCodeRepository] insertBatch called for " + codes.size() + " codes (no-op)");
    }

    @Override
    public void releaseCodeFromDB(String code) throws SQLException {
        // Intentionally a no-op.
        System.out.println("[BistroOrderCodeRepository] releaseCodeFromDB called for code=" + code + " (no-op)");
    }

    @Override
    public int getUserIdByCode(String code) throws SQLException {
        try {
            int conf = Integer.parseInt(code);
            Order o = BistroDataBase_Controller.getOrderByConfirmationCode(conf);
            if (o == null) return -1;
            return o.getMemberID();
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
