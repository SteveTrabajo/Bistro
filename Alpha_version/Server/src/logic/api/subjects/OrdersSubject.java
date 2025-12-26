package logic.api.subjects;

import java.util.List;

import comms.Api;
import comms.Message;
import entities.Order;
import logic.BistroDataBase_Controller;
import logic.api.Router;


/**
 * API handlers related to orders.
 */
public final class OrdersSubject {

    private OrdersSubject() {}

    /**
     * Registers all order-related handlers.
     */
    public static void register(Router router) {

        // Get all orders
        router.on("orders", "list", (msg, client) -> {
            List<Order> orders = BistroDataBase_Controller.getAllOrders();
            client.sendToClient(new Message(Api.REPLY_ORDERS_LIST_RESULT, orders));
        });

        // Update order status
        router.on("orders", "updateStatus", (msg, client) -> {
            Order order = (Order) msg.getData();

            boolean available = BistroDataBase_Controller.isDateAvailable(
                    order.getOrderDate(),
                    order.getConfirmationCode()
            );

            if (!available) {
                client.sendToClient(
                        new Message(Api.REPLY_ORDERS_UPDATE_DATE_NOT_AVAILABLE, null));
                return;
            }

            boolean updated = BistroDataBase_Controller.updateOrder(order);

            if (updated) {
                client.sendToClient(
                        new Message(Api.REPLY_ORDERS_UPDATE_OK, null));
            } else {
                client.sendToClient(
                        new Message(Api.REPLY_ORDERS_UPDATE_INVALID_CONFIRM_CODE, null));
            }
        });

        // Get order by confirmation code
        router.on("orders", "getByCode", (msg, client) -> {
            int code = (int) msg.getData();
            client.sendToClient(
                    new Message(
                            Api.REPLY_ORDERS_GET_BY_CODE_RESULT,
                            BistroDataBase_Controller.getOrderByConfirmationCode(code)
                    )
            );
        });
    }
}
