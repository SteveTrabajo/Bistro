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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import dto.UserData;
import entities.Bill;
import entities.Order;
import entities.Table;
import entities.User;
import enums.UserType;
import enums.OrderStatus;
import enums.OrderType;

/**
 * BistroDataBase_Controller class that manages database connections and
 * operations for a Bistro application.
 */
public class BistroDataBase_Controller {

	// **************************** Instance variables ****************************

	private static BistroDataBase_Controller dataBaseControllerInstance;
	private static ServerLogger logger;

	// **************************** Database Configurations
	// ****************************

	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
	private static final String JDBC_USER = "root";
	private static final String JDBC_PASS = "Aa123456";

	// ******************************* Connection Pool Configurations
	// *****************

	private static final int POOL_SIZE = 10; // Number of connections in the pool
	private static final long BORROW_TIMEOUT_MS = 10_000; // Timeout for borrowing a connection
	private static BlockingQueue<Connection> pool = null; // Connection pool
	private static volatile boolean initialized = false; // Pool initialization flag

	// ********************************
	// Constructors***********************************

	private BistroDataBase_Controller() {
	}

	public static synchronized BistroDataBase_Controller getInstance() {
		if (dataBaseControllerInstance == null) {
			dataBaseControllerInstance = new BistroDataBase_Controller();
		}
		return dataBaseControllerInstance;
	}

	// ******************************Getters and
	// Setters******************************
	public void setLogger(ServerLogger log) {
		logger = log;
	}
	// ****************************** Database Connection Pool Management
	// ******************************

	/**
	 * Initializes the database connection pool.
	 * 
	 * @return true if initialization is successful, false otherwise
	 */
	public synchronized boolean openConnection() {
		if (initialized)
			return true;

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

	/**
	 * Closes all connections in the database connection pool.
	 */
	public synchronized void closeConnection() {
		initialized = false;
		if (pool == null)
			return;

		Connection c;
		while ((c = pool.poll()) != null) {
			try {
				c.close();
			} catch (SQLException ignored) {
			}
		}
		pool = null;
		logger.log("SQL connection pool closed");
	}

	/**
	 * Borrows a connection from the pool.
	 * 
	 * @return A valid database connection
	 */
	private static Connection borrow() throws SQLException {
		if (!initialized || pool == null) {
			throw new SQLException("DB pool not initialized. Call openConnection() first.");
		}
		try {
			Connection c = pool.poll(BORROW_TIMEOUT_MS, TimeUnit.MILLISECONDS);
			if (c == null)
				throw new SQLException("Timed out waiting for a DB connection from the pool.");

			if (c.isClosed() || !c.isValid(2)) {
				try {
					c.close();
				} catch (SQLException ignored) {
				}
				c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
				c.setAutoCommit(true);
			}
			return c;
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new SQLException("Interrupted while waiting for a DB connection.", ie);
		}
	}

	/**
	 * Releases a connection back to the pool.
	 * 
	 * @param c
	 */
	private static void release(Connection c) {
		if (c == null)
			return;

		if (!initialized || pool == null) {
			try {
				c.close();
			} catch (SQLException ignored) {
			}
			return;
		}

		try {
			if (c.isClosed())
				return;
			if (!pool.offer(c))
				c.close();
		} catch (SQLException ignored) {
		}
	}

	// Database Operations Methods:
	// ****************************** User Operations ******************************

	public User findOrCreateGuestUser(String phoneNumber, String email) {
		// check validation of input if both are null or empty throw exception
		if ((phoneNumber == null || phoneNumber.isBlank()) && (email == null || email.isBlank())) {
			throw new IllegalArgumentException("Guest must have phoneNumber or email");
		}
		//query to find existing guest user by phone or email
		String findByPhone = "SELECT user_id, phoneNumber, email FROM users WHERE type = 'GUEST' AND phoneNumber = ?";
		String findByEmail = "SELECT user_id, phoneNumber, email FROM users WHERE type = 'GUEST' AND email = ?";
		String insertQry = "INSERT INTO users (user_id, phoneNumber, email, type) VALUES (?, ?, ?, 'GUEST')";
		Connection conn = null;
		try {
			conn = borrow();
			String findQry = (phoneNumber != null && !phoneNumber.isBlank()) ? findByPhone : findByEmail;
			try (PreparedStatement ps = conn.prepareStatement(findQry)) {
				ps.setString(1, (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber : email);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return new User(rs.getInt("user_id"), rs.getString("phoneNumber"), rs.getString("email"),
								UserType.GUEST);
					}
				}
			}
			// No existing guest found, create new one
			int newUserId = generateRandomUserId(conn);
			try (PreparedStatement psInsert = conn.prepareStatement(insertQry)) {
				psInsert.setInt(1, newUserId);
				if (phoneNumber == null || phoneNumber.isBlank()) {
					psInsert.setNull(2, java.sql.Types.VARCHAR);
				}
				else {
					psInsert.setString(2, phoneNumber);
				}
				if (email == null || email.isBlank()) {
					psInsert.setNull(3, java.sql.Types.VARCHAR);
				}
				else {
					psInsert.setString(3, email);
				}

				psInsert.executeUpdate();
			}
			return new User(newUserId, phoneNumber, email, UserType.GUEST);
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in findOrCreateGuestUser: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		} finally {
			if (conn != null) {
				try {
					release(conn);
				} catch (Exception ignore) {
				}
			}
		}
	}
	
	/**
	 * Generates a unique 6-digit user_id not already present in the users table.
	 * 
	 * @param conn Active database connection
	 * @return Unique 6-digit user_id
	 * @throws SQLException if unable to generate a unique ID after multiple
	 *                      attempts
	 */
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

	/**
	 * Finds a member user by their member code.
	 * 
	 * @param memberCode The member code to search for
	 * @return The User object if found, null otherwise
	 */
	public User findMemberUserByCode(int memberCode) {
		final String sql = "SELECT u.user_id, u.phoneNumber, u.email, u.type, "
				+ "       m.member_code, m.f_name, m.l_name, m.address " + "FROM members m "
				+ "JOIN users u ON u.user_id = m.user_id " + "WHERE m.member_code = ? AND u.type = 'MEMBER' "
				+ "LIMIT 1";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, memberCode);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next())
						return null;
					int userId = rs.getInt("user_id");
					String phone = rs.getString("phoneNumber");
					String email = rs.getString("email");
					String codeStr = String.valueOf(rs.getInt("member_code"));
					String first = rs.getString("f_name");
					String last = rs.getString("l_name");
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
	
	/**
	 * Updates member user data in both members and users tables.
	 * 
	 * @param updatedUser The UserData object containing updated member information
	 * @return true if update is successful, false otherwise
	 */
	public boolean setUpdatedMemberData(UserData updatedUser) {
		if (updatedUser == null || updatedUser.getUserType() == UserType.GUEST) {
			return false;
		}
		// join from members and users table to update both
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
	
	//TODO: Create query for member registration by staff
	
	/**
	 * Finds an employee or manager user by username and verifies the password.
	 * 
	 * @param username The username of the employee/manager
	 * @param password The plaintext password to verify
	 * @return The User object if found and password matches, null otherwise
	 */
	public User findEmployeeUser(String username, String password) {
		// staff_accounts columns: (user_id, username, password)
		final String qry = "SELECT u.user_id, u.phoneNumber, u.email, u.type, " + "       sa.username, sa.password "
				+ "FROM users u " + "JOIN staff_accounts sa ON u.user_id = sa.user_id "
				+ "WHERE sa.username = ? AND u.type IN ('EMPLOYEE', 'MANAGER')";
		User foundUser = null;
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, username);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						String hashedPassword = rs.getString("password");

						// Verify the provided password against the hash
						try {
							if (PasswordUtil.verifyPassword(password, hashedPassword)) {
								int userId = rs.getInt("user_id");
								String phoneNumber = rs.getString("phoneNumber");
								String email = rs.getString("email");
								String type = rs.getString("type");

								foundUser = new User(userId, phoneNumber, email, username, UserType.valueOf(type));

								LoginAttemptTracker.recordSuccessfulLogin(username);
								logger.log("[LOGIN] Successful login for employee user: " + username);
							} else {
								LoginAttemptTracker.recordFailedAttempt(username);
								logger.log("[LOGIN] Failed login attempt for employee user: " + username);
							}
						} catch (Exception e) {
							logger.log("[ERROR] Exception during password verification: " + e.getMessage());
						}
					} else {
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
	 * Creates a new employee account (EMPLOYEE or MANAGER). Performs atomic insert
	 * into both users and staff_accounts tables.
	 * 
	 * @param username    The employee username
	 * @param password    The plaintext password (will be hashed)
	 * @param email       The employee email
	 * @param phoneNumber The employee phone number
	 * @param userType    The user type (EMPLOYEE or MANAGER)
	 * @return The newly created User object, or null if creation failed
	 */
	public User createEmployeeUser(String username, String password, String email, String phoneNumber,
			UserType userType) {
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
				final String insertUserQry = "INSERT INTO users (phoneNumber, email, type) VALUES (?, ?, ?)";
				int userId;
				try (PreparedStatement ps = conn.prepareStatement(insertUserQry,
						java.sql.Statement.RETURN_GENERATED_KEYS)) {
					ps.setString(1, phoneNumber);
					ps.setString(2, email);
					ps.setString(3, userType.name());
					ps.executeUpdate();

					try (ResultSet keys = ps.getGeneratedKeys()) {
						if (!keys.next()) {
							throw new SQLException("Failed to read generated user_id");
						}
						userId = keys.getInt(1);
					}
				}
				// staff_accounts schema: (user_id, username, password)
				final String insertStaffQry = "INSERT INTO staff_accounts (user_id, username, password) VALUES (?, ?, ?)";
				try (PreparedStatement ps = conn.prepareStatement(insertStaffQry)) {
					ps.setInt(1, userId);
					ps.setString(2, username);
					ps.setString(3, hashedPassword);
					ps.executeUpdate();
				}
				conn.commit();
				conn.setAutoCommit(true);
				logger.log("[ADMIN] New staff user created: username=" + username + ", type=" + userType);
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
	
	/**
	 * Retrieves all customers (members and guests) from the database.
	 * 
	 * @return List of User objects representing all customers
	 */
	public List<User> getAllCustomersInDB() {
		List<User> usersList = new ArrayList<>();
		final String qry = "SELECT u.phoneNumber, u.email, u.type, " + "m.member_code, m.f_name, m.l_name "
				+ "FROM users u " + "LEFT JOIN members m ON u.user_id = m.user_id";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						String email = rs.getString("email");
						String phone = rs.getString("phoneNumber");
						UserType type = UserType.valueOf(rs.getString("type"));
						User user = null;
						switch (type) {
						case MEMBER: {
							String fname = rs.getString("f_name");
							String lname = rs.getString("l_name");
							String memberCode = rs.getString("member_code");
							String fullName = fname + " " + lname;
							user = new User(fullName, email, phone, memberCode, UserType.MEMBER);
							break;
						}
						case GUEST: {
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
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getOrderByConfirmationCodeInDB: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		} finally {
			release(conn);
		}
		return usersList;
	}
	
	/**
	 * Generates a unique 6-digit member_code not already present in the members table.
	 * * @param conn Active database connection
	 * @return Unique 6-digit member_code
	 * @throws SQLException if unable to generate a unique ID after multiple attempts
	 */
	public int generateUniqueMemberCode(Connection conn) throws SQLException {
	    Random random = new Random();
	    String checkSql = "SELECT 1 FROM members WHERE member_code = ?"; 
	    
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
	    throw new SQLException("Failed to generate unique 6-digit member_code after 50 attempts.");
	}
	
	/**
	 * Registers a new member in the database.
	 * @param newMemberData
	 * @return The newly generated member code if registration is successful, -1 otherwise
	 */
	public int registerNewMember(List<String> newMemberData) {
		String fName = newMemberData.get(0);
		String lName = newMemberData.get(1);
		String email = newMemberData.get(2);
		String phoneNumber = newMemberData.get(3);
		String address = newMemberData.get(4);

		if (email != null && email.trim().isEmpty())
			email = null;
		if (phoneNumber != null && phoneNumber.trim().isEmpty())
			phoneNumber = null;

		Connection conn = null;
		PreparedStatement psCheck = null;
		PreparedStatement psUpdateUser = null;
		PreparedStatement psInsertUser = null;
		PreparedStatement psInsertMember = null;
		ResultSet rs = null;

		try {
			conn = borrow();
			conn.setAutoCommit(false);
			int userId = -1;

			// first step: check if user exists in users table by phone or email
			String checkSql = "SELECT user_id, type FROM users WHERE (phoneNumber = ? AND phoneNumber IS NOT NULL) OR (email = ? AND email IS NOT NULL)";
			psCheck = conn.prepareStatement(checkSql);
			psCheck.setString(1, phoneNumber);
			psCheck.setString(2, email);
			rs = psCheck.executeQuery();

			if (rs.next()) {
				// case 1: user exists
				String existingType = rs.getString("type");
				userId = rs.getInt("user_id");

				if ("GUEST".equals(existingType)) {
					// case 1a: existing user is a GUEST - update to MEMBER
					String updateSql = "UPDATE users SET type = 'MEMBER', email = COALESCE(?, email), phoneNumber = COALESCE(?, phoneNumber) WHERE user_id = ?";
					psUpdateUser = conn.prepareStatement(updateSql);
					psUpdateUser.setString(1, email);
					psUpdateUser.setString(2, phoneNumber);
					psUpdateUser.setInt(3, userId);
					psUpdateUser.executeUpdate();
				} else {
					// case 1b: existing user is MEMBER or STAFF - cannot register
					System.out.println("Registration failed: User already exists with type " + existingType);
					conn.rollback();// cancel changes
					return -1;
				}

			} else {
				// case 2: user does not exist - create new user
				userId = generateRandomUserId(conn);
				// insert new user as MEMBER
				String insertUserSql = "INSERT INTO users (user_id, phoneNumber, email, type) VALUES (?, ?, ?, 'MEMBER')";
				psInsertUser = conn.prepareStatement(insertUserSql);
				psInsertUser.setInt(1, userId);
				psInsertUser.setString(2, phoneNumber);
				psInsertUser.setString(3, email);
				psInsertUser.executeUpdate();
			}

			// case 3: insert new member record
			int memberCode = generateUniqueMemberCode(conn);
			// insert into members table
			String insertMemberSql = "INSERT INTO members (user_id, member_code, f_name, l_name, address) VALUES (?, ?, ?, ?, ?)";
			psInsertMember = conn.prepareStatement(insertMemberSql);
			psInsertMember.setInt(1, userId);
			psInsertMember.setInt(2, memberCode);
			psInsertMember.setString(3, fName);
			psInsertMember.setString(4, lName);
			psInsertMember.setString(5, address);
			psInsertMember.executeUpdate();
			conn.commit(); // save changes
			return memberCode;
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in registerNewMember: " + ex.getMessage());
			ex.printStackTrace();
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			return -1;
		} finally {
			// close all resources
			try {
				if (rs != null)
					rs.close();
			} catch (SQLException e) {
			}
			try {
				if (psCheck != null)
					psCheck.close();
			} catch (SQLException e) {
			}
			try {
				if (psUpdateUser != null)
					psUpdateUser.close();
			} catch (SQLException e) {
			}
			try {
				if (psInsertUser != null)
					psInsertUser.close();
			} catch (SQLException e) {
			}
			try {
				if (psInsertMember != null)
					psInsertMember.close();
			} catch (SQLException e) {
			}
			// finally block
			if (conn != null) {
				try {
					conn.setAutoCommit(true); // החזרה למצב ברירת מחדל לפני החזרה ל-Pool
				} catch (SQLException e) {
				}
				release(conn); // שחרור החיבור
			}
		}
	}

	// ****************************** Order Operations ******************************

	
	/**
	 * Creates a new order in the orders table.
	 * 
	 * @param orderData List of order data:
	 *  [0] User ID (int)
	 *  [1] Date (LocalDate)
	 *  [2] Diners Amount (int)
	 *  [3] Time (LocalTime)
	 *  [4] Confirmation Code (String)
	 * @param type      The OrderType (RESERVATION or WAITLIST)
	 * @param status    The initial OrderStatus
	 * @return true if order creation is successful, false otherwise
	 */
	public boolean setNewOrder(List<Object> orderData, OrderType type, OrderStatus status) {
		final String sql = "INSERT INTO orders "
				+ "(user_id, order_date, number_of_guests, order_time, confirmation_code, "
				+ "order_type, status, date_of_placing_order, notified_at, canceled_at) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				// [0] User ID
				ps.setInt(1, (int) orderData.get(0));
				if (type == OrderType.WAITLIST) {
	                ps.setNull(2, Types.DATE);
	                ps.setNull(4, Types.TIME);
	            } else {
	                ps.setDate(2, Date.valueOf((LocalDate) orderData.get(1)));
	                ps.setTime(4, Time.valueOf((LocalTime) orderData.get(3)));
	            }
				// [2] Diners Amount
				ps.setInt(3, (int) orderData.get(2));
				
				// [4] Confirmation Code
				ps.setString(5, (String) orderData.get(4));
				ps.setString(6, type.name());
				ps.setString(7, status.name());
				ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
				ps.setNull(9, Types.TIMESTAMP); // notified_at
				ps.setNull(10, Types.TIMESTAMP); // canceled_at
				ps.executeUpdate();
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in createGenericOrder: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			if (conn != null) {
				release(conn);
			}
		}
		return true;
	}
	
	/**
	 * Retrieves a list of orders for a specific date.
	 * 
	 * @param date The date to retrieve orders for
	 * @return List of Order objects for the specified date
	 */
	public List<Order> getOrderbyDate(LocalDate date) {
		List<Order> orders = new ArrayList<>();
		if (date == null) {
			return orders;
		}
		boolean isToday = LocalDate.now().equals(date);
		String qry;
		// Different query logic for current date vs future dates
		if (isToday) {
			qry = "SELECT order_time, number_of_guests " 
			           + "FROM orders " 
			           + "WHERE order_type IN ('RESERVATION', 'WAITLIST') "
			           + "AND order_date = ? " 
			           + "AND status IN ('PENDING', 'NOTIFIED', 'SEATED') "
			           + "AND order_time BETWEEN ? AND ? "
			           + "ORDER BY order_time ASC";
			System.out.println("is today query");
		} else {
			qry = "SELECT order_time, number_of_guests " + "FROM orders " + "WHERE order_type = 'RESERVATION' "
					+ "AND order_date = ? " + "AND status IN ('PENDING') " + "ORDER BY order_time ASC";
		}
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setDate(1, Date.valueOf(date));
				
				if (isToday) {
				    LocalTime now = LocalTime.now();
				    ps.setTime(2, Time.valueOf(now));             
				    ps.setTime(3, Time.valueOf(LocalTime.MAX)); 
				}
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
	
	
	//TODO double check this method
	public List<Order> getFullOrdersByDate(LocalDate date) {
        List<Order> orders = new ArrayList<>();
        if (date == null) return orders;

        // Select ALL fields needed for the staff table
        String qry = "SELECT * FROM orders WHERE order_date = ? AND order_type = 'RESERVATION'";

        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(qry)) {
                ps.setDate(1, Date.valueOf(date));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // 1. Extract Basic Info
                        int id = rs.getInt("order_number");
                        LocalTime time = rs.getTime("order_time").toLocalTime();
                        int diners = rs.getInt("number_of_guests");
                        String statusStr = rs.getString("status");
                        String confirmCode = rs.getString("confirmation_code");
                        int userId = rs.getInt("user_id");
                        int tableId = rs.getInt("table_id"); 
                        
                        // 2. Create Order Object (Full Constructor)
                        Order order = new Order(id, date, time, diners, confirmCode, tableId, null, OrderStatus.valueOf(statusStr), null);
                        order.setUserId(userId); 
                        
                        // 3. Optional: If you need the User's Name (e.g., "John Doe"), 
                        // you might need a JOIN query here or a separate lookup.
                        // For now, we stick to the Order table data.
                        
                        orders.add(order);
                    }
                }
            }
        } catch (SQLException ex) {
            logger.log("[ERROR] getFullOrdersByDate: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            release(conn);
        }
        return orders;
    }

	/**
	 * Checks if an order exists in the database by its confirmation code.
	 * 
	 * @param confirmationCode The confirmation code to check
	 * @return true if the order exists, false otherwise
	 */
	public boolean checkOrderExistsInDB(String confirmationCode) {
		if (confirmationCode == null || confirmationCode.isEmpty()) {
			return false;
		}
		final String qry = "SELECT 1 " + "FROM orders " + "WHERE confirmation_code = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, confirmationCode);

				try (ResultSet rs = ps.executeQuery()) {
					return rs.next(); // rs.next() returns true if at least one matching order exists
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in checkOrderExistsInDB: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}
	
	/**
	 * Retrieves an order from the database by its confirmation code.
	 * 
	 * @param confirmationCode The confirmation code of the order
	 * @return The Order object if found, null otherwise
	 */
	public Order getOrderByConfirmationCodeInDB(String confirmationCode) {
		if (confirmationCode == null || confirmationCode.isEmpty()) {
			return null;
		}
		final String qry = "SELECT order_number, order_date, order_time, number_of_guests, "
				+ "confirmation_code, user_id, order_type, status, date_of_placing_order " + "FROM orders "
				+ "WHERE confirmation_code = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, confirmationCode);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						return null; // not exists such order
					}
					int orderNumber = rs.getInt("order_number");
					LocalDate orderDate = rs.getDate("order_date").toLocalDate();
					LocalTime orderTime = rs.getTime("order_time").toLocalTime();
					int dinersAmount = rs.getInt("number_of_guests");
					int userId = rs.getInt("user_id");
					OrderType orderType = OrderType.valueOf(rs.getString("order_type"));
					OrderStatus status = OrderStatus.valueOf(rs.getString("status"));
					Timestamp sqlPlacedAt = rs.getTimestamp("date_of_placing_order");
			        LocalDateTime dateOfPlacingOrder = (sqlPlacedAt != null) ? sqlPlacedAt.toLocalDateTime() : null;
					return new Order(orderNumber, orderDate, orderTime, dinersAmount, confirmationCode, userId,
							orderType, status, dateOfPlacingOrder);
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getOrderByConfirmationCodeInDB: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		} finally {
			release(conn);
		}
	}
	
	/**
	 * Retrieves a list of active and upcoming orders for the specified date and time.
	 * 
	 * Active orders include those with status 'SEATED'.
	 * Upcoming reservations include those with type 'RESERVATION' and status
	 * 'PENDING' or 'NOTIFIED' within the estimated dining window.
	 * 
	 * @param today           The current date
	 * @param now             The current time
	 * @param walkInEndTime   The end time for walk-in customers
	 * @return List of active and upcoming Order objects
	 */
	public List<Order> getActiveAndUpcomingOrders(LocalDate today, LocalTime now, LocalTime walkInEndTime) {
	    List<Order> orders = new ArrayList<>();
	    
	    // Logic update: 
	    // 1. Check for currently SEATED customers.
	    // 2. Check for upcoming RESERVATIONS (type = RESERVATION) that are PENDING/NOTIFIED 
	    //    and fall within the estimated dining window.
	    String query = "SELECT * FROM orders " 
	                 + "WHERE order_date = ? " 
	                 + "AND (" 
	                 + "   status = 'SEATED' " 
	                 + "   OR " 
	                 + "   (order_type = 'RESERVATION' " 
	                 + "    AND status IN ('PENDING', 'NOTIFIED') " 
	                 + "    AND order_time >= ? AND order_time <= ?) " 
	                 + ")";

	    Connection conn = null;
	    try {
	        conn = borrow();
	        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
	            // Set parameters
	            pstmt.setDate(1, Date.valueOf(today));
	            pstmt.setTime(2, Time.valueOf(now));
	            pstmt.setTime(3, Time.valueOf(walkInEndTime));

	            try (ResultSet rs = pstmt.executeQuery()) {
	                while (rs.next()) {
	                    Order order = new Order();
	                    // Map database columns to the Order object
	                    order.setOrderNumber(rs.getInt("order_number"));
	                    order.setDinersAmount(rs.getInt("number_of_guests"));
	                    order.setOrderDate(rs.getDate("order_date").toLocalDate());
	                    
	                    // Handle time (check for nulls if necessary, though logic implies not null here)
	                    Time time = rs.getTime("order_time");
	                    if (time != null) {
	                        order.setOrderHour(time.toLocalTime());
	                    }

	                    // Convert DB Enum strings to Java Enums
	                    String statusStr = rs.getString("status");
	                    order.setStatus(OrderStatus.valueOf(statusStr));
	                    
	                    String typeStr = rs.getString("order_type");
	                    order.setOrderType(OrderType.valueOf(typeStr));

	                    // Add populated order to the list
	                    orders.add(order);
	                }
	            }
	        }
	    } catch (SQLException ex) {
	        logger.log("[ERROR] SQLException in getActiveAndUpcomingOrders: " + ex.getMessage());
	        ex.printStackTrace();
	        return orders;
	    } finally {
	        release(conn);
	    }
	    return orders;
	}
	
	/**
	 * Updates the status of an order in the database based on its confirmation code.
	 * 
	 * @param confirmationCode The confirmation code of the order
	 * @param status           The new OrderStatus to set
	 * @return true if the update was successful, false otherwise
	 */
	public boolean updateOrderStatusInDB(String confirmationCode, OrderStatus status) {
		if (confirmationCode == null || confirmationCode.trim().isEmpty()) {
			return false;
		}
		// We use 'user_id' because you stated that for waitlist entries,
		// the code is stored in the 'user_id' column.
		final String qry = "UPDATE orders SET status = ? WHERE user_id = ?";

		Connection conn = null;
		try {
			conn = borrow();

			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				// Set the new status (e.g., 'COMPLETED', 'CANCELED')
				ps.setString(1, status.name());

				// Map the confirmationCode string to the 'user_id' column
				ps.setString(2, confirmationCode);

				int rowsAffected = ps.executeUpdate();

				// Returns true if exactly one row was updated
				return rowsAffected > 0;
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in updateOrderStatusInDB: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}


	//  ****************************** Waiting List Operations ******************************
	
	/**
	 * Checks if a user with the given confirmation code is currently in the waiting list.
	 * 
	 * @param confirmationCode The confirmation code to check
	 * @return true if the user is in the waiting list, false otherwise
	 */
	public boolean isUserInWaitingList(String confirmationCode) {
		// Check if a WAITLIST order exists with PENDING status for the given confirmation code
		final String qry = "SELECT 1 " + "FROM orders " + "WHERE confirmation_code = ? "
				+ "AND order_type = 'WAITLIST' " + "AND status = 'PENDING'";
		Connection conn = null;
		try {
			conn = borrow();

			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, confirmationCode);

				try (ResultSet rs = ps.executeQuery()) {
					// If rs.next() is true, the order exists
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
	
	/**
	 * Removes a user from the waiting list by updating their order status to 'CANCELLED'.
	 * 
	 * @param confirmationCode The confirmation code of the order to cancel
	 * @return true if the order was successfully cancelled, false otherwise
	 */
	public boolean removeFromWaitingList(String confirmationCode) {
		final String qry = "UPDATE orders " + "SET status = 'CANCELED', canceled_at = ? "
				+ "WHERE confirmation_code = ? " + "AND order_type = 'WAITLIST' " + "AND status = 'PENDING'";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				// Set the current timestamp for cancelled_at
				ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
				// Set the confirmation code for the WHERE clause
				ps.setString(2, confirmationCode);
				int rowsAffected = ps.executeUpdate();
				if (rowsAffected > 0) {
					// The DB Trigger (trg_cleanup_waiting_list) will automatically
					// remove the entry from the 'waiting_list' table now.
					logger.log("[SUCCESS] Order " + confirmationCode + " canceled successfully.");
					return true;
				} else {
					logger.log("[WARN] No pending WAITLIST order found for code: " + confirmationCode);
					return false;
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in removeFromWaitingList: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}
	
	/**
	 * Adds or updates the quoted wait time for a waiting list order.
	 * 
	 * @param confirmationCode   The confirmation code of the order
	 * @param calculatedWaitTime The calculated wait time in minutes
	 */
	public void addWaitTimeToWaitListOrder(String confirmationCode, int calculatedWaitTime) {
	    // This query inserts the wait time into the waiting_list table.
	    // If the confirmation_code already exists in this table, it updates the time instead.
	    final String qry = "INSERT INTO waiting_list (confirmation_code, quoted_wait_time) " 
	                     + "VALUES (?, ?) " 
	                     + "ON DUPLICATE KEY UPDATE quoted_wait_time = ?";
	    Connection conn = null;
	    try {
	        conn = borrow();
	        try (PreparedStatement ps = conn.prepareStatement(qry)) {
	            ps.setString(1, confirmationCode);
	            ps.setInt(2, calculatedWaitTime);
	            ps.setInt(3, calculatedWaitTime);
	            int rowsAffected = ps.executeUpdate();
	            if (rowsAffected > 0) {
	                logger.log("[SUCCESS] Wait time updated for order: " + confirmationCode);
	            }
	        }
	    } catch (SQLException ex) {
	        logger.log("[ERROR] SQLException in addWaitTimeToWaitListOrder: " + ex.getMessage());
	        ex.printStackTrace();
	    } finally {
	        release(conn);
	    }
	}
	
	/**
	 * Retrieves the current waiting queue from the database view.
	 * 
	 * @return List of Order objects representing the waiting queue
	 */
	public List<Order> getWaitingQueueFromView() {
		List<Order> queue = new ArrayList<>();
		// Query to fetch orders joined with waiting_list details.
		// We filter by 'PENDING' to show only active waiters.
		// We order by date_of_placing_order ASC so the first person who arrived is
		// first in the list.
		String query = "SELECT o.order_number, o.confirmation_code, o.number_of_guests, "
				+ "       o.date_of_placing_order, o.status, o.order_type, " + "       w.quoted_wait_time "
				+ "FROM orders o " + "JOIN waiting_list w ON o.confirmation_code = w.confirmation_code "
				+ "WHERE o.status = 'PENDING' " + "ORDER BY o.date_of_placing_order ASC";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Order order = new Order();
					// Map standard Order fields
					order.setOrderNumber(rs.getInt("order_number"));
					order.setDinersAmount(rs.getInt("number_of_guests"));
					order.setOrderType(OrderType.valueOf(rs.getString("order_type")));
					order.setStatus(OrderStatus.valueOf(rs.getString("status")));
					// Map the timestamp of when they joined the list
					Timestamp placedAt = rs.getTimestamp("date_of_placing_order");
					if (placedAt != null) {
						order.setDateOfPlacingOrder(placedAt.toLocalDateTime());
					}
					int waitTime = rs.getInt("quoted_wait_time");
					queue.add(order);
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getWaitingQueueFromView: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return queue;
	}

	// ****************************** Table Operations ******************************
	
	/**
	 * Retrieves all tables from the database.
	 * 
	 * @return List of Table objects representing all tables in the database
	 */
	public List<Table> getAllTablesFromDB() {
		List<Table> tablesList = new ArrayList<>();
		String qry = "SELECT tableNum, capacity FROM tables";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry); ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {

					tablesList.add(new Table(rs.getInt("tableNum"), rs.getInt("capacity")));
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			release(conn);
		}

		System.out.println("Controller: Fetched " + tablesList.size() + " tables from DB.");
		return tablesList;
	}
	
	public HashMap<Table, String> getTableMap() {
	    HashMap<Table, String> tableSessionsMap = new HashMap<>();
	    
	    String qry = "SELECT t.tableNum, t.capacity, o.confirmation_code " +
	                 "FROM tables t " +
	                 "LEFT JOIN table_sessions ts ON t.tableNum = ts.tableNum AND ts.left_at IS NULL " +
	                 "LEFT JOIN orders o ON ts.order_number = o.order_number";

	    Connection conn = null;
	    try {
	        conn = borrow();
	        try (PreparedStatement ps = conn.prepareStatement(qry); ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                Table table = new Table(rs.getInt("tableNum"), rs.getInt("capacity"));
	                
	                String confirmationCode = rs.getString("confirmation_code");
	                
	                if (confirmationCode == null) {
	                    confirmationCode = "";
	                }
	                
	                tableSessionsMap.put(table, confirmationCode);
	            }
	        }
	    } catch (SQLException ex) {
	        ex.printStackTrace();
	    } finally {
	        release(conn);
	    }

	    System.out.println("Controller: Fetched " + tableSessionsMap.size() + " tables (total status).");
	    return tableSessionsMap;
	}
	
	
	/**
	 * Retrieves the table number associated with a given confirmation code.
	 * 
	 * @param confirmationCode The confirmation code to search for
	 * @return The table number if found, -1 otherwise
	 */
	public int getTableNumberByConfirmationCode(String confirmationCode) {
		if (confirmationCode == null || confirmationCode.isEmpty()) {
			return -1;
		}
		final String qry = "SELECT ts.tableNum " + "FROM orders o " + "JOIN table_sessions ts "
				+ "ON o.order_number = ts.order_number " + "WHERE o.confirmation_code = ? " + "AND ts.left_at IS NULL"; 
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, confirmationCode);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						return -1; // where there is no table that got on qry
					}
					int tableNumber = rs.getInt("tableNum");
					if (rs.wasNull()) {
						return -1; // where there is table but with null value
					}
					return tableNumber;
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getTableNumberByConfirmationCode: " + ex.getMessage());
			ex.printStackTrace();
			return -1;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves the earliest expected end time among active table sessions that can
	 * accommodate the specified number of diners.
	 * 
	 * @param dinersAmount The number of diners to accommodate
	 * @return The earliest expected end time as LocalTime, or null if none found
	 */
	public LocalTime getEarliestExpectedEndTime(int dinersAmount) {
		LocalTime earliestTime = null;
		String query = "SELECT MIN(ts.expected_end_at) as earliest_time " + "FROM table_sessions ts "
				+ "JOIN tables t ON ts.tableNum = t.tableNum " + "WHERE ts.left_at IS NULL " + "AND t.capacity >= ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement pstmt = conn.prepareStatement(query)) {

				pstmt.setInt(1, dinersAmount);

				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						Timestamp timestamp = rs.getTimestamp("earliest_time");
						if (timestamp != null) {
							// Convert SQL Timestamp to Java LocalTime
							earliestTime = timestamp.toLocalDateTime().toLocalTime();
						}
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getEarliestExpectedEndTime: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			if (conn != null) {
				release(conn);
			}
		}

		return earliestTime;
	}
	
	// TODO double check the functions below for table sessions
		/**
		 * Finds the smallest available table that fits the group size. Checks against
		 * table_sessions to ensure the table isn't currently occupied.
		 */
		public int findFreeTableForGroup(int groupSize) {
			// Select tables that fit the group AND are not currently in an active session
			String query = "SELECT t.tableNum FROM tables t " + "WHERE t.capacity >= ? " + "AND t.tableNum NOT IN ("
					+ "    SELECT s.tableNum FROM table_sessions s WHERE s.left_at IS NULL" + ") "
					+ "ORDER BY t.capacity ASC LIMIT 1";

			try (Connection conn = borrow(); PreparedStatement ps = conn.prepareStatement(query)) {

				ps.setInt(1, groupSize);

				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt("tableNum");
					}
				}
			} catch (SQLException e) {
				logger.log("[DB ERROR] Failed to find free table: " + e.getMessage());
				e.printStackTrace();
			}
			return -1; // No table available
		}

		/**
		 * Creates a new session linking the order to the table.
		 */
		public boolean createTableSession(int orderNumber, int tableNum) {
			String query = "INSERT INTO table_sessions (order_number, tableNum, seated_at) VALUES (?, ?, NOW())";

			try (Connection conn = borrow(); PreparedStatement ps = conn.prepareStatement(query)) {

				ps.setInt(1, orderNumber);
				ps.setInt(2, tableNum);

				int rows = ps.executeUpdate();
				return rows > 0;
			} catch (SQLException e) {
				logger.log("[DB ERROR] Failed to create table session: " + e.getMessage());
				return false;
			}
		}

		/**
		 * Closes the active session for a specific order.
		 */
		public void closeSessionForOrder(int orderNumber) {
			String query = "UPDATE table_sessions " + "SET left_at = NOW(), end_reason = 'PAID' "
					+ "WHERE order_number = ? AND left_at IS NULL";

			try (Connection conn = borrow(); PreparedStatement ps = conn.prepareStatement(query)) {

				ps.setInt(1, orderNumber);
				ps.executeUpdate();

			} catch (SQLException e) {
				logger.log("[DB ERROR] Failed to close session: " + e.getMessage());
			}
		}

	// ******************** Restaurant Management Operations ******************
	
	/**
	 * Retrieves today's opening hours from the database, considering special
	 * dates.
	 * 
	 * @return List of LocalTime objects representing opening and closing times.
	 *         Empty list if closed today.
	 */
	public List<LocalTime> getOpeningHoursFromDB() {
		List<LocalTime> hours = new ArrayList<>();
		LocalDate today = LocalDate.now();
		int currentDayOfWeek = (today.getDayOfWeek().getValue() % 7) + 1;
		String qry = "SELECT " + "  COALESCE(s.is_closed, 0) as is_closed_final, "
				+ "  COALESCE(s.open_time, w.open_time) as open_final, "
				+ "  COALESCE(s.close_time, w.close_time) as close_final " + "FROM opening_hours_weekly w "
				+ "LEFT JOIN opening_hours_special s ON s.special_date = ? " + "WHERE w.day_of_week = ?";
		Connection conn = null;
		try {
			conn = borrow();
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
	
	
	//******************************* Payment Operations ******************************
	/**
	 * Updates a bill to mark it as PAID and records the external transaction ID.
	 * @param billId The ID of the bill to update.
	 * @param paymentMethod The method used (e.g., "CREDIT", "CASH").
	 * @param transactionId The reference ID from the payment provider (can be null for Cash).
	 */
	public void markBillAsPaid(int billId, String paymentMethod, String transactionId) {
	    // The SQL query to update the bill
	    String sql = "UPDATE bills SET " +
	                 "payment_status = 'PAID', " +
	                 "paid_at = NOW(), " +
	                 "payment_method = ?, " +
	                 "transaction_id = ? " +
	                 "WHERE billID = ?";
		Connection conn = null;
		try {
			conn = borrow();

			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

				// 1. Set the payment method (CASH/CREDIT)
				pstmt.setString(1, paymentMethod);

				// 2. Set the transaction ID (e.g., from Stripe/PayPal)
				pstmt.setString(2, transactionId);

				// 3. Set the Bill ID to identify which row to update
				pstmt.setInt(3, billId);

				int affectedRows = pstmt.executeUpdate();

				if (affectedRows > 0) {
					System.out.println("[DB] Bill " + billId + " successfully marked as PAID.");
				} else {
					System.out.println("[DB] Error: Bill " + billId + " not found or could not be updated.");
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves a Bill object by its ID to check its current status and amount.
	 * @param billId The ID of the bill to fetch.
	 * @return A Bill object, or null if not found.
	 */
	public Bill getBillById(int billId) {
	    String sql = "SELECT * FROM bills WHERE billID = ?";
	    Bill bill = null;
	    Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setInt(1, billId);

				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						// Map the ResultSet to your Bill entity
						// Note: Adjust the constructor below to match your Bill.java class exactly
						bill = new Bill(rs.getInt("billID"), rs.getDouble("billSum"), rs.getString("payment_status"),
								rs.getString("transaction_id"));

						// If your Bill entity stores more fields, set them here:
						// bill.setPaymentMethod(rs.getString("payment_method"));
						// bill.setTransactionId(rs.getString("transaction_id"));
					}
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return bill;
	}
	
	/**
	 * SQL: Finds the Bill ID associated with a Table Session's Order Number.
	 */
	public Integer getBillIdByOrderNumber(int orderNumber) {
		// Join 'bills' with 'table_sessions' to match order_number
		String sql = "SELECT b.billID FROM bills b " + "JOIN table_sessions ts ON b.session_id = ts.session_id "
				+ "WHERE ts.order_number = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setInt(1, orderNumber);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						return rs.getInt("billID");
					}

				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			release(conn);
		}
		return null; // Return null if no bill found
	}

	/**
	 * SQL: Finds all UNPAID bills for a specific User.
	 * This requires joining Bills -> Session -> Order -> User.
	 */
	public List<Bill> getPendingBillsByUserId(int userId) {
		List<Bill> bills = new ArrayList<>();
		String sql = "SELECT b.* FROM bills b " + "JOIN table_sessions ts ON b.session_id = ts.session_id "
				+ "JOIN orders o ON ts.order_number = o.order_number "
				+ "WHERE o.user_id = ? AND b.payment_status = 'UNPAID'";
		Connection conn = null;
		try {
			conn = borrow();

			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setInt(1, userId);
				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next()) {
						// Adjust this constructor based on your Bill.java entity
						Bill bill = new Bill(rs.getInt("billID"), rs.getDouble("billSum"),
								rs.getString("payment_status"), rs.getString("transaction_id"));
						// Set other fields if your Bill constructor requires them
						bills.add(bill);
					}

				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			release(conn);
		}
		return bills;
	}
	
	//*************************************** Notification Operations ********************************
	// TODO complete the function
		public User getUserById(int userId) {
			// TODO Auto-generated method stub
			return null;
		}

		// TODO complete the function
		public List<Order> getOrdersBetweenTimes(LocalDateTime startWindow, LocalDateTime endWindow, OrderStatus seated) {
			// TODO Auto-generated method stub
			return null;
		}
		
	//*************************************** Reports ********************************
		
	//Method that give the number of the reservation this month
	public int getTotalReservation(LocalDate date) {
		
	    final String qry =	"SELECT COUNT(*) AS total " +
	    					"FROM orders o " +
	    					"WHERE MONTH(o.order_date) = ? " +
	    					"AND YEAR(o.order_date) = ?";
	    					

	    Connection conn = null;

	    try {
	        conn = borrow();

	        try (PreparedStatement ps = conn.prepareStatement(qry)) {
	            ps.setInt(1, date.getMonthValue());
	            ps.setInt(2, date.getYear());

	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) {
	                    return rs.getInt("total");
	                }
	            }
	        }
	    } catch (SQLException ex) {
	        logger.log("[ERROR] SQLException in getTotalMemberOrdersForMonth: " + ex.getMessage());
	        ex.printStackTrace();
	    } finally {
	        release(conn);
	    }

	    return 0;
	}
	
	
	//Method that give the number of the costumers this month
	public int getTotalCostumersInMonth(LocalDate date) {
	    final String qry =	"SELECT COUNT(DISTINCT o.user_id) AS total " +
	    					"FROM orders o " +
	    					"WHERE MONTH(o.order_date) = ? " +
	    					"AND YEAR(o.order_date) = ?";
	    					

	    Connection conn = null;

	    try {
	        conn = borrow();

	        try (PreparedStatement ps = conn.prepareStatement(qry)) {
	            ps.setInt(1, date.getMonthValue());
	            ps.setInt(2, date.getYear());

	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) {
	                    return rs.getInt("total");
	                }
	            }
	        }
	    } catch (SQLException ex) {
	        logger.log("[ERROR] SQLException in getTotalCostumersInMonth: " + ex.getMessage());
	        ex.printStackTrace();
	    } finally {
	        release(conn);
	    }

	    return 0;
	}
	
	//Method that give the number of the late costumers this month
	public int getTotalLateCostumersInMonth(LocalDate date) {
		
		   final String qry =	"SELECT COUNT(*) AS total_late " +
		            			"FROM orders o " +
		            			"JOIN table_sessions ts ON o.order_number = ts.order_number " +
		            			"WHERE o.order_type = 'RESERVATION' " +
		            			"AND MONTH(o.order_date) = ? " +
		            			"AND YEAR(o.order_date) = ? " +
		            			"AND ts.seated_at IS NOT NULL " +
		            			"AND ts.seated_at > TIMESTAMP(o.order_date, o.order_time)";
		    					
		   Connection conn = null;

		   try {
		       conn = borrow();

		       try (PreparedStatement ps = conn.prepareStatement(qry)) {
		            ps.setInt(1, date.getMonthValue());
		            ps.setInt(2, date.getYear());

		           try (ResultSet rs = ps.executeQuery()) {
		               if (rs.next()) {
		                   return rs.getInt("total_late");
		                }
		            }
		        }
		    } catch (SQLException ex) {
		        logger.log("[ERROR] SQLException in getTotalLateCostumersInMonth: " + ex.getMessage());
		        ex.printStackTrace();
		    } finally {
		        release(conn);
		    }

		    return 0;
		}
	
	
	//Method that give the number of the late costumers this month
	public int getTotalOntTimeCostumersInMonth(LocalDate date) {
			
			final String qry =	"SELECT COUNT(*) AS total_on_time " +
			            			"FROM orders o " +
			            			"JOIN table_sessions ts ON o.order_number = ts.order_number " +
			            			"WHERE o.order_type = 'RESERVATION' " +
			            			"AND MONTH(o.order_date) = ? " +
			            			"AND YEAR(o.order_date) = ? " +
			            			"AND ts.seated_at IS NOT NULL " +
			            			"AND ts.seated_at = TIMESTAMP(o.order_date, o.order_time)";
			    					
			 Connection conn = null;

			 try {
			      conn = borrow();

			      try (PreparedStatement ps = conn.prepareStatement(qry)) {
			           	ps.setInt(1, date.getMonthValue());
			            ps.setInt(2, date.getYear());

			            try (ResultSet rs = ps.executeQuery()) {
			               if (rs.next()) {
			                   return rs.getInt("total_on_time");
			                }
			            }
			        }
			 } catch (SQLException ex) {
			        logger.log("[ERROR] SQLException in getTotalOntTimeCostumersInMonth: " + ex.getMessage());
			        ex.printStackTrace();
			 } finally {
			        release(conn);
			 }

			 return 0;
		 }

	public void updateOrderTimeAndDateToNow(String confirmationCode) {
		String sql = "UPDATE orders SET order_date = ?, order_time = ?, order_type = 'RESERVATION' WHERE confirmation_code = ?";
		
	}



}