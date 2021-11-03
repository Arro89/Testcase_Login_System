package Server;


import Objects.RequestObject;
import Security.PasswordHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * Login handler that handles incoming login, account creation and password reset requests from a user that is not
 * logged-in yet.
 */
public class LoginHandler implements Runnable {
    private final LoginServer LOGIN_SERVER;
    private final Socket CLIENT_SOCKET;
    private boolean waitingForRequest;
    private ObjectInputStream ois;
    private final PasswordHandler PASSWORD_HANDLER = new PasswordHandler();

    /**
     * Creates the login handler by saving its LoginServer and client it is communicating with.
     * Creates a new thread and starts it.
     * @param loginServer Instance of LoginServer it's connected to
     * @param clientSocket Socket of communicating client
     */
    public LoginHandler(LoginServer loginServer, Socket clientSocket) {
        this.LOGIN_SERVER = loginServer;
        this.CLIENT_SOCKET = clientSocket;

        waitingForRequest = true;
        Thread loginHandlerThread = new Thread(this);
        loginHandlerThread.start();
    }

    /**
     * Run method that listens for incoming requests from it's connected client, to handle them.
     */
    @Override
    public void run() {
        System.out.println("\nNew LoginHandler created for: " + CLIENT_SOCKET.getInetAddress().getHostAddress() + ". \nWaiting for client request.");
        while (waitingForRequest) {
            try {
                ois = new ObjectInputStream(CLIENT_SOCKET.getInputStream());
                RequestObject request = (RequestObject) ois.readObject();
                handleRequest(request);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error reading data.");
                e.printStackTrace();
                stopThread();
            }
        }
    }

    /**
     * Handles requests by getting the request type from the request and calls the appropriate method to perform
     * operations.
     * @param request RequestObject sent by the client
     */
    private void handleRequest(RequestObject request) {
        switch (request.getREQUEST_TYPE()) {
            case "login" -> loginUser(request);
            case "create" -> createAccount(request);
            case "reset" -> resetPassword(request);
        }
    }

    /**
     * Logs in a user by checking if it exists before it verifies the user.
     * If the users provided password is correct it will ask the login server to assign it a ClientHandler before
     * killing this thread safely.
     * @param requestObject RequestObject sent by the client
     */
    private void loginUser(RequestObject requestObject){
        boolean userFound = LOGIN_SERVER.findUser(requestObject.getUSERNAME());
        if (userFound){
            byte[] dbPassword = LOGIN_SERVER.getHashedPassword(requestObject.getUSERNAME());;
            boolean authenticated = PASSWORD_HANDLER.verifyPassword(requestObject.getPassword(), dbPassword);
            if (authenticated){
                System.out.println(Status.LOGGED_IN);
                LOGIN_SERVER.addLoggedInUser(this.CLIENT_SOCKET, requestObject.getUSERNAME());
                stopThread();
                //send to client that user is logged in
            } else {
                System.out.println(Status.LOG_IN_FAILED);
                //send to client that login failed
            }
        } else {
            userNotFoundMessage();
        }
    }

    /**
     * Method to create an account. If the user does not already exist the password provided will be hashed and a
     * request will be made to the LoginServer to add the username and password in the database.
     * @param requestObject
     */
    private void createAccount(RequestObject requestObject){
        boolean userFound = LOGIN_SERVER.findUser(requestObject.getUSERNAME());
        if (userFound){
            System.out.println(Status.USER_NAME_NOT_AVAILABLE);
            //send to client that username already exists
        } else {
            byte[] hashedPassword = PASSWORD_HANDLER.getHashedPassword(requestObject.getPassword());
            LOGIN_SERVER.createAccount(requestObject.getUSERNAME(), hashedPassword);
            System.out.println(Status.USER_ADDED);
            //send to client that account was successfully created
        }
    }

    /**
     * Method to reset password. If the user exists, the password will be updated in the database with the newly
     * received one.
     * @param requestObject RequestObject sent by the client
     */
    private void resetPassword(RequestObject requestObject){
        boolean userFound = LOGIN_SERVER.findUser(requestObject.getUSERNAME());
        if (userFound){
            byte[] hashedPassword = PASSWORD_HANDLER.getHashedPassword(requestObject.getPassword());
            boolean passwordReset = LOGIN_SERVER.resetPassword(requestObject.getUSERNAME(), hashedPassword);
            if (passwordReset){
                System.out.println(Status.PASSWORD_RESET);
                //send to client that password was successfully changed
            } else {
                System.out.println("Password couldn't be reset");
                //send to client that password was not changed
            }
        } else {
            userNotFoundMessage();
        }
    }

    /**
     * Prints status message if a user is not found in the servers terminal
     */
    private void userNotFoundMessage(){
        System.out.println(Status.USER_NOT_FOUND);
    }

    /**
     * Safely kills this thread. Used when clients disconnect or have logged in.
     */
    private void stopThread(){
        waitingForRequest = false;
    }
}
