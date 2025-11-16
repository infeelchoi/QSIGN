package com.qsign.oqs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * Cryptographic utility functions for OQS
 */
public class CryptoUtils {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtils.class);

    /**
     * Encode a byte array to Base64 string
     */
    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decode a Base64 string to byte array
     */
    public static byte[] decodeBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Encode a public key to Base64 PEM format
     */
    public static String encodePEM(PublicKey publicKey, String algorithm) {
        String base64 = encodeBase64(publicKey.getEncoded());
        return String.format("-----BEGIN %s PUBLIC KEY-----\n%s\n-----END %s PUBLIC KEY-----",
            algorithm, base64, algorithm);
    }

    /**
     * Encode a private key to Base64 PEM format
     */
    public static String encodePEM(PrivateKey privateKey, String algorithm) {
        String base64 = encodeBase64(privateKey.getEncoded());
        return String.format("-----BEGIN %s PRIVATE KEY-----\n%s\n-----END %s PRIVATE KEY-----",
            algorithm, base64, algorithm);
    }

    /**
     * Hash data using SHA-256
     */
    public static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    /**
     * Hash data using SHA-512
     */
    public static byte[] sha512(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        return digest.digest(data);
    }

    /**
     * Generate a secure random byte array
     */
    public static byte[] generateRandomBytes(int length) {
        byte[] random = new byte[length];
        new SecureRandom().nextBytes(random);
        return random;
    }

    /**
     * Convert string to bytes using UTF-8
     */
    public static byte[] toBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Convert bytes to string using UTF-8
     */
    public static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Securely compare two byte arrays (constant-time)
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Generate a hybrid shared secret by combining classical and PQC secrets
     */
    public static byte[] combineSecrets(byte[] classicalSecret, byte[] pqcSecret)
            throws NoSuchAlgorithmException {

        // Concatenate both secrets
        byte[] combined = new byte[classicalSecret.length + pqcSecret.length];
        System.arraycopy(classicalSecret, 0, combined, 0, classicalSecret.length);
        System.arraycopy(pqcSecret, 0, combined, classicalSecret.length, pqcSecret.length);

        // Hash the combined secret with SHA-512
        byte[] hybridSecret = sha512(combined);

        logger.debug("Combined hybrid secret - Classical: {} bytes, PQC: {} bytes, Hybrid: {} bytes",
            classicalSecret.length, pqcSecret.length, hybridSecret.length);

        return hybridSecret;
    }

    /**
     * Format a byte array as hex string
     */
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Parse a hex string to byte array
     */
    public static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
