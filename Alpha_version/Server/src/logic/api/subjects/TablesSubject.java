package logic.api.subjects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import comms.Api;
import comms.Message;
import entities.Table;
import logic.ServerLogger;
import logic.api.Router;
import logic.services.TableService;

public class TablesSubject {

    private TablesSubject() {}

    public static void register(Router router, TableService tableService, ServerLogger logger) {
        
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
        
    }
}