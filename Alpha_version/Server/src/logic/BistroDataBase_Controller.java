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
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import entities.Order;
import entities.Table;
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

	//Database Operations Methods:
    // ******************************   User Operations   ******************************
   
    public User findOrCreateGuestUser(String phoneNumber, String email) {
        if ((phoneNumber == null || phoneNumber.isBlank()) && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("Guest must have phoneNumber or email");
        }

        String findByPhone =
            "SELECT user_id, phoneNumber, email FROM users WHERE type = 'GUEST' AND phoneNumber = ?";
        String findByEmail =
            "SELECT user_id, phoneNumber, email FROM users WHERE type = 'GUEST' AND email = ?";

        String insertQry =
            "INSERT INTO users (user_id, phoneNumber, email, type) VALUES (?, ?, ?, 'GUEST')";

        Connection conn = null;
        try {
            conn = borrow();

            // 1) Find
            String findQry = (phoneNumber != null && !phoneNumber.isBlank()) ? findByPhone : findByEmail;

            try (PreparedStatement ps = conn.prepareStatement(findQry)) {
                ps.setString(1, (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber : email);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new User(
                            rs.getInt("user_id"),
                            rs.getString("phoneNumber"),
                            rs.getString("email"),
                            UserType.GUEST
                        );
                    }
                }
            }

            // 2) Create
            int newUserId = generateRandomUserId(conn);

            try (PreparedStatement psInsert = conn.prepareStatement(insertQry)) {
                psInsert.setInt(1, newUserId);

                if (phoneNumber == null || phoneNumber.isBlank()) psInsert.setNull(2, java.sql.Types.VARCHAR);
                else psInsert.setString(2, phoneNumber);

                if (email == null || email.isBlank()) psInsert.setNull(3, java.sql.Types.VARCHAR);
                else psInsert.setString(3, email);

                psInsert.executeUpdate();
            }

            return new User(newUserId, phoneNumber, email, UserType.GUEST);

        } catch (SQLException ex) {
            logger.log("[ERROR] SQLException in findOrCreateGuestUser: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                try { release(conn); } catch (Exception ignore) {}
            }
        }
    }
    
    private int generateRandomUserId(Connection conn) throws SQLException {
        Random random = new Random();

        String checkSql = "SELECT 1 FROM users WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            for (int i = 0; i < 50; i++) {
                int candidate = 100000 + random.nextInt(900000);

                ps.setInt(1, candidate);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return candidate;
                    }
                }
            }
        }
        throw new SQLException("Failed to generate unique 6-digit user_id after 50 attempts.");
    }

//    public User findGuestUser(String phoneNumber, String email) {
//    	//query to find guest user by phone number and email
//        final String findQry="SELECT * FROM users WHERE type = ? AND (phoneNumber = ? OR email = ?)";
//        String insertQry =
//                "INSERT INTO users (user_id, phoneNumber, email, type) " +
//                "VALUES (?, ?, ?, 'GUEST')";
//        User foundUser = null;
//    	Connection conn = null;
//		try {
//			conn = borrow();
//			try (PreparedStatement ps = conn.prepareStatement(findQry)) {
//				ps.setString(1, UserType.GUEST.name());
//				ps.setString(2, phoneNumber);
//				ps.setString(3, email);
//				try (ResultSet rs = ps.executeQuery()) {
//					if (rs.next()) {
//						foundUser = new User(rs.getInt("id"), rs.getString("phoneNumber"), rs.getString("email"),
//								UserType.GUEST);
//					}
//				}
//			}
//		} catch (SQLException ex) {
//			logger.log("[ERROR] SQLException in findGuestUser: " + ex.getMessage());
//			ex.printStackTrace();
//		}
//		// If guest user not found, create a new one
//		if (foundUser == null) {
//			try {
//				conn = borrow();
//				try (PreparedStatement psInsert = conn.prepareStatement(insertQry, PreparedStatement.RETURN_GENERATED_KEYS)) {
//					psInsert.setNull(1, Types.INTEGER); // Assuming user_id is auto-incremented
//					psInsert.setString(2, phoneNumber);
//					psInsert.setString(3,  email);
//					int affectedRows = psInsert.executeUpdate();
//					if (affectedRows == 0) {
//						throw new SQLException("Creating guest user failed, no rows affected.");
//					}
//					try (ResultSet generatedKeys = psInsert.getGeneratedKeys()) {
//						if (generatedKeys.next()) {
//							int newUserId = generatedKeys.getInt(1);
//							foundUser = new User(newUserId, phoneNumber, email, UserType.GUEST);
//						} else {
//							throw new SQLException("Creating guest user failed, no ID obtained.");
//						}
//					}
//				}
//			} catch (SQLException ex) {
//				logger.log("[ERROR] SQLException in creating new Guest user: " + ex.getMessage());
//				ex.printStackTrace();
//			}
//		}
//		return foundUser;
//    }
    
    public User findMemberUserByCode(int memberCode) {

        final String sql =
            "SELECT u.user_id, u.phoneNumber, u.email, u.type, " +
            "       m.member_code, m.f_name, m.l_name, m.address " +
            "FROM members m " +
            "JOIN users u ON u.user_id = m.user_id " +
            "WHERE m.member_code = ? AND u.type = 'MEMBER' " +
            "LIMIT 1";

        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, memberCode);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;

                    int userId     = rs.getInt("user_id");
                    String phone   = rs.getString("phoneNumber");
                    String email   = rs.getString("email");
                    String codeStr = String.valueOf(rs.getInt("member_code"));
                    String first   = rs.getString("f_name");
                    String last    = rs.getString("l_name");
                    String address = rs.getString("address");

                    return new User(userId, phone, email, codeStr, first, last, address, UserType.MEMBER);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            release(conn);
        }
    }


    
	public User findStaffUser(String username, String password) {
		// query to find staff user by username and password
		final String qry = "SELECT * FROM staff_accounts WHERE username = ? AND password = ?";
		final String qry2 = "SELECT * FROM users WHERE id = ?";
		User foundUser = null;
		int userId = 0;
		String phoneNumber = null, email = null, user_name = null,pass=null , type = null;
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, username);
				ps.setString(2, password);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						userId = rs.getInt("user_id");
						user_name = rs.getString("username");
						pass = rs.getString("password");
						// Now fetch user details from users table
						try (PreparedStatement ps2 = conn.prepareStatement(qry2)) {
							ps2.setInt(1, userId);
							try (ResultSet rs2 = ps2.executeQuery()) {
								if (rs2.next()) {
									phoneNumber = rs2.getString("phoneNumber");
									email = rs2.getString("email");
									type = rs2.getString("type");
									foundUser = new User(userId, phoneNumber, email, user_name, UserType.valueOf(type));
								}
							}
						}
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in findStaffUser: " + ex.getMessage());
			ex.printStackTrace();
		}
		return foundUser;

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
			logger.log("[ERROR] SQLException in setNewOrderToDataBase: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		
	}
	
	
	public List<Order> getReservationsbyDate(LocalDate date) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	public boolean checkOrderExistsInDB(String confirmationCode) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	// ********************Waiting List Operations ******************************
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
	
	// ****************************** Table Operations ******************************
	public List<Table> getAllTablesFromDB() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTableNumberByConfirmationCode(String confirmationCode) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	// ******************** Restaurant Management  Operations ******************
	public List<LocalTime> getOpeningHoursFromDB() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean updateOrderStatusInDB(String confirmationCode, OrderStatus completed) {
		// TODO Auto-generated method stub
		return false;
	}






	




}
