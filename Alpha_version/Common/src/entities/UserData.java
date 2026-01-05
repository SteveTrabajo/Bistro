package entities;

import java.io.Serializable;

import enums.UserType;

public class UserData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String Name;
    private String email;
    private String phone;
    private String MemberCode;
    private UserType userType;
    
    public UserData(String MemberCode, String fullName, String phone, String email, UserType userType) {
		this.MemberCode = MemberCode;
		this.Name = fullName;
		this.phone = phone;
		this.email = email;
		this.userType = userType;
	}

	public String getName() {
		return Name;
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
}
