package logic.api.subjects;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.api.ClientRouter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import entities.Order;

public class ClientOrderSubject {
	
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
		
		router.on("orders","createReservation.fail", msg -> {
			BistroClient.awaitResponse = false;
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Reservation Failed");
			alert.setHeaderText("Could not create reservation");
			alert.setContentText("An error occurred while creating your reservation. Please try again later.");
		});
		
		router.on("orders", "createReservationAsStaff.ok", msg -> {
            BistroClient.awaitResponse = false;
            String confirmationCode = (String) msg.getData();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Reservation Successful");
                alert.setHeaderText("Booking Confirmed!");
                alert.setContentText("Please provide the customer with their confirmation code: " + confirmationCode); 
                alert.showAndWait();
                
                BistroClientGUI.switchScreen("clientStaffDashboardScreen", "Error returning to Staff Dashboard.");
            });
        });

        router.on("orders", "createReservationAsStaff.fail", msg -> {
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
		
		router.on("orders", "order.exists", msg -> {
            BistroClient.awaitResponse = false;
			Order order = (Order) msg.getData();
			BistroClientGUI.client.getReservationCTRL().setOrderDTO(null); // Clear any existing order data
			BistroClientGUI.client.getTableCTRL().setUserAllocatedOrderForTable(order);
		});
		
		router.on("orders", "order.notExists", msg -> {
			BistroClient.awaitResponse = false;
		});	
		
		router.on("orders", "getOrdersByDate.ok", msg -> {
		    BistroClient.awaitResponse = false;
		    @SuppressWarnings
		    ("unchecked")
		    List<Order> orders = (List<Order>) msg.getData();
		    
		    Platform.runLater(() -> {
		        BistroClientGUI.client.getReservationCTRL().receiveStaffReservations(orders);
		    });
		});

		router.on("orders", "getAvailableDates.ok", msg -> {
			BistroClient.awaitResponse = false;
			// Retrieve the list of dates from the server message
			List<LocalDate> dates = (List<LocalDate>) msg.getData();
			// Send it to the controller to update the GUI
			BistroClientGUI.client.getReservationCTRL().setAvailableDates(dates);
		});
		router.on("orders", "getAvailableDates.fail", msg -> {
			BistroClient.awaitResponse = false;
			// Send an empty list or null to indicate failure/no dates
			BistroClientGUI.client.getReservationCTRL().setAvailableDates(new ArrayList<>());
		});
		
		
		router.on("orders", "seatCustomer.ok", msg -> {
			BistroClient.awaitResponse = false;
		    int tableNum = (int) msg.getData();
		    Platform.runLater(() -> {
		        Alert alert = new Alert(Alert.AlertType.INFORMATION);
		        alert.setTitle("Seating Successful");
		        alert.setHeaderText("Table Allocated!");
		        alert.setContentText("Please seat the customer at Table: " + tableNum);
		        alert.show();		        
		        // Refresh the list to show status change
		        BistroClientGUI.client.getReservationCTRL().askReservationsByDate(LocalDate.now());
		    });
		});

		router.on("orders", "seatCustomer.fail", msg -> {
			BistroClient.awaitResponse = false;
		    Platform.runLater(() -> {
		        Alert alert = new Alert(Alert.AlertType.WARNING);
		        alert.setTitle("Seating Failed");
		        alert.setHeaderText("No Table Available");
		        alert.setContentText("There is currently no table available that fits this reservation.\nPlease ask the client to wait.");
		        alert.show();
		    });
		});
		
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
		
		router.on("orders", "getClientHistory.ok", msg -> {
			BistroClient.awaitResponse = false;
			@SuppressWarnings("unchecked")
			List<Order> orders = (List<Order>) msg.getData();
			
			Platform.runLater(() -> {
				//TODO maybe rename method to receiveOrderHistory
				BistroClientGUI.client.getReservationCTRL().receiveStaffReservations(orders);
			});
		});
		
	}
	
}
