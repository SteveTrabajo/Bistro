package logic;

import gui.controllers.ServerConsoleController;

/**
 * Central logger for server-side code.
 * Routes log messages to the JavaFX server console (ServerConsoleController)
 */
public final class ServerLogger {
	
	//********************** Instance variables ************************
	
    private static volatile ServerConsoleController console;
    
	//********************** Constructors *******************************
    public ServerLogger(ServerConsoleController consoleController) {
    	console = consoleController;
    }
    
    //**********************Instance methods **************************
    /**
     * Log to the server console if available; otherwise fallback to terminal.
     * @param message The message to log
     */
    public void log(String message) {
        ServerConsoleController c = console;
        if (c != null) {
            c.displayMessageToConsole(message);
        } else {
            System.out.println(message);
        }
    }
}
// End of ServerLogger.java