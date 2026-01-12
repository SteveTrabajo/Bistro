package logic.api.subjects;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import logic.BistroClient;
import logic.BistroClientGUI;
import logic.UserController;
import logic.api.ClientRouter;

public class ReservationSubject {
	public static void register(ClientRouter router, UserController userController) {

		router.on("reservation", "forgotConfirmationCode.ok", msg -> {
			BistroClient.awaitResponse = false;
			String confirmationCode = (String) msg.getData();
			BistroClientGUI.client.getReservationCTRL().handleForgotConfirmationCodeResponse(confirmationCode);
		});
		router.on("reservation", "forgotConfirmationCode.fail", msg -> {
			BistroClient.awaitResponse = false;
			BistroClientGUI.client.getReservationCTRL().handleForgotConfirmationCodeResponse("NOT_FOUND");
		});
	}
}
