package com.qsign.oqs.crypto;

import org.bouncycastle.pqc.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;

/**
 * KYBER Key Encapsulation Mechanism (KEM) wrapper
 *
 * Provides quantum-resistant key exchange using NIST standardized
 * KYBER algorithm (ML-KEM).
 *
 * Supported variants:
 * - KYBER512:  Security Level 1 (AES-128 equivalent)
 * - KYBER768:  Security Level 3 (AES-192 equivalent)
 * - KYBER1024: Security Level 5 (AES-256 equivalent)
 */
public class KyberKEM {

    private static final Logger logger = LoggerFactory.getLogger(KyberKEM.class);

    public enum KyberVariant {
        KYBER512("KYBER512", 1632, 800),
        KYBER768("KYBER768", 2400, 1184),
        KYBER1024("KYBER1024", 3168, 1568);

        private final String algorithm;
        private final int publicKeySize;
        private final int secretKeySize;

        KyberVariant(String algorithm, int publicKeySize, int secretKeySize) {
            this.algorithm = algorithm;
            this.publicKeySize = publicKeySize;
            this.secretKeySize = secretKeySize;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public int getPublicKeySize() {
            return publicKeySize;
        }

        public int getSecretKeySize() {
            return secretKeySize;
        }
    }

    private final KyberVariant variant;

    public KyberKEM(KyberVariant variant) {
        this.variant = variant;
    }

    /**
     * Generate a new KYBER key pair
     */
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(
            variant.getAlgorithm(),
            BouncyCastlePQCProvider.PROVIDER_NAME
        );

        KeyPair keyPair = keyGen.generateKeyPair();

        logger.debug("Generated {} key pair - Public key: {} bytes, Private key: {} bytes",
            variant.getAlgorithm(),
            keyPair.getPublic().getEncoded().length,
            keyPair.getPrivate().getEncoded().length
        );

        return keyPair;
    }

    /**
     * Encapsulate: Generate a shared secret and encapsulate it with the public key
     *
     * @param publicKey Recipient's public key
     * @return SecretKey with encapsulated ciphertext
     */
    public SecretKeyWithEncapsulation encapsulate(PublicKey publicKey)
            throws GeneralSecurityException {

        KeyGenerator keyGen = KeyGenerator.getInstance(
            variant.getAlgorithm(),
            BouncyCastlePQCProvider.PROVIDER_NAME
        );

        keyGen.init(new SecureRandom());
        SecretKey secretKey = keyGen.generateKey();

        Cipher cipher = Cipher.getInstance(
            variant.getAlgorithm(),
            BouncyCastlePQCProvider.PROVIDER_NAME
        );

        cipher.init(Cipher.WRAP_MODE, publicKey, new SecureRandom());

        byte[] encapsulated = cipher.wrap(secretKey);

        logger.debug("Encapsulated shared secret - Ciphertext: {} bytes", encapsulated.length);

        return (SecretKeyWithEncapsulation) secretKey;
    }

    /**
     * Decapsulate: Extract the shared secret using the private key
     *
     * @param privateKey Recipient's private key
     * @param encapsulated Encapsulated ciphertext
     * @return Shared secret key
     */
    public SecretKey decapsulate(PrivateKey privateKey, byte[] encapsulated)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(
            variant.getAlgorithm(),
            BouncyCastlePQCProvider.PROVIDER_NAME
        );

        cipher.init(Cipher.UNWRAP_MODE, privateKey);

        SecretKey sharedSecret = (SecretKey) cipher.unwrap(
            encapsulated,
            variant.getAlgorithm(),
            Cipher.SECRET_KEY
        );

        logger.debug("Decapsulated shared secret - Key: {} bytes",
            sharedSecret.getEncoded().length);

        return sharedSecret;
    }

    /**
     * Get the KYBER variant being used
     */
    public KyberVariant getVariant() {
        return variant;
    }

    /**
     * Create a KYBER1024 instance (recommended for most use cases)
     */
    public static KyberKEM kyber1024() {
        return new KyberKEM(KyberVariant.KYBER1024);
    }

    /**
     * Create a KYBER768 instance
     */
    public static KyberKEM kyber768() {
        return new KyberKEM(KyberVariant.KYBER768);
    }

    /**
     * Create a KYBER512 instance
     */
    public static KyberKEM kyber512() {
        return new KyberKEM(KyberVariant.KYBER512);
    }
}
