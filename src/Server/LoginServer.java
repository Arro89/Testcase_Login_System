package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * LoginServer that deals with all database communication. It utilizes separate handler threads (that communicate with
 * the clients) to determine what operations to perform and not.
 * (Servern kommunicerar via sockets just nu med klienten och skickar ett custom made object, men det går att ordna
 * så den kan skicka och ta emot JSON objekt, hade bara lite problem med att ta ned senaste simple json toolkitet.)
 */
public class LoginServer implements Runnable {
    private static final int port = 2000;
    private boolean isRunning;
    private ServerSocket serverSocket;
    private Connection dbConnection;
    private ArrayList<Socket> loginHandlerSocketList = new ArrayList<>();
    private HashMap<Socket, String> loggedInUsers = new HashMap<>();

    /**
     * Server constructor. Sets the server socket to a specific port to listen for incoming client connections and
     * starts the server thread.
     */
    public LoginServer() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Error creating server socket");
            e.printStackTrace();
            shutdown();
        }
        connectToDB();
        isRunning = true;
        Thread serverThread = new Thread(this);
        serverThread.start();
    }

    /**
     * Run method. While the thread is still running it keeps listening for clients wanting to connect to the server.
     * Connected clients are assigned a LoginHandler for future communication.
     */
    @Override
    public void run() {
        System.out.println("Server started. Waiting for new connection...");
        while (isRunning) {
            try {
                Socket acceptedSocket = serverSocket.accept();
                System.out.println("Socket accepted: " + acceptedSocket.getInetAddress().getHostAddress()
                        + ". Assigning new login handler.");
                assignLoginHandler(acceptedSocket);
            } catch (IOException e) {
                System.err.println("Error accepting socket");
                e.printStackTrace();
            }
        }
    }

    /**
     * Assigns each connected client a LoginHandler (separated thread) that handles all communication.
     * Connected clients are stored in an arraylist to keep track of what clients are not logged in.
     *
     * @param acceptedSocket Socket of the connected client
     */
    private void assignLoginHandler(Socket acceptedSocket) {
        loginHandlerSocketList.add(acceptedSocket);
        new LoginHandler(this, acceptedSocket);
    }

    /**
     * Adds an authenticated and logged-in user in a hashmap containing all online users.
     * Assigns the logged-in user a ClientHandler to handle future communication.
     *
     * @param clientSocket Socket of the client
     * @param username     String representing clients username
     */
    protected void addLoggedInUser(Socket clientSocket, String username) {
        removeUserFromLoggingInList(clientSocket);
        String role = getUserRole(username);
        loggedInUsers.put(clientSocket, role);
        new ClientHandler(this, clientSocket, role);
    }

    /**
     * Method used to remove logged out (or disconnected) users from the list of logged-in users.
     *
     * @param clientSocket Socket of logged in user
     */
    protected void logoutUser(Socket clientSocket) {
        loggedInUsers.remove(clientSocket);
        System.out.println("A user has been logged out");
    }

    /**
     * Removes socket from the list of users awaiting login.
     * Used when the client disconnects or has been authenticated and logged in.
     */
    protected void removeUserFromLoggingInList(Socket clientSocket) {
        loginHandlerSocketList.remove(clientSocket);
    }

    /**
     * Gets the role that a user has, which is used to control what operations they are allowed to perform in
     * the database.
     * (Currently, only admins are allowed to delete users)
     *
     * @param username String representing clients username
     * @return String of the users role
     */
    private String getUserRole(String username) {
        String userRole = "";
        String query = "SELECT userRole FROM usertestcase WHERE username = ?";
        try {
            PreparedStatement ps = dbConnection.prepareStatement(query);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userRole = rs.getString("userRole");
            }
        } catch (SQLException throwables) {
            System.err.println("Could not find role");
            throwables.printStackTrace();
        }
        return userRole;
    }

    /**
     * Fetches the hashed password from the database by selecting the password of the username provided.
     *
     * @param username String representing clients username
     * @return Bytearray of the hashed password
     */
    protected synchronized byte[] getHashedPassword(String username) {
        byte[] hashedPassword = null;
        String query = "SELECT password FROM usertestcase WHERE username = ?";
        try {
            PreparedStatement ps = dbConnection.prepareStatement(query);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                hashedPassword = rs.getBytes("password");
            }
        } catch (SQLException throwables) {
            System.err.println("Could not retrieve password");
            throwables.printStackTrace();
        }
        return hashedPassword;
    }

    /**
     * Resets the password by taking the clients username and the new password as a bytearray, and replaces the previous
     * password in the database with the new one.
     *
     * @param username    String representing clients username
     * @param newPassword Bytearray of the new password (hashed)
     * @return boolean value whether the password was changed or not.
     */
    protected synchronized boolean resetPassword(String username, byte[] newPassword) {
        boolean passChanged = false;
        String query = "UPDATE usertestcase SET password = ? WHERE username = ?";
        try {
            PreparedStatement ps = dbConnection.prepareStatement(query);
            ps.setBytes(1, newPassword);
            ps.setString(2, username);
            ps.executeUpdate();
            passChanged = true;
        } catch (SQLException throwables) {
            System.err.println("Could not reset password");
            throwables.printStackTrace();
        }
        return passChanged;
    }

    /**
     * Searches for a user in the database based on their username.
     * (Currently only returns boolean value, can be changed to return all info of the user if needed. Depends on what
     * it's supposed to be used for)
     *
     * @param username String representing clients username
     * @return Boolean value if the user was found. True if found, false if not found.
     */
    protected synchronized boolean findUser(String username) {
        boolean userFound = false;
        String query = "SELECT username FROM usertestcase WHERE username = ?";
        try {
            PreparedStatement ps = dbConnection.prepareStatement(query);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userFound = true;
            }
        } catch (SQLException throwables) {
            System.err.println(Status.USER_NOT_FOUND);
            throwables.printStackTrace();
        }
        return userFound;
    }

    /**
     * Creates a new account by storing the username, password and user role in the database.
     * User role is set as "user". Admin accounts cannot be created with this method to avoid giving any user the
     * possibility to create admin accounts.
     *
     * @param username String representing clients username
     * @param password Bytearray of the hashed password
     */
    protected synchronized void createAccount(String username, byte[] password) {
        String query = "INSERT INTO usertestcase (username, password, userRole) VALUES (?, ?, ?)";
        try {
            PreparedStatement ps = dbConnection.prepareStatement(query);
            ps.setString(1, username);
            ps.setBytes(2, password);
            ps.setString(3, "user");
            ps.executeUpdate();
        } catch (SQLException throwables) {
            System.err.println("Account creation failed");
            throwables.printStackTrace();
        }
    }

    /**
     * Deletes a user from the database based on the username. The row is completely removed from the database.
     * (Only usable by admin users).
     *
     * @param username String representing clients username
     */
    protected synchronized void deleteUser(String username) {
        String query = "DELETE FROM usertestcase where username = ?";
        try {
            PreparedStatement ps = dbConnection.prepareStatement(query);
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            System.err.println("Could not delete user from db");
            throwables.printStackTrace();
        }
    }

    /**
     * Creates a connection to a MySQL database with the url of the database, the username and password.
     * (Information removed since it's my private database on DSV).
     */
    private void connectToDB() {
        try {
            Class.forName("com.mysql.jdbc.Driver").getDeclaredConstructor().newInstance();
            String url = "jdbc:mysql://[db url]/[db namn]"; //borttagna privata uppgifter
            String username = ""; //borttagna privata uppgifter
            String password = ""; // borttaget - privata uppgifter
            dbConnection = DriverManager.getConnection(url, username, password);
            System.out.println("CONNECTED TO DATABASE: " + url + "\n");
        } catch (ReflectiveOperationException | SQLException err) {
            System.err.println("COULD NOT CONNECT TO DATABASE");
            err.printStackTrace();
        }
    }

    /**
     * Shuts down the server by closing the server socket and stopping the while-loop, which kills the thread safely.
     */
    private void shutdown() {
        System.out.println("Shutting down server.");
        isRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error when closing socket");
            e.printStackTrace();
        }
        System.exit(1);
    }

    /**
     * Starts the server.
     *
     * @param args
     */
    public static void main(String[] args) {
        new LoginServer();
    }
}
