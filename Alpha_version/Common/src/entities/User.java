package entities;

import java.util.List;

/*
 * This class represents a user in the Bistro system.
 */
public class User {
	
	//*************************** Instance variables ***************************//
	
	private UserType userType;
	private String phoneNumber;
	private String id;
	private String barcode;
	private String email;
	private String password;
	private String firstName;
	private String lastName;
	private String address;
	private List<Order> orderHistory;
	
	//****************************** Constructors ******************************//
	
	/* 
	 * constructor for GUEST user
	 * 
	 * @param phoneNumber
	 * @param email
	 * @param userType
	 */
	public User(String phoneNumber, String email,UserType userType) {
		this.phoneNumber = phoneNumber;
		this.email = email;
	}
	
	/* 
	 * constructor for MEMBER user
	 * 
	 * @param id
	 * @param barcode
	 * @param firstName
	 * @param lastName
	 * @param address
	 * @param phoneNumber
	 * @param email
	 * @param userType
	 */
	public User(String id, String barcode, String firstName, String lastName, String address, String phoneNumber,
			String email, UserType userType) {
		this.id = id;
		this.barcode = barcode;
		this.firstName = firstName;
		this.lastName = lastName;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.userType = userType;
		this.orderHistory = null; // initialize order history as empty
	}
	
	/* 
	 * constructor for EMPLOYEE and MANAGER user
	 * 
	 * @param email
	 * @param password
	 * @param firstName
	 * @param lastName
	 * @param userType
	 */
	public User(String email, String password, String firstName ,String lastName, UserType userType) {
		this.email = email;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.userType = userType;
	}
	
	//****************************** Getters And Setters ******************************//
	
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getID() {
		return id;
	}
	
	public String getBarcode() {
		return barcode;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public void setAddress(String address) {
		this.address=address;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return password;
	}
	
	public UserType getUserType() {
		return userType;
	}
}
//End of User class