package gui.logic.staff;

import entities.Table;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import logic.BistroClientGUI;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import dto.Holiday;
import dto.WeeklyHour;

public class RestaurantManagementPanel {

    // --- Opening Hours Section ---
    @FXML 
    private ComboBox<String> cmboxOpen1, cmboxOpen2, cmboxOpen3, cmboxOpen4, cmboxOpen5, cmboxOpen6, cmboxOpen7;
    @FXML 
    private ComboBox<String> cmboxClose1, cmboxClose2, cmboxClose3, cmboxClose4, cmboxClose5, cmboxClose6, cmboxClose7;
    @FXML 
    private CheckBox ActiveCheck1, ActiveCheck2, ActiveCheck3, ActiveCheck4, ActiveCheck5, ActiveCheck6, ActiveCheck7;
    @FXML 
    private Button btnSaveHours;

    // --- Holiday Section ---
    @FXML 
    private VBox holidaysListBox;
    @FXML 
    private DatePicker dpHoliday;
    @FXML 
    private TextField txtHolidayName;
    @FXML 
    private CheckBox holyShitCheck; // "Restaurant closed on this day"
    @FXML 
    private Button btnAddHoliday;

    // --- Table Management Section ---
    @FXML 
    private TableView<Table> tablesTable;
    @FXML 
    private TableColumn<Table, Integer> colTableId;
    @FXML 
    private TableColumn<Table, Integer> colSeats;
    @FXML 
    private TextField txtTableID;
    @FXML 
    private Spinner<Integer> spinDinersAmount;
    @FXML 
    private Button btnAddTable;
    @FXML 
    private Button btnRemoveTable;

    // --- Data Lists ---
    private ObservableList<Table> tableList = FXCollections.observableArrayList();
    private List<ComboBox<String>> openBoxes = new ArrayList<>();
    private List<ComboBox<String>> closeBoxes = new ArrayList<>();
    private List<CheckBox> activeChecks = new ArrayList<>();

    @FXML
    public void initialize() {
        initHoursArrays();
        setupTimeComboBoxes();
        setupTableManagement();
    }

    private void setupTableManagement() {
        // Setup Spinner
        spinDinersAmount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 20, 4));

        // Setup Columns
        colTableId.setCellValueFactory(new PropertyValueFactory<>("tableID"));
        colTableId.setCellFactory(column -> new TableCell<Table, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("T" + item); 
                }
            }
        });
        
        colSeats.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        tablesTable.setItems(tableList);
        
        if (BistroClientGUI.client != null) {
            BistroClientGUI.client.getTableCTRL().setTablesListener(this::updateTableList);
            BistroClientGUI.client.getTableCTRL().askAllTables();
        }
    }
    
    private void updateTableList(List<Table> tables) {
        Platform.runLater(() -> {
            tableList.clear();
            if (tables != null) {
                tableList.addAll(tables);
            }
        });
    }
    
    @FXML
    void btnAddTable(ActionEvent event) {
        String idStr = txtTableID.getText();
        if (idStr == null || idStr.trim().isEmpty()) {
            showAlert("Input Error", "Please enter a Table ID.");
            return;
        }
        
        try {
            String cleanInput = idStr.trim().toUpperCase();
            int id;
            if (cleanInput.startsWith("T")) {
            	String numericPart = cleanInput.substring(1);
            	id = Integer.parseInt(numericPart);
            } else {
				id = Integer.parseInt(cleanInput);
			}
            
            int seats = spinDinersAmount.getValue();

            // Check if exists
            for(Table t : tableList) {
                if(t.getTableID() == id) {
                    showAlert("Duplicate", "Table ID " + id + " already exists.");
                    return;
                }
            }

            Table newTable = new Table(id, seats, false);
            if (BistroClientGUI.client != null) {
                BistroClientGUI.client.getTableCTRL().askAddTable(newTable);
                txtTableID.clear();
            }

        } catch (NumberFormatException e) {
            showAlert("Input Error", "Invalid Table ID. Format must be 'T' followed by numbers (e.g., T12).");
        }
    }

    @FXML
    void btnRemoveTable(ActionEvent event) {
        Table selected = tablesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Please select a table to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete Table T" + selected.getTableID() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES && BistroClientGUI.client != null) {
                BistroClientGUI.client.getTableCTRL().askRemoveTable(selected.getTableID());
            }
        });
    }
    
    
    private void initHoursArrays() {
        // Group fields into lists for easier handling
        openBoxes.add(cmboxOpen1); openBoxes.add(cmboxOpen2); openBoxes.add(cmboxOpen3);
        openBoxes.add(cmboxOpen4); openBoxes.add(cmboxOpen5); openBoxes.add(cmboxOpen6); openBoxes.add(cmboxOpen7);

        closeBoxes.add(cmboxClose1); closeBoxes.add(cmboxClose2); closeBoxes.add(cmboxClose3);
        closeBoxes.add(cmboxClose4); closeBoxes.add(cmboxClose5); closeBoxes.add(cmboxClose6); closeBoxes.add(cmboxClose7);

        activeChecks.add(ActiveCheck1); activeChecks.add(ActiveCheck2); activeChecks.add(ActiveCheck3);
        activeChecks.add(ActiveCheck4); activeChecks.add(ActiveCheck5); activeChecks.add(ActiveCheck6); activeChecks.add(ActiveCheck7);
    }

    private void setupTimeComboBoxes() {
        List<String> times = generateTimeSlots();
        for (int i = 0; i < 7; i++) {
            openBoxes.get(i).getItems().addAll(times);
            closeBoxes.get(i).getItems().addAll(times);
            
            // Default Values (Example: 10:00 - 20:00)
            openBoxes.get(i).getSelectionModel().select("08:00");
            closeBoxes.get(i).getSelectionModel().select("22:00");
        }
    }

    private List<String> generateTimeSlots() {
        List<String> times = new ArrayList<>();
        LocalTime start = LocalTime.of(6, 0);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        for (int i = 0; i < 35; i++) { // 30 min intervals
            times.add(start.format(dtf));
            start = start.plusMinutes(30);
        }
        return times;
    }

    @FXML
    void btnSaveHours(ActionEvent event) {
        List<WeeklyHour> weeklyHours = new ArrayList<>();
        boolean hasErrors = false;

        for (int i = 0; i < 7; i++) {
        	int dayId = i+1; // 1=Sun,2=Mon,...,7=Sat
			if (activeChecks.get(i).isSelected()) {
				String openStr = openBoxes.get(i).getValue();
				String closeStr = closeBoxes.get(i).getValue();

				if (openStr != null && closeStr != null) {
					LocalTime openTime = LocalTime.parse(openStr);
					LocalTime closeTime = LocalTime.parse(closeStr);

					// VALIDATION 1: Closing time must be after opening time
					if (!closeTime.isAfter(openTime)) {
						showAlert("Invalid Hours", "Day " + dayId + ": Closing time must be after opening time.");
						hasErrors = true;
					} 
					// VALIDATION 2: Shift must be at least 2 hours
					else if (closeTime.isBefore(openTime.plusHours(2))) {
						showAlert("Invalid Hours", "Day " + dayId + ": The restaurant must be open for at least 2 hours.");
						hasErrors = true;
					} else {
						weeklyHours.add(new WeeklyHour(dayId, openTime, closeTime));
					}
				}
			} else {
				// Inactive day
				weeklyHours.add(new WeeklyHour(dayId, null, null));
			}
        }

        if (!hasErrors && BistroClientGUI.client != null) {
        	BistroClientGUI.client.getTableCTRL().askSaveWeeklyHours(weeklyHours);
            showAlert("Success", "Opening hours updated successfully.");
        }
    }

    @FXML
    void btnAddHoliday(ActionEvent event) {
        if (dpHoliday.getValue() == null || txtHolidayName.getText().isEmpty()) {
            showAlert("Error", "Please select a date and enter a holiday name.");
            return;
        }
        
        Holiday holiday = new Holiday (dpHoliday.getValue(), txtHolidayName.getText(), holyShitCheck.isSelected());
        if (BistroClientGUI.client != null) {
			BistroClientGUI.client.getTableCTRL().askAddHoliday(holiday);
			
			Label holidayLabel = new Label("• " +holiday.toString());
			holidayLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
			holidaysListBox.getChildren().add(holidayLabel);
			
			// Clear inputs
			dpHoliday.setValue(null);
			txtHolidayName.clear();
			holyShitCheck.setSelected(false);
		}

    }

    

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}