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
    public static final String ASK_LOGIN_GUEST = "login.guest";
    public static final String ASK_LOGIN_MEMBER = "login.member";
    public static final String ASK_LOGIN_EMPLOYEE = "login.employee";
    public static final String ASK_LOGIN_MANAGER = "login.manager";
    public static final String ASK_SIGNOUT_GUEST = "signout.guest";
    public static final String ASK_SIGNOUT_MEMBER = "signout.member";
    public static final String ASK_SIGNOUT_EMPLOYEE = "signout.employee";
    public static final String ASK_SIGNOUT_MANAGER = "signout.manager";
    
// Responses
    public static final String REPLY_LOGIN_GUEST_OK = "login.guest.ok";
    public static final String REPLY_LOGIN_MEMBER_OK = "login.member.ok";
    public static final String REPLY_LOGIN_EMPLOYEE_OK = "login.employee.ok";
    public static final String REPLY_LOGIN_MANAGER_OK = "login.manager.ok";
    public static final String REPLY_LOGIN_GUEST_NOT_FOUND = "login.guest.notFound";
    public static final String REPLY_LOGIN_MEMBER_NOT_FOUND = "login.member.notFound";
    public static final String REPLY_LOGIN_EMPLOYEE_NOT_FOUND = "login.employee.notFound";
    public static final String REPLY_LOGIN_MANAGER_NOT_FOUND = "login.manager.notFound";
    public static final String REPLY_SIGNOUT_GUEST_OK = "signout.guest.ok";
    public static final String REPLY_SIGNOUT_MEMBER_OK = "signout.member.ok";
    public static final String REPLY_SIGNOUT_EMPLOYEE_OK = "signout.employee.ok";
    public static final String REPLY_SIGNOUT_MANAGER_OK = "signout.manager.ok";
    public static final String REPLY_SIGNOUT_GUEST_FAIL = "signout.guest.fail";
    public static final String REPLY_SIGNOUT_MEMBER_FAIL = "signout.member.fail";
    public static final String REPLY_SIGNOUT_EMPLOYEE_FAIL = "signout.employee.fail";
    public static final String REPLY_SIGNOUT_MANAGER_FAIL = "signout.manager.fail";

    // == User subject == //
    // Requests
    public static final String ASK_MEMBER_UPDATE_INFO = "member.updateInfo";
    public static final String ASK_FORGOT_MEMBER_ID = "User.forgotMemberID";
    public static final String ASK_REGISTER_NEW_MEMBER = "user.registerNewMember";
    public static final String ASK_REGISTERATION_STATS = "member.registerationStats";
    //Responses
    public static final String REPLY_MEMBER_UPDATE_INFO_OK = "member.updateInfo.ok";
    public static final String REPLY_MEMBER_UPDATE_INFO_FAILED = "member.updateInfo.failed";
    public static final String REPLY_REGISTER_NEW_MEMBER_OK = "user.registerNewMember.ok";
    public static final String REPLY_REGISTER_NEW_MEMBER_FAILED = "user.registerNewMember.failed";
    public static final String REPLY_FORGOT_MEMBER_ID_OK = "User.forgotMemberID.ok";
    public static final String REPLY_FORGOT_MEMBER_ID_FAILED = "User.forgotMemberID.failed";
    public static final String REPLY_REGISTERATION_STATS_OK = "member.registerationStats.ok";
    public static final String REPLY_REGISTERATION_STATS_FAILED = "member.registerationStats.failed";
    
    // == Orders subject == //
    
// Requests
    public static final String ASK_CREATE_RESERVATION 	= "orders.createReservation";
    public static final String ASK_ORDER_AVAILABLE_HOURS = "orders.getAvailableHours";
    public static final String ASK_GET_ORDER = "orders.getOrder";
    public static final String ASK_CHECK_ORDER_EXISTS = "orders.checkOrderExists";
    public static final String ASK_GET_ALLOCATED_TABLE = "orders.getAllocatedTable";
    public static final String ASK_PAYMENT_UPDATE = "orders.paymentUpdate";
    public static final String ASK_CANCEL_RESERVATION = "orders.cancelReservation";
    public static final String ASK_UPDATE_RESERVATION = "orders.updateReservation";
    public static final String ASK_GET_RESERVATIONS_BY_DATE = "orders.getOrdersByDate";
    
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
    public static final String REPLY_UPDATE_RESERVATION_OK = "orders.updateReservation.ok";
    public static final String REPLY_UPDATE_RESERVATION_FAIL = "orders.updateReservation.fail";
    public static final String REPLY_GET_RESERVATIONS_BY_DATE_OK = "orders.getOrdersByDate.ok";
    public static final String REPLY_GET_RESERVATIONS_BY_DATE_FAIL = "orders.getOrdersByDate.fail";
    
    // == Restaurant Management subject == //
// Requests
    public static final String ASK_TABLE_STATUS = "tables.getStatus";
    
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
    public static final String REPLY_WAITING_LIST_NOTIFIED_OK = "waitinglist.notified.ok";//
    public static final String REPLY_WAITING_LIST_NOTIFIED_FAIL = "waitinglist.notified.fail";//
    
    // == Payment subject == //
// Requests
    public static final String ASK_PAYMENT_COMPLETE = "payment.complete";
    
// Responses
	public static final String REPLY_PAYMENT_COMPLETE_OK = "payment.complete.ok";
	public static final String REPLY_PAYMENT_COMPLETE_FAIL = "payment.complete.fail";
  
    
    // == System responses == //
    
    public static final String REPLY_UNKNOWN_COMMAND = "system.unknownCommand";
}
