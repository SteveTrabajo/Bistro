package logic;

import java.util.ArrayList;
import java.util.Map;

import comms.*;
import entities.User;
import enums.UserType;

/*
 * This class represents the controller for user-related operations in the BistroClient.
 */
public class User_Controller {
	
	//****************************** Instance variables ******************************
	
	private final BistroClient client; //final reference to the BistroClient to ensure only one instance is associated
	private User loggedInUser;
	
	//******************************** Constructors ***********************************
	
	/*
	 * Constructor to initialize the User_Controller with a reference to the BistroClient.
	 * 
	 * @param client The BistroClient instance for server communication.
	 */
	public User_Controller(BistroClient client) {
		this.client=client;
	}
	
	//******************************** Getters And Setters ***********************************
	
	/*
	 * Getter for the currently logged-in user.
	 * 
	 * @return The User object representing the logged-in user.
	 */
	public User getLoggedInUser() {
		return loggedInUser;
	}
	
	/*
	 * Setter for the currently logged-in user.
	 * 
	 * @param user The User object to set as the logged-in user.
	 */
	public void setLoggedInUser(User user) {
		this.loggedInUser = user;
	}
	
	//******************************** Instance Methods ***********************************
	
	/*
	 * Method to sign in a user with the provided login data.
	 * 
	 * @param userLoginData An ArrayList containing user login information.
	 */
	public void signInUser(Map<String, Object> userLoginData, String massageType, UserType type) {
		if (type == UserType.GUEST) {
			setLoggedInUser(new User((String) (userLoginData.get("phoneNumber")),
					(String) (userLoginData.get("emailAddress")), type));
		} else {
			Message req = new Message(massageType, (Object) userLoginData);
			client.handleMessageFromClientUI(req);
		}
	}
	
	public void signOutUser() {
		this.loggedInUser = null;
	}
	
	/*
	 * Method to check if a user is currently logged in.
	 * 
	 * @return true if a user is logged in, false otherwise.
	 */
	public boolean isUserLoggedIn() {
		return this.loggedInUser != null;
	}
	
	/*
	 * Method to get the type of the currently logged-in user.
	 * 
	 * @return The UserType of the logged-in user, or null if no user is logged in.
	 */
	public UserType getLoggedInUserType() {
		if (isUserLoggedIn()) {
			return this.loggedInUser.getUserType();
		}
		return null;
	}
	
}
