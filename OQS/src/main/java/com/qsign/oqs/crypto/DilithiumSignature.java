package com.qsign.oqs.crypto;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;

/**
 * DILITHIUM Digital Signature wrapper
 *
 * Provides quantum-resistant digital signatures using NIST standardized
 * DILITHIUM algorithm (ML-DSA).
 *
 * Supported variants:
 * - DILITHIUM2: Security Level 2 (AES-128 equivalent)
 * - DILITHIUM3: Security Level 3 (AES-192 equivalent)
 * - DILITHIUM5: Security Level 5 (AES-256 equivalent)
 */
public class DilithiumSignature {

    private static final Logger logger = LoggerFactory.getLogger(DilithiumSignature.class);

    public enum DilithiumVariant {
        DILITHIUM2("DILITHIUM2", 1312, 2528, 2420),
        DILITHIUM3("DILITHIUM3", 1952, 4000, 3293),
        DILITHIUM5("DILITHIUM5", 2592, 4864, 4595);

        private final String algorithm;
        private final int publicKeySize;
        private final int privateKeySize;
        private final int signatureSize;

        DilithiumVariant(String algorithm, int publicKeySize, int privateKeySize, int signatureSize) {
            this.algorithm = algorithm;
            this.publicKeySize = publicKeySize;
            this.privateKeySize = privateKeySize;
            this.signatureSize = signatureSize;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public int getPublicKeySize() {
            return publicKeySize;
        }

        public int getPrivateKeySize() {
            return privateKeySize;
        }

        public int getSignatureSize() {
            return signatureSize;
        }
    }

    private final DilithiumVariant variant;

    public DilithiumSignature(DilithiumVariant variant) {
        this.variant = variant;
    }

    /**
     * Generate a new DILITHIUM key pair
     */
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(
            variant.getAlgorithm(),
            BouncyCastlePQCProvider.PROVIDER_NAME
        );

        keyGen.initialize(new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        logger.debug("Generated {} key pair - Public key: {} bytes, Private key: {} bytes",
            variant.getAlgorithm(),
            keyPair.getPublic().getEncoded().length,
            keyPair.getPrivate().getEncoded().length
        );

        return keyPair;
    }

    /**
     * Sign a message with the private key
     *
     * @param privateKey Signer's private key
     * @param message Message to sign
     * @return Digital signature
     */
    public byte[] sign(PrivateKey privateKey, byte[] message)
            throws NoSuchAlgorithmException, NoSuchProviderException,
                   InvalidKeyException, SignatureException {

        Signature signature = Signature.getInstance(
            variant.getAlgorithm(),
            BouncyCastlePQCProvider.PROVIDER_NAME
        );

        signature.initSign(privateKey, new SecureRandom());
        signature.update(message);

        byte[] signatureBytes = signature.sign();

        logger.debug("Created {} signature - Message: {} bytes, Signature: {} bytes",
            variant.getAlgorithm(),
            message.length,
            signatureBytes.length
        );

        return signatureBytes;
    }

    /**
     * Verify a signature with the public key
     *
     * @param publicKey Signer's public key
     * @param message Original message
     * @param signatureBytes Signature to verify
     * @return true if signature is valid, false otherwise
     */
    public boolean verify(PublicKey publicKey, byte[] message, byte[] signatureBytes)
            throws NoSuchAlgorithmException, NoSuchProviderException,
                   InvalidKeyException, SignatureException {

        Signature signature = Signature.getInstance(
            variant.getAlgorithm(),
            BouncyCastlePQCProvider.PROVIDER_NAME
        );

        signature.initVerify(publicKey);
        signature.update(message);

        boolean isValid = signature.verify(signatureBytes);

        logger.debug("Verified {} signature - Valid: {}", variant.getAlgorithm(), isValid);

        return isValid;
    }

    /**
     * Get the DILITHIUM variant being used
     */
    public DilithiumVariant getVariant() {
        return variant;
    }

    /**
     * Create a DILITHIUM3 instance (recommended for most use cases)
     */
    public static DilithiumSignature dilithium3() {
        return new DilithiumSignature(DilithiumVariant.DILITHIUM3);
    }

    /**
     * Create a DILITHIUM5 instance (highest security)
     */
    public static DilithiumSignature dilithium5() {
        return new DilithiumSignature(DilithiumVariant.DILITHIUM5);
    }

    /**
     * Create a DILITHIUM2 instance (fastest)
     */
    public static DilithiumSignature dilithium2() {
        return new DilithiumSignature(DilithiumVariant.DILITHIUM2);
    }
}
