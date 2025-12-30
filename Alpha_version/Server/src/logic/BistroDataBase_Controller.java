package logic;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import enums.OrderStatus;
import enums.OrderType;

/**
 * BistroDataBase_Controller class that manages database connections and operations
 * for a Bistro application.
 */
public class BistroDataBase_Controller {
	
	// **************************** Instance variables ****************************
	
	private static BistroDataBase_Controller dataBaseControllerInstance;
	private static ServerLogger logger;
	
	//**************************** Database Configurations ****************************
	
    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "Aa123456";
    
    //******************************* Connection Pool Configurations *****************
    
    private static final int POOL_SIZE = 10; // Number of connections in the pool
    private static final long BORROW_TIMEOUT_MS = 10_000; // Timeout for borrowing a connection
    private static BlockingQueue<Connection> pool = null; // Connection pool
    private static volatile boolean initialized = false; // Pool initialization flag
 
    // ******************************** Constructors***********************************
    
    private BistroDataBase_Controller() {}
    
	public static synchronized BistroDataBase_Controller getInstance() {
		if (dataBaseControllerInstance == null) {
			dataBaseControllerInstance = new BistroDataBase_Controller();
		}
		return dataBaseControllerInstance;
	}
    //******************************Getters and Setters******************************
	public void setLogger(ServerLogger log) {
		logger = log;
	}
    //****************************** Database Connection Pool Management ******************************
	
    public synchronized boolean openConnection() {
        if (initialized) return true;

        try {
            pool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) {
                Connection c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
                c.setAutoCommit(true);
                pool.offer(c);
            }
            initialized = true;
            logger.log("SQL connection pool initialized. Size=" + POOL_SIZE);
            return true;
        } catch (SQLException ex) {
        	logger.log("Failed to initialize SQL connection pool: " + ex.getMessage());
            ex.printStackTrace();
            closeConnection();
            return false;
        }
    }

    public synchronized void closeConnection() {
        initialized = false;
        if (pool == null) return;

        Connection c;
        while ((c = pool.poll()) != null) {
            try { c.close(); } catch (SQLException ignored) {}
        }
        pool = null;
        logger.log("SQL connection pool closed");
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

	// ****************************** Database Operations ******************************
    // ******************************   User Operations   ******************************
    /**
     * Client sends: { userType: MEMBER, id: <member_code> }
     */
    public User getUserInfo(Map<String, Object> userLoginData) {
    	if (userLoginData == null) {
    		return null;
    	}
    	Object idObj = userLoginData.get("id");
    	if (idObj == null) {
    		return null;
    	}
    	String memberCode = String.valueOf(idObj).trim();
    	if (memberCode.isEmpty()) {
    		return null;
    	}
    	return getMemberByCode(memberCode);
    }
    
    
    private static User getMemberByCode(String memberCode) {
    	final String sql =
    			"SELECT u.user_id, u.phoneNumber, u.email, m.member_code, m.f_name, m.l_name " +
    					"FROM users u " +
    					"JOIN members m ON u.user_id = m.user_id " +
    					"WHERE m.member_code = ?";
    	
    	Connection conn = null;
    	try {
    		conn = borrow();
    		try (PreparedStatement pst = conn.prepareStatement(sql)) {
    			pst.setString(1, memberCode);
    			try (ResultSet rs = pst.executeQuery()) {
    				if (!rs.next()) return null;
    				
    				int userId = rs.getInt("user_id");
    				String phone = rs.getString("phoneNumber");
    				String email = rs.getString("email");
    				String code = rs.getString("member_code");
    				String fName = rs.getString("f_name");
    				String lName = rs.getString("l_name");
    				
    				return new User(userId, code, fName, lName, phone, email);
    			}
    		}
    	} catch (SQLException ex) {
    		logger.log("SQLException in getMemberByCode: " + ex.getMessage());
    		ex.printStackTrace();
    		return null;
    	} finally {
    		release(conn);
    	}
    }
    
    public boolean updateUserInfo(User updatedUser) {
		// TODO Auto-generated method stub
		return false;
	}
    
    // ****************************** Order Operations ******************************	
	
	public boolean setNewOrderToDataBase(List<Object> orderData) {
		// orderData order: userId, date ,dinersAmount , time, orderNumber,
		// confirmationCode, orderType, status
		final String sql = "INSERT INTO orders " + "(order_number," + " confirmation_code," + " user_id, "
				+ "number_of_guests," + " order_date, " + "order_time," + " date_of_placing_order," + " order_type,"
				+ " status," + "notified_at," + "canceled_at)" + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {

				ps.setObject(1, orderData.get(4)); // order_number
				ps.setString(2, (String) orderData.get(5)); // confirmation_code
				ps.setInt(3, (int) orderData.get(0)); // user_id
				ps.setInt(4, (int) orderData.get(2)); // number_of_guests

				ps.setDate(5, Date.valueOf((LocalDate) orderData.get(1))); // order_date
				ps.setTime(6, Time.valueOf((LocalTime) orderData.get(3))); // order_time

				ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now())); // date_of_placing_order

				ps.setString(8, orderData.get(6).toString()); // order_type
				ps.setString(9, orderData.get(7).toString()); // status
				// For new orders, notified_at and canceled_at are null
				ps.setNull(10, Types.TIMESTAMP); // notified_at
				ps.setNull(11, Types.TIMESTAMP); // canceled_at

				ps.executeUpdate();
				return true;
			}
		}catch (SQLException ex) {
			logger.log("SQLException in setNewOrderToDataBase: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		
	}
	
	
	
	// ****************************** Waiting List Operations ******************************
	public Order addToWaitingList(Map<String, Object> userData) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean removeFromWaitingList(String confirmationCode) {
		// TODO Auto-generated method stub
		return false;
	}

	public int assignTableForWaitingListOrder(Order createdOrder) {
		// TODO implement this method to return the assigned table number for the order
		return 0;
	}


	




}
