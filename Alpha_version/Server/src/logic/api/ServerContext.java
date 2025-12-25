package logic.api;

import gui.controllers.ServerConsoleController;

public class ServerContext {

    private final ServerConsoleController console;

    public ServerContext(ServerConsoleController console) {
        this.console = console;
    }

    public void log(String text) {
        System.out.println(text);
        if (console != null) {
            console.displayMessageToConsole(text);
        }
    }
}
