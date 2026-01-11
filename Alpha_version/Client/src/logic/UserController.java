package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import comms.*;
import dto.UserData;
import entities.User;
import enums.UserType;
import javafx.application.Platform;

/*
 * This class represents the controller for user-related operations in the BistroClient.
 */
public class UserController {

	// ****************************** Instance variables ******************************

	private final BistroClient client; // final reference to the BistroClient to ensure only one instance is associated
	private User loggedInUser;
	
	
	
	// User registration related variables:
	private Consumer<String> onMemberIDFoundListener; // Listener for forgot member ID responses
	private boolean registrationSuccessFlag = false;
	private boolean userUpdateSuccessFlag = false;
	private int newMemberID; //to store newly registered member ID
	
	//staff pages related variables:
	private List<UserData> customersData = new ArrayList<UserData>(); //to store customers data for manager/employee view
	private ArrayList<Integer> memberRegistrationStats; //for member registration statistics
	private boolean staffCreationSuccessFlag = false;
	private String staffCreationErrorMessage = null;
	
	// ******************************** Constructors ***********************************
	/*
	 * Constructor to initialize the User_Controller with a reference to the
	 * BistroClient.
	 * 
	 * @param client The BistroClient instance for server communication.
	 */
	public UserController(BistroClient client) {
		this.client = client;
	}

	// ******************************** Getters And Setters ***********************************

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

	/**
	 * Method to retrieve member registration statistics.
	 * 
	 * @return An ArrayList containing member registration statistics.
	 */
	public ArrayList<Integer> getMemberRegistrationStats() {
		return this.memberRegistrationStats;
	}

	/**
	 * Method to set member registration statistics.
	 * 
	 * @param stats An ArrayList containing member registration statistics.
	 */
	public void setMemberRegistrationStats(ArrayList<Integer> stats) {
		this.memberRegistrationStats = stats;
	}
	
	/**
	 * Method to get the registration success flag.
	 * 
	 * @return A boolean indicating if registration was successful.
	 */
	public boolean getRegistrationSuccessFlag() {
		return this.registrationSuccessFlag;
	}
	
	/**
	 * Method to set the registration success flag.
	 * 
	 * @param registrationSuccessFlag A boolean indicating if registration was successful.
	 */
	public void setRegistrationSuccessFlag(boolean registrationSuccessFlag) {
		this.registrationSuccessFlag = registrationSuccessFlag;
	}
	
	/**
	 * Method to set the user update success flag.
	 * 
	 * @param success A boolean indicating if the user update was successful.
	 */
	public void setUserUpdateSuccessFlag(boolean success) {
		this.userUpdateSuccessFlag = success;
	}
	
	/**
	 * Method to get the user update success flag.
	 * 
	 * @return A boolean indicating if the user update was successful.
	 */
	public boolean getUserUpdateSuccessful() {
		return this.userUpdateSuccessFlag;
	}

	/**
	 * Method to get customers data retrieved from the server.
	 * 
	 * @return A list of UserData objects representing customers data.
	 */
	public List<UserData> getCustomersData() {
		return customersData;
	}
	
	/**
	 * Method to set customers data retrieved from the server.
	 * 
	 * @param customersDataNew A list of UserData objects representing customers data.
	 */
	public void setCustomersData(List<UserData> customersDataNew) {
		this.customersData = customersDataNew;
	}
	
	/**
	 * Set the staff creation success flag
	 */
	public void setStaffCreationSuccess(boolean success) {
		this.staffCreationSuccessFlag = success;
	}
	
	/**
	 * Get the staff creation success flag
	 */
	public boolean getStaffCreationSuccess() {
		return this.staffCreationSuccessFlag;
	}
	
	/**
	 * Set the staff creation error message (if creation failed)
	 */
	public void setStaffCreationErrorMessage(String message) {
		this.staffCreationErrorMessage = message;
	}

	/**
	 * Get the staff creation error message
	 */
	public String getStaffCreationErrorMessage() {
		return this.staffCreationErrorMessage;
	}
	
	
	public int getNewMemberID() {
		return newMemberID;
	}
	
	public void setNewMemberID(int newMemberID) {
		this.newMemberID = newMemberID;
	}
	
	// ******************************** Instance Methods ***********************************
	
	
	//******************************** Login/Logout Methods ********************************
	/**
	 * Method to sign in a user with the provided login data.
	 * 
	 * @param userLoginData An ArrayList containing user login information.
	 */
	public void signInUser(Map<String, Object> userLoginData) {
		UserType userType = ((UserType) userLoginData.get("userType"));
		switch (userType) {
		case GUEST:
			client.handleMessageFromClientUI(new Message(Api.ASK_LOGIN_GUEST, userLoginData));
			break;
		case MEMBER:
			client.handleMessageFromClientUI(new Message(Api.ASK_LOGIN_MEMBER, userLoginData));
			break;
		case EMPLOYEE, MANAGER:
			// Unified staff login. Server determines whether the account is EMPLOYEE or
			// MANAGER.
			client.handleMessageFromClientUI(new Message(Api.ASK_LOGIN_STAFF, userLoginData));
			break;
		default:
			System.out.println("Unknown user type");
		}
	}

	/*
	 * Method to sign out the currently logged-in user.
	 */
	public void signOutUser() {
		if (this.loggedInUser == null) {
			return; // Already signed out
		}
		String apiCommand;
		// Determine the correct API constant based on the user's role
		switch (this.loggedInUser.getUserType()) {
		case GUEST:
			apiCommand = Api.ASK_SIGNOUT_GUEST;
			break;
		case MEMBER:
			apiCommand = Api.ASK_SIGNOUT_MEMBER;
			break;
		case EMPLOYEE:
			apiCommand = Api.ASK_SIGNOUT_EMPLOYEE;
			break;
		case MANAGER:
			apiCommand = Api.ASK_SIGNOUT_MANAGER;
			break;
		default:
			return; // Unknown role
		}
		// Send the message to the server
		client.handleMessageFromClientUI(new Message(apiCommand, null));
	}

	/**
	 * Method to handle forgotten member ID requests.
	 * 
	 * @param email       The email address associated with the member account.
	 * @param phoneNumber The phone number associated with the member account.
	 */
	public void forgotMemberID(String email, String phoneNumber) {
		Map<String, String> userContactInfo = new HashMap<>();
		userContactInfo.put("email", email);
		userContactInfo.put("phoneNumber", phoneNumber);
		client.handleMessageFromClientUI(new Message(Api.ASK_FORGOT_MEMBER_ID, userContactInfo));
	}
	
	
	/*
	 * Method to check if a user is currently logged in.
	 * 
	 * @return true if a user is logged in, false otherwise.
	 */
	public boolean isUserLoggedInAs(UserType expectedType) {
		return this.loggedInUser != null && this.loggedInUser.getUserType() == expectedType;
	}

	/**
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
	
	//******************************** User Management Methods ***********************************

	/**
	 * Method to update the details of the currently logged-in user.
	 * 
	 * @param updatedUser The User object containing the updated user details.
	 */
	public void updateUserDetails(UserData updatedUser) {
		client.handleMessageFromClientUI(new Message(Api.ASK_MEMBER_UPDATE_INFO, updatedUser));
	}
	
	
	/**
	 * Method to register a new member with the provided data.
	 * 
	 * @param newMemberData An ArrayList containing new member registration data.
	 */
	public void RegisterNewMember(ArrayList<String> newMemberData) {
		this.setRegistrationSuccessFlag(false);
		client.handleMessageFromClientUI(new Message(Api.ASK_REGISTER_NEW_MEMBER, newMemberData));
	}

	//******************************** Staff/Manager Methods ***********************************
	/**
	 * Method to request member registration statistics from the server.
	 */
	public void requestMemberRegistrationStats() {
		client.handleMessageFromClientUI(new Message(Api.ASK_REGISTERATION_STATS, null));
	}

	/**
	 * Method to load customers data from the server for staff/manager view.
	 */	
	public void loadCustomersData() {
		client.handleMessageFromClientUI(new Message(Api.ASK_LOAD_CUSTOMERS_DATA, null));
	}
	
	/**
	 * Method to check if customers data has been loaded.
	 * 
	 * @return true if customers data is loaded, false otherwise.
	 */
	public boolean isCustomersDataLoaded() {
		return !customersData.isEmpty();
	}
	
	/**
	 * Method to clear customers data.
	 */
	public void clearCustomersData() {
		this.customersData.clear();
	}

	/**
	 * Method to create a new employee account. Called by manager from the add
	 * employee form.
	 * 
	 * @param username    The new staff username (3-20 chars)
	 * @param password    The new staff password (min 4 chars)
	 * @param email       The new staff email address
	 * @param phoneNumber The new staff phone number (9-15 digits)
	 * @param userType    The role: EMPLOYEE
	 */
	public void createNewEmployee(String username, String password, String email, String phoneNumber) {
		Map<String, Object> staffData = new HashMap<>();
		staffData.put("username", username);
		staffData.put("password", password);
		staffData.put("email", email);
		staffData.put("phoneNumber", phoneNumber);
		staffData.put("userType", UserType.EMPLOYEE.name());
		client.handleMessageFromClientUI(new Message(Api.ASK_STAFF_CREATE, staffData));
	}

	/**
	 * Clear staff creation status for next operation
	 */
	public void clearStaffCreationStatus() {
		this.staffCreationSuccessFlag = false;
		this.staffCreationErrorMessage = null;
	}

	//******************************** Event Listeners Methods ***********************************
	public void setOnMemberIDFoundListener(Consumer<String> listener) {
		this.onMemberIDFoundListener = listener;
	}

	public void handleForgotIDResponse(String result) {
		if (onMemberIDFoundListener != null) {
			Platform.runLater(() -> {
				onMemberIDFoundListener.accept(result);
				// Clear after use to prevent memory leaks
				onMemberIDFoundListener = null;
			});
		}
	}
}
// End of UserController.java