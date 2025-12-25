package logic;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import entities.Order;

/*
 * BistroDataBase_Controller class manages the connection to the Bistro prototype database
 * and provides methods to interact with the orders table.
 */
public class BistroDataBase_Controller {
	// Database connection parameters:
	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
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
	public static Order getOrderByConfirmationCode(int confCode) {

	    if (conn == null && !openConnection()) {
	        System.out.println("getOrderByConfirmationCode: No DB connection available.");
	        return null;
	    }

	    String orderQuery =
	            "SELECT order_number, order_date, order_time, number_of_guests, confirmation_code, member_id, " +
	            "date_of_placing_order, order_active, wait_list " +
	            "FROM orders WHERE confirmation_code = ?";

	    try (PreparedStatement pst = conn.prepareStatement(orderQuery)) {
	        pst.setInt(1, confCode);

	        try (ResultSet rs = pst.executeQuery()) {
	            if (!rs.next()) return null;

	            int orderNumber = rs.getInt("order_number");
	            Date sqlOrderDate = rs.getDate("order_date");
	            Time sqlOrderTime = rs.getTime("order_time");
	            int dinersAmount = rs.getInt("number_of_guests");
	            int confirmationCode = rs.getInt("confirmation_code");
	            int memberId = rs.getInt("member_id");
	            Date sqlPlacedDate = rs.getDate("date_of_placing_order");
	            boolean orderActive = rs.getBoolean("order_active");
	            boolean waitList = rs.getBoolean("wait_list");

	            return new Order(
	                    orderNumber,
	                    (sqlOrderDate == null) ? null : sqlOrderDate.toLocalDate(),
	                    (sqlOrderTime == null) ? null : sqlOrderTime.toLocalTime(),
	                    dinersAmount,
	                    confirmationCode,
	                    memberId,
	                    orderActive,
	                    waitList,
	                    (sqlPlacedDate == null) ? null : sqlPlacedDate.toLocalDate()
	            );
	        }
	    } catch (SQLException ex) {
	        System.out.println("SQLException in getOrderByConfirmationCode: " + ex.getMessage());
	        ex.printStackTrace();
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

	    if (conn == null && !openConnection()) {
	        System.out.println("updateOrder: No DB connection available.");
	        return false;
	    }

	    String updateQuery =
	            "UPDATE orders SET order_date = ?, number_of_guests = ? WHERE confirmation_code = ?";

	    try (PreparedStatement pst = conn.prepareStatement(updateQuery)) {

	        pst.setDate(1, orderUpdateData.getOrderDate() == null ? null : Date.valueOf(orderUpdateData.getOrderDate()));
	        pst.setInt(2, orderUpdateData.getDinersAmount());
	        pst.setInt(3, orderUpdateData.getConfirmationCode());

	        int rowsAffected = pst.executeUpdate();

	        if (rowsAffected > 0) {
	            System.out.println("Order updated successfully, confirmation code: " +
	                    orderUpdateData.getConfirmationCode());
	            return true;
	        } else {
	            System.out.println("No order found with confirmation code: " +
	                    orderUpdateData.getConfirmationCode());
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

	    if (conn == null && !openConnection()) {
	        System.out.println("getAllOrders: No DB connection available.");
	        return null;
	    }

	    List<Order> allOrders = new ArrayList<>();

	    String orderQuery =
	            "SELECT order_number, order_date, order_time, number_of_guests, confirmation_code, member_id, " +
	            "date_of_placing_order, order_active, wait_list " +
	            "FROM orders";

	    try (PreparedStatement pst = conn.prepareStatement(orderQuery);
	         ResultSet rs = pst.executeQuery()) {

	        while (rs.next()) {
	            int orderNumber = rs.getInt("order_number");
	            Date sqlOrderDate = rs.getDate("order_date");
	            Time sqlOrderTime = rs.getTime("order_time");
	            int dinersAmount = rs.getInt("number_of_guests");
	            int confirmationCode = rs.getInt("confirmation_code");
	            int memberId = rs.getInt("member_id");
	            Date sqlPlacedDate = rs.getDate("date_of_placing_order");
	            boolean orderActive = rs.getBoolean("order_active");
	            boolean waitList = rs.getBoolean("wait_list");

	            Order currentOrder = new Order(
	                    orderNumber,
	                    (sqlOrderDate == null) ? null : sqlOrderDate.toLocalDate(),
	                    (sqlOrderTime == null) ? null : sqlOrderTime.toLocalTime(),
	                    dinersAmount,
	                    confirmationCode,
	                    memberId,
	                    orderActive,
	                    waitList,
	                    (sqlPlacedDate == null) ? null : sqlPlacedDate.toLocalDate()
	            );

	            allOrders.add(currentOrder);
	        }

	        return allOrders;

	    } catch (SQLException ex) {
	        System.out.println("SQLException in getAllOrders: " + ex.getMessage());
	        ex.printStackTrace();
	        return null;
	    }
	}



	public static boolean isDateAvailable(LocalDate date, int excludeConfCode) {

	    if (conn == null && !openConnection()) {
	        System.out.println("isDateAvailable: No DB connection available.");
	        return false;
	    }

	    String dateQuery = "SELECT 1 FROM orders WHERE order_date = ? AND confirmation_code != ? LIMIT 1";

	    try (PreparedStatement pst = conn.prepareStatement(dateQuery)) {
	        pst.setDate(1, date == null ? null : Date.valueOf(date));
	        pst.setInt(2, excludeConfCode);

	        try (ResultSet rs = pst.executeQuery()) {
	            return !rs.next(); // if found -> not available
	        }
	    } catch (SQLException ex) {
	        System.out.println("SQLException in isDateAvailable: " + ex.getMessage());
	        ex.printStackTrace();
	        return false;
	    }
	}
}
// End of BistroDataBase_Controller.java