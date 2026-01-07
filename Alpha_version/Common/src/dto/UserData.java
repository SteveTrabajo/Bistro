package dto;

import java.io.Serializable;

import enums.UserType;

public class UserData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String firstName;
    private String lastName;
    private String address;
    private String email;
    private String phone;
    private String MemberCode;
    private UserType userType;

    
    public UserData(String firstName, String lastName, String MemberCode, String phone, String email, UserType userType) {
		this.MemberCode = MemberCode;
		this.phone = phone;
		this.email = email;
		this.userType = userType;
	}
    
  public String getFirstName() {
	  		return firstName;
  }
  
  public String getLastName() {
  		return lastName;
  }
	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getMemberCode() {
		return MemberCode;
	}

	public UserType getUserType() {
		return userType;
	}
	public String getAddress() {
		return address;
	}
}
