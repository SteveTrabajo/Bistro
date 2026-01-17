package gui.logic.staff;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import logic.BistroClientGUI;

public class FullReleaseController {

    @FXML
    private Button btnBack;    
    @FXML
    private Button btnOk;

    /**
     * Navigates back to the Client Employee Login Screen.
     * This method is used by both the "Back" arrow and the "Got it" button.
     */
    @FXML
    void btnBack(ActionEvent event) {
        BistroClientGUI.switchScreen(event, "clientLoginScreen", "Back to login");
    }
    
    /**
	 * Navigates back to the Client Employee Login Screen.
	 * This method is used by both the "Back" arrow and the "Got it" button.
	 */
    @FXML
    void btnOk(ActionEvent event) {
		BistroClientGUI.switchScreen(event, "clientLoginScreen", "Back to login");
	}
}
// end of FullReleaseController.java