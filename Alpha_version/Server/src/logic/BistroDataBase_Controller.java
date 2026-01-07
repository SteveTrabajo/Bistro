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

import dto.UserData;
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


    
	public User findEmployeeUser(String username, String password) {
		// Optimized query to find employee user by username, joining users and staff_accounts tables
		final String qry = "SELECT u.user_id, u.phoneNumber, u.email, u.type, " +
		                   "sa.username, sa.password_hash " +
		                   "FROM users u " +
		                   "JOIN staff_accounts sa ON u.user_id = sa.user_id " +
		                   "WHERE sa.username = ? AND u.type IN ('EMPLOYEE', 'MANAGER')";
		
		User foundUser = null;
		Connection conn = null;
		
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, username);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						String hashedPassword = rs.getString("password_hash");
						
						// Verify the provided password against the hash
						try {
							if (PasswordUtil.verifyPassword(password, hashedPassword)) {
								int userId = rs.getInt("user_id");
								String phoneNumber = rs.getString("phoneNumber");
								String email = rs.getString("email");
								String type = rs.getString("type");
								foundUser = new User(userId, phoneNumber, email, username, UserType.valueOf(type));
								
								// Record successful login
								LoginAttemptTracker.recordSuccessfulLogin(username);
								logger.log("[LOGIN] Successful login for employee user: " + username);
							} else {
								// Record failed attempt
								LoginAttemptTracker.recordFailedAttempt(username);
								logger.log("[LOGIN] Failed login attempt for employee user: " + username);
							}
						} catch (Exception e) {
							logger.log("[ERROR] Exception during password verification: " + e.getMessage());
						}
					} else {
						// Username not found
						logger.log("[LOGIN] Username not found: " + username);
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in findEmployeeUser: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return foundUser;

    }
	
	/**
	 * Checks if a username already exists in the staff_accounts table.
	 * 
	 * @param username The username to check
	 * @return true if username exists, false otherwise
	 */
	public boolean employeeUsernameExists(String username) {
		final String qry = "SELECT 1 FROM staff_accounts WHERE LOWER(username) = LOWER(?)";
		Connection conn = null;
		
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, username);
				try (ResultSet rs = ps.executeQuery()) {
					return rs.next();
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in employeeUsernameExists: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}
	
	/**
	 * Creates a new employee account (EMPLOYEE or MANAGER).
	 * Performs atomic insert into both users and staff_accounts tables.
	 * 
	 * @param username The employee username
	 * @param password The plaintext password (will be hashed)
	 * @param email The employee email
	 * @param phoneNumber The employee phone number
	 * @param userType The user type (EMPLOYEE or MANAGER)
	 * @return The newly created User object, or null if creation failed
	 */
	public User createEmployeeUser(String username, String password, String email, String phoneNumber, UserType userType) {
		if (userType != UserType.EMPLOYEE && userType != UserType.MANAGER) {
			logger.log("[ERROR] Invalid user type for employee creation: " + userType);
			return null;
		}
		
		Connection conn = null;
		
		try {
			conn = borrow();
			conn.setAutoCommit(false); // Start transaction
			
			try {
				// Hash the password
				String hashedPassword = PasswordUtil.hashPassword(password);
				
				// Generate unique user ID
				int userId = generateRandomUserId(conn);
				
				// Insert into users table
				final String insertUserQry = "INSERT INTO users (user_id, phoneNumber, email, type) " +
				                             "VALUES (?, ?, ?, ?)";
				try (PreparedStatement ps = conn.prepareStatement(insertUserQry)) {
					ps.setInt(1, userId);
					ps.setString(2, phoneNumber);
					ps.setString(3, email);
					ps.setString(4, userType.name());
					ps.executeUpdate();
				}
				
				// Insert into staff_accounts table
				final String insertStaffQry = "INSERT INTO staff_accounts (user_id, username, password_hash, created_at) " +
				                              "VALUES (?, ?, ?, NOW())";
				try (PreparedStatement ps = conn.prepareStatement(insertStaffQry)) {
					ps.setInt(1, userId);
					ps.setString(2, username);
					ps.setString(3, hashedPassword);
					ps.executeUpdate();
				}
				
				conn.commit();
				conn.setAutoCommit(true);
				
				logger.log("[ADMIN] New staff user created: username=" + username + ", type=" + userType);
				
				// Return the created user
				return new User(userId, phoneNumber, email, username, userType);
				
			} catch (Exception e) {
				conn.rollback();
				conn.setAutoCommit(true);
				logger.log("[ERROR] Failed to create staff user: " + e.getMessage());
				e.printStackTrace();
				return null;
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in createStaffUser: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		} finally {
			release(conn);
		}
	}
    
	public boolean setUpdatedMemberData(UserData updatedUser) {
		if (updatedUser == null || updatedUser.getUserType() == UserType.GUEST) {
			return false;
		}
		//join from members and users table to update both
		final String sql = "UPDATE members m JOIN users u ON m.user_id = u.user_id SET m.f_name = ?,m.l_name = ?,m.address = ?, u.email = ?, u.phoneNumber = ? WHERE m.member_code = ?";
	
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {

				ps.setString(1, updatedUser.getFirstName());
				ps.setString(2, updatedUser.getLastName());
				ps.setString(3, updatedUser.getAddress());
				ps.setString(4, updatedUser.getEmail());
				ps.setString(5, updatedUser.getPhone());
				ps.setInt(6, Integer.parseInt(updatedUser.getMemberCode()));

				int rowsAffected = ps.executeUpdate();
				return rowsAffected > 0;
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in setUpdatedMemberData: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
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
	    List<Order> orders = new ArrayList<>();
	    
	    if (date == null) {
	        return orders; 
	    }

	    String qry;
	    
	    if (LocalDate.now().equals(date)) {
	        qry = "SELECT order_time, number_of_guests "
	            + "FROM orders "
	            + "WHERE order_type = 'RESERVATION' "
	            + "AND order_date = ? "
	            + "AND status IN ('PENDING', 'NOTIFIED', 'SEATED') "
	            + "ORDER BY order_time ASC";
	    } else {
	        qry = "SELECT order_time, number_of_guests "
	            + "FROM orders "
	            + "WHERE order_type = 'RESERVATION' "
	            + "AND order_date = ? "
	            + "AND status IN ('PENDING', 'NOTIFIED') " 
	            + "ORDER BY order_time ASC";
	    }

	    Connection conn = null;

	    try {
	        conn = borrow();

	        try (PreparedStatement ps = conn.prepareStatement(qry)) {
	            ps.setDate(1, Date.valueOf(date));

	            try (ResultSet rs = ps.executeQuery()) {
	                while (rs.next()) {
	                    Time sqlTime = rs.getTime("order_time");
	                    LocalTime orderHour = (sqlTime != null) ? sqlTime.toLocalTime() : null;
	                    
	                    int dinersAmount = rs.getInt("number_of_guests");
	                    
	                    if (orderHour != null) {
	                        orders.add(new Order(orderHour, dinersAmount));
	                    }
	                }
	            }
	        }

	    } catch (SQLException ex) {
	        logger.log("[ERROR] SQLException in getReservationsbyDate: " + ex.getMessage());
	        ex.printStackTrace();
	        return orders; 
	    } finally {
	        release(conn);
	    }

	    return orders;
	}
	

	
	public boolean checkOrderExistsInDB(String confirmationCode) {
		if( confirmationCode == null  || confirmationCode.isEmpty())
    	{
    		return false;
    	}
		
    	final String qry =   "SELECT 1 "
    	          			+ "FROM orders "
    	          			+ "WHERE confirmation_code = ?";
    	
    	Connection conn = null;
    	
    	try {
			conn = borrow();
			
			try(PreparedStatement ps = conn.prepareStatement(qry))
			{
				ps.setString(1, confirmationCode); 
					
				try(ResultSet rs = ps.executeQuery())
				{
					return rs.next();			// rs.next() returns true if at least one matching order exists
				}
			}
			
    	}
    	catch (SQLException ex) {
			logger.log("[ERROR] SQLException in checkOrderExistsInDB: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		}
    	finally{
    		release(conn);
    	}  	
	}
	
	
	
	public Order getOrderByConfirmationCodeInDB(String confirmationCode)
	{
		if( confirmationCode == null  || confirmationCode.isEmpty())
    	{
    		return null;
    	}
		
		final String qry =	"SELECT order_number, order_date, order_time, number_of_guests, "
	          				+ "confirmation_code, user_id, order_type, status, date_of_placing_order "
	          				+ "FROM orders "
	          				+ "WHERE confirmation_code = ?";
    	
    	Connection conn = null;
    	
    	try {
			conn = borrow();
			
			try(PreparedStatement ps = conn.prepareStatement(qry))
			{
				ps.setString(1, confirmationCode); 
					
				try(ResultSet rs = ps.executeQuery())
				{
					if(!rs.next())
					{
						return null;		// not exists such order
					}
					int orderNumber	= rs.getInt("order_number");
                    LocalDate orderDate   = rs.getDate("order_date").toLocalDate();
                    LocalTime orderTime   = rs.getTime("order_time").toLocalTime();
                    int dinersAmount = rs.getInt("number_of_guests");
                    int userId = rs.getInt("user_id");
                    OrderType orderType	= OrderType.valueOf(rs.getString("order_type"));
                    OrderStatus status = OrderStatus.valueOf(rs.getString("status"));
                    LocalDate dateOfPlacingOrder   = rs.getDate("date_of_placing_order").toLocalDate();	
                    
                    return new Order(orderNumber, orderDate, orderTime, dinersAmount,confirmationCode, userId, orderType, status ,dateOfPlacingOrder);
					
				}
			}
			
    	}
    	catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getOrderByConfirmationCodeInDB: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
    	finally{
    		release(conn);
    	}  	
	}
		
	
	
	
	// ********************Waiting List Operations ******************************
	
	public boolean isUserInWaitingList(String userId) {
		if (userId == null || userId.isEmpty()) {
			return false;
		}
		
		int userIdInt;
		try {
			userIdInt = Integer.parseInt(userId);
		} catch (NumberFormatException e) {
			logger.log("[ERROR] Invalid userId format: " + userId);
			return false;
		}
		
		final String qry = "SELECT 1 " + "FROM orders " + "WHERE user_id = ? " + "AND order_type = 'WAITLIST' "
				+ "AND status = 'PENDING'";
		
		Connection conn = null;
		
		try {
			conn = borrow();
			
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				
				ps.setInt(1, userIdInt); 
				
				try (ResultSet rs = ps.executeQuery()) {
					return rs.next();
				}
			}
			
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in isUserInWaitingList: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}
	
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
	    List<Table> tablesList = new ArrayList<>();
	    String qry = "SELECT tableNum, capacity FROM tables"; // ודא ששמות העמודות נכונים!

	    Connection conn = null;
	    try {
	        conn = borrow();
	        try (PreparedStatement ps = conn.prepareStatement(qry);
	             ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                // שים לב: זה מניח שיש לך בנאי ב-Table שמקבל (מספר, קיבולת)
	                tablesList.add(new Table(rs.getInt("tableNum"), rs.getInt("capacity")));
	            }
	        }
	    } catch (SQLException ex) {
	        ex.printStackTrace();
	    } finally {
	        release(conn);
	    }
	    
	    System.out.println("Controller: Fetched " + tablesList.size() + " tables from DB."); // הדפסה לבדיקה
	    return tablesList;
	}
	

	public int getTableNumberByConfirmationCode(String confirmationCode) {
		if(confirmationCode == null || confirmationCode.isEmpty())
		{
			return -1;
		}
		
		final String qry =  "SELECT ts.tableNum "
						+ "FROM orders o "
						+ "JOIN table_sessions ts "
						+ "ON o.order_number = ts.order_number "
						+ "WHERE o.confirmation_code = ? "
						+ "AND ts.left_at IS NULL";			// need to see if remove this row.
		
		Connection conn = null;
		
		try {
			conn = borrow();
			
			try(PreparedStatement ps = conn.prepareStatement(qry))
			{
				ps.setString(1, confirmationCode); 				//change id db table orders confirmationCode from int to varchar
				
				try(ResultSet rs = ps.executeQuery())
				{
					if(!rs.next())
					{
						return -1; 			// where there is no table that got on qry
					}
					
					int tableNumber = rs.getInt("tableNum");
					
					if( rs.wasNull())
					{
						return -1; 			// where there is table but with null value 
					}
					
					return tableNumber;
				}
			}
									
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getTableNumberByConfirmationCode: " + ex.getMessage());
			ex.printStackTrace();
			return -1;
		}
		finally {
	    	release(conn);
	    }		
	}
	
	// ******************** Restaurant Management  Operations ******************
	public List<LocalTime> getOpeningHoursFromDB() {
	    List<LocalTime> hours = new ArrayList<>();
	    
	    LocalDate today = LocalDate.now();
	   
	    int currentDayOfWeek = (today.getDayOfWeek().getValue() % 7) + 1;

	    String qry = "SELECT " +
	                 "  COALESCE(s.is_closed, 0) as is_closed_final, " +
	                 "  COALESCE(s.open_time, w.open_time) as open_final, " +
	                 "  COALESCE(s.close_time, w.close_time) as close_final " +
	                 "FROM opening_hours_weekly w " +
	                 "LEFT JOIN opening_hours_special s ON s.special_date = ? " +
	                 "WHERE w.day_of_week = ?";

	    Connection conn = null;
	    try {
	        conn = borrow(); // הנחת שיש לך Connection Pool
	        
	        try (PreparedStatement ps = conn.prepareStatement(qry)) {
	            ps.setDate(1, Date.valueOf(today));
	            ps.setInt(2, currentDayOfWeek);              
	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) {
	                    boolean isClosed = rs.getInt("is_closed_final") == 1;
	                    
	                    // אם המקום פתוח, נשלוף את השעות
	                    if (!isClosed) {
	                        Time openSql = rs.getTime("open_final");
	                        Time closeSql = rs.getTime("close_final");

	                        if (openSql != null && closeSql != null) {
	                            hours.add(openSql.toLocalTime());  
	                            hours.add(closeSql.toLocalTime()); 
	                        }
	                    }
	                }
	            }
	        }
	    } catch (SQLException ex) {
	        ex.printStackTrace(); 
	    } finally {
	        release(conn);
	    }
	    
	    return hours;
	}
	
	

	public boolean updateOrderStatusInDB(String confirmationCode, OrderStatus completed) {
		
		  if (confirmationCode == null || confirmationCode.trim().isEmpty() ) {
		        return false;
		    }
		  
		  final String qry =	"UPDATE orders "
		          				+ "SET status = ? "
		          				+ "WHERE confirmation_code = ?";
		  
		  Connection conn = null;
		  
		  try {
		        conn = borrow();

		        try (PreparedStatement ps = conn.prepareStatement(qry)) {

		        	ps.setString(1, completed.name());
		            ps.setString(2, confirmationCode);

		            int success = ps.executeUpdate();
					
					return success == 1;			//when success get 1 the updated worked and changed the row well in table
		        }
		  } catch (SQLException ex) {
		       	logger.log("[ERROR] SQLException in updateOrderStatusInDB: " + ex.getMessage());
		        ex.printStackTrace();
		        return false;

		  } finally {
		        release(conn);
		  }
	}
	
	
	
	public List<User> getAllCustomersInDB() {
		List<User> usersList = new ArrayList<>();
		
		
		final String qry =	    "SELECT u.phoneNumber, u.email, u.type, "
		          				+ "m.member_code, m.f_name, m.l_name "
		          				+ "FROM users u "
		          				+ "LEFT JOIN members m ON u.user_id = m.user_id";

		Connection conn = null;

		try {
			conn = borrow();

			try(PreparedStatement ps = conn.prepareStatement(qry))
			{
						
				try(ResultSet rs = ps.executeQuery()){
					
					while(rs.next()){	
											
						String email = rs.getString("email");
						String phone = rs.getString("phoneNumber");
						UserType type = UserType.valueOf(rs.getString("type"));
						
						User user = null;
						
						switch(type) {
							case MEMBER:{
								String fname = rs.getString("f_name");
								String lname = rs.getString("l_name");
								String memberCode = rs.getString("member_code");
								String fullName = fname + " " + lname;
								user = new User(fullName, email, phone, memberCode, UserType.MEMBER);
								break;
							}
						
							case GUEST:{
								user = new User(null, email, phone, null, UserType.GUEST);
								break;				
							}
							
							default:
								continue;
						
						}
						usersList.add(user);
					}
				}
			}		
		}
		catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getOrderByConfirmationCodeInDB: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
		finally{
			release(conn);
		}  	
		return usersList;
	}	

}