package logic.api.subjects;

import java.util.ArrayList;
import java.util.List;

import dto.UserData;
import entities.User;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.UserController;
import logic.api.ClientRouter;
import enums.UserType;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public class ClientUserSubject {

	public static void register(ClientRouter router, UserController userController) {
		
		// Login-Signout Customers responses:
		for (UserType type : List.of(UserType.GUEST, UserType.MEMBER, UserType.EMPLOYEE, UserType.MANAGER)) {
			String typeKey = type.name().toLowerCase();
			
			router.on("login", typeKey + ".ok", msg -> {
				BistroClient.awaitResponse = false;
				User user = (User) msg.getData();
				userController.setLoggedInUser(user);
			});

			router.on("signout", typeKey + ".ok", msg -> {
				BistroClient.awaitResponse = false;
				userController.setLoggedInUser(null);
			});

			router.on("login", typeKey + ".notFound", msg -> {
				BistroClient.awaitResponse = false;
				Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Login Failed");
				alert.setHeaderText("User Not Found");
				alert.setContentText("The username not found. Please check your username and try again.");
				alert.showAndWait();
				});
			});

			router.on("signout", typeKey + ".fail", msg -> {
				BistroClient.awaitResponse = false;
			});
		}
		// Unified staff login responses:
		router.on("login", "staff.ok", msg -> {
			BistroClient.awaitResponse = false;
			User user = (User) msg.getData();
			if(user!=null && (user.getUserType() == UserType.EMPLOYEE || user.getUserType() == UserType.MANAGER)) {
				userController.setLoggedInUser(user);
			}else {
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Login Failed");
					alert.setHeaderText("Invalid User Type");
					alert.setContentText("The logged in user is not a staff member.");
					alert.showAndWait();
				});
			}
		});
		
		router.on("login", "staff.invalidCredentials", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Login Failed");
				alert.setHeaderText("Invalid Credentials");
				alert.setContentText("The username or password is incorrect.");
				alert.showAndWait();
			});
		});
		
		router.on("login", "staff.accountLocked", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Account Locked");
				alert.setHeaderText("Too Many Failed Login Attempts");
				alert.setContentText(
						"Your account has been locked due to multiple failed login attempts. Please wait and try again later.");
				alert.showAndWait();
			});
		});
		
		router.on("user", "forgotMemberID.ok", msg -> {
			BistroClient.awaitResponse = false;
			String memberID = (String) msg.getData();
			// Execute UI changes on the JavaFX thread
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Member Recovery");
				alert.setHeaderText("ID Retrieval Successful");
				Label label = new Label("Your Member ID is: " + memberID);
				label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
				alert.getDialogPane().setContent(label);
				alert.showAndWait();
				userController.handleForgotIDResponse(memberID);
				BistroClientGUI.switchScreen("clientLoginScreen",
						"Failed to load Login Screen after retrieving Member ID.");
			});
		});
		
		router.on("user", "forgotMemberID.fail", msg -> {
			BistroClient.awaitResponse = false;
			userController.handleForgotIDResponse("NOT_FOUND");
		});
		
		// Member info update responses:
		router.on("member", "updateInfo.ok", msg -> {
			BistroClient.awaitResponse = false;
			UserData updatedUser = (UserData) msg.getData();
			User currentUser = userController.getLoggedInUser();
			currentUser.setFirstName(updatedUser.getFirstName());
			currentUser.setLastName(updatedUser.getLastName());
			currentUser.setAddress(updatedUser.getAddress());
			currentUser.setEmail(updatedUser.getEmail());
			currentUser.setPhoneNumber(updatedUser.getPhone());
			currentUser.setMemberCode(updatedUser.getMemberCode());
			userController.setUserUpdateSuccessFlag(true);
		});
		
		router.on("member", "updateInfo.fail", msg -> {
			BistroClient.awaitResponse = false;
			userController.setUserUpdateSuccessFlag(false);
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Update Failed");
			alert.setHeaderText("Failed to Update User Information");
			alert.setContentText("An error occurred while updating your information. Please try again later.");
			alert.showAndWait();
		});
		
		// Member registration responses:
		router.on("user", "registerNewMember.ok", msg -> {
		    BistroClient.awaitResponse = false;
		    int newMemberCode = (int) msg.getData();

		    Platform.runLater(() -> {
		        userController.handleRegisterNewMemberOk(newMemberCode);
		    });
		});

		router.on("user", "registerNewMember.failed", msg -> {
		    BistroClient.awaitResponse = false;

		    // Server now sends a String reason (can be null)
		    String reason = (msg.getData() instanceof String) ? (String) msg.getData() : null;
		    if (reason == null || reason.isBlank()) {
		        reason = "Member registration failed. Email/phone may already exist.";
		    }

		    final String finalReason = reason;
		    Platform.runLater(() -> {
		        userController.handleRegisterNewMemberFail(finalReason);
		    });
		});

		
		router.on("member", "registerationStats.ok", msg -> {
			BistroClient.awaitResponse = false;
			ArrayList<Integer> count = (ArrayList<Integer>) msg.getData();
			BistroClientGUI.client.getUserCTRL().setMemberRegistrationStats(count);
		});
		
		router.on("member", "registerationStats.fail", msg -> {
			BistroClient.awaitResponse = false;
		});
		
		// Get all customers data responses:
		router.on("customers", "getalldata.ok", msg -> {
			BistroClient.awaitResponse = false;
			List<UserData> customersData = (List<UserData>) msg.getData();
			BistroClientGUI.client.getUserCTRL().setCustomersData(customersData);
		});

		router.on("customers", "getalldata.fail", msg -> {
			BistroClient.awaitResponse = false;
			BistroClientGUI.client.getUserCTRL().setCustomersData(new ArrayList<>());
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Failed to Retrieve Customer Data");
			alert.setContentText("An error occurred while retrieving customer data. Please try again later.");
			alert.showAndWait();
		});

		// Staff creation responses:
		router.on("staff", "create.ok", msg -> {
			BistroClient.awaitResponse = false;
			User newEmployee = (User) msg.getData();
			BistroClientGUI.client.getUserCTRL().setStaffCreationSuccess(true);
			BistroClientGUI.client.getUserCTRL().setStaffCreationErrorMessage(null);
		});
		
		router.on("staff", "create.invalidData", msg -> {
			BistroClient.awaitResponse = false;
			BistroClientGUI.client.getUserCTRL().setStaffCreationSuccess(false);
			BistroClientGUI.client.getUserCTRL()
					.setStaffCreationErrorMessage("Invalid staff data provided. Please check all fields.");
		});

		router.on("staff", "create.usernameExists", msg -> {
			BistroClient.awaitResponse = false;
			BistroClientGUI.client.getUserCTRL().setStaffCreationSuccess(false);
			BistroClientGUI.client.getUserCTRL()
					.setStaffCreationErrorMessage("Username already exists. Please choose a different username.");
		});

		router.on("staff", "create.failed", msg -> {
			BistroClient.awaitResponse = false;
			BistroClientGUI.client.getUserCTRL().setStaffCreationSuccess(false);
			BistroClientGUI.client.getUserCTRL()
					.setStaffCreationErrorMessage("Failed to create staff account. Please try again.");
		});
		
		
	}
}
