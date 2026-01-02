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
    public static final String NOTIFY_CONNECTION = "connection.notifyConnection";
    
// Responses
    public static final String REPLY_CONNECTION_CONNECT_OK = "connection.connect.ok";
    public static final String REPLY_CONNECTION_DISCONNECT_OK = "connection.disconnect.ok";

    // == Login/signOut subject == //
// Requests
    public static final String ASK_SIGNOUT_USER = "signout.user";
    public static final String ASK_LOGIN_GUEST = "login.guest";
    public static final String ASK_LOGIN_EMPLOYEE = "login.employee";
    public static final String ASK_LOGIN_MANAGER = "login.manager";
    public static final String ASK_LOGIN_MEMBER = "login.member";
// Responses
    public static final String REPLY_LOGIN_USER_OK = "login.user.ok";
    public static final String REPLY_LOGIN_USER_NOT_FOUND = "login.user.notFound";
    public static final String REPLY_SIGNOUT_USER_OK = "signout.user.ok";

    // == User subject == //
    // Requests
    public static final String ASK_MEMBER_UPDATE_INFO = "member.updateInfo";
    //Responses
    public static final String REPLY_MEMBER_UPDATE_INFO_OK = "member.updateInfo.ok";
    public static final String REPLY_MEMBER_UPDATE_INFO_FAILED = "member.updateInfo.failed";
    
    // == Orders subject == //
    
// Requests
    public static final String ASK_CREATE_RESERVATION 	= "orders.createReservation";
    public static final String ASK_ORDER_AVAILABLE_HOURS = "orders.getAvailableHours";
    public static final String ASK_GET_ORDER = "orders.getOrder";
    public static final String ASK_CHECK_ORDER_EXISTS = "orders.checkOrderExists";
    public static final String ASK_GET_ALLOCATED_TABLE = "orders.getAllocatedTable";
    public static final String ASK_PAYMENT_UPDATE = "orders.paymentUpdate";
    public static final String ASK_CANCEL_RESERVATION = "orders.cancelReservation";
    
// Responses
    public static final String REPLY_CREATE_RESERVATION_OK = "orders.createReservation.ok";
    public static final String REPLY_CREATE_RESERVATION_FAIL = "orders.createReservation.fail";
    public static final String REPLY_ORDER_AVAILABLE_HOURS_OK= "orders.getAvailableHours.ok";
    public static final String REPLY_ORDER_AVAILABLE_HOURS_FAIL = "orders.getAvailableHours.fail";
    public static final String REPLY_GET_ORDER_OK = "orders.getOrder.ok";
    public static final String REPLY_GET_ORDER_FAIL = "orders.getOrder.fail";
    public static final String REPLY_ORDER_EXISTS = "orders.order.exists";
    public static final String REPLY_ORDER_NOT_EXISTS = "orders.order.notExists";
    public static final String REPLY_GET_ALLOCATED_TABLE_OK = "orders.getAllocatedTable.ok";
    public static final String REPLY_GET_ALLOCATED_TABLE_FAIL = "orders.getAllocatedTable.fail";
    public static final String REPLY_PAYMENT_UPDATE_OK = "Orders.paymentUpdate.ok";
    public static final String REPLY_PAYMENT_UPDATE_FAIL = "Orders.paymentUpdate.fail";
    public static final String REPLY_CANCEL_RESERVATION_OK = "orders.cancelReservation.ok";
    public static final String REPLY_CANCEL_RESERVATION_FAIL = "orders.cancelReservation.fail";
    
    // == Restaurant Management subject == //
// Requests
    
// Responses
    
    // == WaitList subject == //
  // Requests
	public static final String ASK_WAITING_LIST_JOIN = "waitingList.join";
	public static final String ASK_WAITING_LIST_LEAVE = "waitingList.leave";
	public static final String ASK_IS_IN_WAITLIST = "waitinglist.isInWaitingList";
 // Responses
    public static final String REPLY_WAITING_LIST_JOIN_OK = "waitinglist.join.ok";
    public static final String REPLY_WAITING_LIST_SKIPPED = "waitinglist.join.skipped";
    public static final String REPLY_WAITING_LIST_JOIN_FAIL = "waitinglist.join.fail";
    public static final String REPLY_WAITING_LIST_LEAVE_OK = "waitinglist.leave.ok";
    public static final String REPLY_WAITING_LIST_LEAVE_FAIL = "waitinglist.leave.fail";
    public static final String REPLY_IS_IN_WAITLIST_YES = "waitinglist.isInWaitingList.yes";
    public static final String REPLY_IS_IN_WAITLIST_NO = "waitinglist.isInWaitingList.no";
    
    // == Payment subject == //
// Requests
    public static final String ASK_PAYMENT_COMPLETE = "payment.complete";
    
// Responses
	public static final String REPLY_PAYMENT_COMPLETE_OK = "payment.complete.ok";
	public static final String REPLY_PAYMENT_COMPLETE_FAIL = "payment.complete.fail";
  
    
    // == System responses == //
    
    public static final String REPLY_UNKNOWN_COMMAND = "system.unknownCommand";
}
