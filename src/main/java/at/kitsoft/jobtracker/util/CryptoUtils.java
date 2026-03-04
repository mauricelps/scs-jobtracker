package at.kitsoft.jobtracker.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    private static final String SECRET_KEY_PHRASE = "MySuperSecretSCS-TrackerKey-12345";
    private static final String ALGORITHM = "AES";

    /**
     * Erzeugt einen gültigen AES-Schlüssel aus unserem Passwort.
     */
    private static SecretKeySpec generateKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] key = SECRET_KEY_PHRASE.getBytes(StandardCharsets.UTF_8);
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // AES-128 benötigt einen 16-Byte-Schlüssel
        return new SecretKeySpec(key, ALGORITHM);
    }

    /**
     * Verschlüsselt einen String.
     * @param data Der zu verschlüsselnde Text (unser JSON-String).
     * @return Ein Base64-kodierter, verschlüsselter String.
     */
    public static String encrypt(String data) {
        try {
            SecretKeySpec secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            System.err.println("Fehler beim Verschlüsseln: " + e.getMessage());
            return null;
        }
    }

    /**
     * Entschlüsselt einen String.
     * @param encryptedData Der verschlüsselte Base64-String aus der Datei.
     * @return Der ursprüngliche, entschlüsselte Text (unser JSON-String).
     */
    public static String decrypt(String encryptedData) {
        try {
            SecretKeySpec secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Fehler beim Entschlüsseln (vielleicht eine alte/leere Datei?): " + e.getMessage());
            return null;
        }
    }

}
