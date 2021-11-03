package Security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Class that handles passwords. Can hash passwords (using simple SHA256 hash with no salt) and verify password.
 */
public class PasswordHandler {

    /**
     * Method used to hash a password to avoid saving it in plaintext in the database.
     * @param password String representing the password
     * @return Bytearray of the hashed password
     */
    public byte[] getHashedPassword(String password){
        return hashPassword(password);
    }

    /**
     * Verifies whether a password is correct or not by hashing the password received from a client, and the hashed
     * password saved in the database.
     * @param receivedPassword String of the password provided by the client
     * @param expectedPassword Bytearray of the password saved in the database
     * @return Boolean value if the password was correct or not
     */
    public boolean verifyPassword(String receivedPassword, byte[] expectedPassword){
        byte[] passwordBytes = hashPassword(receivedPassword);
        return Arrays.equals(passwordBytes, expectedPassword);
    }

    /**
     * Method that hashes a String representation of a password and returns the hashed one.
     * @param password String of the password
     * @return Bytearray of hashed password
     */
    private byte[] hashPassword(String password){
        byte[]  hashedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA256");
            md.update(password.getBytes(StandardCharsets.UTF_8));
            hashedPassword = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashedPassword;
    }

}
