package logic.api.subjects;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import entities.Order;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.api.ClientRouter;

/**
 * ClientOrderSubject handles order-related events
 * for the BistroClient application.
 */
public class ClientOrderSubject {
	
	/**
	 * Private constructor to prevent instantiation.
	 */
	private ClientOrderSubject() {}
	
	public static void register(ClientRouter router) {
		// Handler for new reservation creation messages
		router.on("orders", "createReservation.ok", msg -> {
            BistroClient.awaitResponse = false;
			Order createdOrder = (Order) msg.getData();
			if (createdOrder == null) {
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Reservation Failed");
					alert.setHeaderText("Invalid confirmation code received");
					alert.setContentText("An error occurred while creating your reservation. Please try again later.");
					alert.showAndWait();
				});
				return;
			}
			Platform.runLater(() ->{ 
			BistroClientGUI.client.getReservationCTRL().setOrderDTO(createdOrder);
			BistroClientGUI.switchScreen("clientNewReservationCreatedScreen", "Failed to load Reservation Success Screen after creating reservation.");
			});
		});
		// Handler for reservation creation failure messages
		router.on("orders","createReservation.fail", msg -> {
			BistroClient.awaitResponse = false;
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Reservation Failed");
			alert.setHeaderText("Could not create reservation");
			alert.setContentText("An error occurred while creating your reservation. Please try again later.");
		});
		// Handler for staff creating reservation on behalf of a client
		router.on("orders", "createReservation.asStaff.ok", msg -> {
            BistroClient.awaitResponse = false;
            Order createdOrder = (Order) msg.getData();
            String confirmationCode = createdOrder.getConfirmationCode();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Reservation Successful");
                alert.setHeaderText("Booking Confirmed!");
                alert.setContentText("Please provide the customer with their confirmation code: " + confirmationCode); 
                alert.showAndWait();
                System.out.println("Created Reservation Confirmation Code: " + confirmationCode);
                BistroClientGUI.switchScreen("staff/clientStaffDashboardScreen", "Error returning to Staff Dashboard.");
            });
        });
		// Handler for staff reservation creation failure messages
        router.on("orders", "createReservation.asStaff.fail", msg -> {
            BistroClient.awaitResponse = false;
            String errorMessage = (String) msg.getData(); 
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Booking Failed");
                alert.setHeaderText("Could not create reservation");
                alert.setContentText(errorMessage != null ? errorMessage : "Unknown error occurred.");
                alert.showAndWait();
            });
        });
		
		// This tells the router: "When the server sends 'getAvailableHours.ok', update the controller."
		router.on("orders", "getAvailableHours.ok", (msg) -> {
            BistroClient.awaitResponse = false;
            @SuppressWarnings("unchecked")
            List<String> slots = (List<String>) msg.getData();
            Platform.runLater(() -> BistroClientGUI.client.getReservationCTRL().setAvailableTimeSlots(slots));  
        });
		// Handler for failure to retrieve available hours
		router.on("orders", "getAvailableHours.fail", (msg) -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Could not retrieve available hours");
				alert.setContentText("An error occurred while fetching available time slots. Please try again later.");
				alert.showAndWait();
			});
		});
		// Handler for checking if an order exists for check-in
		router.on("orders", "order.exists", msg -> {
            BistroClient.awaitResponse = false;
			BistroClientGUI.client.getReservationCTRL().notifyCheckInResult(true, "Check-in successful!");
		});
		// Handler for non-existing order during check-in
		router.on("orders", "order.notExists", msg -> {
			BistroClient.awaitResponse = false;
			BistroClientGUI.client.getReservationCTRL().notifyCheckInResult(false, "Invalid confirmation code or reservation does not belong to you.");
		});	
		// Handler for staff viewing reservations by date
		router.on("orders", "getOrdersByDate.ok", msg -> {
		    BistroClient.awaitResponse = false;
		    @SuppressWarnings
		    ("unchecked")
		    List<Order> orders = (List<Order>) msg.getData();
		    
		    Platform.runLater(() -> {
		        BistroClientGUI.client.getReservationCTRL().receiveStaffReservations(orders);
		    });
		});
		// Handler for failure to retrieve reservations by date
		router.on("orders", "getMemberActiveReservations.ok", msg -> {
		    BistroClient.awaitResponse = false;
		    @SuppressWarnings("unchecked")
		    List<Order> orders = (List<Order>) msg.getData();
		    BistroClientGUI.client.getReservationCTRL().handleMemberReservationsListResponse(orders);
		});
		// Handler for failure to retrieve member active reservations
		router.on("orders", "getMemberActiveReservations.fail", msg -> {
		    BistroClient.awaitResponse = false;
		    BistroClientGUI.client.getReservationCTRL().handleMemberReservationsListResponse(new ArrayList<>());
		});
		
		// Handler for failure to retrieve reservations by date
		router.on("orders", "getAvailableDates.ok", msg -> {
			BistroClient.awaitResponse = false;
			// Retrieve the list of dates from the server message
			List<LocalDate> dates = (List<LocalDate>) msg.getData();
			// Send it to the controller to update the GUI
			BistroClientGUI.client.getReservationCTRL().setAvailableDates(dates);
		});
		// Handler for failure to retrieve available dates
		router.on("orders", "getAvailableDates.fail", msg -> {
			BistroClient.awaitResponse = false;
			// Send an empty list or null to indicate failure/no dates
			BistroClientGUI.client.getReservationCTRL().setAvailableDates(new ArrayList<>());
		});
		// Handler for seating a customer successfully
		router.on("orders", "seatCustomer.ok", msg -> {
			BistroClient.awaitResponse = false;
		    int tableNum = (int) msg.getData();
		    
		    Platform.runLater(() -> {
		        if (BistroClientGUI.client.getReservationCTRL().hasCheckInListener()) {
		            // Send the Table Number as a string to the screen
		            BistroClientGUI.client.getReservationCTRL().notifyCheckInResult(true, String.valueOf(tableNum));
		        } 
		        // Otherwise, assume Staff Dashboard (Staff Flow)
		        else {
    		        Alert alert = new Alert(Alert.AlertType.INFORMATION);
    		        alert.setTitle("Seating Successful");
    		        alert.setHeaderText("Table Allocated!");
    		        alert.setContentText("Please seat the customer at Table: " + tableNum);
    		        alert.show();		        
    		        BistroClientGUI.client.getReservationCTRL().askReservationsByDate(LocalDate.now());
		        }
		    });
		});
		// Handler for seating a customer failure	
		router.on("orders", "seatCustomer.fail", msg -> {
			BistroClient.awaitResponse = false;
			String failMsg = (String) msg.getData();
			if (failMsg == null) failMsg = "Seating failed.";
			
			final String finalMsg = failMsg; // for lambda
			
		    Platform.runLater(() -> {
		    	if (BistroClientGUI.client.getReservationCTRL().hasCheckInListener()) {
		    		BistroClientGUI.client.getReservationCTRL().notifyCheckInResult(false, finalMsg);
		    	} else {
			        Alert alert = new Alert(Alert.AlertType.WARNING);
			        alert.setTitle("Seating Failed");
			        alert.setHeaderText("Action Failed");
			        alert.setContentText(finalMsg);
			        alert.show();
		    	}
		    });
		});
		// Handler for successful reservation cancellation
		router.on("orders", "cancelReservation.ok", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Cancellation Successful");
				alert.setHeaderText("Reservation Cancelled");
				alert.setContentText("The reservation has been successfully cancelled.");
				alert.show();
				// Refresh the list to show status change
				BistroClientGUI.client.getReservationCTRL().notifyCancelResult(true);
			});
		});
		// Handler for failed reservation cancellation
		router.on("orders", "cancelReservation.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Cancellation Failed");
				alert.setHeaderText("Could not cancel reservation");
				alert.setContentText("Can't cancel an already SEATED or COMPLETED reservation.");
				alert.show();
			});
		});
		// Handler for client viewing their own order history
		router.on("orders", "getClientHistory.ok", msg -> {
			BistroClient.awaitResponse = false;
			@SuppressWarnings("unchecked")
			List<Order> orders = (List<Order>) msg.getData();
			
			Platform.runLater(() -> {
				BistroClientGUI.client.getReservationCTRL().receiveStaffReservations(orders);
			});
		});
		// Handler for failure to retrieve client order history
		router.on("orders", "getClientHistory.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Could not retrieve order history");
				alert.setContentText("An error occurred while fetching the order history. Please try again later.");
				alert.showAndWait();
			});
		});
		
		// Handler for staff viewing member history
		router.on("orders", "getMemberHistory.ok", msg -> {
			BistroClient.awaitResponse = false;
			@SuppressWarnings("unchecked")
			List<Order> orders = (List<Order>) msg.getData();
			System.out.println("[DEBUG] Received member history: " + (orders == null ? "null" : orders.size() + " orders"));
			
			Platform.runLater(() -> {
				BistroClientGUI.client.getReservationCTRL().receiveStaffReservations(orders);
			});
		});
		
		// Handler for failure to retrieve member history
		router.on("orders", "getMemberHistory.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setTitle("Member Not Found");
				alert.setHeaderText("Could not retrieve member history");
				alert.setContentText("No history found for this member ID. Please verify the ID and try again.");
				alert.showAndWait();
			});
		});
		// Handler for retrieving forgotten confirmation code
		router.on("reservation", "forgotConfirmationCode.ok", msg -> {
		    BistroClient.awaitResponse = false;

		    //Cast the data directly to String
		    String confirmationCode = (String) msg.getData();

		    Platform.runLater(() -> {
		        //Create a Styled Dialog
		        Alert alert = new Alert(Alert.AlertType.INFORMATION);
		        alert.setTitle("Code Retrieved");
		        alert.setHeaderText(null);
		        alert.setGraphic(null);
		        // Create container
		        VBox content = new VBox(15);
		        content.setAlignment(Pos.CENTER);
		        content.setPadding(new Insets(20));
		        // Styled Title
		        Label lblTitle = new Label("Your Confirmation Code is:");
		        lblTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");
		        // Styled Code Box
		        TextField codeField = new TextField(confirmationCode);
		        codeField.setEditable(false); // Read-only
		        codeField.setAlignment(Pos.CENTER);
		        // CSS
		        codeField.setStyle(
		            "-fx-font-size: 24px; " +
		            "-fx-font-weight: bold; " +
		            "-fx-text-fill: #2c3e50; " +
		            "-fx-background-color: #f0f2f5; " +
		            "-fx-background-radius: 8px; " +
		            "-fx-border-color: #d1d8e0; " +
		            "-fx-border-radius: 8px;"
		        );
		        // Remove focus ring
		        codeField.setFocusTraversable(false);
		        content.getChildren().addAll(lblTitle, codeField);
		        alert.getDialogPane().setContent(content);
		        alert.showAndWait();
		        // Pass the single code to the controller
		        BistroClientGUI.client.getReservationCTRL().handleForgotConfirmationCodeResponse(confirmationCode);
		    });
		});
		
		// Handler for failure to retrieve forgotten confirmation code
		router.on("reservation", "forgotConfirmationCode.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Could not retrieve confirmation code");
				alert.setContentText("An error occurred while retrieving your confirmation code. Please try again later.");
				alert.showAndWait();
			});
		});
		
		//Handler for retrieving member seated reservations
		router.on("orders", "getMemberSeatedReservations.ok", msg -> {
		    BistroClient.awaitResponse = false;
		    @SuppressWarnings("unchecked")
		    List<Order> orders = (List<Order>) msg.getData();
		    BistroClientGUI.client.getReservationCTRL().handleMemberSeatedListResponse(orders);
		});
		
		// Handler for failure to retrieve member seated reservations
		router.on("orders", "getMemberSeatedReservations.fail", msg -> {
		    BistroClient.awaitResponse = false;
		    BistroClientGUI.client.getReservationCTRL().handleMemberSeatedListResponse(new ArrayList<>());
		});

		//Handler for retrieving guest seated code
		router.on("orders", "recoverGuestSeatedCode.ok", msg -> {
		    BistroClient.awaitResponse = false;
		    String code = (String) msg.getData();
		    BistroClientGUI.client.getReservationCTRL().handleGuestSeatedCodeResponse(code);
		});

		// Handler for failure to retrieve guest seated code
		router.on("orders", "recoverGuestSeatedCode.fail", msg -> {
		    BistroClient.awaitResponse = false;
		    BistroClientGUI.client.getReservationCTRL().handleGuestSeatedCodeResponse("NOT_FOUND");
		});	
	}
}
// End of ClientOrderSubject.java