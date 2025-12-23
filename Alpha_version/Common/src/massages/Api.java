package massages;

public final class Api {
	// Requests
	public static final String REQ_TO_LOGIN = "user.login";
	public static final String REQ_ORDERS_LIST = "orders.list";
	public static final String REQ_ORDERS_UPDATE_STATUS = "orders.updateStatus";
	public static final String REQ_ORDERS_GET_BY_CODE = "orders.getByCode";
	public static final String REQ_CONNECTION_CONNECT = "connection.connect";
	public static final String REQ_CONNECTION_DISCONNECT = "connection.disconnect";
	// Responses
	public static final String RES_ORDERS_LIST_RESULT = "orders.list.result";
	public static final String RES_ORDERS_UPDATE_OK = "orders.updateStatus.ok";
	public static final String RES_ORDERS_UPDATE_DATE_NOT_AVAILABLE = "orders.updateStatus.dateNotAvailable";
	public static final String RES_ORDERS_UPDATE_INVALID_CONFIRM_CODE = "orders.updateStatus.invalidConfirmCode";
	public static final String RES_ORDERS_GET_BY_CODE_RESULT = "orders.getByCode.result";
	public static final String RES_UNKNOWN_COMMAND = "system.unknownCommand";
}
