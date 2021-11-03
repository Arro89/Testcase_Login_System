package Server;

import Objects.RequestObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * Client handler thread that handles all incoming requests from logged-in users.
 */
public class ClientHandler implements Runnable {
    private final LoginServer LOGIN_SERVER;
    private final Socket SOCKET;
    private final String ROLE;
    private ObjectInputStream ois;
    boolean waitingForRequest;

    /**
     * Constructor for the client handler. Keeps track of the login server, the socket its communicating with, and the
     * users' role (to prevent certain operations in the DB). Starts the ClientHandler as a separate thread.
     *
     * @param loginServer Instance of the loginServer
     * @param socket      Socket of the logged in client
     * @param role        String of the clients role
     */
    public ClientHandler(LoginServer loginServer, Socket socket, String role) {
        this.LOGIN_SERVER = loginServer;
        this.SOCKET = socket;
        this.ROLE = role;

        waitingForRequest = true;
        Thread clientHandlerThread = new Thread(this);
        clientHandlerThread.start();
    }

    /**
     * Run method that listens to incoming requests from the client to handle them.
     * Listens for an object of the type RequestObject (can be changed for something else depending on what the client
     * wants to communicate)
     */
    @Override
    public void run() {
        System.out.println("Waiting for user request");
        while (waitingForRequest) {
            try {
                ois = new ObjectInputStream(SOCKET.getInputStream());
                RequestObject request = (RequestObject) ois.readObject();
                handleRequest(request);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Could not read object");
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles a client request by controlling what they want to do before performing that operation.
     *
     * @param request String of the request type sent from the client.
     */
    private void handleRequest(RequestObject request) {
        switch (request.getREQUEST_TYPE().toLowerCase()) {
            case "search" -> searchForUser(request.getUSERNAME());
            case "delete" -> deleteUser(request.getUSERNAME());
            case "logout" -> logout(SOCKET);
        }
    }

    /**
     * Method used when a client makes a search request of a user.
     * Currently, the server only prints to itself whether a user was found or not.
     *
     * @param username String of the username
     */
    private void searchForUser(String username) {
        boolean userFound = LOGIN_SERVER.findUser(username);
        System.out.println((userFound) ? Status.USER_FOUND : Status.USER_NOT_FOUND);
    }

    /**
     * Method to handle removal of users from the database.
     * (Only admins are currently allowed to perform this operation).
     *
     * @param username String of the username of user to be deleted
     */
    private void deleteUser(String username) {
        boolean userFound = LOGIN_SERVER.findUser(username);
        if (userFound && this.ROLE.equals("admin")) {
            LOGIN_SERVER.deleteUser(username);
            System.out.println(Status.USER_REMOVED);
        } else {
            System.out.println("Could not delete user");
        }
    }

    /**
     * Logs out a user by asking the LoginServer to remove the client from the list of logged-in users, then finally
     * kills the thread safely.
     *
     * @param socket Socket of user that logs out.
     */
    private void logout(Socket socket) {
        LOGIN_SERVER.logoutUser(socket);
        waitingForRequest = false;
    }
}
