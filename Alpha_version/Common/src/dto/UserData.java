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
    
    public UserData(String firstName, String lastName, String MemberCode, String phone, String email, UserType userType, String address) {
    	this.firstName = firstName;
    	this.lastName = lastName;
    	this.address = address;
		this.phone = phone;
		this.email = email;
		this.userType = userType;
		this.MemberCode = MemberCode;
	}
    
  public UserData(int userId, String phoneNumber, String email2, Object object, UserType userType2) {
		this.phone = phoneNumber;
		this.email = email2;
		this.userType = userType2;
		this.MemberCode = String.valueOf(object);
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
	public String getAddress() {
		return address;
	}
	public UserType getUserType() {
		return userType;
	}
}
