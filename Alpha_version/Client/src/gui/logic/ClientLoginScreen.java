package gui.logic;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;
import java.util.HashMap;
import java.util.Map;
import common.InputCheck;
import javafx.event.Event;
import enums.UserType;
import logic.QRCodeDecoder;
import java.io.File;
import javafx.stage.FileChooser;

/**
 * This class represents the client login screen controller.
 */
public class ClientLoginScreen {

	// ****************************** FXML Variables *****************************

	@FXML
	private Button btnGuest;
	@FXML
	private Button btnSignIn;
	@FXML
	private Button btnScanQR;
	@FXML
	private Hyperlink lnkForgotMemberID;
	@FXML
	private Hyperlink lnkEmployee;
	@FXML
	private TextField txtMemberID;
	@FXML
	private TextField txtPhoneNumber;
	@FXML
	private TextField txtEmailAddress;	
	@FXML
	private BorderPane contentPane;
	@FXML
	private StackPane mainPane;	
	@FXML
	private StackPane modalOverlay; // Overlay pane for modals
	@FXML
	private Label lblError;	
	
	private Parent ForgotIDModalRoot;
	
	private Map<String, Object> userLoginData; // holds received user login data
	
	private ClientForgotIDScreen forgotModalsCTRL;
	
	
	// ****************************** FXML Methods *****************************

	/**
	 * Initializes the client login screen. Sets up input validation for the member ID text field.
	 */
	@FXML
	public void initialize() {
		txtMemberID.setTextFormatter(new TextFormatter<String>(change -> {
			String newText = change.getControlNewText();
			// allow empty so backspace/delete works
			if (newText.isEmpty())
				return change;
			// digits only, up to 6
			if (!InputCheck.isDigitsUpTo(newText, 6))
				return null;
			// enforce "no leading 0" while typing
			if (newText.length() >= 1 && newText.charAt(0) == '0')
				return null;
			return change;
		}));
	}
	
	/**
	 * Handles the guest login button click event. Validates the phone number and
	 * email address inputs. If valid, attempts to log in the guest user and switch
	 * to the client dashboard screen. Displays error messages for invalid inputs or
	 * login failures.
	 * 
	 * @param event The event triggered by clicking the guest login button.
	 */
	@FXML
	public void btnGuest(Event event) {
		String phoneNumber = txtPhoneNumber.getText().trim();
		String emailAddress = txtEmailAddress.getText().trim();
		if (phoneNumber.isEmpty())
			phoneNumber = null;
		if (emailAddress.isEmpty())
			emailAddress = null;
		String errorMessage = InputCheck.isValidGuestInfo(phoneNumber, emailAddress);
		UserType userType = UserType.GUEST;
		if (!errorMessage.equals("")) {
			BistroClientGUI.display(lblError, errorMessage.trim(), Color.RED);
		} else {
			userLoginData = new HashMap<>();
			userLoginData.put("userType", userType);
			userLoginData.put("phoneNumber", phoneNumber);
			userLoginData.put("email", emailAddress);
			TaskRunner.run(event, () -> {
				BistroClientGUI.client.getUserCTRL().signInUser(userLoginData);
			}, () -> {
				if (BistroClientGUI.client.getUserCTRL().isUserLoggedInAs(userType)) {
					BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error Message");
				} else {
					BistroClientGUI.display(lblError, "Error has occured!", Color.RED);
					return;
				}
			});
		}
	}

	/**
     * Handles the "Sign In" button click event.
     * Validates input, shows a loading screen, and attempts to log the user in via a background thread.
     *
     * @param event The event triggered by clicking the button (used for screen switching and locating the root pane).
     */
    @FXML
    public void btnSignIn(Event event) {
        String memberCodeText = txtMemberID.getText();
        UserType userType = UserType.MEMBER;
        //intput validation
        String err = InputCheck.validateMemberCode6DigitsNoLeadingZero(memberCodeText);
        if (!err.isEmpty()) {
            lblError.setText(err);
            return;
        }
        lblError.setText("");
        // Prepare login data
        userLoginData = new HashMap<>();
        userLoginData.put("userType", userType);
        userLoginData.put("memberCode", Integer.parseInt(memberCodeText));
		// Run login in background thread
        TaskRunner.run(event, 
            () -> {
                BistroClientGUI.client.getUserCTRL().signInUser(userLoginData);
            }, 
            () -> {
                if (BistroClientGUI.client.getUserCTRL().isUserLoggedInAs(userType)) {
                    BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client Dashboard Error");
                } else {
                    lblError.setText("Member code does not exist.");
                }
            }
        );
    }
    
    /**
	 * Handles the "Scan QR" button click event.
	 * Opens a file chooser to select an image file containing a QR code,
	 * decodes the QR code, and attempts to log in the user using the decoded member ID.
	 *
	 * @param event The event triggered by clicking the button (used for screen switching and locating the root pane).
	 */
    @FXML
    public void btnScanQR(Event event) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select QR Code Image");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            //open file dialog
            File file = chooser.showOpenDialog(btnScanQR.getScene().getWindow());
            if (file == null) {
                return; // user cancelled
            }
			// Decode QR code from selected file
            String decoded = QRCodeDecoder.decodeFromFile(file);
            if (decoded == null || decoded.isBlank()) {
                BistroClientGUI.display(lblError, "QR code could not be decoded.", Color.RED);
                return;
            }
            // Behave exactly like manual member login
            txtMemberID.setText(decoded.trim());
            btnSignIn(event);
        } catch (com.google.zxing.NotFoundException nf) {
            BistroClientGUI.display(lblError, "No QR code was found in the selected image.", Color.RED);
        } catch (Exception e) {
            e.printStackTrace();
            BistroClientGUI.display(lblError, "Failed to read QR code image.", Color.RED);
        }
    }

	/**
	 * Handles the forgot member ID hyperlink click event. Opens the forgot member
	 * ID modal dialog.
	 * 
	 * @param event The event triggered by clicking the forgot member ID hyperlink.
	 */
	@FXML
	public void lnkForgotMemberID(Event event) {
		if (ForgotIDModalRoot == null) {
			var url = getClass().getResource("/gui/fxml/clientForgotMemberIDModal.fxml");
			if (url == null) {
				BistroClientGUI.display(lblError, "FXML not found: /gui/fxml/clientForgotMemberIDModal.fxml",
						Color.RED);
				return;
			}
			FXMLLoader loader = new FXMLLoader(url);
			try {
				ForgotIDModalRoot = loader.load();
			} catch (Exception e) {
				e.printStackTrace();
				BistroClientGUI.display(lblError, "Unable to open Forgot Member ID screen.", Color.RED);
				return;
			}
			forgotModalsCTRL = loader.getController();
			forgotModalsCTRL.setParentCtrl(this);
			modalOverlay.getChildren().add(ForgotIDModalRoot);
		}
		mainPane.setEffect(new GaussianBlur(18));
		modalOverlay.setVisible(true);
		modalOverlay.setManaged(true);
	}

	/**
	 * Handles the employee login hyperlink click event. Switches to the employee
	 * login screen.
	 * 
	 * @param event The event triggered by clicking the employee login hyperlink.
	 */
	@FXML
	public void lnkEmployee(Event event) {
		BistroClientGUI.switchScreen(event, "staff/clientEmployeeLoginScreen", "Employee Login Error Message");
	}
	
	/**
	 * Closes the forgot member ID modal dialog.
	 */
	public void closeForgotIDScreen() {
		mainPane.setEffect(null);
		modalOverlay.setVisible(false);
		modalOverlay.setManaged(false);
	}
}
//End of ClientLoginScreen.java