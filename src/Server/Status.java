package Server;

/**
 * Enums used by the server to keep track of the statuses of operations.
 */
public enum Status {
    LOGGED_IN,
    LOG_IN_FAILED,
    USER_ADDED,
    USER_FOUND,
    USER_NOT_FOUND,
    USER_REMOVED,
    USER_NAME_NOT_AVAILABLE,
    PASSWORD_RESET
}
