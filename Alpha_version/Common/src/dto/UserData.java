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
    
    /*
	 * Creates a UserData instance.
	 * @param firstName   the user's first name
	 * @param lastName    the user's last name
	 * @param MemberCode  the user's member code
	 * @param phone       the user's phone number
	 * @param email       the user's email address
	 * @param userType    the type of user
	 * @param address     the user's address
	 */
    public UserData(String firstName, String lastName, String MemberCode, String phone, String email, UserType userType, String address) {
    	this.firstName = firstName;
    	this.lastName = lastName;
    	this.address = address;
		this.phone = phone;
		this.email = email;
		this.userType = userType;
		this.MemberCode = MemberCode;
	}
    
    /*
     * Creates a UserData instance with minimal information.
     * @param userId       the user's ID
     * @param phoneNumber  the user's phone number
     * @param email2       the user's email address
     * @param object       an object representing the member code
     * @param userType2    the type of user
     */
    public UserData(int userId, String phoneNumber, String email2, Object object, UserType userType2) {
		this.phone = phoneNumber;
		this.email = email2;
		this.userType = userType2;
		this.MemberCode = String.valueOf(object);
	}

    /*
	 * Gets the user's first name.
	 * @return the first name
	 */
    public String getFirstName() {
	  	return firstName;
    }
  
    /*
     * Gets the user's last name.
     * @return the last name
     */
    public String getLastName() {
  		return lastName;
    }
    
    /*
	 * Gets the user's email address.
	 * @return the email address
	 */
	public String getEmail() {
		return email;
	}

	/*
	 * Gets the user's phone number.
	 * @return the phone number
	 */
	public String getPhone() {
		return phone;
	}

	/*
	 * Gets the user's member code.
	 * @return the member code
	 */
	public String getMemberCode() {
		return MemberCode;
	}
	
	/*
	 * Gets the user's address.
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	
	/*
	 * Gets the user's type.
	 * @return the user type
	 */
	public UserType getUserType() {
		return userType;
	}
}
// End of UserData.java