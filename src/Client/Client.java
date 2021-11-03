package Client;

import Objects.RequestObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * En mindre test client för att testa skicka requests till servern. Hanterar inte direkt att ta emot något eftersom
 * fokuset ligger på backend snarare än frontend.
 */
public class Client implements Runnable {

    private final Scanner INPUT = new Scanner(System.in);
    private static final String HOST_ADDRESS = "127.0.0.1";
    private static final int PORT = 2000;
    private Socket socket;
    private static boolean isRunning;
    private ObjectOutputStream oos;

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        try {
            socket = new Socket(HOST_ADDRESS, PORT);
            System.out.println("Connected to " + socket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        isRunning = true;
        Thread clientThread = new Thread(this);
        clientThread.start();
    }


    @Override
    public void run() {
        System.out.println("Please provide a command. To show commands type \"commands\":");
        while (isRunning) {
            String command = INPUT.nextLine();
            handleCommands(command);
        }
    }

    private void handleCommands(String command) {
        switch (command.toLowerCase()) {
            case "login", "create", "reset" -> sendAccountRequest(command.toLowerCase().trim());
            case "search", "delete", "logout" -> sendFunctionRequest(command.toLowerCase().trim());
            default -> printCommands();
        }
    }

    private void sendAccountRequest(String requestType) {
        System.out.println("Username: ");
        String username = INPUT.nextLine();
        System.out.println("Password: ");
        String password = INPUT.nextLine();
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            RequestObject request = new RequestObject(requestType, username, password);
            oos.writeObject(request);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFunctionRequest(String requestType) {
        System.out.println("Username:");
        String username = INPUT.nextLine();
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            RequestObject request = new RequestObject(requestType, username);
            oos.writeObject(request);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printCommands() {
        System.out.println("""
                The commands are:\s
                                
                While not logged in:
                    ---> Login (login user)
                    ---> Create (Create account)
                    ---> Reset (Reset password)
                                
                While logged in:
                    ---> Search (Search for user)
                    ---> Delete (Delete user)"""
        );
    }


}
