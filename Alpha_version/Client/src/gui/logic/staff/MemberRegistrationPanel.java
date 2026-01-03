package gui.logic.staff;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
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
    private Label lblTotalMembers;
    @FXML 
    private Label lblThisMonth;
    @FXML 
    private Label lblThisWeek;
    @FXML
    private Label lblError;

    private int totalMembers = 0;
    private int monthCount = 0;
    private int weekCount = 0;

    private LocalDate statsDateAnchor = LocalDate.now(); // used to reset week/month counters when time changes
    
    //****************************** FXML Methods ******************************
    
    public void initialize() {
    	BistroClientGUI.client.getUserCTRL().requestMemberRegistrationStats();
		ArrayList<Integer> stats = BistroClientGUI.client.getUserCTRL().getMemberRegistrationStats();
		refreshStatsLabels(stats);
	}
    
    @FXML
    public void btnRegister(Event event) {
        String firstName = txtfirstName.getText().trim();
        String lastName = txtlastName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String address = txtAddress.getText().trim();
        String errorMassage = "";
        errorMassage =InputCheck.validateFirstName(firstName);
        errorMassage +=InputCheck.validateLastName(lastName);
        errorMassage +=InputCheck.validateEmail(email);
        errorMassage +=InputCheck.validatePhoneNumber(phone);
        errorMassage +=InputCheck.validateAddress(address);
        if (!errorMassage.isEmpty()) {
			ArrayList<String> newMemberData = new ArrayList<String>();
			newMemberData.add(firstName);
			newMemberData.add(lastName);
			newMemberData.add(email);
			newMemberData.add(phone);
			newMemberData.add(address);
			BistroClientGUI.client.getUserCTRL().RegisterNewMember(newMemberData);
			clearForm();
		}else {
        	showError("Invalid Input", errorMassage);
        }
    }
    
    public void registerSuccess(List<Integer> updatedStats) {
    	showInfo("Registration Successful", "New member has been registered successfully.");
		refreshStatsLabels(updatedStats);
	}

    public void clearForm() {
    	txtfirstName.clear();
    	txtlastName.clear();
    	txtEmail.clear();
    	txtPhone.clear();
    	txtAddress.clear();
    }

    public void refreshStatsLabels(List<Integer> updatedStats) {
    	this.totalMembers = updatedStats.get(0);
		this.monthCount = updatedStats.get(1);
		this.weekCount = updatedStats.get(2);
    	lblTotalMembers.setText(String.valueOf(totalMembers));
    	lblThisMonth.setText(String.valueOf(monthCount));
    	lblThisWeek.setText(String.valueOf(weekCount));
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

