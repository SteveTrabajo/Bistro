package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import comms.*;
import entities.User;
import entities.UserData;
import enums.UserType;
import gui.logic.staff.CustomersPanel;
import javafx.application.Platform;

/*
 * This class represents the controller for user-related operations in the BistroClient.
 */
public class UserController {

	// ****************************** Instance variables
	// ******************************

	private final BistroClient client; // final reference to the BistroClient to ensure only one instance is associated
	private User loggedInUser;
	private ArrayList<Integer> memberRegistrationStats;
	private boolean registrationSuccessFlag = false;
	// ******************************** Constructors
	// ***********************************
	private List<UserData> customersData = new ArrayList<UserData>();
	private boolean userUpdateSuccessFlag = false;
	private Consumer<String> onMemberIDFoundListener;

	/*
	 * Constructor to initialize the User_Controller with a reference to the
	 * BistroClient.
	 * 
	 * @param client The BistroClient instance for server communication.
	 */
	public UserController(BistroClient client) {
		this.client = client;
	}

	// ******************************** Getters And Setters
	// ***********************************

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

	public List<UserData> getCustomersData() {
		return customersData;
	}

	public void clearCustomersData() {
		this.customersData.clear();
	}

	/**
	 * Method to retrieve member registration statistics.
	 * 
	 * @return An ArrayList containing member registration statistics.
	 */
	public ArrayList<Integer> getMemberRegistrationStats() {

		return memberRegistrationStats;
	}

	/**
	 * Method to set member registration statistics.
	 * 
	 * @param stats An ArrayList containing member registration statistics.
	 */
	public void setMemberRegistrationStats(ArrayList<Integer> stats) {
		this.memberRegistrationStats = stats;
	}

	public boolean getRegistrationSuccessFlag() {
		return registrationSuccessFlag;
	}

	public void setRegistrationSuccessFlag(boolean registrationSuccessFlag) {
		this.registrationSuccessFlag = registrationSuccessFlag;
	}
	// ******************************** Instance Methods
	// ***********************************

	/**
	 * Method to sign in a user with the provided login data.
	 * 
	 * @param userLoginData An ArrayList containing user login information.
	 */
	public void signInUser(String userLoginData, UserType userType) {
		switch (userType) {
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

	/*
	 * Method to sign out the currently logged-in user.
	 */
	public void signOutUser() {
		this.loggedInUser = null;
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

	/**
	 * Method to update the details of the currently logged-in user.
	 * 
	 * @param updatedUser The User object containing the updated user details.
	 */
	public void updateUserDetails(UserData updatedUser) {
		client.handleMessageFromClientUI(new Message(Api.ASK_MEMBER_UPDATE_INFO, updatedUser));
	}

	public void RegisterNewMember(Object newMemberData) {
		this.setRegistrationSuccessFlag(false);
		client.handleMessageFromClientUI(new Message(Api.ASK_REGISTER_NEW_MEMBER, newMemberData));
	}

	/**
	 * Method to check if the user update was successful by comparing the old user
	 * details with the current logged-in user details.
	 * 
	 * @param oldUser
	 * @return
	 */
	public boolean isUpdateSuccessful(User oldUser) {
		return !oldUser.equals(this.loggedInUser);
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

	public void requestMemberRegistrationStats() {
		client.handleMessageFromClientUI(new Message(Api.ASK_REGISTERATION_STATS, null));
	}

	public boolean isEmployeeLoginSuccess() {
		if (this.loggedInUser != null && (this.loggedInUser.getUserType() == UserType.EMPLOYEE)) {
			return true;
		}

		return false;
	}

	public boolean isManagerLoginSuccess() {
		if (this.loggedInUser != null && (this.loggedInUser.getUserType() == UserType.MANAGER)) {
			return true;
		}

		return false;
	}

	public void staffLogin(String username, String password) {
		String userLoginData = username + "_" + password;
		client.handleMessageFromClientUI(new Message(Api.ASK_LOGIN_EMPLOYEE, userLoginData));
		if (isEmployeeLoginSuccess()) {
			return;
		} else {
			client.handleMessageFromClientUI(new Message(Api.ASK_LOGIN_MANAGER, userLoginData));
		}

	}

	public void loadCustomersData() {
		client.handleMessageFromClientUI(new Message(Api.ASK_LOAD_CUSTOMERS_DATA, null));

	}

	public boolean isCustomersDataLoaded() {
		return !customersData.isEmpty();
	}

	public void setCustomersData(List<UserData> customersDataNew) {
		this.customersData = customersDataNew;

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

	public void createNewEmployee(String username, String password, String email, String phoneNumber,
			UserType userType) {
		Map<String, Object> staffData = new HashMap<>();
		staffData.put("username", username);
		staffData.put("password", password);
		staffData.put("email", email);
		staffData.put("phoneNumber", phoneNumber);
		staffData.put("userType", UserType.EMPLOYEE);

		client.handleMessageFromClientUI(new Message(Api.ASK_STAFF_CREATE, staffData));
	}

	/**
	 * Flag to track if staff creation was successful
	 */
	private boolean staffCreationSuccessFlag = false;
	private String staffCreationErrorMessage = null;

	/**
	 * Set the staff creation success flag
	 */
	public void setStaffCreationSuccess(boolean success) {
		this.staffCreationSuccessFlag = success;
	}

	/**
	 * Get the staff creation success flag
	 */
	public boolean isStaffCreationSuccess() {
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

	/**
	 * Clear staff creation status for next operation
	 */
	public void clearStaffCreationStatus() {
		this.staffCreationSuccessFlag = false;
		this.staffCreationErrorMessage = null;
	}

	public boolean isUserUpdateSuccessful() {
		return userUpdateSuccessFlag;
	}

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