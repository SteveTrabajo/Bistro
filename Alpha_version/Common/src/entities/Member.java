package entities;

import java.util.List;

public class Member extends Guest {
	private int memberID;
	private String memberCode;
	private String firstName;
	private String lastName;
	private List<Order> orderHistory;

	public Member(String phoneNumber, String email, int memberID, String memberCode, String firstName) {
		super(phoneNumber, email);
		this.memberID = memberID;
		this.memberCode = memberCode;
		this.firstName = firstName;
		this.lastName = lastName;
		// Initialize orderHistory as an empty list
		this.orderHistory = null;
	}
	
	public int getMemberID() {
		return memberID;
	}
	
	public String getMemberCode() {
		return memberCode;
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
	
	public List<Order> getOrderHistory() {
		return orderHistory;
	}
}
