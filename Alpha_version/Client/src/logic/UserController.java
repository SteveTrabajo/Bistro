package logic;

import java.util.ArrayList;
import java.util.Map;

import comms.*;
import entities.User;
import enums.UserType;

/*
 * This class represents the controller for user-related operations in the BistroClient.
 */
public class UserController {
	
	//****************************** Instance variables ******************************
	
	private final BistroClient client; //final reference to the BistroClient to ensure only one instance is associated
	private User loggedInUser;
	
	//******************************** Constructors ***********************************
	
	/*
	 * Constructor to initialize the User_Controller with a reference to the BistroClient.
	 * 
	 * @param client The BistroClient instance for server communication.
	 */
	public UserController(BistroClient client) {
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
	public void signInUser(String userLoginData, UserType userType) {
		
		switch(userType) {
			case GUEST:
				client.handleMessageFromClientUI(new Message(Api.ASK_LOGIN_GUEST, userLoginData));
				break;
			case EMPLOYEE:
				client.handleMessageFromClientUI(new Message(Api.ASK_LOGIN_EMPLOYEE, userLoginData));
				break;
			case MEMBER:
				client.handleMessageFromClientUI(new Message(Api.ASK_LOGIN_MEMBER, userLoginData));
				break;
			case MANAGER:
				client.handleMessageFromClientUI(new Message(Api.ASK_LOGIN_MANAGER, userLoginData));
				break;
			default:
				System.out.println("Unknown user type");
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
	public boolean isUserLoggedInAs( UserType expectedType) {
		return this.loggedInUser != null && this.loggedInUser.getUserType() == expectedType;
	}
	
	/*
	 * Method to get the type of the currently logged-in user.
	 * 
	 * @return The UserType of the logged-in user, or null if no user is logged in.
	 */
	public UserType getLoggedInUserType() {
		if (this.loggedInUser == null) {
			return null;
		}
		return this.loggedInUser.getUserType();
	}
	public void updateUserDetails(User updatedUser) {
		client.handleMessageFromClientUI(new Message(Api.ASK_MEMBER_UPDATE_INFO, updatedUser));

    }

	public boolean isUpdateSuccessful(User oldUser) {
		return !oldUser.equals(this.loggedInUser);
		
		
		
	}
	
}