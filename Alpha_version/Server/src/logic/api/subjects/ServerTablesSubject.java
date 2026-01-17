package logic.api.subjects;

import java.util.HashMap;
import java.util.List;

import comms.Api;
import comms.Message;
import dto.Holiday;
import dto.WeeklyHour;
import entities.Order;
import entities.Table;
import logic.ServerLogger;
import logic.api.ServerRouter;
import logic.services.TableService;

public class ServerTablesSubject {

    private ServerTablesSubject() {}

    public static void register(ServerRouter router, TableService tableService, ServerLogger logger) {
        
        // Handle request for table map status
        router.on("tables", "getStatus", (msg, client) -> {
            // Get data from Service
        	HashMap<Table,String> tables = tableService.getAllTablesMap();
            
            if (tables != null) {
                client.sendToClient(new Message(Api.REPLY_TABLE_STATUS_OK, tables));
                logger.log("[INFO] Sent table statuses to " + client);
            } else {
                client.sendToClient(new Message(Api.REPLY_TABLE_STATUS_FAIL, null));
                logger.log("[ERROR] Failed to fetch table statuses for " + client);
            }
        });
        
        router.on("tables", "askSeatedOrder", (msg, client) -> {
        	int userId = (int) msg.getData();
        	Order seatedOrder = tableService.getSeatedOrderForClient(userId);
        	if (seatedOrder != null) {
				client.sendToClient(new Message(Api.REPLY_SEATED_ORDER_OK, seatedOrder));
			} else {
				client.sendToClient(new Message(Api.REPLY_SEATED_ORDER_FAIL, null));
			}
		
		});
        
        router.on("tables", "getAll", (msg, client) -> {
            List<Table> tables = tableService.getAllTables();
            client.sendToClient(new Message(Api.REPLY_ALL_TABLES_OK, tables)); 
        });

        router.on("tables", "add", (msg, client) -> {
            Table newTable = (Table) msg.getData();
            boolean success = tableService.addNewTable(newTable);
            if (success) {
                List<Table> updatedList = tableService.getAllTables();
                client.sendToClient(new Message(Api.REPLY_ALL_TABLES_OK, updatedList));
            }
        });

        router.on("tables", "remove", (msg, client) -> {
            int tableId = (int) msg.getData();
            boolean success = tableService.deleteTable(tableId);
            if (success) {
                List<Table> updatedList = tableService.getAllTables();
                client.sendToClient(new Message(Api.REPLY_ALL_TABLES_OK, updatedList));
            }
        });
        
    }
        
}
