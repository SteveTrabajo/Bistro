package logic;

import java.util.ArrayList;


import entities.User;
import massages.*;

/*
 * This class represents the controller for user-related operations in the BistroClient.
 */
public class User_Controller {
	
	//****************************** Instance variables ******************************
	
	private final BistroClient client; //final refrence to the BistroClient to ensure only one instance is associated
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
	public User getLoggedInUser() {
		return loggedInUser;
	}
	
	public void setLoggedInUser(User user) {
		this.loggedInUser = user;
	}
	//******************************** Instance Methods ***********************************
	
	/*
	 * Method to sign in a user with the provided login data.
	 * 
	 * @param userLoginData An ArrayList containing user login information.
	 */
	public void signInUser(ArrayList<String> userLoginData,String massageType) {
		Message req = new Message(massageType,(Object) userLoginData);
		client.handleMessageFromClientUI(req);
	}
}
