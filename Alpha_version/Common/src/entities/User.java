package entities;

public abstract class User {
	private String phoneNumber;
	private String email;
	//private type userType;
	
	public User(String phoneNumber, String email) {
		this.phoneNumber = phoneNumber;
		this.email = email;
		//this.userType = ;
	}
}
