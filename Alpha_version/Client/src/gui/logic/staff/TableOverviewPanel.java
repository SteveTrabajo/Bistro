package gui.logic.staff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import entities.Table;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import logic.BistroClientGUI;

public class TableOverviewPanel {
	
	@FXML
    private Label lblAvailable;

    @FXML
    private Label lblOccupied;

    @FXML
    private TilePane tablesPane;

    @FXML
    public void initialize() {
    	BistroClientGUI.client.getTableCTRL().requestTableStatus();
    	System.out.println("Requested table statuses");
    	updateTableStatus(BistroClientGUI.client.getTableCTRL().getTableStatuses());
	}

  
    public void updateTableStatus(Map<Table, String> tableMap) {
    	
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateTableStatus(tableMap));
            return;
        }
        
        tablesPane.getChildren().clear();

        int occupiedCount = 0;
        int totalTables = tableMap.size();

        HashMap<Table, String> sortedList = (HashMap<Table, String>)tableMap;

		for (Map.Entry<Table, String> entry : sortedList.entrySet()) {
			Table table = entry.getKey();
			String code = entry.getValue();

			boolean isOccupied = (code != null && !code.isEmpty());

			if (isOccupied) {
				occupiedCount++;
			}

            VBox tableNode = createTableNode(table, isOccupied, code);
            tablesPane.getChildren().add(tableNode);
        }

        int availableCount = totalTables - occupiedCount;
        lblOccupied.setText(String.valueOf(occupiedCount));
        lblAvailable.setText(String.valueOf(availableCount));
    }

    
    public VBox createTableNode(Table table, boolean isOccupied, String code) {
        VBox box = new VBox(2);
        box.getStyleClass().add("sm-table");

        if (isOccupied) {
            box.getStyleClass().add("sm-table-occupied");
        } else {
            box.getStyleClass().add("sm-table-available");
        }

        Label idLabel = new Label(String.valueOf(table.getTableID()));
        idLabel.getStyleClass().add("sm-table-id");

        Label capLabel = new Label("Seats: " + table.getCapacity());
        capLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.8);");

        String statusText = isOccupied ? code : "Available";
        Label statusLabel = new Label(statusText);
        statusLabel.getStyleClass().add("sm-table-meta");

        box.getChildren().addAll(idLabel, capLabel, statusLabel);
        
        return box;
    }
}
