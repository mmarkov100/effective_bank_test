package com.effective.bank.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CardSecurityUtil {

    @Value("${card.encryption.secret}")
    private String encryptionSecret;

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    public String encrypt(String cardNumber) {
        try {
            SecretKey key = getKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting card number: " + e.getMessage(), e);
        }
    }

    public String decrypt(String encryptedCardNumber) {
        try {
            SecretKey key = getKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = Base64.getDecoder().decode(encryptedCardNumber);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting card number", e);
        }
    }

    public String mask(String encryptedCardNumber) {
        try {
            String plainNumber = decrypt(encryptedCardNumber);
            String lastFour = plainNumber.substring(plainNumber.length() - 4);
            return "**** **** **** " + lastFour;
        } catch (Exception e) {
            return "**** **** **** ****";
        }
    }

    public String extractLastFour(String plainCardNumber) {
        if (plainCardNumber.length() >= 4) {
            return plainCardNumber.substring(plainCardNumber.length() - 4);
        }
        return plainCardNumber;
    }

    private SecretKey getKey() {
        byte[] keyBytes = encryptionSecret.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 16));
        return new SecretKeySpec(key, "AES");
    }
}
