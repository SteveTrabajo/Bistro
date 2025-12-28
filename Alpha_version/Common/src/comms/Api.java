package comms;

/**
 * Shared API contract between Client and Server.
 * Message IDs are namespaced as: <subject>.<action>
 *
 * Naming convention for constants:
 *   ASK_*   = client -> server requests
 *   REPLY_* = server -> client responses
 */

public final class Api {
    private Api() {}

    // == Connection subject == // 
    
// Requests
    public static final String ASK_CONNECTION_CONNECT    = "connection.connect";
    public static final String ASK_CONNECTION_DISCONNECT = "connection.disconnect";
    
// Responses
    public static final String REPLY_CONNECTION_CONNECT_OK = "connection.connect.ok";


    // == Login subject == //
// Requests
    public static final String ASK_LOGIN_USER = "login.user";
    
// Responses
    public static final String REPLY_LOGIN_USER_OK = "login.user.ok";
    public static final String REPLY_LOGIN_USER_NOT_FOUND = "login.user.notFound";
    
    
    // == Orders subject == //
    
// Requests
    public static final String ASK_CREATE_RESERVATION 	= "orders.createReservation";
    public static final String ASK_ORDERS_LIST          = "orders.list";
    public static final String ASK_ORDERS_UPDATE_STATUS = "orders.updateStatus";
    public static final String ASK_ORDERS_GET_BY_CODE   = "orders.getByCode";
    
// Responses
    public static final String REPLY_CREATE_RESERVATION_OK       		= "orders.createReservation.ok";
    public static final String REPLY_ORDERS_LIST_RESULT                 = "orders.list.result";
    public static final String REPLY_ORDERS_UPDATE_OK                   = "orders.updateStatus.ok";
    public static final String REPLY_ORDERS_UPDATE_DATE_NOT_AVAILABLE   = "orders.updateStatus.dateNotAvailable";
    public static final String REPLY_ORDERS_UPDATE_INVALID_CONFIRM_CODE = "orders.updateStatus.invalidConfirmCode";
    public static final String REPLY_ORDERS_GET_BY_CODE_RESULT          = "orders.getByCode.result";

    // == Restaurant subject == //
// Requests
    public static final String ASK_AVAIL_HOURS = "restaurant.getAvailableHours";
    
// Responses
    public static final String REPLY_AVAIL_HOURS= "restaurant.getAvailableHours.result";
    
    // == WaitList subject == //
  // Requests
    public static final String ASK_WAITLIST_ADD_USER    = "waitlist.addUser";
    public static final String ASK_WAITLIST_REMOVE_USER = "waitlist.removeUser";
    public static final String ASK_WAITLIST_IS_USER_ON  = "waitlist.isUserOn";
 // Responses
    public static final String REPLY_WAITLIST_ADD_USER_OK    = "waitlist.addUser.ok";
    public static final String REPLY_WAITLIST_REMOVE_USER_OK = "waitlist.removeUser.ok";
    public static final String REPLY_WAITLIST_IS_USER_ON_RESULT  = "waitlist.isUserOn.result";
    
    // == System responses == //
    
    public static final String REPLY_UNKNOWN_COMMAND = "system.unknownCommand";
	
}
