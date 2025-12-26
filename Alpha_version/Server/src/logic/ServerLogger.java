package logic;

import gui.controllers.ServerConsoleController;

/**
 * Central logger for server-side code.
 * Routes log messages to the JavaFX server console (ServerConsoleController)
 */
public final class ServerLogger {

    private static volatile ServerConsoleController console;

    private ServerLogger() {}
    
    /**
     * Call once when the server GUI is ready (server created).
     */
    public static void setConsole(ServerConsoleController consoleController) {
        console = consoleController;
    }

    /**
     * Log to the server console if available; otherwise fallback to terminal.
     */
    public static void log(String message) {
        ServerConsoleController c = console;
        if (c != null) {
            c.displayMessageToConsole(message);
        } else {
            System.out.println(message);
        }
    }
}
