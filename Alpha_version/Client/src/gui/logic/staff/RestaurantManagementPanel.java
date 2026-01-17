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

	@FXML
	private ComboBox<String> cmboxOpen1, cmboxOpen2, cmboxOpen3, cmboxOpen4, cmboxOpen5, cmboxOpen6, cmboxOpen7;
	@FXML
	private ComboBox<String> cmboxClose1, cmboxClose2, cmboxClose3, cmboxClose4, cmboxClose5, cmboxClose6, cmboxClose7;
	@FXML
	private CheckBox ActiveCheck1, ActiveCheck2, ActiveCheck3, ActiveCheck4, ActiveCheck5, ActiveCheck6, ActiveCheck7;
	@FXML
	private Button btnSaveHours;
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
	private Button btnEditTable;
	@FXML
	private ComboBox<String> cmbHolidayOpen;
	@FXML
	private ComboBox<String> cmbHolidayClose;

	private final ObservableList<Table> tableList = FXCollections.observableArrayList();
	private final List<ComboBox<String>> openBoxes = new ArrayList<>();
	private final List<ComboBox<String>> closeBoxes = new ArrayList<>();
	private final List<CheckBox> activeChecks = new ArrayList<>();

	/*
	 * Initializes the controller class. This method is automatically called
	 * after the fxml file has been loaded.
	 */
	@FXML
	public void initialize() {
		initHoursArrays();
		setupTimeComboBoxes();
		setupTableManagement();
		// disable holiday time pickers when "closed" is checked
		if (holyShitCheck != null) {
			holyShitCheck.selectedProperty().addListener((obs, oldV, isClosed) -> {
				if (cmbHolidayOpen != null) {
					cmbHolidayOpen.setDisable(isClosed);
					if (isClosed)
						cmbHolidayOpen.getSelectionModel().clearSelection();
				}
				if (cmbHolidayClose != null) {
					cmbHolidayClose.setDisable(isClosed);
					if (isClosed)
						cmbHolidayClose.getSelectionModel().clearSelection();
				}
			});
			// apply initial state
			boolean isClosed = holyShitCheck.isSelected();
			if (cmbHolidayOpen != null)
				cmbHolidayOpen.setDisable(isClosed);
			if (cmbHolidayClose != null)
				cmbHolidayClose.setDisable(isClosed);
		}
		loadWeeklyHoursFromDB();
		loadHolidaysFromDB();
	}

	/*
	 * Database Loading and Rendering
	 * Load weekly hours from DB and render them
	 */
	private void loadWeeklyHoursFromDB() {
		if (BistroClientGUI.client == null)
			return;
		try {
			BistroClientGUI.client.getTableCTRL().setWeeklyHoursListener(this::renderWeeklyHours);
			BistroClientGUI.client.getTableCTRL().askGetWeeklyHours();
		} catch (Exception e) {
			System.out.println("[WARN] Weekly hours load hooks missing: " + e.getMessage());
		}
	}

	/*
	 * Load holidays from DB and render them
	 */
	private void loadHolidaysFromDB() {
		if (BistroClientGUI.client == null)
			return;

		try {
			BistroClientGUI.client.getTableCTRL().setHolidaysListener(this::renderHolidays);
			BistroClientGUI.client.getTableCTRL().askGetHolidays();
		} catch (Exception e) {
			System.out.println("[WARN] Holidays load hooks missing: " + e.getMessage());
		}
	}

	/*
	 * Rendering Methods
	 * Renders the weekly hours in the UI
	 * @param hours
	 */
	private void renderWeeklyHours(List<WeeklyHour> hours) {
		for (int i = 0; i < 7; i++) {
			activeChecks.get(i).setSelected(false);
			openBoxes.get(i).getSelectionModel().clearSelection();
			closeBoxes.get(i).getSelectionModel().clearSelection();
		}
		if (hours == null)
			return;
		for (WeeklyHour wh : hours) {
			int idx = wh.getDayOfWeek() - 1;
			if (idx < 0 || idx > 6)
				continue;
			boolean active = wh.getOpenTime() != null && wh.getCloseTime() != null;
			activeChecks.get(idx).setSelected(active);
			if (active) {
				openBoxes.get(idx).setValue(formatHHmm(wh.getOpenTime()));
				closeBoxes.get(idx).setValue(formatHHmm(wh.getCloseTime()));
			}
		}
	}

	/*
	 * Renders the list of holidays in the UI
	 * @param holidays
	 */
	private void renderHolidays(List<Holiday> holidays) {
		holidaysListBox.getChildren().clear();
		if (holidays == null)
			return;
		for (Holiday h : holidays) {
			Label lbl = new Label("� " + h.toString());
			lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
			holidaysListBox.getChildren().add(lbl);
		}
	}

	/*
	 * Helper Methods
	 */
	private String formatHHmm(LocalTime t) {
		String s = t.toString();
		return s.length() >= 5 ? s.substring(0, 5) : s;
	}

	/*
	 * Table Management Methods
	 */
	private void setupTableManagement() {
		spinDinersAmount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 20, 4));
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

	/*
	 * Updates the table list in the UI
	 * @param tables
	 */
	private void updateTableList(List<Table> tables) {
		Platform.runLater(() -> {
			tableList.clear();
			if (tables != null) {
				tableList.addAll(tables);
			}
		});
	}

	/*
	 * Button Event Handlers
	 * Add new table to the system
	 * @param event
	 */
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

	/*
	 * Edit selected table's number of seats
	 * @param event
	 */
	@FXML
	void btnEditTable(ActionEvent event) {
		Table selected = tablesTable.getSelectionModel().getSelectedItem();
		if (selected == null) {
			showAlertInfo("Selection Error", "Please select a table to edit.");
			return;
		}
		// Create a dialog to edit the number of seats
		Dialog<Integer> dialog = new Dialog<>();
		dialog.setTitle("Edit Table");
		dialog.setHeaderText("Edit Table T" + selected.getTableID());
		// Set up buttons
		ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
		// Create content
		VBox content = new VBox(10);
		content.setPadding(new javafx.geometry.Insets(20));

		Label label = new Label("Number of Seats:");
		Spinner<Integer> seatsSpinner = new Spinner<>(1, 20, selected.getCapacity());
		seatsSpinner.setEditable(true);
		seatsSpinner.setPrefWidth(100);

		content.getChildren().addAll(label, seatsSpinner);
		dialog.getDialogPane().setContent(content);
		// Convert result
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == saveButtonType) {
				return seatsSpinner.getValue();
			}
			return null;
		});
		dialog.showAndWait().ifPresent(newSeats -> {
			if (newSeats != selected.getCapacity()) {
				// Update the table with new seat count
				if (BistroClientGUI.client != null) {
					BistroClientGUI.client.getTableCTRL().askUpdateTableSeats(selected.getTableID(), newSeats);
				}
			}
		});
	}

	/*
	 * Weekly Hours Management Methods
	 */
	private void initHoursArrays() {
		openBoxes.add(cmboxOpen1);
		openBoxes.add(cmboxOpen2);
		openBoxes.add(cmboxOpen3);
		openBoxes.add(cmboxOpen4);
		openBoxes.add(cmboxOpen5);
		openBoxes.add(cmboxOpen6);
		openBoxes.add(cmboxOpen7);

		closeBoxes.add(cmboxClose1);
		closeBoxes.add(cmboxClose2);
		closeBoxes.add(cmboxClose3);
		closeBoxes.add(cmboxClose4);
		closeBoxes.add(cmboxClose5);
		closeBoxes.add(cmboxClose6);
		closeBoxes.add(cmboxClose7);

		activeChecks.add(ActiveCheck1);
		activeChecks.add(ActiveCheck2);
		activeChecks.add(ActiveCheck3);
		activeChecks.add(ActiveCheck4);
		activeChecks.add(ActiveCheck5);
		activeChecks.add(ActiveCheck6);
		activeChecks.add(ActiveCheck7);
	}

	/*
	 * Sets up the time combo boxes with 30-minute intervals from 06:00 to 23:30
	 */
	private void setupTimeComboBoxes() {
		List<String> times = generateTimeSlots();
		for (int i = 0; i < 7; i++) {
			openBoxes.get(i).getItems().setAll(times);
			closeBoxes.get(i).getItems().setAll(times);
			openBoxes.get(i).getSelectionModel().clearSelection();
			closeBoxes.get(i).getSelectionModel().clearSelection();
		}

		if (cmbHolidayOpen != null) {
			cmbHolidayOpen.getItems().setAll(times);
			cmbHolidayOpen.getSelectionModel().clearSelection();
		}
		if (cmbHolidayClose != null) {
			cmbHolidayClose.getItems().setAll(times);
			cmbHolidayClose.getSelectionModel().clearSelection();
		}
	}

	/*
	 * Generates time slots from 06:00 to 23:30 in 30-minute intervals
	 * @return List of time strings in "HH:mm" format
	 */
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

	/*
	 * Save Weekly Hours Button Handler
	 * @param event
	 */
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
				// validation
				if (!closeTime.isAfter(openTime)) {
					showAlertInfo("Invalid Hours", "Day " + dayId + ": Closing time must be after opening time.");
					hasErrors = true;
				}
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

	/*
	 * Add Holiday Button Handler
	 */
	@FXML
	void btnAddHoliday(ActionEvent event) {
		if (dpHoliday.getValue() == null || txtHolidayName.getText() == null
				|| txtHolidayName.getText().trim().isEmpty()) {
			showAlertInfo("Error", "Please select a date and enter a holiday name.");
			return;
		}
		boolean isClosed = holyShitCheck.isSelected();
		LocalTime openTime = null;
		LocalTime closeTime = null;
		// If not closed - require + validate times
		if (!isClosed) {
			String openStr = (cmbHolidayOpen == null) ? null : cmbHolidayOpen.getValue();
			String closeStr = (cmbHolidayClose == null) ? null : cmbHolidayClose.getValue();
			if (openStr == null || closeStr == null) {
				showAlertInfo("Invalid Hours", "Please select open and close times for this holiday (or mark it as closed).");
				return;
			}
			openTime = LocalTime.parse(openStr);
			closeTime = LocalTime.parse(closeStr);
			if (!closeTime.isAfter(openTime)) {
				showAlertInfo("Invalid Hours", "Closing time must be after opening time.");
				return;
			}
			if (closeTime.isBefore(openTime.plusHours(2))) {
				showAlertInfo("Invalid Hours", "The restaurant must be open for at least 2 hours.");
				return;
			}
		}
		Holiday holiday = new Holiday(dpHoliday.getValue(), txtHolidayName.getText().trim(), isClosed, openTime, closeTime);
		if (BistroClientGUI.client != null) {
			BistroClientGUI.client.getTableCTRL().askAddHoliday(holiday);
		}

		dpHoliday.setValue(null);
		txtHolidayName.clear();
		holyShitCheck.setSelected(false);

		// Clear holiday time pickers
		if (cmbHolidayOpen != null)
			cmbHolidayOpen.getSelectionModel().clearSelection();
		if (cmbHolidayClose != null)
			cmbHolidayClose.getSelectionModel().clearSelection();
	}

	/*
	 * Shows an information alert with the given title and content.
	 */
	private void showAlertInfo(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}
// End of RestaurantManagementPanel.java