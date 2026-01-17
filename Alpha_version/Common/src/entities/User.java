package entities;

import java.io.Serializable;

import enums.UserType;

/*
 * Represents a user in the system with various attributes based on user type.
 */
public class User implements Serializable {
	//****************************** Instance variables******************************
	private static final long serialVersionUID = 1L;
	// Common fields
	private int userId;
	private UserType type; // GUEST / MEMBER / EMPLOYEE / MANAGER
	private String phoneNumber;
	private String email; 
	private String firstName;
	private String lastName;
	private String username;
	
	// Member specific fields
	private String memberCode;
	private String address;
	private String fullName;
	
	//****************************** Constructors ******************************
	
	// Default constructor for GUEST users
	/*
	 * Creates a User instance for GUEST users.
	 * @param userid       the user ID
	 * @param phoneNumber  the user's phone number
	 * @param email        the user's email address
	 * @param type         the type of user
	 */
	public User(int userid,String phoneNumber, String email, UserType type ) {
		this.userId=userid;
		this.type = type;
		this.phoneNumber = phoneNumber;
		this.email = email;
	}
	
	// Constructor for MEMBER users
	/*
	 * Creates a User instance for MEMBER users.
	 * @param userid       the user ID
	 * @param phoneNumber  the user's phone number
	 * @param email        the user's email address
	 * @param memberCode   the member code
	 * @param firstName    the user's first name
	 * @param lastName     the user's last name
	 * @param address      the user's address
	 * @param type         the type of user
	 */
	public User(int userid, String phoneNumber, String email, String memberCode, String firstName,String lastName, String address,UserType type) {
		this.userId=userid;	
		this.type = type;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.memberCode = memberCode;
		this.firstName = firstName;
		this.lastName = lastName;
		this.address = address;
	}
	
	// Constructor for EMPLOYEE and MANAGER users
	/*
	 * Creates a User instance for EMPLOYEE and MANAGER users.
	 * @param userid       the user ID
	 * @param phoneNumber  the user's phone number
	 * @param email        the user's email address
	 * @param username     the username
	 * @param type         the type of user
	 */
	public User(int userid,String phoneNumber,String email,String username, UserType type) {
		this.userId=userid;
		this.type = type;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.username = username;
	}
	
	//Constructor for display User in Management tool
	/*
	 * Creates a User instance for displaying in the Management tool.
	 * @param fullName     the user's full name
	 * @param email        the user's email address
	 * @param phoneNumber  the user's phone number
	 * @param memberCode   the member code
	 * @param type         the type of user
	 */
	public User(String fullName, String email,String phoneNumber ,String memberCode, UserType type) {
		this.fullName = fullName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.memberCode = memberCode;
		this.type = type;
	}

	//****************************** Getters and Setters ******************************
	/*
	 * Gets the user ID.
	 * @return the user ID
	 */
	public int getUserId() {
		return userId;
	}

	/*
	 * Sets the user ID.
	 * @param userId the user ID to set
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/*
	 * Gets the user type.
	 * @return the user type
	 */
	public UserType getUserType() {
		return type;
	}

	/*
	 * Sets the user type.
	 * @param userType the user type to set
	 */
	public void setUserType(UserType userType) {
		this.type = userType;
	}

	/*
	 * Gets the phone number.
	 * @return the phone number
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/*
	 * Sets the phone number.
	 * @param phoneNumber the phone number to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/*
	 * Gets the email.
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/*
	 * Sets the email.
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/*
	 * Gets the member code.
	 * @return the member code
	 */
	public String getMemberCode() {
		return memberCode;
	}

	/*
	 * Sets the member code.
	 * @param memberCode the member code to set
	 */
	public void setMemberCode(String memberCode) {
		this.memberCode = memberCode;
	}

	/*
	 * Gets the first name.
	 * @return the first name
	 */
	public String getFirstName() {
		return firstName;
	}

	/*
	 * Sets the first name.
	 * @param firstName the first name to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/*
	 * Gets the last name.
	 * @return the last name
	 */
	public String getLastName() {
		return lastName;
	}

	/*
	 * Sets the last name.
	 * @param lastName the last name to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	/*
	 * Gets the full name.
	 * @return the full name
	 */
	public String getFullName() {
		return fullName;
	}
	
	/*
	 * Sets the full name.
	 * @param fullName the full name to set
	 */
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	
	/*
	 * Gets the address.
	 * @return the address
	 */
	public String getAddress() {
		return this.address;
	}
	
	/*
	 * Sets the address.
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	
	/*
	 * Gets the username.
	 * @return the username
	 */
	public String getUsername() {
		return this.username;
	}
	
	/*
	 * Sets the username.
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
}
// End of User class