package Objects;

import java.io.Serializable;

/**
 * Custom object created to test communication between client and server.
 */
public class RequestObject implements Serializable {
    private final String REQUEST_TYPE;
    private final String USERNAME;
    private String password;

    /**
     * First constructor creating RequestObject with 3 parameters. Used mainly for creating accounts, logging in
     * and resetting passwords - since password is needed for these 3 operations.
     *
     * @param requestType String of the request type
     * @param username    String of the username
     * @param password    String of the password
     */
    public RequestObject(String requestType, String username, String password) {
        this.REQUEST_TYPE = requestType;
        this.USERNAME = username;
        this.password = password;
    }

    /**
     * Second constructor creating RequestObjects with 2 parameters. Used mainly for communication of logged in accounts
     * since password is not needed. (can be used for searching and deleting users).
     *
     * @param requestType String of the request type
     * @param username    String of the password
     */
    public RequestObject(String requestType, String username) {
        this.REQUEST_TYPE = requestType;
        this.USERNAME = username;
    }

    /**
     * Method to get the username
     *
     * @return String of the username
     */
    public String getUSERNAME() {
        return USERNAME;
    }

    /**
     * Method to get the password
     *
     * @return String of the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Method to get the request type
     *
     * @return String of the request type
     */
    public String getREQUEST_TYPE() {
        return REQUEST_TYPE;
    }
}
