package gui.logic.staff;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import common.InputCheck;

public class MemberRegistrationPanel{

    //****************************** FXML Variables ******************************
    @FXML 
    private TextField txtfirstName;
    
    @FXML
    private TextField txtlastName;
    
    @FXML
    private TextField txtEmail;
    
    @FXML 
    private TextField txtPhone;
    
    @FXML
    private TextField txtAddress;

    @FXML
    private Button btnRegister;

    //****************************** FXML Methods ******************************
    
    @FXML
    public void btnRegister(Event event) {
        String firstName = txtfirstName.getText().trim();
        String lastName = txtlastName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String address = txtAddress.getText().trim();
        String errorMessage = InputCheck.validatePersonalDetails(firstName, lastName, phone, email, address);
		if (!errorMessage.isEmpty()) {
			showError("Invalid Input", errorMessage);
			return;
		}
		
		else{
			ArrayList<String> newMemberData = new ArrayList<String>();
			newMemberData.add(firstName);
			newMemberData.add(lastName);
			newMemberData.add(email);
			newMemberData.add(phone);
			newMemberData.add(address);
			BistroClientGUI.client.getUserCTRL().RegisterNewMember(newMemberData);
			if(BistroClientGUI.client.getUserCTRL().getRegistrationSuccessFlag()) {
			    showInfo("Registration Successful", "New member has been registered successfully.");
			    clearForm();
			}
        }
    }
    
    public void clearForm() {
    	txtfirstName.clear();
    	txtlastName.clear();
    	txtEmail.clear();
    	txtPhone.clear();
    	txtAddress.clear();
    }
    
    public void showError(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    public void showInfo(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

}

