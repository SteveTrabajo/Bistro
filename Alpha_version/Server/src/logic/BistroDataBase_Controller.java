package logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import entities.Order;
import entities.User;
import enums.UserType;

/**
 * Matches the normalized schema you applied:
 *   users(user_id, phoneNumber, email, type)
 *   members(user_id, member_code, f_name, l_name)
 *   orders(order_number, confirmation_code, user_id, number_of_guests,
 *          order_date, order_time, date_of_placing_order, order_type, status,
 *          notified_at, cancelled_at)
 */
public class BistroDataBase_Controller {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "Aa123456";

    private static final int POOL_SIZE = 10;
    private static final long BORROW_TIMEOUT_MS = 10_000;

    private static BlockingQueue<Connection> pool = null;
    private static volatile boolean initialized = false;

    public static synchronized boolean openConnection() {
        if (initialized) return true;

        try {
            pool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) {
                Connection c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
                c.setAutoCommit(true);
                pool.offer(c);
            }
            initialized = true;
            ServerLogger.log("SQL connection pool initialized. Size=" + POOL_SIZE);
            return true;
        } catch (SQLException ex) {
            ServerLogger.log("Failed to initialize SQL connection pool: " + ex.getMessage());
            ex.printStackTrace();
            closeConnection();
            return false;
        }
    }

    public static synchronized void closeConnection() {
        initialized = false;
        if (pool == null) return;

        Connection c;
        while ((c = pool.poll()) != null) {
            try { c.close(); } catch (SQLException ignored) {}
        }
        pool = null;
        ServerLogger.log("SQL connection pool closed");
    }

    private static Connection borrow() throws SQLException {
        if (!initialized || pool == null) {
            throw new SQLException("DB pool not initialized. Call openConnection() first.");
        }
        try {
            Connection c = pool.poll(BORROW_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (c == null) throw new SQLException("Timed out waiting for a DB connection from the pool.");

            if (c.isClosed() || !c.isValid(2)) {
                try { c.close(); } catch (SQLException ignored) {}
                c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
                c.setAutoCommit(true);
            }
            return c;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a DB connection.", ie);
        }
    }

    private static void release(Connection c) {
        if (c == null) return;

        if (!initialized || pool == null) {
            try { c.close(); } catch (SQLException ignored) {}
            return;
        }

        try {
            if (c.isClosed()) return;
            if (!pool.offer(c)) c.close();
        } catch (SQLException ignored) {}
    }

    // Orders

    public static Order getOrderByConfirmationCode(int confCode) {
        final String sql =
                "SELECT "
                        + "order_number, order_date, order_time, number_of_guests, confirmation_code, user_id, date_of_placing_order, "
                        + "(order_type = 'WAITLIST') AS wait_list, "
                        + "(status IN ('PENDING','NOTIFIED','SEATED')) AS order_active "
                        + "FROM orders WHERE confirmation_code = ?";

        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, confCode);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) return null;

                    int orderNumber = rs.getInt("order_number");

                    java.sql.Date od = rs.getDate("order_date");
                    java.sql.Time ot = rs.getTime("order_time");
                    LocalDate orderDate = (od == null) ? null : od.toLocalDate();
                    LocalTime orderTime = (ot == null) ? null : ot.toLocalTime();

                    int diners = rs.getInt("number_of_guests");
                    int confirmationCode = rs.getInt("confirmation_code");
                    int userId = rs.getInt("user_id");

                    Timestamp placedTs = rs.getTimestamp("date_of_placing_order");
                    LocalDate placedDate = (placedTs == null) ? null : placedTs.toLocalDateTime().toLocalDate();

                    boolean orderActive = rs.getBoolean("order_active");
                    boolean waitList = rs.getBoolean("wait_list");

                    return new Order(orderNumber, orderDate, orderTime, diners, confirmationCode,
                            userId, orderActive, waitList, placedDate);
                }
            }
        } catch (SQLException ex) {
            ServerLogger.log("SQLException in getOrderByConfirmationCode: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        } finally {
            release(conn);
        }
    }

    public static List<Order> getAllOrders() {
        final List<Order> allOrders = new ArrayList<>();
        final String sql =
                "SELECT "
                        + "order_number, order_date, order_time, number_of_guests, confirmation_code, user_id, date_of_placing_order, "
                        + "(order_type = 'WAITLIST') AS wait_list, "
                        + "(status IN ('PENDING','NOTIFIED','SEATED')) AS order_active "
                        + "FROM orders";

        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {

                while (rs.next()) {
                    int orderNumber = rs.getInt("order_number");

                    java.sql.Date od = rs.getDate("order_date");
                    java.sql.Time ot = rs.getTime("order_time");
                    LocalDate orderDate = (od == null) ? null : od.toLocalDate();
                    LocalTime orderTime = (ot == null) ? null : ot.toLocalTime();

                    int diners = rs.getInt("number_of_guests");
                    int confirmationCode = rs.getInt("confirmation_code");
                    int userId = rs.getInt("user_id");

                    Timestamp placedTs = rs.getTimestamp("date_of_placing_order");
                    LocalDate placedDate = (placedTs == null) ? null : placedTs.toLocalDateTime().toLocalDate();

                    boolean orderActive = rs.getBoolean("order_active");
                    boolean waitList = rs.getBoolean("wait_list");

                    allOrders.add(new Order(orderNumber, orderDate, orderTime, diners, confirmationCode,
                            userId, orderActive, waitList, placedDate));
                }
            }
        } catch (SQLException ex) {
            ServerLogger.log("SQLException in getAllOrders: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            release(conn);
        }
        return allOrders;
    }

    public static boolean updateOrder(Order orderUpdateData) {
        final String sql =
                "UPDATE orders "
                        + "SET order_date = ?, order_time = ?, number_of_guests = ? "
                        + "WHERE confirmation_code = ?";

        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement pst = conn.prepareStatement(sql)) {

                if (orderUpdateData.getOrderDate() == null) {
                    pst.setNull(1, java.sql.Types.DATE);
                } else {
                    pst.setDate(1, java.sql.Date.valueOf(orderUpdateData.getOrderDate()));
                }

                if (orderUpdateData.getOrderHour() == null) {
                    pst.setNull(2, java.sql.Types.TIME);
                } else {
                    pst.setTime(2, java.sql.Time.valueOf(orderUpdateData.getOrderHour()));
                }

                pst.setInt(3, orderUpdateData.getDinersAmount());
                pst.setInt(4, orderUpdateData.getConfirmationCode());

                return pst.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            ServerLogger.log("SQLException in updateOrder: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        } finally {
            release(conn);
        }
    }

    public static boolean isDateAvailable(LocalDate date, int confirmationCodeToExclude) {
        if (date == null) return false;

        final String sql =
                "SELECT 1 FROM orders "
                        + "WHERE order_type = 'RESERVATION' "
                        + "AND status IN ('PENDING','NOTIFIED','SEATED') "
                        + "AND order_date = ? "
                        + "AND confirmation_code <> ? "
                        + "LIMIT 1";

        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setDate(1, java.sql.Date.valueOf(date));
                pst.setInt(2, confirmationCodeToExclude);
                try (ResultSet rs = pst.executeQuery()) {
                    return !rs.next();
                }
            }
        } catch (SQLException ex) {
            ServerLogger.log("SQLException in isDateAvailable: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        } finally {
            release(conn);
        }
    }

    // Users

    /**
     * Client sends: { userType: MEMBER, id: <member_code> }
     */
    public static User getUserInfo(Map<String, Object> userLoginData) {
        if (userLoginData == null) return null;

        Object idObj = userLoginData.get("id");
        if (idObj == null) return null;

        String memberCode = String.valueOf(idObj).trim();
        if (memberCode.isEmpty()) return null;

        return getMemberByCode(memberCode);
    }

    private static User getMemberByCode(String memberCode) {
        final String sql =
                "SELECT u.phoneNumber, u.email, m.member_code, m.f_name, m.l_name "
                        + "FROM users u "
                        + "JOIN members m ON u.user_id = m.user_id "
                        + "WHERE m.member_code = ?";

        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, memberCode);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) return null;

                    String phone = rs.getString("phoneNumber");
                    String email = rs.getString("email");
                    String fName = rs.getString("f_name");
                    String lName = rs.getString("l_name");
                    
                    return new User(memberCode, memberCode, fName, lName, null, phone, email, UserType.MEMBER);
                }
            }
        } catch (SQLException ex) {
            ServerLogger.log("SQLException in getMemberByCode: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        } finally {
            release(conn);
        }
    }
}
