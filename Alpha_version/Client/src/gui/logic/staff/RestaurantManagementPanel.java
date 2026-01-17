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
    private final ObservableList<Table> tableList = FXCollections.observableArrayList();
    private final List<ComboBox<String>> openBoxes = new ArrayList<>();
    private final List<ComboBox<String>> closeBoxes = new ArrayList<>();
    private final List<CheckBox> activeChecks = new ArrayList<>();

    @FXML
    public void initialize() {
        initHoursArrays();
        setupTimeComboBoxes();    
        setupTableManagement();
        
        loadWeeklyHoursFromDB();
        loadHolidaysFromDB();
    }

    private void loadWeeklyHoursFromDB() {
        if (BistroClientGUI.client == null) return;

        try {
            BistroClientGUI.client.getTableCTRL().setWeeklyHoursListener(this::renderWeeklyHours);
            BistroClientGUI.client.getTableCTRL().askGetWeeklyHours();
        } catch (Exception e) {
            System.out.println("[WARN] Weekly hours load hooks missing: " + e.getMessage());
        }
    }

    private void loadHolidaysFromDB() {
        if (BistroClientGUI.client == null) return;

        try {
            BistroClientGUI.client.getTableCTRL().setHolidaysListener(this::renderHolidays);
            BistroClientGUI.client.getTableCTRL().askGetHolidays();
        } catch (Exception e) {
            System.out.println("[WARN] Holidays load hooks missing: " + e.getMessage());
        }
    }

    private void renderWeeklyHours(List<WeeklyHour> hours) {
        for (int i = 0; i < 7; i++) {
            activeChecks.get(i).setSelected(false);
            openBoxes.get(i).getSelectionModel().clearSelection();
            closeBoxes.get(i).getSelectionModel().clearSelection();
        }

        if (hours == null) return;

        for (WeeklyHour wh : hours) {
            int idx = wh.getDayOfWeek() - 1;
            if (idx < 0 || idx > 6) continue;

            boolean active = wh.getOpenTime() != null && wh.getCloseTime() != null;
            activeChecks.get(idx).setSelected(active);

            if (active) {
                openBoxes.get(idx).setValue(formatHHmm(wh.getOpenTime()));
                closeBoxes.get(idx).setValue(formatHHmm(wh.getCloseTime()));
            }
        }
    }


    private void renderHolidays(List<Holiday> holidays) {
        holidaysListBox.getChildren().clear();
        if (holidays == null) return;

        for (Holiday h : holidays) {
            Label lbl = new Label("• " + h.toString());
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
            holidaysListBox.getChildren().add(lbl);
        }
    }


    private String formatHHmm(LocalTime t) {
        String s = t.toString();
        return s.length() >= 5 ? s.substring(0, 5) : s;
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
            showAlertInfo("Input Error", "Please enter a Table ID.");
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
            for (Table t : tableList) {
                if (t.getTableID() == id) {
                    showAlertInfo("Duplicate", "Table ID " + id + " already exists.");
                    return;
                }
            }

            Table newTable = new Table(id, seats, false);
            if (BistroClientGUI.client != null) {
                BistroClientGUI.client.getTableCTRL().askAddTable(newTable);
                txtTableID.clear();
            }

        } catch (NumberFormatException e) {
            showAlertInfo("Input Error", "Invalid Table ID. Format must be 'T' followed by numbers (e.g., T12).");
        }
    }

    @FXML
    void btnRemoveTable(ActionEvent event) {
        Table selected = tablesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlertInfo("Selection Error", "Please select a table to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete Table T" + selected.getTableID() + "?",
                ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES && BistroClientGUI.client != null) {
                BistroClientGUI.client.getTableCTRL().askRemoveTable(selected.getTableID());
            }
        });
    }

  

    private void initHoursArrays() {
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
            openBoxes.get(i).getItems().setAll(times);
            closeBoxes.get(i).getItems().setAll(times);

            openBoxes.get(i).getSelectionModel().clearSelection();
            closeBoxes.get(i).getSelectionModel().clearSelection();
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
            int dayId = i + 1; // 1=Sun..7=Sat

            if (activeChecks.get(i).isSelected()) {
                String openStr = openBoxes.get(i).getValue();
                String closeStr = closeBoxes.get(i).getValue();

                if (openStr == null || closeStr == null) {
                    showAlertInfo("Invalid Hours", "Day " + dayId + ": Please select open and close times.");
                    hasErrors = true;
                    continue;
                }

                LocalTime openTime = LocalTime.parse(openStr);
                LocalTime closeTime = LocalTime.parse(closeStr);

                // VALIDATION 1: Closing time must be after opening time
                if (!closeTime.isAfter(openTime)) {
                    showAlertInfo("Invalid Hours", "Day " + dayId + ": Closing time must be after opening time.");
                    hasErrors = true;
                }
                // VALIDATION 2: Shift must be at least 2 hours
                else if (closeTime.isBefore(openTime.plusHours(2))) {
                    showAlertInfo("Invalid Hours", "Day " + dayId + ": The restaurant must be open for at least 2 hours.");
                    hasErrors = true;
                } else {
                    weeklyHours.add(new WeeklyHour(dayId, openTime, closeTime));
                }
            } else {
                // Inactive day
                weeklyHours.add(new WeeklyHour(dayId, null, null));
            }
        }

        if (!hasErrors && BistroClientGUI.client != null) {
            BistroClientGUI.client.getTableCTRL().askSaveWeeklyHours(weeklyHours);


        }
    }

 

    @FXML
    void btnAddHoliday(ActionEvent event) {
        if (dpHoliday.getValue() == null || txtHolidayName.getText() == null || txtHolidayName.getText().trim().isEmpty()) {
            showAlertInfo("Error", "Please select a date and enter a holiday name.");
            return;
        }

        Holiday holiday = new Holiday(dpHoliday.getValue(), txtHolidayName.getText().trim(), holyShitCheck.isSelected());

        if (BistroClientGUI.client != null) {
            BistroClientGUI.client.getTableCTRL().askAddHoliday(holiday);
        }

        dpHoliday.setValue(null);
        txtHolidayName.clear();
        holyShitCheck.setSelected(false);
    }


    private void showAlertInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
