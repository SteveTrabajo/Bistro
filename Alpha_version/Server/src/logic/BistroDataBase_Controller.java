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
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

import dto.Holiday;
import dto.UserData;
import dto.WeeklyHour;
import entities.Bill;
import entities.Item;
import entities.Order;
import entities.Table;
import entities.User;
import enums.UserType;
import enums.EndTableSessionType;
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

	// **************************** Database Configuration
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
		boolean hasPhone = phoneNumber != null && !phoneNumber.isBlank();
		boolean hasEmail = email != null && !email.isBlank();

		if (!hasPhone && !hasEmail) {
			throw new IllegalArgumentException("Guest must have phoneNumber or email");
		}

		final String FIND_BY_PHONE = "SELECT user_id, phoneNumber, email FROM users WHERE type='GUEST' AND phoneNumber = ?";
		final String FIND_BY_EMAIL = "SELECT user_id, phoneNumber, email FROM users WHERE type='GUEST' AND email = ?";

		final String INSERT_GUEST = "INSERT INTO users (phoneNumber, email, type) VALUES (?, ?, 'GUEST')";

		final String UPDATE_PHONE = "UPDATE users SET phoneNumber = ? WHERE user_id = ? AND type='GUEST'";
		final String UPDATE_EMAIL = "UPDATE users SET email = ? WHERE user_id = ? AND type='GUEST'";

		final String SELECT_BY_ID = "SELECT user_id, phoneNumber, email FROM users WHERE user_id = ? AND type='GUEST'";

		Connection conn = null;

		try {
			conn = borrow();

			// CASE 1: ONLY ONE IDENTIFIER - OLD BEHAVIOR

			if (!(hasPhone && hasEmail)) {
				String sql = hasPhone ? FIND_BY_PHONE : FIND_BY_EMAIL;
				String key = hasPhone ? phoneNumber : email;

				try (PreparedStatement ps = conn.prepareStatement(sql)) {
					ps.setString(1, key);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							return new User(rs.getInt("user_id"), rs.getString("phoneNumber"), rs.getString("email"),
									UserType.GUEST);
						}
					}
				}

				// create new guest (AUTO_INCREMENT)
				int newId;
				try (PreparedStatement ps = conn.prepareStatement(INSERT_GUEST,
						java.sql.Statement.RETURN_GENERATED_KEYS)) {

					if (hasPhone)
						ps.setString(1, phoneNumber);
					else
						ps.setNull(1, java.sql.Types.VARCHAR);

					if (hasEmail)
						ps.setString(2, email);
					else
						ps.setNull(2, java.sql.Types.VARCHAR);

					int affected = ps.executeUpdate();
					if (affected != 1)
						throw new SQLException("Insert guest failed: affected rows=" + affected);

					try (ResultSet keys = ps.getGeneratedKeys()) {
						if (!keys.next())
							throw new SQLException("Insert guest: no generated key returned");
						newId = keys.getInt(1);
					}
				}

				return new User(newId, hasPhone ? phoneNumber : null, hasEmail ? email : null, UserType.GUEST);
			}

			// CASE 2: BOTH IDENTIFIERS - FILL / MERGE LOGIC

			conn.setAutoCommit(false);

			User phoneUser = null;
			User emailUser = null;

			// find by phone
			try (PreparedStatement ps = conn.prepareStatement(FIND_BY_PHONE)) {
				ps.setString(1, phoneNumber);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						phoneUser = new User(rs.getInt("user_id"), rs.getString("phoneNumber"), rs.getString("email"),
								UserType.GUEST);
					}
				}
			}

			// find by email
			try (PreparedStatement ps = conn.prepareStatement(FIND_BY_EMAIL)) {
				ps.setString(1, email);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						emailUser = new User(rs.getInt("user_id"), rs.getString("phoneNumber"), rs.getString("email"),
								UserType.GUEST);
					}
				}
			}

			// none found - create new with BOTH
			if (phoneUser == null && emailUser == null) {
				int newId;
				try (PreparedStatement ps = conn.prepareStatement(INSERT_GUEST,
						java.sql.Statement.RETURN_GENERATED_KEYS)) {
					ps.setString(1, phoneNumber);
					ps.setString(2, email);

					int affected = ps.executeUpdate();
					if (affected != 1)
						throw new SQLException("Insert guest failed: affected rows=" + affected);

					try (ResultSet keys = ps.getGeneratedKeys()) {
						if (!keys.next())
							throw new SQLException("Insert guest: no generated key returned");
						newId = keys.getInt(1);
					}
				}

				User result = fetchGuestById(conn, SELECT_BY_ID, newId);
				conn.commit();
				return result;
			}

			// phone exists, email doesn't - fill email if missing
			if (phoneUser != null && emailUser == null) {
				if (phoneUser.getEmail() == null || phoneUser.getEmail().isBlank()) {
					try (PreparedStatement ps = conn.prepareStatement(UPDATE_EMAIL)) {
						ps.setString(1, email);
						ps.setInt(2, phoneUser.getUserId());
						ps.executeUpdate();
					}
				}
				User result = fetchGuestById(conn, SELECT_BY_ID, phoneUser.getUserId());
				conn.commit();
				return result;
			}

			// email exists, phone doesn't - fill phone if missing
			if (phoneUser == null) { // emailUser != null
				if (emailUser.getPhoneNumber() == null || emailUser.getPhoneNumber().isBlank()) {
					try (PreparedStatement ps = conn.prepareStatement(UPDATE_PHONE)) {
						ps.setString(1, phoneNumber);
						ps.setInt(2, emailUser.getUserId());
						ps.executeUpdate();
					}
				}
				User result = fetchGuestById(conn, SELECT_BY_ID, emailUser.getUserId());
				conn.commit();
				return result;
			}

			// both found
			if (phoneUser.getUserId() == emailUser.getUserId()) {
				User result = fetchGuestById(conn, SELECT_BY_ID, phoneUser.getUserId());
				conn.commit();
				return result;
			}

			// TRUE MERGE (two different rows) without violating chk_user_contact
			int primaryId = phoneUser.getUserId(); // keep the phone row
			int secondaryId = emailUser.getUserId(); // delete the email row (after moving FKs)

			// 1) move foreign keys first (orders/members/staff_accounts etc.)
			moveForeignKeys(conn, primaryId, secondaryId);

			// 2) delete secondary row (no FK references it now)
			try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id = ? AND type='GUEST'")) {
				ps.setInt(1, secondaryId);
				ps.executeUpdate();
			}

			// 3) fill missing fields on primary
			try (PreparedStatement ps = conn.prepareStatement(
					"UPDATE users SET email = COALESCE(email, ?) WHERE user_id = ? AND type='GUEST'")) {
				ps.setString(1, email);
				ps.setInt(2, primaryId);
				ps.executeUpdate();
			}

			conn.commit();
			return fetchGuestById(conn, SELECT_BY_ID, primaryId);

		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignore) {
			}
			logger.log("[ERROR] findOrCreateGuestUser: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		} finally {
			try {
				if (conn != null) {
					conn.setAutoCommit(true);
					release(conn);
				}
			} catch (Exception ignore) {
			}
		}
	}

	private User fetchGuestById(Connection conn, String sql, int userId) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					throw new SQLException("Guest not found by id=" + userId);
				return new User(rs.getInt("user_id"), rs.getString("phoneNumber"), rs.getString("email"),
						UserType.GUEST);
			}
		}
	}

	private void moveForeignKeys(Connection conn, int primaryId, int secondaryId) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET user_id = ? WHERE user_id = ?")) {
			ps.setInt(1, primaryId);
			ps.setInt(2, secondaryId);
			ps.executeUpdate();
		}

		try (PreparedStatement ps = conn.prepareStatement("UPDATE members SET user_id = ? WHERE user_id = ?")) {
			ps.setInt(1, primaryId);
			ps.setInt(2, secondaryId);
			ps.executeUpdate();
		}

		try (PreparedStatement ps = conn.prepareStatement("UPDATE staff_accounts SET user_id = ? WHERE user_id = ?")) {
			ps.setInt(1, primaryId);
			ps.setInt(2, secondaryId);
			ps.executeUpdate();
		}
	}

	/**
	 * Finds a member user by their member code.
	 * 
	 * @param memberCode The member code to search for
	 * @return The User object if found, null otherwise
	 */
	public User findMemberUserByCode(int memberCode) {
		logger.log("[DEBUG] findMemberUserByCode called with memberCode: " + memberCode);
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
					if (!rs.next()) {
						logger.log("[DEBUG] No result found for memberCode: " + memberCode);
						return null;
					}
					int userId = rs.getInt("user_id");
					logger.log("[DEBUG] Found userId: " + userId + " for memberCode: " + memberCode);
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
			logger.log("[ERROR] findMemberUserByCode SQLException: " + ex.getMessage());
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

	// TODO: Create query for member registration by staff

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
						String encryptedPassword = rs.getString("password");

						// Decrypt and verify the password
						try {
							String decryptedPassword = PasswordUtil.decrypt(encryptedPassword);
							if (password.equals(decryptedPassword)) {
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

	// Returns user_id for an exact phone match, or null if not found
	public Integer findUserIdByPhone(String phoneNumber) {
		phoneNumber = (phoneNumber == null) ? null : phoneNumber.trim();
		if (phoneNumber == null || phoneNumber.isEmpty())
			return null;

		final String qry = "SELECT user_id FROM users WHERE phoneNumber=?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, phoneNumber);
				try (ResultSet rs = ps.executeQuery()) {
					return rs.next() ? rs.getInt(1) : null;
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in findUserIdByPhone: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		} finally {
			release(conn);
		}
	}

	// Returns user_id for an exact email match, or null if not found
	public Integer findUserIdByEmail(String email) {
		email = (email == null) ? null : email.trim();
		if (email == null || email.isEmpty())
			return null;

		final String qry = "SELECT user_id FROM users WHERE email=?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, email);
				try (ResultSet rs = ps.executeQuery()) {
					return rs.next() ? rs.getInt(1) : null;
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in findUserIdByEmail: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		} finally {
			release(conn);
		}
	}

	// True if that user already has a staff_accounts row
	public boolean staffAccountExistsForUserId(int userId) {
		final String qry = "SELECT 1 FROM staff_accounts WHERE user_id=?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, userId);
				try (ResultSet rs = ps.executeQuery()) {
					return rs.next();
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in staffAccountExistsForUserId: " + ex.getMessage());
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
			UserType userType, String firstName, String lastName, String address) {
		if (userType != UserType.EMPLOYEE && userType != UserType.MANAGER) {
			logger.log("[ERROR] Invalid user type for employee creation: " + userType);
			return null;
		}

		phoneNumber = (phoneNumber == null) ? null : phoneNumber.trim();
		email = (email == null) ? null : email.trim();
		if (phoneNumber != null && phoneNumber.isEmpty())
			phoneNumber = null;
		if (email != null && email.isEmpty())
			email = null;

		Connection conn = null;
		try {
			conn = borrow();
			conn.setAutoCommit(false);

			try {
				Integer userIdByPhone = null;
				Integer userIdByEmail = null;

				if (phoneNumber != null) {
					try (PreparedStatement ps = conn
							.prepareStatement("SELECT user_id FROM users WHERE phoneNumber=?")) {
						ps.setString(1, phoneNumber);
						try (ResultSet rs = ps.executeQuery()) {
							if (rs.next())
							userIdByPhone = rs.getInt(1);
						}
					}
				}

				if (email != null) {
					try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE email=?")) {
						ps.setString(1, email);
						try (ResultSet rs = ps.executeQuery()) {
							if (rs.next())
								userIdByEmail = rs.getInt(1);
						}
					}
				}

				// If phone belongs to one user and email belongs to another => ambiguous
				if (userIdByPhone != null && userIdByEmail != null && !userIdByPhone.equals(userIdByEmail)) {
					conn.rollback();
					conn.setAutoCommit(true);
					logger.log("[STAFF_CREATE] Conflict: phone and email belong to different users.");
					return null;
				}

				Integer userId = (userIdByPhone != null) ? userIdByPhone : userIdByEmail;

				// Encrypt password (reversible for password recovery)
				String encryptedPassword = PasswordUtil.encrypt(password);

				if (userId == null) {
					// Create new user
					try (PreparedStatement ps = conn.prepareStatement(
							"INSERT INTO users (phoneNumber, email, type) VALUES (?, ?, ?)",
							java.sql.Statement.RETURN_GENERATED_KEYS)) {
						ps.setString(1, phoneNumber);
						ps.setString(2, email);
						ps.setString(3, userType.name());
						ps.executeUpdate();
						try (ResultSet keys = ps.getGeneratedKeys()) {
							if (!keys.next())
								throw new SQLException("Failed to read generated user_id");
							userId = keys.getInt(1);
						}
					}
				} else {
					// Promote existing user + fill missing contact fields
					try (PreparedStatement ps = conn.prepareStatement(
							"UPDATE users SET type=?, phoneNumber=COALESCE(phoneNumber, ?), email=COALESCE(email, ?) WHERE user_id=?")) {
						ps.setString(1, userType.name());
						ps.setString(2, phoneNumber);
						ps.setString(3, email);
						ps.setInt(4, userId);
						ps.executeUpdate();
					}

					// Prevent creating staff twice
					try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM staff_accounts WHERE user_id=?")) {
						ps.setInt(1, userId);
						try (ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								conn.rollback();
								conn.setAutoCommit(true);
								logger.log("[STAFF_CREATE] user_id already has staff_accounts row.");
								return null;
							}
						}
					}
				}

				// Insert staff account
				try (PreparedStatement ps = conn.prepareStatement(
						"INSERT INTO staff_accounts (user_id, username, password) VALUES (?, ?, ?)")) {
					ps.setInt(1, userId);
					ps.setString(2, username);
					ps.setString(3, encryptedPassword);
					ps.executeUpdate();
				}

				// Ensure members row exists (staff are members too)
				boolean memberExists;
				try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM members WHERE user_id=?")) {
					ps.setInt(1, userId);
					try (ResultSet rs = ps.executeQuery()) {
						memberExists = rs.next();
					}
				}

				if (!memberExists) {
					int memberCode = generateUniqueMemberCode(conn);
					try (PreparedStatement ps = conn.prepareStatement(
							"INSERT INTO members (user_id, member_code, f_name, l_name, address) VALUES (?, ?, ?, ?, ?)")) {
						ps.setInt(1, userId);
						ps.setInt(2, memberCode);
						ps.setString(3, firstName);
						ps.setString(4, lastName);
						ps.setString(5, address);
						ps.executeUpdate();
					}
				} else {
					// Keep member details aligned with input
					try (PreparedStatement ps = conn
							.prepareStatement("UPDATE members SET f_name=?, l_name=?, address=? WHERE user_id=?")) {
						ps.setString(1, firstName);
						ps.setString(2, lastName);
						ps.setString(3, address);
						ps.setInt(4, userId);
						ps.executeUpdate();
					}
				}

				conn.commit();
				conn.setAutoCommit(true);

				logger.log("[ADMIN] Staff created/promoted: username=" + username + ", type=" + userType + ", user_id="
						+ userId);
				return new User(userId, phoneNumber, email, username, userType);

			} catch (Exception e) {
				conn.rollback();
				conn.setAutoCommit(true);
				logger.log("[ERROR] Failed to create/promote staff user: " + e.getMessage());
				e.printStackTrace();
				return null;
			}

		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in createEmployeeUser: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		} finally {
			release(conn);
		}
	}

	/**
	 * Recovers staff credentials (username and password) based on email or phone.
	 * * @param email The staff member's email (can be null/empty)
	 * 
	 * @param phoneNumber The staff member's phone (can be null/empty)
	 * @return A string in the format "username:password" if found, "NOT_FOUND" if
	 *         no match, or "ERROR_DB" on failure.
	 */
	public String recoverStaffLogin(String email, String phoneNumber) {
		String searchEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : "IMPOSSIBLE_EMAIL_XYZ";
		String searchPhone = (phoneNumber != null && !phoneNumber.trim().isEmpty()) ? phoneNumber.trim()
				: "IMPOSSIBLE_PHONE_XYZ";

		final String qry = "SELECT sa.user_id, sa.username " + "FROM users u "
				+ "JOIN staff_accounts sa ON u.user_id = sa.user_id " + "WHERE (u.email = ? OR u.phoneNumber = ?) "
				+ "AND u.type IN ('EMPLOYEE', 'MANAGER')";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, searchEmail);
				ps.setString(2, searchPhone);

				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						int userId = rs.getInt("user_id");
						String foundUser = rs.getString("username");

						// Generate a new temporary password
						String tempPassword = generateTempPassword();

						// Encrypt password (must match findEmployeeUser which decrypts)
						String encryptedPassword = PasswordUtil.encrypt(tempPassword);

						// Update the password in the database
						String updateQry = "UPDATE staff_accounts SET password = ? WHERE user_id = ?";
						try (PreparedStatement updatePs = conn.prepareStatement(updateQry)) {
						    updatePs.setString(1, encryptedPassword);
						    updatePs.setInt(2, userId);
						    updatePs.executeUpdate();
						}

						// Return username and new temporary password
						return foundUser + ":" + tempPassword;
					} else {
						return "NOT_FOUND";
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] recoverStaffLogin: " + ex.getMessage());
			ex.printStackTrace();
			return "ERROR_DB";
		} finally {
			release(conn);
		}
	}
	
	/**
	 * Generates a random 8-character temporary password.
	 */
	private String generateTempPassword() {
		String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 8; i++) {
			sb.append(chars.charAt(random.nextInt(chars.length())));
		}
		return sb.toString();
	}

	/*
	 * Retrieves all customers and converts them to DTOs for the client. Returns
	 * List<UserData>.
	 */
	public List<UserData> getAllCustomers() {
		List<UserData> usersList = new ArrayList<>();
		final String qry = "SELECT u.phoneNumber, u.email, u.type, " + "m.member_code, m.f_name, m.l_name, m.address "
				+ "FROM users u " + "LEFT JOIN members m ON u.user_id = m.user_id";

		// Execute query and build UserData list
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						String email = rs.getString("email");
						String phone = rs.getString("phoneNumber");
						UserType type = UserType.valueOf(rs.getString("type"));
						// Build UserData based on type
						if (type == UserType.MEMBER) {
							String fname = rs.getString("f_name");
							String lname = rs.getString("l_name");
							String address = rs.getString("address");
							String memberCode = rs.getString("member_code");

							usersList.add(new UserData(fname, lname, memberCode, phone, email, type, address));
						} else if (type == UserType.GUEST) {
							// sending null because GUEST has no additional data
							usersList.add(new UserData(null, null, null, phone, email, UserType.GUEST, null));
						}
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getAllCustomersInDB: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		} finally {
			release(conn);
		}
		return usersList;
	}

	/**
	 * Generates a unique 6-digit member_code not already present in the members
	 * table.
	 * 
	 * @param conn Active database connection
	 * @return Unique 6-digit member_code
	 * @throws SQLException if unable to generate a unique ID after multiple
	 *                      attempts
	 */
	public int generateUniqueMemberCode(Connection conn) throws SQLException {
		Random random = new Random();
		String checkSql = "SELECT 1 FROM members WHERE member_code = ?";
		// PreparedStatement for checking existence
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
	 * Registers a new member user. Handles existing users by upgrading GUESTs or
	 * returning error codes for conflicts.
	 * 
	 * @param newMemberData List containing [fName, lName, email, phoneNumber,
	 *                      address]
	 * @return member_code if successful, negative error code otherwise -1:
	 *         DATABASE_ERROR -2: ALREADY_MEMBER -3: ALREADY_STAFF -4:
	 *         CONTACT_CONFLICT
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

		try {
			conn = borrow();
			conn.setAutoCommit(false);

			Integer userIdByPhone = null;
			String typeByPhone = null;

			Integer userIdByEmail = null;
			String typeByEmail = null;

			// Look up phone match
			if (phoneNumber != null) {
				final String qPhone = "SELECT user_id, type FROM users WHERE phoneNumber = ? LIMIT 1";
				try (PreparedStatement ps = conn.prepareStatement(qPhone)) {
					ps.setString(1, phoneNumber);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							userIdByPhone = rs.getInt("user_id");
							typeByPhone = rs.getString("type");
						}
					}
				}
			}

			// Look up email match
			if (email != null) {
				final String qEmail = "SELECT user_id, type FROM users WHERE email = ? LIMIT 1";
				try (PreparedStatement ps = conn.prepareStatement(qEmail)) {
					ps.setString(1, email);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							userIdByEmail = rs.getInt("user_id");
							typeByEmail = rs.getString("type");
						}
					}
				}
			}

			// Conflict: phone belongs to one user, email belongs to another
			if (userIdByPhone != null && userIdByEmail != null && !userIdByPhone.equals(userIdByEmail)) {
				conn.rollback();
				return -4; // CONTACT_CONFLICT
			}

			Integer userId = (userIdByPhone != null) ? userIdByPhone : userIdByEmail;
			String existingType = (userIdByPhone != null) ? typeByPhone : typeByEmail;

			// If user exists
			if (userId != null) {
				if ("GUEST".equals(existingType)) {
					// upgrade guest -> member; fill missing only
					final String upgradeSql = "UPDATE users " + "SET type = 'MEMBER', "
							+ "    email = COALESCE(?, email), " + "    phoneNumber = COALESCE(?, phoneNumber) "
							+ "WHERE user_id = ?";

					try (PreparedStatement ps = conn.prepareStatement(upgradeSql)) {
						ps.setString(1, email);
						ps.setString(2, phoneNumber);
						ps.setInt(3, userId);
						ps.executeUpdate();
					}
				} else if ("MEMBER".equals(existingType)) {
					conn.rollback();
					return -2; // ALREADY_MEMBER
				} else {
					// EMPLOYEE / MANAGER / anything else
					conn.rollback();
					return -3; // ALREADY_STAFF
				}
			}

			// If not found, insert new MEMBER user
			if (userId == null) {
				final String insertUserSql = "INSERT INTO users (phoneNumber, email, type) VALUES (?, ?, 'MEMBER')";

				try (PreparedStatement psInsertUser = conn.prepareStatement(insertUserSql,
						java.sql.Statement.RETURN_GENERATED_KEYS)) {

					if (phoneNumber == null)
						psInsertUser.setNull(1, Types.VARCHAR);
					else
						psInsertUser.setString(1, phoneNumber);

					if (email == null)
						psInsertUser.setNull(2, Types.VARCHAR);
					else
						psInsertUser.setString(2, email);

					int affected = psInsertUser.executeUpdate();
					if (affected != 1)
						throw new SQLException("Insert MEMBER user failed: affected rows=" + affected);

					try (ResultSet keys = psInsertUser.getGeneratedKeys()) {
						if (!keys.next())
							throw new SQLException("Failed to read generated user_id");
						userId = keys.getInt(1);
					}
				}
			}

			// Insert members row
			int memberCode = generateUniqueMemberCode(conn);

			final String insertMemberSql = "INSERT INTO members (user_id, member_code, f_name, l_name, address) VALUES (?, ?, ?, ?, ?)";

			try (PreparedStatement psInsertMember = conn.prepareStatement(insertMemberSql)) {
				psInsertMember.setInt(1, userId);
				psInsertMember.setInt(2, memberCode);
				psInsertMember.setString(3, fName);
				psInsertMember.setString(4, lName);
				psInsertMember.setString(5, address);
				psInsertMember.executeUpdate();
			}

			conn.commit();
			return memberCode;

		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in registerNewMember: " + ex.getMessage());
			ex.printStackTrace();
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignore) {
			}
			return -1;

		} finally {
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
				} catch (SQLException ignore) {
				}
				release(conn);
			}
		}
	}

	/**
	 * Finds a member's member_code by their email or phone number.
	 * 
	 * @param email       The member's email (can be null/empty)
	 * @param phoneNumber The member's phone number (can be null/empty)
	 * @return The member_code if found, -1 otherwise
	 * @throws IllegalArgumentException if both email and phoneNumber are null/empty
	 */
	public int findMemberCodeByEmailOrPhone(String email, String phoneNumber) {
		boolean hasPhone = phoneNumber != null && !phoneNumber.isBlank();
		boolean hasEmail = email != null && !email.isBlank();
		if (!hasPhone && !hasEmail) {
			throw new IllegalArgumentException("Must provide phoneNumber or email to find member code");
		}
		// Build dynamic SQL based on provided parameters
		final String FIND_MEMBER_SQL = "SELECT m.member_code " + "FROM members m "
				+ "JOIN users u ON m.user_id = u.user_id " + "WHERE u.type = 'MEMBER' " + "AND ("
				+ (hasPhone ? "u.phoneNumber = ?" : "") + (hasPhone && hasEmail ? " OR " : "")
				+ (hasEmail ? "u.email = ?" : "") + ") LIMIT 1";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(FIND_MEMBER_SQL)) {
				int paramIndex = 1;
				if (hasPhone) {
					ps.setString(paramIndex++, phoneNumber);
				}
				if (hasEmail) {
					ps.setString(paramIndex++, email);
				}
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt("member_code");
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in findMemberCodeByEmailOrPhone: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);

		}
		return -1;
	}

	// ****************************** Order Operations
	// ******************************

	/**
	 * Creates a new order in the orders table.
	 * 
	 * @param orderData List of order data: [0] User ID (int) [1] Date (LocalDate)
	 *                  [2] Diners Amount (int) [3] Time (LocalTime) [4]
	 *                  Confirmation Code (String)
	 * @param type      The OrderType (RESERVATION or WAITLIST)
	 * @param status    The initial OrderStatus
	 * @return true if order creation is successful, false otherwise
	 */
	public boolean setNewOrder(List<Object> orderData, OrderType type, OrderStatus status) {
		final String sql = "INSERT INTO orders "
				+ "(user_id, order_date, number_of_guests, order_time, confirmation_code, "
				+ "order_type, status, date_of_placing_order, notified_at, cancelled_at) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		// Execute insert
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {

				int userId = (int) orderData.get(0);
				LocalDate date = (LocalDate) orderData.get(1);
				int diners = (int) orderData.get(2);
				LocalTime time = (LocalTime) orderData.get(3);
				String code = (String) orderData.get(4);

				ps.setInt(1, userId);
				// ? MUST match chk_order_slot_rules
				if (type == OrderType.WAITLIST) {
					ps.setNull(2, Types.DATE);
					ps.setNull(4, Types.TIME);
				} else { // RESERVATION
					if (date == null || time == null) {
						logger.log("[ERROR] RESERVATION requires non-null order_date and order_time");
						return false;
					}
					ps.setDate(2, Date.valueOf(date));
					ps.setTime(4, Time.valueOf(time));
				}

				ps.setInt(3, diners);
				ps.setString(5, code);
				ps.setString(6, type.name());
				ps.setString(7, status.name());
				ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
				ps.setNull(9, Types.TIMESTAMP); // notified_at
				ps.setNull(10, Types.TIMESTAMP); // cancelled_at

				ps.executeUpdate();
				return true;
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in setNewOrder: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves a list of orders for a specific date.
	 * 
	 * @param date The date to retrieve orders for
	 * @return List of Order objects for the specified date
	 */
	public List<Order> getOrdersByDate(LocalDate date) {
		List<Order> orders = new ArrayList<>();
		if (date == null) {
			return orders;
		}
		boolean isToday = LocalDate.now().equals(date);
		String qry;
		// Different query logic for current date vs future dates
		if (isToday) {
			qry = "SELECT order_time, number_of_guests " + "FROM orders "
					+ "WHERE order_type IN ('RESERVATION', 'WAITLIST') " + "AND order_date = ? "
					+ "AND status IN ('PENDING', 'NOTIFIED', 'SEATED') " + "AND order_time BETWEEN ? AND ? "
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

	/**
	 * Retrieves full order details for reservations on a specific date, including
	 * associated table numbers and user types.
	 * 
	 * @param date The date to retrieve orders for
	 * @return List of Order objects with full details for the specified date
	 */
	public List<Order> getFullOrdersByDate(LocalDate date) {
		List<Order> orders = new ArrayList<>();
		if (date == null)
			return orders;

		// Select ALL fields needed for the staff table
		String qry = "SELECT o.*, ts.tableNum, u.type " + "FROM orders o " + "JOIN users u ON o.user_id = u.user_id "
				+ "LEFT JOIN table_sessions ts ON o.order_number = ts.order_number AND ts.left_at IS NULL "
				+ "WHERE o.order_date = ? AND o.order_type = 'RESERVATION'";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setDate(1, Date.valueOf(date));

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						int id = rs.getInt("order_number");
						LocalTime time = rs.getTime("order_time").toLocalTime();
						int diners = rs.getInt("number_of_guests");
						String statusStr = rs.getString("status");
						String confirmCode = rs.getString("confirmation_code");
						int userId = rs.getInt("user_id");

						int tableId = rs.getInt("tableNum"); // can be null if no table assigned
						if (rs.wasNull()) {
							tableId = 0; // to indicate no table assigned
						}

						String uType = rs.getString("type");

						Order order = new Order(id, date, time, diners, confirmCode, userId, null,
								OrderStatus.valueOf(statusStr), null);
						order.setTableId(tableId);
						order.setUserTypeStr(uType);
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
	 * Retrieves all orders associated with a specific user ID.
	 * 
	 * @param userId The user ID to retrieve orders for
	 * @return List of Order objects for the specified user
	 */
	public List<Order> getOrdersByUserId(int userId) {
		logger.log("[DEBUG] getOrdersByUserId called with userId: " + userId);
		List<Order> orders = new ArrayList<>();
		// Query joins with table_sessions to get tableNum and users to get user type
		String query = "SELECT o.*, ts.tableNum, u.type as user_type " + "FROM orders o "
				+ "LEFT JOIN table_sessions ts ON o.order_number = ts.order_number AND ts.left_at IS NULL "
				+ "LEFT JOIN users u ON o.user_id = u.user_id " + "WHERE o.user_id = ? "
				+ "ORDER BY o.order_date DESC, o.order_time DESC";
		// Execute query and map results to Order objects
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setInt(1, userId);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						// Map ResultSet to Order
						int orderNumber = rs.getInt("order_number");
						Date sqlDate = rs.getDate("order_date");
						LocalDate orderDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;
						Time sqlTime = rs.getTime("order_time");
						LocalTime orderTime = (sqlTime != null) ? sqlTime.toLocalTime() : null;
						int diners = rs.getInt("number_of_guests");
						String code = rs.getString("confirmation_code");
						OrderStatus status = OrderStatus.valueOf(rs.getString("status"));
						String typeStr = rs.getString("order_type");
						OrderType type = (typeStr != null) ? OrderType.valueOf(typeStr) : OrderType.RESERVATION;
						int tableId = rs.getInt("tableNum");
						if (rs.wasNull())
							tableId = 0; // no table assigned
						String userType = rs.getString("user_type");
						Order order = new Order(orderNumber, orderDate, orderTime, diners, code, userId, type, status,
								null);
						order.setTableId(tableId); // set tableId if applicable
						order.setUserTypeStr(userType); // set user type for display
						orders.add(order);
					}
				}
			}
			logger.log("[DEBUG] getOrdersByUserId returning " + orders.size() + " orders for userId: " + userId);
		} catch (SQLException ex) {
			logger.log("[ERROR] getOrdersByUserId: " + ex.getMessage());
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
					// Map ResultSet to Order object
					int orderNumber = rs.getInt("order_number");
					Date d = rs.getDate("order_date");
					Time t = rs.getTime("order_time");
					LocalDate orderDate = (d != null) ? d.toLocalDate() : null;
					LocalTime orderTime = (t != null) ? t.toLocalTime() : null;
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
	 * Retrieves a list of active and upcoming orders for the specified date and
	 * time.
	 * 
	 * Active orders include those with status 'SEATED'. Upcoming reservations
	 * include those with type 'RESERVATION' and status 'PENDING' or 'NOTIFIED'
	 * within the estimated dining window.
	 * 
	 * @param today         The current date
	 * @param now           The current time
	 * @param walkInEndTime The end time for walk-in customers
	 * @return List of active and upcoming Order objects
	 */
	public List<Order> getActiveAndUpcomingOrders(LocalDate today, LocalTime now, LocalTime walkInEndTime) {
		List<Order> orders = new ArrayList<>();

		// Logic update:
		// 1. Check for currently SEATED customers.
		// 2. Check for upcoming RESERVATIONS (type = RESERVATION) that are
		// PENDING/NOTIFIED
		// and fall within the estimated dining window.
		String query = "SELECT * FROM orders " + "WHERE order_date = ? " + "AND (" + "   status = 'SEATED' " + "   OR "
				+ "   (order_type = 'RESERVATION' " + "    AND status IN ('PENDING', 'NOTIFIED') "
				+ "    AND order_time >= ? AND order_time <= ?) " + ")";

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

						// Handle time (check for nulls if necessary, though logic implies not null
						// here)
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
	 * Retrieves all active reservations for a specific member for today. Active
	 * reservations are those with status PENDING, NOTIFIED, or APPROVED.
	 * 
	 * @param userId The ID of the member user
	 * @return List of active Order objects for today
	 */
	public List<Order> getMemberActiveReservationsForToday(int userId) {
		List<Order> orders = new ArrayList<>();

		// Logic: User's ID + Type is RESERVATION + Status is valid + Date is TODAY
		String qry = "SELECT * FROM orders " + "WHERE user_id = ? " + "AND order_type = 'RESERVATION' "
				+ "AND status IN ('PENDING', 'NOTIFIED', 'APPROVED') " + "AND order_date = CURRENT_DATE "
				+ "ORDER BY order_time ASC";
		// Execute query and map results to Order objects
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, userId);

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						Order order = new Order();
						order.setOrderNumber(rs.getInt("order_number"));
						order.setUserId(rs.getInt("user_id"));

						Date sqlDate = rs.getDate("order_date");
						if (sqlDate != null)
							order.setOrderDate(sqlDate.toLocalDate());

						Time sqlTime = rs.getTime("order_time");
						if (sqlTime != null)
							order.setOrderHour(sqlTime.toLocalTime());

						order.setDinersAmount(rs.getInt("number_of_guests"));
						order.setConfirmationCode(rs.getString("confirmation_code"));
						order.setStatus(OrderStatus.valueOf(rs.getString("status")));
						order.setOrderType(OrderType.valueOf(rs.getString("order_type")));

						orders.add(order);
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] getMemberActiveReservationsForToday: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return orders;
	}

	/**
	 * Updates the status of an order in the database based on the confirmation
	 * code.
	 * 
	 * @param confirmationCode The confirmation code of the order
	 * @param status           The new OrderStatus to set
	 * @return true if the update is successful, false otherwise
	 */
	public boolean updateOrderStatusInDB(String confirmationCode, OrderStatus status) {
		if (confirmationCode == null || confirmationCode.trim().isEmpty() || status == null) {
			return false;
		}
		String sql;
		switch (status) {
		case NOTIFIED:
			sql = "UPDATE orders SET status='NOTIFIED', notified_at=NOW() "
					+ "WHERE confirmation_code=? AND status='PENDING'";
			break;
		case NO_SHOW:
			sql = "UPDATE orders SET status='NO_SHOW', cancelled_at=NOW() "
					+ "WHERE confirmation_code=? AND status IN ('PENDING','NOTIFIED')";
			break;

		case SEATED:
			sql = "UPDATE orders SET status='SEATED' WHERE confirmation_code=?";
			break;

		case COMPLETED:
			sql = "UPDATE orders SET status='COMPLETED' WHERE confirmation_code=?";
			break;

		case CANCELLED:
			sql = "UPDATE orders SET status='CANCELLED', cancelled_at=NOW() WHERE confirmation_code=?";
			break;

		case PENDING:
			sql = "UPDATE orders SET status=? WHERE confirmation_code=?";
			break;

		default:
			logger.log("[WARN] Unsupported OrderStatus: " + status);
			return false;
		}
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {

				if (status == OrderStatus.PENDING) {
					ps.setString(1, status.name());
					ps.setString(2, confirmationCode);
				} else {
					ps.setString(1, confirmationCode);
				}

				boolean ok = ps.executeUpdate() > 0;

				if (ok) {
					switch (status) {
					case NOTIFIED:
						updateWaitingListStatus(confirmationCode, "NOTIFIED");
						break;
					case NO_SHOW:
						updateWaitingListStatus(confirmationCode, "EXPIRED");
						break;
					case SEATED:
						updateWaitingListStatus(confirmationCode, "SEATED");
						break;
					case CANCELLED:
						updateWaitingListStatus(confirmationCode, "CANCELLED");
						break;
					default:
						break;
					}
				}
				return ok;
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in updateOrderStatusInDB: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves the status of an order from the database by its confirmation code.
	 * 
	 * @param confirmationCode The confirmation code of the order
	 * @return The OrderStatus if found, null otherwise
	 */
	public OrderStatus getOrderStatusInDB(String confirmationCode) {
		String qry = "SELECT status FROM orders WHERE confirmation_code = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, confirmationCode);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return OrderStatus.valueOf(rs.getString("status"));
					} else {
						return null; // Order not found
					}
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getOrderStatusInDB: " + e.getMessage());
			return null;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves all SEATED reservations for a specific member for today.
	 * 
	 * @param userId The ID of the member user
	 * @return List of SEATED Order objects for today
	 */
	public List<Order> getMemberSeatedReservationsForToday(int userId) {
		List<Order> orders = new ArrayList<>();
		String qry = "SELECT * FROM orders " + "WHERE user_id = ? " + "AND status = 'SEATED' "
				+ "AND order_date = CURRENT_DATE"; // Only care about active session today

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, userId);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						Order order = new Order();
						order.setOrderNumber(rs.getInt("order_number"));
						order.setConfirmationCode(rs.getString("confirmation_code"));
						order.setDinersAmount(rs.getInt("number_of_guests"));

						Time t = rs.getTime("order_time");
						if (t != null)
							order.setOrderHour(t.toLocalTime());

						orders.add(order);
					}
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return orders;
	}

	/**
	 * Recovers the confirmation code for a guest user who is currently SEATED
	 * today, based on their email or phone number.
	 * 
	 * @param email The email of the guest user
	 * @param phone The phone number of the guest user
	 * @return The confirmation code if found, "NOT_FOUND" otherwise
	 */
	public String recoverGuestSeatedCode(String email, String phone) {
		// Prevent empty string matching
		String sEmail = (email != null && !email.isBlank()) ? email : "NO_MATCH";
		String sPhone = (phone != null && !phone.isBlank()) ? phone : "NO_MATCH";

		String qry = "SELECT o.confirmation_code " + "FROM orders o " + "JOIN users u ON o.user_id = u.user_id "
				+ "WHERE (u.email = ? OR u.phoneNumber = ?) " + "AND o.status = 'SEATED' "
				+ "AND o.order_date = CURRENT_DATE " + "LIMIT 1";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, sEmail);
				ps.setString(2, sPhone);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getString("confirmation_code");
					}
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return "NOT_FOUND";
	}

	// ****************************** Waiting List Operations
	// ******************************

	/**
	 * Checks if a user with the given confirmation code is currently in the waiting
	 * list.
	 * 
	 * @param confirmationCode The confirmation code to check
	 * @return true if the user is in the waiting list, false otherwise
	 */
	public boolean isUserInWaitingList(int userID) {
		String qry = "SELECT 1 FROM orders " + "WHERE user_id = ? " + "AND order_type = 'WAITLIST' "
				+ "AND status IN ('PENDING','NOTIFIED')";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, userID);
				try (ResultSet rs = ps.executeQuery()) {
					return rs.next(); // returns true if at least one matching order exists
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] isUserInWaitingList: " + e.getMessage());
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Removes a user from the waiting list by updating their order status to
	 * 'CANCELLED'.
	 * 
	 * @param confirmationCode The confirmation code of the order to cancel
	 * @return true if the order was successfully cancelled, false otherwise
	 */
	public boolean removeFromWaitingList(String confirmationCode) {
		final String qry = "UPDATE orders " + "SET status = 'CANCELLED', cancelled_at = ? "
				+ "WHERE confirmation_code = ? " + "AND order_type = 'WAITLIST' "
				+ "AND status IN ('PENDING','NOTIFIED')";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
				ps.setString(2, confirmationCode);

				int rowsAffected = ps.executeUpdate();
				if (rowsAffected > 0) {
					// If you're keeping waiting_list rows (no trigger delete), update status there
					// too
					updateWaitingListStatus(confirmationCode, "CANCELLED");

					logger.log("[SUCCESS] Order " + confirmationCode + " cancelled successfully.");
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
	 * Enqueues a WAITLIST order into the waiting_list table with the calculated
	 * wait time. If the order already exists in the waiting_list, it updates the
	 * wait time and priority.
	 * 
	 * @param confirmationCode   The confirmation code of the order
	 * @param calculatedWaitTime The calculated wait time in minutes
	 */
	public void enqueueWaitingList(String confirmationCode, int calculatedWaitTime) {
		final String qry = "INSERT INTO waiting_list (confirmation_code, quoted_wait_time, priority, requested_time, wl_status) "
				+ "SELECT o.confirmation_code, ?, 2, o.date_of_placing_order, 'WAITING' " + "FROM orders o "
				+ "WHERE o.confirmation_code = ? " + "ON DUPLICATE KEY UPDATE "
				+ "  quoted_wait_time = VALUES(quoted_wait_time), " + "  priority = VALUES(priority), "
				+ "  requested_time = VALUES(requested_time), " + "  wl_status = 'WAITING'";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, calculatedWaitTime);
				ps.setString(2, confirmationCode);
				int rowsAffected = ps.executeUpdate();
				if (rowsAffected > 0) {
					logger.log("[SUCCESS] Enqueued WAITLIST order: " + confirmationCode);
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in addWaitTimeToWaitListOrder/enqueue: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves the next order from the waiting queue that fits the specified table
	 * capacity.
	 * 
	 * @param tableCapacity The capacity of the table
	 * @return The next Order object that fits, or null if none found
	 */
	public Order getNextFromWaitingQueueThatFits(int tableCapacity) {
		final String sql = "SELECT o.order_number, o.confirmation_code, o.user_id, o.number_of_guests, o.order_type, o.status, o.date_of_placing_order "
				+ "FROM waiting_list w " + "JOIN orders o ON o.confirmation_code = w.confirmation_code "
				+ "WHERE w.wl_status = 'WAITING' " + "  AND o.number_of_guests <= ? "
				+ "ORDER BY w.priority ASC, w.requested_time ASC, w.joined_at ASC " + "LIMIT 1";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, tableCapacity);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next())
						return null;

					Order o = new Order();
					o.setOrderNumber(rs.getInt("order_number"));
					o.setConfirmationCode(rs.getString("confirmation_code"));
					o.setUserId(rs.getInt("user_id"));
					o.setDinersAmount(rs.getInt("number_of_guests"));
					o.setOrderType(OrderType.valueOf(rs.getString("order_type")));
					o.setStatus(OrderStatus.valueOf(rs.getString("status")));

					Timestamp placedAt = rs.getTimestamp("date_of_placing_order");
					if (placedAt != null)
						o.setDateOfPlacingOrder(placedAt.toLocalDateTime());

					return o;
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getNextFromWaitingQueueThatFits: " + e.getMessage());
			return null;
		} finally {
			release(conn);
		}
	}

	/**
	 * Updates the status of a waiting list entry based on the confirmation code.
	 * 
	 * @param confirmationCode The confirmation code of the waiting list entry
	 * @param wlStatus         The new status to set (e.g., 'NOTIFIED', 'SEATED',
	 *                         'CANCELLED', 'EXPIRED')
	 * @return true if the update was successful, false otherwise
	 */
	public boolean updateWaitingListStatus(String confirmationCode, String wlStatus) {
		final String sql = "UPDATE waiting_list SET wl_status = ? WHERE confirmation_code = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, wlStatus);
				ps.setString(2, confirmationCode);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			logger.log("[ERROR] updateWaitingListStatus: " + e.getMessage());
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Marks a WAITLIST order as NOTIFIED in both orders and waiting_list tables.
	 * 
	 * @param orderNumber The order number of the WAITLIST order
	 * @param notifiedAt  The timestamp when the notification was sent
	 * @return true if the update was successful, false otherwise
	 */
	public boolean markWaitlistAsNotified(int orderNumber, LocalDateTime notifiedAt) {
		final String sql = "UPDATE orders " + "SET status='NOTIFIED', notified_at=? "
				+ "WHERE order_number=? AND order_type='WAITLIST' AND status='PENDING'";

		// Execute update
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setTimestamp(1, Timestamp.valueOf(notifiedAt));
				ps.setInt(2, orderNumber);
				int rows = ps.executeUpdate();
				if (rows == 1) {
					try (PreparedStatement ps2 = conn.prepareStatement(
							"UPDATE waiting_list w " + "JOIN orders o ON o.confirmation_code=w.confirmation_code "
									+ "SET w.wl_status='NOTIFIED' " + "WHERE o.order_number=?")) {
						ps2.setInt(1, orderNumber);
						ps2.executeUpdate();
					}
					return true;
				}
				return false;
			}
		} catch (SQLException e) {
			logger.log("[ERROR] markWaitlistAsNotified: " + e.getMessage());
			return false;
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
				+ "o.date_of_placing_order, o.status, o.order_type, "
				+ "w.quoted_wait_time, w.priority, w.requested_time, w.wl_status " + "FROM orders o "
				+ "JOIN waiting_list w ON o.confirmation_code = w.confirmation_code "
				+ "WHERE w.wl_status IN ('WAITING', 'NOTIFIED') "
				+ "ORDER BY w.priority, w.requested_time, w.joined_at";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Order order = new Order();
					// Map standard Order fields
					order.setConfirmationCode(rs.getString("confirmation_code"));
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

	/**
	 * Deletes an active table session for the specified order number. An active
	 * session is defined as one where 'left_at' is NULL.
	 * 
	 * @param orderNumber The order number associated with the session to delete
	 */
	public void deleteActiveSession(int orderNumber) {
		String sql = "DELETE FROM table_sessions WHERE order_number = ? AND left_at IS NULL";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, orderNumber);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			logger.log("[ERROR] Failed to rollback/delete session for order " + orderNumber + ": " + e.getMessage());
		} finally {
			release(conn);
		}
	}

	// ****************************** Table Operations
	// ******************************

	/**
	 * Retrieves the capacity of a table by its table number.
	 * 
	 * @param tableNum The table number
	 * @return The capacity of the table, or -1 if not found or on error
	 */
	public int getTableCapacity(int tableNum) {
		final String sql = "SELECT capacity FROM tables WHERE tableNum = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, tableNum);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next())
						return -1;
					return rs.getInt("capacity");
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getTableCapacity: " + e.getMessage());
			return -1;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves all tables from the database.
	 * 
	 * @return List of Table objects representing all tables in the database
	 */
	public List<Table> getAllTablesFromDB() {
		List<Table> tablesList = new ArrayList<>();

		String qry = "SELECT t.tableNum, t.capacity, " + "EXISTS ( " + "   SELECT 1 FROM table_sessions ts "
				+ "   WHERE ts.tableNum = t.tableNum AND ts.left_at IS NULL " + ") AS occupiedNow " + "FROM tables t";

		// Execute query and map results to Table objects
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry); ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					tablesList
							.add(new Table(rs.getInt("tableNum"), rs.getInt("capacity"), rs.getBoolean("occupiedNow")));
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
	 * Retrieves the active table number associated with a given order number.
	 * 
	 * @param orderNumber The order number to search for
	 * @return The active table number if found, null otherwise
	 */
	public Integer getActiveTableNumByOrderNumber(int orderNumber) {
		final String sql = "SELECT tableNum FROM table_sessions WHERE order_number = ? AND left_at IS NULL";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, orderNumber);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next())
						return null;
					return rs.getInt("tableNum");
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getActiveTableNumByOrderNumber: " + e.getMessage());
			return null;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves the active order confirmation code for a given table number.
	 * 
	 * @param tableID The table number to search for
	 * @return The active order confirmation code if found, null otherwise
	 */
	public String getActiveOrderConfirmationCodeByTableNum(int tableID) {
		String sql = "SELECT o.confirmation_code " + "FROM orders o "
				+ "JOIN table_sessions ts ON o.order_number = ts.order_number "
				+ "WHERE ts.tableNum = ? AND ts.left_at IS NULL";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, tableID);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getString("confirmation_code");
					} else {
						return null; // No active order found for the table
					}
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getActiveOrderConfirmationCodeByTableNum: " + e.getMessage());
			return null;
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

	/**
	 * Finds a free table that can accommodate the specified group size.
	 * 
	 * @param groupSize The size of the group
	 * @return The table number of a suitable free table, or -1 if none available
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
	 * Creates a new table session for the given order number and table number.
	 * 
	 * @param orderNumber   The order number associated with the table session
	 * @param tableNum      The table number being assigned
	 * @param diningMinutes The estimated dining duration in minutes
	 * @return true if the table session was created successfully, false otherwise
	 */
	public boolean createTableSession(int orderNumber, int tableNum, int diningMinutes) {

		String insertSessionSql = "INSERT INTO table_sessions (order_number, tableNum, seated_at, expected_end_at) "
				+ "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? MINUTE))";

		String insertBillSql = "INSERT INTO bills (session_id, billSum, subtotal_amount, discount_percent) "
				+ "VALUES (?, 0.00, 0.00, 0.00)";

		Connection conn = null;
		try {
			conn = borrow();
			conn.setAutoCommit(false);

			int sessionId;

			// 1) create session
			try (PreparedStatement ps = conn.prepareStatement(insertSessionSql,
					java.sql.Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, orderNumber);
				ps.setInt(2, tableNum);
				ps.setInt(3, diningMinutes);

				int affected = ps.executeUpdate();
				if (affected != 1) {
					conn.rollback();
					return false;
				}

				try (ResultSet keys = ps.getGeneratedKeys()) {
					if (!keys.next()) {
						conn.rollback();
						throw new SQLException("Failed to read generated session_id");
					}
					sessionId = keys.getInt(1);
				}
			}

			// 2) create bill for that session
			try (PreparedStatement ps = conn.prepareStatement(insertBillSql)) {
				ps.setInt(1, sessionId);
				ps.executeUpdate();
			}

			conn.commit();
			return true;

		} catch (SQLException e) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignore) {
			}
			logger.log("[DB ERROR] Failed to create session+bill: " + e.getMessage());
			e.printStackTrace();
			return false;

		} finally {
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
				} catch (SQLException ignore) {
				}
				release(conn);
			}
		}
	}

	/**
	 * Closes an active table session for the specified order number.
	 * 
	 * @param orderNumber The order number associated with the session to close
	 * @param endType     The reason for ending the session
	 */
	public void closeTableSessionForOrder(int orderNumber, EndTableSessionType endType) {
		String sql = "UPDATE table_sessions " + "SET left_at = NOW(), end_reason = ? "
				+ "WHERE order_number = ? AND left_at IS NULL";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, endType.name());
				ps.setInt(2, orderNumber);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			logger.log("[DB ERROR] Failed to close session: " + e.getMessage());
		} finally {
			release(conn);
		}
	}

	// ******************** Restaurant Management Operations ******************

	/**
	 * Retrieves today's opening hours from the database. Acts as a
	 * wrapper/convenience method for getOpeningHoursFromDB(LocalDate). * @return
	 * List containing [OpenTime, CloseTime] or empty list if closed.
	 */
	public List<LocalTime> getOpeningHoursFromDB() {
		return getOpeningHoursFromDB(LocalDate.now());
	}

	/**
	 * Retrieves opening hours for a SPECIFIC DATE from the database, accounting for
	 * weekly schedules and special overrides (holidays/events). * @param date The
	 * date to check availability for.
	 * 
	 * @return A list of LocalTime objects: [0] = Open Time, [1] = Close Time.
	 *         Returns an empty list if the restaurant is closed or an error occurs.
	 */
	public List<LocalTime> getOpeningHoursFromDB(LocalDate date) {
		List<LocalTime> hours = new ArrayList<>();
		int currentDayOfWeek = (date.getDayOfWeek().getValue() % 7) + 1; // 1=Sunday, 7=Saturday

		String qry = "SELECT " + "  COALESCE(s.is_closed, 0) as is_closed_final, "
				+ "  COALESCE(s.open_time, w.open_time) as open_final, "
				+ "  COALESCE(s.close_time, w.close_time) as close_final " + "FROM opening_hours_weekly w "
				+ "LEFT JOIN opening_hours_special s ON s.special_date = ? " + "WHERE w.day_of_week = ?";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setDate(1, Date.valueOf(date));
				ps.setInt(2, currentDayOfWeek);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						boolean isClosed = rs.getInt("is_closed_final") == 1;

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

	// ******************************* Payment Operations
	// ******************************
	/**
	 * Updates a bill to mark it as PAID and records the external transaction ID.
	 * 
	 * @param billId        The ID of the bill to update.
	 * @param paymentMethod The method used (e.g., "CREDIT", "CASH").
	 * @param transactionId The reference ID from the payment provider (can be null
	 *                      for Cash).
	 */
	public void markBillAsPaid(int billId, String paymentMethod, String transactionId) {
		// The SQL query to update the bill
		String sql = "UPDATE bills SET " + "payment_status = 'PAID', " + "paid_at = NOW(), " + "payment_method = ?, "
				+ "transaction_id = ? " + "WHERE billID = ?";
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
	 * 
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
	 * Retrieves the Bill ID associated with a specific order number. This requires
	 * joining the Bills table with the Table Sessions table.
	 * 
	 * @param orderNumber The order number to search for
	 * @return The Bill ID if found, or null if not found
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
	 * Retrieves all pending (UNPAID) bills for active table sessions today.
	 * 
	 * @return List of Bill objects representing pending bills
	 */
	public List<Bill> getPendingBillsByUserId() {

		List<Bill> bills = new ArrayList<>();

		String sql = "SELECT " + "  b.billID, " + "  b.billSum, " + "  b.payment_status, " + "  b.transaction_id, "
				+ "  ts.tableNum, " + "  ts.order_number, " + "  ts.seated_at, " + "  o.confirmation_code "
				+ "FROM table_sessions ts " + "JOIN bills b ON b.session_id = ts.session_id "
				+ "JOIN orders o ON o.order_number = ts.order_number " + "WHERE ts.left_at IS NULL "
				+ "  AND b.payment_status = 'UNPAID' " + "  AND DATE(ts.seated_at) = CURRENT_DATE "
				+ "ORDER BY ts.seated_at ASC";

		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {

					Bill bill = new Bill(rs.getInt("billID"), rs.getDouble("billSum"), rs.getString("payment_status"),
							rs.getString("transaction_id"));

					bill.setTableId(rs.getInt("tableNum"));
					bill.setOrderNumber(rs.getInt("order_number"));
					bill.setDate(rs.getTimestamp("seated_at"));
					bill.setConfirmationCode(rs.getString("confirmation_code"));

					bills.add(bill);
				}
			}

		} catch (SQLException e) {
			logger.log("[ERROR] getActiveUnpaidBillsForStaffToday: " + e.getMessage());
			e.printStackTrace();
		} finally {
			release(conn);
		}

		return bills;
	}

	// *************************************** Notification Operations
	// ********************************

	/**
	 * Retrieves reservations within a specified time window that match the given
	 * status and have not yet been notified.
	 * 
	 * @param startWindow The start of the time window
	 * @param endWindow   The end of the time window
	 * @param status      The status of the reservations to filter by
	 * @return List of Order objects representing the matching reservations
	 */
	public List<Order> getReservationsBetweenTimes(LocalDateTime startWindow, LocalDateTime endWindow,
			OrderStatus status) {
		final String sql = "SELECT order_number, user_id, order_date, order_time, number_of_guests, confirmation_code, status, order_type "
				+ "FROM orders " + "WHERE order_type = 'RESERVATION' " + "  AND status = ? "
				+ "  AND notified_at IS NULL " + "  AND TIMESTAMP(order_date, order_time) BETWEEN ? AND ?";

		List<Order> list = new ArrayList<>();
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, status.name());
				ps.setTimestamp(2, Timestamp.valueOf(startWindow));
				ps.setTimestamp(3, Timestamp.valueOf(endWindow));

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						Order o = new Order();
						o.setOrderNumber(rs.getInt("order_number"));
						o.setUserId(rs.getInt("user_id"));
						Date d = rs.getDate("order_date");
						LocalDate orderDate = (d != null) ? d.toLocalDate() : null;

						Time t = rs.getTime("order_time");
						LocalTime orderTime = (t != null) ? t.toLocalTime() : null;
						int dinersAmount = rs.getInt("number_of_guests");
						String confirmCode = rs.getString("confirmation_code");
						OrderType type = OrderType.valueOf(rs.getString("order_type"));
						OrderStatus orderStatus = OrderStatus.valueOf(rs.getString("status"));
						Timestamp placingTs = rs.getTimestamp("date_of_placing_order");
						LocalDateTime dateOfPlacing = placingTs != null ? placingTs.toLocalDateTime() : null;

						o.setOrderNumber(rs.getInt("order_number"));
						o.setUserId(rs.getInt("user_id"));
						if (orderDate != null)
							o.setOrderDate(orderDate);
						if (orderTime != null)
							o.setOrderHour(orderTime);
						o.setDinersAmount(dinersAmount);
						o.setConfirmationCode(confirmCode);
						o.setStatus(orderStatus);
						o.setOrderType(type);
						o.setDateOfPlacingOrder(dateOfPlacing);

						list.add(o);
					}
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getReservationsBetweenTimes: " + e.getMessage());
			e.printStackTrace();
			return null;
		} finally {
			release(conn);
		}
		return list;
	}

	/**
	 * Marks a reservation as having received a reminder notification.
	 * 
	 * @param orderNumber The order number of the reservation
	 * @param sentAt      The timestamp when the reminder was sent
	 * @return true if the update was successful, false otherwise
	 */
	public boolean markReservationReminderSent(int orderNumber, LocalDateTime sentAt) {
		final String sql = "UPDATE orders " + "SET notified_at = ? " + "WHERE order_number = ? "
				+ "  AND order_type = 'RESERVATION' " + "  AND notified_at IS NULL";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setTimestamp(1, Timestamp.valueOf(sentAt));
				ps.setInt(2, orderNumber);
				return ps.executeUpdate() == 1;
			}
		} catch (SQLException e) {
			logger.log("[ERROR] markReservationReminderSent: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves all orders with status 'SEATED' within a specified time window.
	 * 
	 * @param startWindow The start of the time window
	 * @param endWindow   The end of the time window
	 * @return List of Order objects representing the seated orders
	 */
	public List<Order> getSeatedOrdersBetweenTimes(LocalDateTime startWindow, LocalDateTime endWindow) {
		final String sql = "SELECT order_number, user_id, order_date, order_time, number_of_guests, confirmation_code, status, order_type "
				+ "FROM orders " + "WHERE status = 'SEATED' "
				+ "  AND TIMESTAMP(order_date, order_time) BETWEEN ? AND ?";

		List<Order> list = new ArrayList<>();
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setTimestamp(1, Timestamp.valueOf(startWindow));
				ps.setTimestamp(2, Timestamp.valueOf(endWindow));

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						Order o = new Order();
						o.setOrderNumber(rs.getInt("order_number"));
						o.setUserId(rs.getInt("user_id"));
						Date d = rs.getDate("order_date");
						LocalDate orderDate = (d != null) ? d.toLocalDate() : null;

						Time t = rs.getTime("order_time");
						LocalTime orderTime = (t != null) ? t.toLocalTime() : null;
						int dinersAmount = rs.getInt("number_of_guests");
						String confirmCode = rs.getString("confirmation_code");
						o.setStatus(OrderStatus.valueOf(rs.getString("status")));
						o.setOrderType(OrderType.valueOf(rs.getString("order_type")));

						o.setOrderNumber(rs.getInt("order_number"));
						o.setUserId(rs.getInt("user_id"));
						if (orderDate != null)
							o.setOrderDate(orderDate);
						if (orderTime != null)
							o.setOrderHour(orderTime);
						o.setDinersAmount(dinersAmount);
						o.setConfirmationCode(confirmCode);

						list.add(o);
					}
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getSeatedOrdersBetweenTimes: " + e.getMessage());
			e.printStackTrace();
			return null;
		} finally {
			release(conn);
		}
		return list;
	}

	// *************************************** Reports
	// ********************************

	/**
	 * Method that give the number of the total Reservation this month
	 * 
	 * @param date the date to check the month and year
	 * @return the total number of reservations in that month
	 */
	public int getTotalReservation(LocalDate date) {

		if (date == null) {
			return 0;
		}

		final String qry = "SELECT COUNT(*) AS total " + "FROM orders o " + "WHERE MONTH(o.order_date) = ? "
				+ "AND YEAR(o.order_date) = ?";

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

	/**
	 * Method that give the number of the total costumers this month
	 * 
	 * @param date the date to check the month and year
	 * @return the total number of costumers in that month
	 */

	public int getTotalCostumersInMonth(LocalDate date) {

		if (date == null) {
			return 0;
		}

		final String qry = "SELECT COUNT(DISTINCT o.user_id) AS total " + "FROM orders o "
				+ "WHERE MONTH(o.order_date) = ? " + "AND YEAR(o.order_date) = ?";

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

	/*
	 * Method that give the number of the costumers that arrive late this month
	 * 
	 * @param date the date to check the month and year
	 * 
	 * @return the total number of late costumers in that month
	 */
	public int getTotalLateCostumersInMonth(LocalDate date) {

		if (date == null) {
			return 0;
		}

		// SQL query to count late customers
		final String qry = "SELECT COUNT(*) AS total_late " + "FROM orders o "
				+ "JOIN table_sessions ts ON o.order_number = ts.order_number " + "WHERE o.order_type = 'RESERVATION' "
				+ "AND MONTH(o.order_date) = ? " + "AND YEAR(o.order_date) = ? " + "AND ts.seated_at IS NOT NULL "
				+ "AND ts.seated_at >= TIMESTAMP(o.order_date, o.order_time)  "
				+ "AND ts.seated_at <= TIMESTAMP(o.order_date, o.order_time) + INTERVAL 15 MINUTE ";

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

	/*
	 * Method that give the number of the costumers that arrive on time this month
	 * 
	 * @param date the date to check the month and year
	 * 
	 * @return the total number of on-time costumers in that month
	 * 
	 */
	public int getTotalOntTimeCostumersInMonth(LocalDate date) {

		if (date == null) {
			return 0;
		}

		final String qry = "SELECT COUNT(*) AS total_on_time " + "FROM orders o "
				+ "JOIN table_sessions ts ON o.order_number = ts.order_number " + "WHERE o.order_type = 'RESERVATION' "
				+ "AND MONTH(o.order_date) = ? " + "AND YEAR(o.order_date) = ? " + "AND ts.seated_at IS NOT NULL "
				+ "AND ts.seated_at <= TIMESTAMP(o.order_date, o.order_time)";

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

	/*
	 * Method that give the number of the total Members Reservation this month
	 * 
	 * @param date the date to check the month and year
	 * 
	 * @return the total number of members reservations in that month
	 */
	public int getTotalMembersReservationInMonth(LocalDate date) {

		if (date == null) {
			return 0;
		}

		final String qry = "SELECT COUNT(*) AS total " + "FROM orders o " + "JOIN users u ON o.user_id = u.user_id "
				+ "WHERE u.type = 'MEMBER' " + "AND MONTH(o.order_date) = ? " + "AND YEAR(o.order_date) = ?";

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
			logger.log("[ERROR] SQLException in getTotalMembersReservationInMonth: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}

		return 0;
	}

	/**
	 * Updates the order date and time to the current date and time for a given
	 * confirmation code. Also sets the order type to 'RESERVATION'.
	 * 
	 * @param confirmationCode The confirmation code of the order to update
	 */
	public void updateOrderTimeAndDateToNow(String confirmationCode) {
		String sql = "UPDATE orders SET order_date = ?, order_time = ?, order_type = 'RESERVATION' WHERE confirmation_code = ?";

	}

	// ======================== NO-SHOW MANAGEMENT METHODS ========================

	/**
	 * Retrieves all orders for a specific date with a given status and type. Used
	 * by NoShowManager to find orders that need no-show checking.
	 * 
	 * @param date   The order date to check
	 * @param status The order status (PENDING, NOTIFIED, etc.)
	 * @param type   The order type (RESERVATION, WAITLIST)
	 * @return List of matching Order objects
	 */
	public List<Order> getOrdersByDateAndStatus(LocalDate date, OrderStatus status, OrderType orderType) {
		List<Order> orders = new ArrayList<>();
		if (date == null) {
			return orders;
		}
		String qry = "SELECT order_number, user_id, order_date, order_time, number_of_guests, "
				+ "confirmation_code, order_type, status, date_of_placing_order "
				+ "FROM orders WHERE order_date = ? AND status = ? AND order_type = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setDate(1, Date.valueOf(date));
				ps.setString(2, status.name());
				ps.setString(3, orderType.name());

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						int orderNumber = rs.getInt("order_number");
						int userId = rs.getInt("user_id");
						LocalDate orderDate = rs.getDate("order_date").toLocalDate();
						LocalTime orderTime = rs.getTime("order_time").toLocalTime();
						int dinersAmount = rs.getInt("number_of_guests");
						String confirmCode = rs.getString("confirmation_code");
						OrderType type = OrderType.valueOf(rs.getString("order_type"));
						OrderStatus orderStatus = OrderStatus.valueOf(rs.getString("status"));
						Timestamp placingTs = rs.getTimestamp("date_of_placing_order");
						LocalDateTime dateOfPlacing = placingTs != null ? placingTs.toLocalDateTime() : null;

						Order order = new Order(orderNumber, orderDate, orderTime, dinersAmount, confirmCode, userId,
								type, orderStatus, dateOfPlacing);
						orders.add(order);
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getOrdersByDateAndStatus: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return orders;
	}

	/**
	 * Gets the notification time (when status was changed to NOTIFIED) for a
	 * waitlist order.
	 * 
	 * @param orderNumber The order number to check
	 * @return LocalDateTime when the order was notified, or null if not found
	 */
	public LocalDateTime getOrderNotificationTime(int orderNumber) {
		String qry = "SELECT notified_at FROM orders WHERE order_number = ?";
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, orderNumber);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						Timestamp ts = rs.getTimestamp("notified_at");
						return ts != null ? ts.toLocalDateTime() : null;
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getOrderNotificationTime: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return null;
	}

	/**
	 * Updates the status of an order identified by its confirmation code.
	 * 
	 * @param confirmationCode The confirmation code of the order
	 * @param newStatus        The new status to set
	 * @return true if the update was successful, false otherwise
	 */
	public boolean updateOrderStatusByConfirmCode(String confirmationCode, OrderStatus newStatus) {
		String qry = "UPDATE orders SET status = ? WHERE confirmation_code = ?";
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, newStatus.name());
				ps.setString(2, confirmationCode);
				int rowsAffected = ps.executeUpdate();
				return rowsAffected > 0;
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in updateOrderStatus: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Updates the status of an order identified by its user ID.
	 * 
	 * @param userid    The user ID associated with the order
	 * @param newStatus The new status to set
	 * @return true if the update was successful, false otherwise
	 */
	public boolean updateOrderStatusByUserId(int userid, OrderStatus newStatus) {
		// TODO user_id or confirmation_code?
		String qry = "UPDATE orders SET status = ? WHERE user_id = ?";
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, newStatus.name());
				ps.setInt(2, userid);
				int rowsAffected = ps.executeUpdate();
				return rowsAffected > 0;
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in updateOrderStatus: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Updates the status of an order identified by its order number.
	 * 
	 * @param orderNumber The order number of the order
	 * @param completed   The new status to set
	 * @return true if the update was successful, false otherwise
	 */
	public boolean updateOrderStatusByOrderNumber(int orderNumber, OrderStatus completed) {
		String qry = "UPDATE orders SET status = ? WHERE order_number = ?";
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setString(1, completed.name());
				ps.setInt(2, orderNumber);
				int rowsAffected = ps.executeUpdate();
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

	/**
	 * Frees up a table reservation when an order is marked as NO_SHOW. Sets the
	 * table status back to available.
	 * 
	 * @param orderNumber The order number for the no-show reservation
	 * @return true if table was freed, false otherwise
	 */
	public boolean freeReservationTable(int orderNumber) {
		String qry = "UPDATE orders SET table_id = NULL WHERE order_number = ?";
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, orderNumber);
				int rowsAffected = ps.executeUpdate();
				return rowsAffected > 0;
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in freeReservationTable: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves a user by their user ID. Used by NoShowManager to get customer info
	 * for notifications.
	 * 
	 * @param userId The user ID to look up
	 * @return User object if found, null otherwise
	 */
	public User getUserById(int userId) {
		String qry = "SELECT user_id, phoneNumber, email, type FROM users WHERE user_id = ?";
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, userId);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						String phoneNumber = rs.getString("phoneNumber");
						String email = rs.getString("email");
						String userType = rs.getString("type");
						return new User(userId, phoneNumber, email, null, UserType.valueOf(userType));
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getUserById: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return null;
	}

	/**
	 * Updates the weekly opening hours in the database.
	 * 
	 * @param hours List of WeeklyHour objects representing the opening hours for
	 *              each day
	 * @return true if update was successful, false otherwise
	 */
	public boolean updateWeeklyHours(List<WeeklyHour> hours) {
		// Simple strategy: Update specific days
		String query = "INSERT INTO opening_hours_weekly (day_of_week, open_time, close_time) " + "VALUES (?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE open_time = VALUES(open_time), close_time = VALUES(close_time)";

		Connection conn = null;
		try {
			conn = borrow();
			conn.setAutoCommit(false);
			try (PreparedStatement ps = conn.prepareStatement(query)) {
				for (WeeklyHour wh : hours) {
					// Skip days that are closed (null times)
					if (wh.getOpenTime() == null || wh.getCloseTime() == null)
						continue;

					ps.setInt(1, wh.getDayOfWeek());
					ps.setTime(2, Time.valueOf(wh.getOpenTime()));
					ps.setTime(3, Time.valueOf(wh.getCloseTime()));
					ps.addBatch();
				}
				ps.executeBatch();
				conn.commit();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Adds or updates a holiday/special opening hours in the database.
	 * 
	 * @param holiday Holiday object representing the special date
	 * @return true if operation was successful, false otherwise
	 */
	public boolean addHoliday(Holiday holiday) {
		String query = "INSERT INTO opening_hours_special "
				+ "(special_date, holiday_name, is_closed, open_time, close_time) " + "VALUES (?, ?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE " + "holiday_name = VALUES(holiday_name), "
				+ "is_closed = VALUES(is_closed), " + "open_time = VALUES(open_time), "
				+ "close_time = VALUES(close_time)";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setDate(1, Date.valueOf(holiday.getDate()));
				ps.setString(2, holiday.getName());
				ps.setInt(3, holiday.isClosed() ? 1 : 0);

				// If closed -> NULL times. If open -> set provided times (or NULL if not
				// provided).
				if (holiday.isClosed() || holiday.getOpenTime() == null || holiday.getCloseTime() == null) {
					ps.setNull(4, java.sql.Types.TIME);
					ps.setNull(5, java.sql.Types.TIME);
				} else {
					ps.setTime(4, Time.valueOf(holiday.getOpenTime()));
					ps.setTime(5, Time.valueOf(holiday.getCloseTime()));
				}

				ps.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Removes a holiday/special opening hours from the database.
	 * 
	 * @param holiday Holiday object representing the special date to remove
	 * @return true if removal was successful, false otherwise
	 */
	public boolean removeHoliday(Holiday holiday) {
		String query = "DELETE FROM opening_hours_special WHERE special_date = ?";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setDate(1, Date.valueOf(holiday.getDate()));
				int affected = ps.executeUpdate();
				return affected > 0; // true
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves all holidays/special opening hours from the database.
	 * 
	 * @return List of Holiday objects
	 */
	public List<Holiday> getHolidays() {
		String sql = "SELECT special_date, holiday_name, is_closed, open_time, close_time "
				+ "FROM opening_hours_special " + "ORDER BY special_date";

		List<Holiday> list = new ArrayList<>();
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					LocalDate date = rs.getDate("special_date").toLocalDate();
					String name = rs.getString("holiday_name");
					boolean closed = rs.getInt("is_closed") == 1;

					Time ot = rs.getTime("open_time");
					Time ct = rs.getTime("close_time");
					LocalTime open = (ot == null) ? null : ot.toLocalTime();
					LocalTime close = (ct == null) ? null : ct.toLocalTime();

					list.add(new Holiday(date, name, closed, open, close));

				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		} finally {
			release(conn);
		}

		return list;
	}

	/**
	 * Adds a new table to the database.
	 * 
	 * @param table Table object representing the table to add
	 * @return true if addition was successful, false otherwise
	 */
	public boolean addTable(Table table) {
		String query = "INSERT INTO tables (tableNum, capacity) VALUES (?, ?)";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setInt(1, table.getTableID());
				ps.setInt(2, table.getCapacity());
				ps.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			logger.log("[ERROR] Error adding table: " + e.getMessage());
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Removes a table from the database.
	 * 
	 * @param tableId The table ID to remove
	 * @return true if removal was successful, false otherwise
	 */
	public boolean removeTable(int tableId) {
		String query = "DELETE FROM tables WHERE tableNum = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setInt(1, tableId);
				int rows = ps.executeUpdate();
				return rows > 0;
			}
		} catch (SQLException e) {
			logger.log("[ERROR] Error removing table: " + e.getMessage());
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Updates the capacity (number of seats) for a table.
	 * 
	 * @param tableId     The table ID to update
	 * @param newCapacity The new number of seats
	 * @return true if update was successful, false otherwise
	 */
	public boolean updateTableCapacity(int tableId, int newCapacity) {
		String query = "UPDATE tables SET capacity = ? WHERE tableNum = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setInt(1, newCapacity);
				ps.setInt(2, tableId);
				int rows = ps.executeUpdate();
				return rows > 0;
			}
		} catch (SQLException e) {
			logger.log("[ERROR] Error updating table capacity: " + e.getMessage());
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Retrieves the currently seated order for a specific user.
	 * 
	 * @param userId The user ID to look up
	 * @return Order object if found, null otherwise
	 */
	public Order getSeatedOrderForUser(int userId) {
		String qry = "SELECT o.order_number, o.user_id, o.order_date, o.order_time, o.number_of_guests, "
				+ "       o.confirmation_code, o.order_type, o.status, o.date_of_placing_order, "
				+ "       ts.tableNum AS seated_tableNum " + "FROM orders o "
				+ "JOIN table_sessions ts ON ts.order_number = o.order_number " + "WHERE o.user_id = ? "
				+ "  AND o.status = 'SEATED' " + "  AND ts.left_at IS NULL " + "LIMIT 1";

		Connection conn = null;
		Order order = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, userId);

				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						int orderNumber = rs.getInt("order_number");

						Date sqlDate = rs.getDate("order_date");
						LocalDate orderDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;

						Time sqlTime = rs.getTime("order_time");
						LocalTime orderTime = (sqlTime != null) ? sqlTime.toLocalTime() : null;

						int dinersAmount = rs.getInt("number_of_guests");
						String confirmCode = rs.getString("confirmation_code");
						OrderType type = OrderType.valueOf(rs.getString("order_type"));
						OrderStatus orderStatus = OrderStatus.valueOf(rs.getString("status"));

						Timestamp placingTs = rs.getTimestamp("date_of_placing_order");
						LocalDateTime dateOfPlacing = placingTs != null ? placingTs.toLocalDateTime() : null;

						int tableNum = rs.getInt("seated_tableNum");
						System.out.println("DEBUG: Seated tableNum for order " + orderNumber + " is " + tableNum);

						order = new Order(orderNumber, orderDate, orderTime, dinersAmount, confirmCode, userId, type,
								orderStatus, dateOfPlacing);

						order.setTableId(tableNum);
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getSeatedOrderForUser: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}

		return order;
	}

	/**
	 * Retrieves the waiting list order for a specific user.
	 * 
	 * @param userID The user ID to look up
	 * @return Order object if found, null otherwise
	 */
	public Order getWaitingListOrderByUserId(int userID) {
		String qry = "SELECT order_number, user_id, order_date, order_time, number_of_guests, "
				+ "confirmation_code, order_type, status, date_of_placing_order "
				+ "FROM orders WHERE user_id = ? AND order_type = 'WAITLIST' AND status IN ('PENDING', 'NOTIFIED')";
		Connection conn = null;
		Order order = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				ps.setInt(1, userID);
				// Execute query
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						int orderNumber = rs.getInt("order_number");
						Date sqlDate = rs.getDate("order_date");
						LocalDate orderDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;
						Time sqlTime = rs.getTime("order_time");
						LocalTime orderTime = (sqlTime != null) ? sqlTime.toLocalTime() : null;
						int dinersAmount = rs.getInt("number_of_guests");
						String confirmCode = rs.getString("confirmation_code");
						OrderType type = OrderType.valueOf(rs.getString("order_type"));
						OrderStatus orderStatus = OrderStatus.valueOf(rs.getString("status"));
						Timestamp placingTs = rs.getTimestamp("date_of_placing_order");
						LocalDateTime dateOfPlacing = placingTs != null ? placingTs.toLocalDateTime() : null;

						order = new Order(orderNumber, orderDate, orderTime, dinersAmount, confirmCode, userID, type,
								orderStatus, dateOfPlacing);
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getWaitingListOrderByUserId: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return order;
	}

	/**
	 * Generates a list of bill items for a given billId, simulating realistic
	 * ordering behavior. The number of items and their quantities are influenced by
	 * the number of diners.
	 *
	 * @param orderNumber The ID of the bill to generate items for.
	 * @param requester   The user requesting the bill items (not used in this
	 *                    example).
	 * @return A list of Item objects representing the bill items.
	 */
	public List<Item> getBillItemsList(int orderNumber, User requester) {
		Connection conn = null;
		try {
			conn = borrow();
			int dinersAmount = fetchDinersAmountByOrder(conn, orderNumber);
			if (dinersAmount <= 0)
				dinersAmount = 1;

			// Determine number of items to fetch based on diners
			int itemsCount = Math.min(8, Math.max(2, (int) Math.ceil(dinersAmount * 1.5)));
			final String sql = "SELECT item_name, MAX(unit_price) AS unit_price " + "FROM bill_items "
					+ "GROUP BY item_name " + "ORDER BY RAND() " + "LIMIT ?";

			// Generate items with quantities
			List<Item> result = new ArrayList<>();
			Random rnd = new Random();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, itemsCount);
				try (ResultSet rs = ps.executeQuery()) {
					int fakeId = 1;
					while (rs.next()) {
						String name = rs.getString("item_name");
						double unitPrice = rs.getDouble("unit_price");
						int maxQty = Math.max(1, (dinersAmount / 2) + 1);
						int qty = 1 + rnd.nextInt(maxQty);
						if (dinersAmount >= 6 && unitPrice <= 20 && rnd.nextDouble() < 0.35) {
							qty += 1;
						}
						result.add(new Item(fakeId++, name, unitPrice, qty));
					}
				}
			}
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return List.of();
		} finally {
			release(conn);
		}
	}

	/**
	 * Fetches the number of diners associated with a specific order.
	 *
	 * @param conn        Active database connection
	 * @param orderNumber The order number to look up
	 * @return Number of diners for the order
	 * @throws SQLException If a database access error occurs
	 */
	private int fetchDinersAmountByOrder(Connection conn, int orderNumber) throws SQLException {
		final String sql = "SELECT number_of_guests FROM orders WHERE order_number = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, orderNumber);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("number_of_guests");
			}
		}
		return 1;
	}

	/**
	 * Retrieves the order number associated with a specific bill ID.
	 *
	 * @param billId The bill ID to look up
	 * @return The order number if found, null otherwise
	 */
	public Integer getOrderNumberByBillId(int billId) {
		final String sql = "SELECT ts.order_number " + "FROM bills b "
				+ "JOIN table_sessions ts ON b.session_id = ts.session_id " + "WHERE b.billID = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setInt(1, billId);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						return rs.getInt("order_number");
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
	 * Retrieves the user ID associated with a specific member code.
	 *
	 * @param memberCode The member code to look up
	 * @return The user ID if found, -1 otherwise
	 */
	public int getUserIdByMemberCode(int memberCode) {
		final String sql = "SELECT user_id " + "FROM users " + "WHERE member_code = ?";
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, memberCode);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt("user_id");
					}
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getUserIdByMemberCode: " + e.getMessage());
			e.printStackTrace();
		} finally {
			release(conn);
		}
		return -1; // Return -1 if no user found
	}

	// ===================== Reports (Persisted Monthly Reports)
	// =====================

	/**
	 * Retrieves the report payload for a given report type and month/year.
	 *
	 * @param type  report type ("TIMES" or "MEMBERS")
	 * @param year  report year
	 * @param month report month
	 * @return byte array of the report payload, or null if not found
	 */
	public byte[] getReportPayload(String type, int year, int month) {
		final String sql = "SELECT payload FROM reports WHERE report_type=? AND report_year=? AND report_month=?";
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, type.toUpperCase());
				ps.setInt(2, year);
				ps.setInt(3, month);

				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next())
						return rs.getBytes("payload");
					return null;
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getReportPayload: " + e.getMessage());
			return null;
		} finally {
			release(conn);
		}
	}

	/**
	 * Inserts or updates the report payload for a given report type and month/year.
	 *
	 * @param type    report type ("TIMES" or "MEMBERS")
	 * @param year    report year
	 * @param month   report month
	 * @param payload byte array of the report payload
	 * @return true if operation was successful, false otherwise
	 */
	public boolean upsertReportPayload(String type, int year, int month, byte[] payload) {
		final String sql = "INSERT INTO reports (report_type, report_year, report_month, payload) "
				+ "VALUES (?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE payload=VALUES(payload), generated_at=CURRENT_TIMESTAMP";

		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, type.toUpperCase());
				ps.setInt(2, year);
				ps.setInt(3, month);
				ps.setBytes(4, payload);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			logger.log("[ERROR] upsertReportPayload: " + e.getMessage());
			return false;
		} finally {
			release(conn);
		}
	}

	/**
	 * Lists months that have data for the requested report type.
	 *
	 * @param type report type ("TIMES" or "MEMBERS")
	 * @return list of {year, month} pairs in descending order
	 */
	public List<int[]> listReportMonths(String type) {

		final String sqlMembers = "SELECT DISTINCT YEAR(x.dt) AS report_year, MONTH(x.dt) AS report_month " + "FROM ( "
				+ "  SELECT o.order_date AS dt " + "  FROM orders o "
				+ "  WHERE o.order_type='RESERVATION' AND o.order_date IS NOT NULL " + "  UNION ALL "
				+ "  SELECT wl.joined_at AS dt " + "  FROM waiting_list wl " + "  WHERE wl.joined_at IS NOT NULL "
				+ ") x " + "WHERE x.dt IS NOT NULL " + "ORDER BY report_year DESC, report_month DESC";

		final String sqlTimes = "SELECT DISTINCT YEAR(o.order_date) AS report_year, MONTH(o.order_date) AS report_month "
				+ "FROM orders o " + "JOIN table_sessions ts ON ts.order_number = o.order_number "
				+ "WHERE o.order_type='RESERVATION' AND o.order_date IS NOT NULL "
				+ "ORDER BY report_year DESC, report_month DESC";

		final String baseSql = "TIMES".equalsIgnoreCase(type) ? sqlTimes : sqlMembers;

		List<int[]> months = new ArrayList<>();
		Connection conn = null;

		try {
			conn = borrow();

			// 1) Load all possible months (activity-based)
			try (PreparedStatement ps = conn.prepareStatement(baseSql); ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					months.add(new int[] { rs.getInt("report_year"), rs.getInt("report_month") });
				}
			}

			// 2) Exclude current month IF that doesn't make the list empty
			if (!months.isEmpty()) {
				java.time.YearMonth now = java.time.YearMonth.now();

				List<int[]> filtered = new ArrayList<>();
				for (int[] ym : months) {
					if (!(ym[0] == now.getYear() && ym[1] == now.getMonthValue())) {
						filtered.add(ym);
					}
				}

				// if removing current month leaves at least one month -> use filtered
				if (!filtered.isEmpty()) {
					months = filtered;
				}
			}

		} catch (SQLException e) {
			logger.log("[ERROR] listReportMonths: " + e.getMessage());
		} finally {
			release(conn);
		}

		return months;
	}

	/**
	 * Returns counts of reservations per day for a given month.
	 *
	 * @param year  report year
	 * @param month report month (1-12)
	 * @return map day->count
	 */
	public Map<Integer, Integer> getReservationsByDay(int year, int month) {
		final String sql = "SELECT DAY(order_date) d, COUNT(*) c " + "FROM orders "
				+ "WHERE YEAR(order_date)=? AND MONTH(order_date)=? AND order_type='RESERVATION' "
				+ "GROUP BY DAY(order_date)";

		Map<Integer, Integer> out = new HashMap<>();
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, year);
				ps.setInt(2, month);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next())
						out.put(rs.getInt("d"), rs.getInt("c"));
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getReservationsByDay: " + e.getMessage());
		} finally {
			release(conn);
		}

		return out;
	}

	/**
	 * Returns counts of waitlist joins per day for a given month.
	 *
	 * @param year  report year
	 * @param month report month (1-12)
	 * @return map day->count
	 */
	public Map<Integer, Integer> getWaitlistJoinsByDay(int year, int month) {
		final String sql = "SELECT DAY(joined_at) d, COUNT(*) c " + "FROM waiting_list "
				+ "WHERE YEAR(joined_at)=? AND MONTH(joined_at)=? " + "GROUP BY DAY(joined_at)";

		Map<Integer, Integer> out = new HashMap<>();
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, year);
				ps.setInt(2, month);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next())
						out.put(rs.getInt("d"), rs.getInt("c"));
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getWaitlistJoinsByDay: " + e.getMessage());
		} finally {
			release(conn);
		}

		return out;
	}

	/**
	 * Returns counts of late arrivals per day for a given month. Late is defined as
	 * arrival after scheduled time and up to 15 minutes (story cancellation
	 * threshold).
	 *
	 * @param year  report year
	 * @param month report month (1-12)
	 * @return map day->count
	 */
	public Map<Integer, Integer> getLateArrivalsByDay(int year, int month) {
		final String sql = "SELECT DAY(o.order_date) d, COUNT(*) c " + "FROM orders o "
				+ "JOIN table_sessions ts ON ts.order_number = o.order_number " + "WHERE o.order_type='RESERVATION' "
				+ "AND YEAR(o.order_date)=? AND MONTH(o.order_date)=? " + "AND ts.seated_at IS NOT NULL "
				+ "AND ts.seated_at >= TIMESTAMP(o.order_date, o.order_time) "
				+ "AND ts.seated_at <= TIMESTAMP(o.order_date, o.order_time) + INTERVAL 15 MINUTE "
				+ "GROUP BY DAY(o.order_date)";

		Map<Integer, Integer> out = new HashMap<>();
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, year);
				ps.setInt(2, month);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next())
						out.put(rs.getInt("d"), rs.getInt("c"));
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getLateArrivalsByDay: " + e.getMessage());
		} finally {
			release(conn);
		}

		return out;
	}

	/**
	 * Returns counts of on-time arrivals per day for a given month. On-time is
	 * defined as arrival at or before the scheduled time.
	 *
	 * @param year  report year
	 * @param month report month (1-12)
	 * @return map day->count
	 */
	public Map<Integer, Integer> getOnTimeArrivalsByDay(int year, int month) {
		final String sql = "SELECT DAY(o.order_date) d, COUNT(*) c " + "FROM orders o "
				+ "JOIN table_sessions ts ON ts.order_number = o.order_number " + "WHERE o.order_type='RESERVATION' "
				+ "AND YEAR(o.order_date)=? AND MONTH(o.order_date)=? " + "AND ts.seated_at IS NOT NULL "
				+ "AND ts.seated_at <= TIMESTAMP(o.order_date, o.order_time) " + "GROUP BY DAY(o.order_date)";

		Map<Integer, Integer> out = new HashMap<>();
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, year);
				ps.setInt(2, month);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next())
						out.put(rs.getInt("d"), rs.getInt("c"));
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getOnTimeArrivalsByDay: " + e.getMessage());
		} finally {
			release(conn);
		}

		return out;
	}

	/**
	 * Builds arrival-lateness distribution buckets for a given month. Lateness is
	 * defined as minutes between scheduled time and actual seated time.
	 *
	 * @param year  report year
	 * @param month report month (1-12)
	 * @return map bucketLabel->count
	 */
	public Map<String, Integer> getLatenessBuckets(int year, int month) {
		final String sql = "SELECT " + "  CASE "
				+ "    WHEN TIMESTAMPDIFF(MINUTE, TIMESTAMP(o.order_date,o.order_time), ts.seated_at) BETWEEN 0 AND 5 THEN '0-5' "
				+ "    WHEN TIMESTAMPDIFF(MINUTE, TIMESTAMP(o.order_date,o.order_time), ts.seated_at) BETWEEN 6 AND 15 THEN '6-15' "
				+ "    WHEN TIMESTAMPDIFF(MINUTE, TIMESTAMP(o.order_date,o.order_time), ts.seated_at) BETWEEN 16 AND 30 THEN '16-30' "
				+ "    WHEN TIMESTAMPDIFF(MINUTE, TIMESTAMP(o.order_date,o.order_time), ts.seated_at) >= 31 THEN '31+' "
				+ "    ELSE 'early' " + "  END AS bucket, " + "  COUNT(*) c " + "FROM orders o "
				+ "JOIN table_sessions ts ON ts.order_number = o.order_number " + "WHERE o.order_type='RESERVATION' "
				+ "AND YEAR(o.order_date)=? AND MONTH(o.order_date)=? " + "AND ts.seated_at IS NOT NULL "
				+ "GROUP BY bucket";

		Map<String, Integer> out = new HashMap<>();
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, year);
				ps.setInt(2, month);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next())
						out.put(rs.getString("bucket"), rs.getInt("c"));
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getLatenessBuckets: " + e.getMessage());
		} finally {
			release(conn);
		}

		return out;
	}

	/**
	 * Builds leaving-delay (overstay) distribution buckets for a given month.
	 * Overstay is defined as minutes beyond the 2-hour reservation slot: (left_at -
	 * seated_at) - 120.
	 *
	 * @param year  report year
	 * @param month report month (1-12)
	 * @return map bucketLabel->count
	 */
	public Map<String, Integer> getOverstayBuckets(int year, int month) {
		final String sql = "SELECT " + "  CASE " + "    WHEN ts.left_at IS NULL OR ts.seated_at IS NULL THEN 'unknown' "
				+ "    WHEN TIMESTAMPDIFF(MINUTE, ts.seated_at, ts.left_at) <= 120 THEN '0' "
				+ "    WHEN TIMESTAMPDIFF(MINUTE, ts.seated_at, ts.left_at) BETWEEN 121 AND 130 THEN '1-10' "
				+ "    WHEN TIMESTAMPDIFF(MINUTE, ts.seated_at, ts.left_at) BETWEEN 131 AND 150 THEN '11-30' "
				+ "    WHEN TIMESTAMPDIFF(MINUTE, ts.seated_at, ts.left_at) BETWEEN 151 AND 180 THEN '31-60' "
				+ "    ELSE '61+' " + "  END AS bucket, " + "  COUNT(*) c " + "FROM orders o "
				+ "JOIN table_sessions ts ON ts.order_number = o.order_number " + "WHERE o.order_type='RESERVATION' "
				+ "AND YEAR(o.order_date)=? AND MONTH(o.order_date)=? " + "GROUP BY bucket";

		Map<String, Integer> out = new HashMap<>();
		Connection conn = null;

		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, year);
				ps.setInt(2, month);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next())
						out.put(rs.getString("bucket"), rs.getInt("c"));
				}
			}
		} catch (SQLException e) {
			logger.log("[ERROR] getOverstayBuckets: " + e.getMessage());
		} finally {
			release(conn);
		}

		return out;
	}

	/**
	 * Retrieves the weekly opening hours from the database.
	 * 
	 * @return List of WeeklyHour objects representing the opening hours for each
	 *         day
	 */
	public List<WeeklyHour> getWeeklyHours() {
		String qry = "SELECT day_of_week, open_time, close_time FROM opening_hours_weekly";
		List<WeeklyHour> hours = new ArrayList<>();
		Connection conn = null;
		try {
			conn = borrow();
			try (PreparedStatement ps = conn.prepareStatement(qry)) {
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						int dayOfWeek = rs.getInt("day_of_week");
						Time openTimeSql = rs.getTime("open_time");
						Time closeTimeSql = rs.getTime("close_time");
						LocalTime openTime = openTimeSql != null ? openTimeSql.toLocalTime() : null;
						LocalTime closeTime = closeTimeSql != null ? closeTimeSql.toLocalTime() : null;
						hours.add(new WeeklyHour(dayOfWeek, openTime, closeTime));
					}
				}
			}
		} catch (SQLException ex) {
			logger.log("[ERROR] SQLException in getWeeklyHours: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			release(conn);
		}
		return hours;
	}
}
// End of BistroDataBase_Controller.java
