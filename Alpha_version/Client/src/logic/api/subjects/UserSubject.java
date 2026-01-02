package logic.api.subjects;

import entities.User;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.api.ClientRouter;

public class UserSubject {

    public static void register(ClientRouter router) {

        router.on("login", "user.ok", msg -> {
            User user = (User) msg.getData();
            BistroClientGUI.client.getUserCTRL().setLoggedInUser(user);
            BistroClient.awaitResponse = false;
        });

        router.on("login", "user.notFound", msg -> {
            BistroClientGUI.client.getUserCTRL().setLoggedInUser(null);
            BistroClient.awaitResponse = false;
        });
    }
}
