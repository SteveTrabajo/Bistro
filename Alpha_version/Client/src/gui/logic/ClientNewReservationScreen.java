package gui.logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dto.WeeklyHour;
import entities.User;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import logic.BistroClientGUI;

/**
 * Controller class for the Client New Reservation Screen.
 * Handles the logic for checking availability and creating bookings.
 */
public class ClientNewReservationScreen {
    
    //*********************** FXML Variables ************************//
    
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> dinersAmountComboBox;
    @FXML
    private GridPane timeSlotsGridPane;
    @FXML
    private Button btnConfirmReservation;
    @FXML
    private Button btnBack;
    @FXML
    private Label lblUser;
    @FXML
    private Label lblSunday;
    @FXML
    private Label lblMonday;
    @FXML
    private Label lblTuesday;
    @FXML
    private Label lblWednesday;
    @FXML
    private Label lblThursday;
    @FXML
    private Label lblFriday;
    @FXML
    private Label lblSaturday;
    
    private String selectedTimeSlot = null;
    private Map<String, Object> staffProxyData = null;
    private List<LocalDate> serverAllowedDates = new ArrayList<>();
    
    //*********************** Initialization ************************//
    
    @FXML
    public void initialize() {
    	Locale.setDefault(Locale.ENGLISH);
        setupDinersAmountComboBox();
        setupDatePicker();
          
        // Initial state: Date selection is disabled until diners are chosen
        datePicker.setDisable(true); 
        btnConfirmReservation.setDisable(true);

        // Set user label based on login status
        if (BistroClientGUI.client != null && BistroClientGUI.client.getUserCTRL().getLoggedInUser() != null) {
            User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
            if (lblUser != null) {
                lblUser.setText( currentUser.getUserType().name());
            }
        }
        
        // Listener: When diner count changes, fetch available dates
        dinersAmountComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                fetchAvailableDatesFromServer();
            }
        });

        // Listener: When date changes, fetch available time slots
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                refreshTimeSlots();
            }
        });
        
        askUpdateDayLabels();
    }
    

	//*********************** Logic Methods ************************//

    private void askUpdateDayLabels() {
		BistroClientGUI.client.getReservationCTRL().askWeeklyHours();
		Platform.runLater(() -> {
			List<WeeklyHour> weeklyHours = BistroClientGUI.client.getReservationCTRL().getWeeklyHours();
			updateLabelsWithData(weeklyHours);
		});
    }
    
    public void updateLabelsWithData(List<WeeklyHour> data) {
		if (data != null && !data.isEmpty()) {
			for (WeeklyHour wh : data) {
				int day = wh.getDayOfWeek();
				String hours = wh.getOpenTime() + " - " + wh.getCloseTime();
				switch (day) {
				case 1:
					lblSunday.setText("Sunday: " + hours);
					break;
				case 2:
					lblMonday.setText("Monday: " + hours);
					break;
				case 3:
					lblTuesday.setText("Tuesday: " + hours);
					break;
				case 4:
					lblWednesday.setText("Wednesday: " + hours);
					break;
				case 5:
					lblThursday.setText("Thursday: " + hours);
					break;
				case 6:
					lblFriday.setText("Friday: " + hours);
					break;
				case 7:
					lblSaturday.setText("Saturday: " + hours);
					break;
				}
			}
		}
		else {
			lblSunday.setText("Restaurant Hours Unavailable");
		}
    }
    
    
    
    /**
	 * Fetches available reservation dates from the server based on selected number of diners.
	 * Updates the DatePicker to enable only those dates.
	 */
    
    private void fetchAvailableDatesFromServer() {
        int diners = parseDiners(dinersAmountComboBox.getValue());

        // Reset UI state before request
        datePicker.setValue(null);
        datePicker.setDisable(true);
        timeSlotsGridPane.getChildren().clear();
        selectedTimeSlot = null;
        btnConfirmReservation.setDisable(true);

        // Register callback for server response
        BistroClientGUI.client.getReservationCTRL().setDatesUpdateListener((dates) -> {
            Platform.runLater(() -> {
                this.serverAllowedDates = dates; 
                // Refresh date picker cells to apply new restrictions
                setupDatePicker(); 
                datePicker.setDisable(false); 
                datePicker.show(); 
            });
        });

        // Send request
        BistroClientGUI.client.getReservationCTRL().askAvailableDates(diners);
    }
    /**
	 * Sets the booking context for a customer when accessed by staff.
	 * Updates the user label accordingly.
	 * 
	 * @param customerData Map containing customer details like name and identifier.
	 */
    public void setBookingForCustomer(Map<String, Object> customerData) {
        this.staffProxyData = customerData;
        
        String name = (String) customerData.get("name");
        String id = (String) customerData.get("identifier");
        
        if (lblUser != null) {
            if (name != null && !name.isEmpty()) {
                lblUser.setText("Booking for: " + name);
            } else {
                lblUser.setText("Booking for Member: " + id);
            }
        }
    }
    
    private void refreshTimeSlots() {
        if (timeSlotsGridPane == null) return;
        
        LocalDate date = datePicker.getValue();
        if (date == null) {
            timeSlotsGridPane.getChildren().clear();
            return;
        }

        int diners = parseDiners(dinersAmountComboBox.getValue());

        timeSlotsGridPane.getChildren().clear(); 
        selectedTimeSlot = null;
        btnConfirmReservation.setDisable(true);

        BistroClientGUI.client.getReservationCTRL().setUIUpdateListener((availableSlots) -> {
            Platform.runLater(() -> generateTimeSlots(availableSlots));
        });
        
        BistroClientGUI.client.getReservationCTRL().askAvailableHours(date, diners);
    }

    private int parseDiners(String value) {
        if (value != null && value.contains(" ")) {
            String numberPart = value.split(" ")[0];
            try {
                return Integer.parseInt(numberPart);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return 2; 
    }
    //*********************** UI Setup Methods ************************//
    // Enhanced DatePicker setup with advanced styling and logic
    private void setupDatePicker() {
        // 1. Custom Cell Factory for DatePicker
        final String BASE_STYLE = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; " +
                                  "-fx-background-insets: 0; -fx-border-width: 0; " +
                                  "-fx-font-weight: bold; -fx-font-size: 16px; " + 
                                  "-fx-alignment: CENTER;";

        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                // 2. Collapse Unavailable Cells
                boolean isOtherMonth = getStyleClass().contains("next-month") || getStyleClass().contains("previous-month");
                if (empty || date == null || isOtherMonth) {
                    collapseCell();
                    return;
                }

                setOpacity(1.0);
                setManaged(true);
                setVisible(true);
                setText(String.valueOf(date.getDayOfMonth()));
                
                // set fixed size for uniformity
                setPrefWidth(45);
                setPrefHeight(45);

                boolean isPast = date.isBefore(LocalDate.now());
                boolean isNotAllowed = (serverAllowedDates != null && !serverAllowedDates.contains(date));
                boolean isToday = date.equals(LocalDate.now());

                if (isPast || isNotAllowed) {
                    setDisable(true);
                    setStyle(BASE_STYLE + "-fx-background-color: #f0f0f0; -fx-text-fill: #bebebe;");
                } else {
                    updateCellStyle(date, isToday, BASE_STYLE);

                    // Hover Effects
                    setOnMouseEntered(e -> {
                        if (!date.equals(datePicker.getValue())) {
                            setStyle(BASE_STYLE + "-fx-background-color: #bbdefb; -fx-text-fill: #0d47a1; -fx-cursor: hand;");
                        }
                    });

                    setOnMouseExited(e -> updateCellStyle(date, isToday, BASE_STYLE));
                }
            }

            private void collapseCell() {
                setText(null);
                setGraphic(null);
                setManaged(false);
                setVisible(false);
                setStyle("-fx-background-color: transparent;");
            }

            private void updateCellStyle(LocalDate date, boolean isToday, String base) {
                if (date.equals(datePicker.getValue())) {
                    // Blue background with White text for selected date
                    setStyle(base + "-fx-background-color: #007bff; -fx-text-fill: white; -fx-background-radius: 5;");
                } else if (isToday) {
                    // Yellow background with Dark Yellow text for today
                    setStyle(base + "-fx-background-color: #fff9c4; -fx-text-fill: #f57f17; -fx-border-color: #f57f17; -fx-border-width: 2; -fx-background-radius: 5;");
                } else {
                    // Default style for normal selectable dates
                    setStyle(base + "-fx-background-color: white; -fx-text-fill: #212121; -fx-border-color: #e0e0e0; -fx-border-width: 0.5;");
                }
            }
        });
    }
    private void setupDinersAmountComboBox() {
        for (int i = 1; i <= 12; i++) {
            dinersAmountComboBox.getItems().add(i + " People");
        }
    }
    
    private void generateTimeSlots(List<String> availableTimeSlots) {
        if (timeSlotsGridPane == null) return;
        
        timeSlotsGridPane.getChildren().clear(); 
        
        if (availableTimeSlots == null || availableTimeSlots.isEmpty()) {
            Label lblNoSlots = new Label("No time slots available.");
            lblNoSlots.setStyle("-fx-text-fill: blue; -fx-font-size: 14px;");
            timeSlotsGridPane.add(lblNoSlots, 0, 0);
            GridPane.setColumnSpan(lblNoSlots, 4);
            return;
        }
        
        ToggleGroup timeSlotToggleGroup = new ToggleGroup();
        int col = 0;
        int row = 0;
        
        for (String timeSlot : availableTimeSlots) {
            ToggleButton timeSlotButton = new ToggleButton(timeSlot);
            timeSlotButton.setToggleGroup(timeSlotToggleGroup);
            timeSlotButton.setPrefWidth(104);
            timeSlotButton.setPrefHeight(37);
            timeSlotButton.getStyleClass().add("time-slot");
            
            timeSlotButton.setOnAction(event -> {
                if (timeSlotButton.isSelected()) {
                    selectedTimeSlot = timeSlot;
                    btnConfirmReservation.setDisable(false);
                } else {
                    selectedTimeSlot = null;
                    btnConfirmReservation.setDisable(true);
                }
            });
            
            timeSlotsGridPane.add(timeSlotButton, col, row);
            col++;
            if (col >= 4) { 
                col = 0;
                row++;
            }
        }
    }
    
    //*********************** Event Handlers ************************//
    
    @FXML
    void btnConfirmReservation(Event event) {
        LocalDate date = datePicker.getValue();
        String dinersStr = dinersAmountComboBox.getValue();
        int diners = parseDiners(dinersStr);
        
        if (selectedTimeSlot == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Please select a time slot.");
            alert.showAndWait();
            return;
        }

        if (staffProxyData != null) {
            String type = (String) staffProxyData.get("customerType");
            String id = (String) staffProxyData.get("identifier");
            String name = (String) staffProxyData.get("name");
            
            BistroClientGUI.client.getReservationCTRL().createReservationAsStaff(date, LocalTime.parse(selectedTimeSlot), diners, type, id, name);
        } else {
            BistroClientGUI.client.getReservationCTRL().createNewReservation(date, selectedTimeSlot, diners);
        }
    }
    
    @FXML
    void btnBack(Event event) {
        if (staffProxyData != null) {
            BistroClientGUI.switchScreen(event, "staff/clientStaffDashboardScreen", "Error returning to Staff Reservations Panel");
        } else {
            BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Error returning to Client Dashboard");
        }
    }
}