package logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import entities.Order;
import entities.User;
import enums.UserType;

/*
 * BistroDataBase_Controller manages a simple JDBC connection pool (no external dependencies)
 * and provides methods to interact with the orders table.
 *
 * Key idea:
 * - The server initializes the pool once (openConnection()).
 * - Each DB method borrows one Connection from the pool, uses it, and releases it back.
 * - The server closes the pool once (closeConnection()).
 */
public class BistroDataBase_Controller {

	// Database connection parameters:
	private static final String JDBC_URL =
			"jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
	private static final String JDBC_USER = "root";
	private static final String JDBC_PASS = "Aa123456";

	// Pool configuration:
	private static final int POOL_SIZE = 10;               // how many connections are kept open
	private static final long BORROW_TIMEOUT_MS = 10_000;  // how long a thread waits for a free connection

	// Thread-safe pool container:
	private static BlockingQueue<Connection> pool = null;

	// Indicates if pool is initialized and usable:
	private static volatile boolean initialized = false;

	/*
	 * Initializes the pool (creates POOL_SIZE connections).
	 * Kept the same name openConnection() so BistroServer.serverStarted() stays unchanged.
	 */
	public static synchronized boolean openConnection() {
		if (initialized) {
			return true; // already initialized
		}

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
			// If partial init happened, close what we managed to create:
			closeConnection();
			return false;
		}
	}

	/*
	 * Closes all pooled connections and clears the pool.
	 * Kept the same name closeConnection() so BistroServer.serverStopped() stays unchanged.
	 */
	public static synchronized void closeConnection() {
		initialized = false;

		if (pool == null) {
			return;
		}

		Connection c;
		while ((c = pool.poll()) != null) {
			try {
				c.close();
			} catch (SQLException ignored) {
			}
		}

		pool = null;
		ServerLogger.log("SQL connection pool closed");
	}

	/*
	 * Borrow a connection from the pool.
	 *
	 * Important:
	 * - This blocks up to BORROW_TIMEOUT_MS waiting for an available connection.
	 * - Using a timeout prevents the server from hanging forever if a connection is leaked.
	 */
	private static Connection borrow() throws SQLException {
		if (!initialized || pool == null) {
			throw new SQLException("DB pool not initialized. Call openConnection() first.");
		}

		try {
			Connection c = pool.poll(BORROW_TIMEOUT_MS, TimeUnit.MILLISECONDS);
			if (c == null) {
				throw new SQLException("Timed out waiting for a DB connection from the pool.");
			}

			// Validate the connection. MySQL may drop idle connections.
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

	/*
	 * Release the borrowed connection back to the pool.
	 *
	 * CRITICAL RULE:
	 * - Every borrow() must have a matching release(conn) inside a finally block.
	 */
	private static void release(Connection c) {
		if (c == null) return;

		// If pool was closed while request is running, just close the connection:
		if (!initialized || pool == null) {
			try {
				c.close();
			} catch (SQLException ignored) {
			}
			return;
		}

		try {
			if (c.isClosed()) return;

			// Return it back to the pool. If offer fails (shouldn't happen), close it to avoid leaking.
			if (!pool.offer(c)) {
				c.close();
			}
		} catch (SQLException ignored) {
		}
	}

	/*
	 * Retrieves an Order by its confirmation code.
	 */
	public static Order getOrderByConfirmationCode(int ConfCode) {
		String orderQuery = "SELECT "
				+ "order_number,"
				+ " order_date,"
				+ " number_of_guests,"
				+ " confirmation_code,"
				+ " user_id,"
				+ " date_of_placing_order,"
				+ " order_time,"
				+ " order_active,"
				+ " wait_list"
				+ " FROM orders WHERE confirmation_code = ?";

		Connection conn = null;
		try {
			conn = borrow();

			try (PreparedStatement pst = conn.prepareStatement(orderQuery)) {
				pst.setInt(1, ConfCode);

				try (ResultSet rs = pst.executeQuery()) {
					if (!rs.next()) {
						return null;
					}

					int order_number = rs.getInt("order_number");
					LocalDate order_date = rs.getDate("order_date").toLocalDate();
					int number_of_guests = rs.getInt("number_of_guests");
					int confirmation_code = rs.getInt("confirmation_code");
					int user_id = rs.getInt("user_id");
					LocalDate date_of_placing_order = rs.getDate("date_of_placing_order").toLocalDate();
					LocalTime order_time = rs.getTime("order_time").toLocalTime();
					boolean order_active = rs.getBoolean("order_active");
					boolean wait_list = rs.getBoolean("wait_list");
					

					return new Order(order_number, order_date, order_time, number_of_guests, confirmation_code,
							user_id, order_active, wait_list, date_of_placing_order);
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

	/*
	 * Updates an existing order in the database.
	 */
	public static boolean updateOrder(Order orderUpdateData) {
		String updateQuery = "UPDATE orders SET order_date = ?, number_of_guests = ? WHERE confirmation_code = ?";

		Connection conn = null;
		try {
			conn = borrow();

			try (PreparedStatement pst = conn.prepareStatement(updateQuery)) {
				pst.setDate(1, java.sql.Date.valueOf(orderUpdateData.getOrderDate()));
				pst.setInt(2, orderUpdateData.getDinersAmount());
				pst.setInt(3, orderUpdateData.getConfirmationCode());

				int rowsAffected = pst.executeUpdate();

				if (rowsAffected > 0) {
					ServerLogger.log("Order updated successfully, confirmation code: "
							+ orderUpdateData.getConfirmationCode());
					return true;
				} else {
					ServerLogger.log("No order found with confirmation code: "
							+ orderUpdateData.getConfirmationCode());
					return false;
				}
			}

		} catch (SQLException ex) {
			ServerLogger.log("SQLException in updateOrder: " + ex.getMessage());
			
			ex.printStackTrace();
			return false;
		} finally {
			release(conn);
		}
	}

	/*
	 * Retrieves all orders from the database.
	 *
	 * - Returns an empty list (not null) on SQL error.
	 */
	public static List<Order> getAllOrders() {
		List<Order> allOrders = new ArrayList<>();
		String orderQuery = "SELECT * from orders";

		Connection conn = null;
		try {
			conn = borrow();

			try (PreparedStatement pst = conn.prepareStatement(orderQuery);
				 ResultSet rs = pst.executeQuery()) {

				while (rs.next()) {
					int order_number = rs.getInt("order_number");
					LocalDate order_date = rs.getDate("order_date").toLocalDate();
					int number_of_guests = rs.getInt("number_of_guests");
					int confirmation_code = rs.getInt("confirmation_code");
					int user_id = rs.getInt("user_id");
					LocalDate date_of_placing_order = rs.getDate("date_of_placing_order").toLocalDate();
					LocalTime order_time = rs.getTime("order_time").toLocalTime();
					boolean order_active = rs.getBoolean("order_active");
					boolean wait_list = rs.getBoolean("wait_list");

					Order currentOrder = new Order(order_number, order_date, order_time, number_of_guests, confirmation_code,
							user_id, order_active, wait_list, date_of_placing_order);

					allOrders.add(currentOrder);
				}
			}

		} catch (SQLException ex) {
			ServerLogger.log("SQLException in getAllOrders: " + ex.getMessage());
			ex.printStackTrace();
			// keep allOrders empty on error
		} finally {
			release(conn);
		}

		return allOrders;
	}

	/*
	 * Checks if a date is available (no other order has that date, excluding a confirmation code).
	 */
	public static boolean isDateAvailable(LocalDate date, int confirmationCodeToExclude) {
		String dateQuery = "SELECT * FROM orders WHERE order_date = ? AND confirmation_code != ?";

		Connection conn = null;
		try {
			conn = borrow();

			try (PreparedStatement pst = conn.prepareStatement(dateQuery)) {
				pst.setDate(1, java.sql.Date.valueOf(date));
				pst.setInt(2, confirmationCodeToExclude);

				try (ResultSet rs = pst.executeQuery()) {
					// If any row exists -> date is taken, not available
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

	
	//get user info by his user type (we need in db to add address, barcode,pass for admin) we need to talk with client side
	public static User getUserInfo(String phoneNum, String Email)
	{
		if((phoneNum == null || phoneNum.isEmpty()) && (Email == null || Email.isEmpty()))
		{
			return null;
		}
		String userQuery = "SELECT "
							+ "u.user_id, "
							+ "u.name, "
							+ "u.type, "
							+ "m.member_code, "
							+ "m.f_name "
							+ "m.l_name "
							+ "FROM users u "
							+ "LEFT JOIN members m "
							+ "ON u.user_id = m.user_id "
							+ "WHERE "
							+ "(u.phoneNumber = ? OR ? IS NULL) "
							+ "AND (u.email = ? OR ? IS NULL)";
		
		Connection conn = null;
		try {
			conn = borrow();

			try (PreparedStatement pst = conn.prepareStatement(userQuery)) {
				pst.setString(1, phoneNum);
		        pst.setString(2, phoneNum);
		        pst.setString(3, Email);
		        pst.setString(4, Email);

		        try (ResultSet rs = pst.executeQuery()) {
					if (!rs.next()) {
						return null;
					}
					String userID = String.valueOf(rs.getInt("user_id"));
					String typeValue = rs.getString("type");
					UserType type = null;
					if (typeValue != null) {
					    type = UserType.valueOf(typeValue.toUpperCase());
					}
					
					User user;
					switch (type) {
				    case GUEST:
				        user = new User(phoneNum, Email,type );
				        break;

				    case MEMBER:
				        user = new User(userID, rs.getString("barcode"),rs.getString("f_name"),rs.getString("l_name"),rs.getString("address"),
				        		phoneNum,Email,type );
				     
				        break;
				       
				   
				    case MANAGER:
				        user = new User(Email, rs.getString("password"),rs.getString("f_name"),rs.getString("l_name"),
		        				rs.getString("address"),phoneNum,Email,type );
				        break;
				        
				        
				    case EMPLOYEE:
				        user = new User(Email, rs.getString("password"),rs.getString("f_name"),rs.getString("l_name"),
		        				rs.getString("address"),phoneNum,Email,type);
				        break;
					}
				}
				
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
			
		} finally {
			release(conn);
		}
	}
				

		return ;
	}
}
