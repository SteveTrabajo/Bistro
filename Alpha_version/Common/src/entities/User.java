package entities;

import java.io.Serializable;

import enums.UserType;

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
	public User(int userid,String phoneNumber, String email, UserType type ) {
		this.userId=userid;
		this.type = type;
		this.phoneNumber = phoneNumber;
		this.email = email;
	}
	
	// Constructor for MEMBER users
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
	public User(int userid,String phoneNumber,String email,String username, UserType type) {
		this.userId=userid;
		this.type = type;
		this.phoneNumber = phoneNumber;
		this.email = email;
	}
	
	//Constructor for display User in Management tool
	public User(String fullName, String email,String phoneNumber ,String memberCode, UserType type) {
		this.fullName = fullName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.memberCode = memberCode;
		this.type = type;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public UserType getUserType() {
		return type;
	}

	public void setUserType(UserType userType) {
		this.type = userType;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMemberCode() {
		return memberCode;
	}

	public void setMemberCode(String memberCode) {
		this.memberCode = memberCode;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getFullName() {
		return fullName;
	}
	
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	
	public String getAddress() {
		return this.address;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
}
