package logic;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entities.Order;

/*
 * BistroDataBase_Controller class manages the connection to the Bistro prototype database
 * and provides methods to interact with the orders table.
 */
public class BistroDataBase_Controller {
	// Database connection parameters:
	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/bistro_prototype?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
	private static final String JDBC_USER = "root";
	private static final String JDBC_PASS = "Aa123456";

	private static Connection conn = null; // Shared connection instance
	
	/*
	 * Method that opens a connection to the database if not already open.
	 * @return true if the connection is successfully opened or already open, false otherwise.
	 */
	public static synchronized boolean openConnection() {
		if (conn != null)
			return true;
		try {
			conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
			System.out.println("SQL connection opened");
			return true;
		} catch (SQLException ex) {
			System.out.println("Failed to open SQL connection: " + ex.getMessage());
			conn = null;
			return false;
		}
	}
	
	
	/*
	 * Method that closes the database connection if it is open.
	 */
	public static synchronized void closeConnection() {
		if (conn == null)
			return;
		try {
			conn.close();
			System.out.println("SQL connection closed");
		} catch (SQLException ex) {
			System.out.println("Error closing SQL connection: " + ex.getMessage());
		} finally {
			conn = null;
		}
		return;
	}
	
	
	/*
	 * Method that returns the current database connection.
	 * @return The current Connection object.
	 */
	public static synchronized Connection getConnection() {
		return conn;
	}
	
	
	/*
	 * Method that retrieves an Order object from the database by its confirmation code.
	 * 
	 * @param ConfCode The confirmation code of the order to retrieve.
	 * @return The Order object if found, otherwise null.
	 */
	public static Order getOrderByConfirmationCode(int ConfCode) {
		String orderQuery = "SELECT "
				+ "order_number,"
				+ " order_date,"
				+ " number_of_guests,"
				+ " confirmation_code,"
				+ " member_id,"
				+ " date_of_placing_order"
				+ " FROM orders WHERE confirmation_code = ?";
		// Ensure we have a connection
		try (PreparedStatement pst = conn.prepareStatement(orderQuery)) {
			pst.setInt(1, ConfCode); // Set the confirmation code parameter
			try (ResultSet rs = pst.executeQuery()) { // Execute the query
				if (!rs.next()) {
					return null;
				}
				// Extract order details from the result set
				int order_number = rs.getInt("order_number");
				Date order_date = rs.getDate("order_date");
				int number_of_guests = rs.getInt("number_of_guests");
				int confirmation_code = rs.getInt("confirmation_code");
				int member_id = rs.getInt("member_id");
				Date date_of_placing_order = rs.getDate("date_of_placing_order");
				// Create and return the Order object
				return new Order(order_number, order_date, number_of_guests, confirmation_code, member_id,
						date_of_placing_order);
			}
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			return null;
		}
	}
	
	
	/*
	 * Method that updates an existing order in the database with new data.
	 * 
	 * @param orderUpdateData An Order object containing the updated data.
	 * @return true if the update was successful, false otherwise.
	 */
	public static boolean updateOrder(Order orderUpdateData) {
		// Ensure we have a connection
		if (conn == null && !openConnection()) {
			System.out.println("updateOrder: No DB connection available.");
			return false;
		}
		// SQL update query
		String updateQuery = "UPDATE orders SET order_date = ?, number_of_guests = ? WHERE confirmation_code = ?";
		// Prepare and execute the update statement
		try (PreparedStatement pst = conn.prepareStatement(updateQuery)) {
			// Use values from the Order object
			pst.setDate(1, orderUpdateData.getOrderDate()); // java.sql.Date
			pst.setInt(2, orderUpdateData.getDinersAmount()); // int
			pst.setInt(3, orderUpdateData.getConfirmationCode()); // int
			// Execute the update
			int rowsAffected = pst.executeUpdate();
			// Check if any rows were updated
			if (rowsAffected > 0) {
				System.out.println("Order updated successfully, confirmation code: " + orderUpdateData.getConfirmationCode());
				return true;
			} else {
				System.out.println("No order found with confirmation code: " + orderUpdateData.getConfirmationCode());
				return false;
			}
		} catch (SQLException ex) {
			System.out.println("SQLException in updateOrder: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		}
	}
	
	
	/*
	 * Method that retrieves all orders from the database.
	 * 
	 * @return A list of all Order objects in the database.
	 */
	public static List<Order> getAllOrders() {
		List<Order> allOrders = new ArrayList<>(); // List to hold all orders
		String orderQuery = "SELECT * from orders"; // SQL query to select all orders
		// Prepare and execute the query
		try (PreparedStatement pst = conn.prepareStatement(orderQuery)) {
			try (ResultSet rs = pst.executeQuery()) {
				while (rs.next()) {
					int order_number = rs.getInt("order_number");
					Date order_date = rs.getDate("order_date");
					int number_of_guests = rs.getInt("number_of_guests");
					int confirmation_code = rs.getInt("confirmation_code");
					int member_id = rs.getInt("member_id");
					Date date_of_placing_order = rs.getDate("date_of_placing_order");
					// Create Order object and add to the list
					Order currentOrder = new Order(order_number, order_date, number_of_guests, confirmation_code,
							member_id, date_of_placing_order);
					// Add the current order to the list
					allOrders.add(currentOrder);
				}
			}
			return allOrders;
		} catch (SQLException ex) {
			System.out.println("SQLException in getAllOrders: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
	}


	public static boolean isDateAvailable(Date date, int i) {
		String dateQuery = "SELECT * FROM orders WHERE order_date = ? AND confirmation_code != ?";
		try (PreparedStatement pst = conn.prepareStatement(dateQuery)) {
			pst.setDate(1, date); // Set the date parameter
			pst.setInt(2, i); // Set the confirmation code parameter to exclude
			try (ResultSet rs = pst.executeQuery()) { // Execute the query
				if (rs.next()) {
					// Date is taken by another order
					return false;
				} else {
					// Date is available
					return true;
				}
			}
		} catch (SQLException ex) {
			System.out.println("SQLException in isDateAvailable: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		}
	}
}
// End of BistroDataBase_Controller.java