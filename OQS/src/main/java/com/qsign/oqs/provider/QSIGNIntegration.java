package com.qsign.oqs.provider;

import com.qsign.oqs.OQSProvider;
import com.qsign.oqs.crypto.DilithiumSignature;
import com.qsign.oqs.crypto.KyberKEM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration layer for QSIGN IAM Platform
 *
 * Provides simplified API for integrating PQC algorithms
 * with Keycloak and other QSIGN components.
 */
public class QSIGNIntegration {

    private static final Logger logger = LoggerFactory.getLogger(QSIGNIntegration.class);
    private static boolean initialized = false;

    /**
     * Configuration options for QSIGN integration
     */
    public static class Config {
        private boolean enableHybridMode = true;
        private DilithiumSignature.DilithiumVariant signatureVariant = DilithiumSignature.DilithiumVariant.DILITHIUM3;
        private KyberKEM.KyberVariant kemVariant = KyberKEM.KyberVariant.KYBER1024;
        private boolean enableLogging = true;

        public Config setHybridMode(boolean enable) {
            this.enableHybridMode = enable;
            return this;
        }

        public Config setSignatureVariant(DilithiumSignature.DilithiumVariant variant) {
            this.signatureVariant = variant;
            return this;
        }

        public Config setKemVariant(KyberKEM.KyberVariant variant) {
            this.kemVariant = variant;
            return this;
        }

        public Config setLogging(boolean enable) {
            this.enableLogging = enable;
            return this;
        }

        public boolean isHybridMode() {
            return enableHybridMode;
        }

        public DilithiumSignature.DilithiumVariant getSignatureVariant() {
            return signatureVariant;
        }

        public KyberKEM.KyberVariant getKemVariant() {
            return kemVariant;
        }

        public boolean isLoggingEnabled() {
            return enableLogging;
        }
    }

    private static Config config = new Config();

    /**
     * Initialize QSIGN integration with OQS
     */
    public static synchronized void initialize() {
        initialize(new Config());
    }

    /**
     * Initialize QSIGN integration with custom configuration
     */
    public static synchronized void initialize(Config customConfig) {
        if (!initialized) {
            config = customConfig;

            logger.info("======================================================================");
            logger.info("   🔐 QSIGN-OQS Integration");
            logger.info("======================================================================");
            logger.info("   Hybrid Mode: {}", config.isHybridMode());
            logger.info("   Signature Algorithm: {}", config.getSignatureVariant().getAlgorithm());
            logger.info("   KEM Algorithm: {}", config.getKemVariant().getAlgorithm());

            // Install OQS Provider
            OQSProvider.install();

            // Verify installation
            if (Security.getProvider("OQS") != null) {
                logger.info("   ✅ OQS Provider registered successfully");
            } else {
                logger.error("   ❌ Failed to register OQS Provider");
            }

            initialized = true;
            logger.info("======================================================================");
        }
    }

    /**
     * Create a PQC signature provider for Keycloak
     */
    public static DilithiumSignature createSignatureProvider() {
        ensureInitialized();
        return new DilithiumSignature(config.getSignatureVariant());
    }

    /**
     * Create a PQC KEM provider
     */
    public static KyberKEM createKEMProvider() {
        ensureInitialized();
        return new KyberKEM(config.getKemVariant());
    }

    /**
     * Generate keys for QSIGN JWT signing
     *
     * @return Map containing "dilithium" KeyPair for PQC signing
     */
    public static Map<String, KeyPair> generateJWTSigningKeys() throws Exception {
        ensureInitialized();

        Map<String, KeyPair> keys = new HashMap<>();

        // Generate Dilithium key pair for PQC signing
        DilithiumSignature dilithium = createSignatureProvider();
        KeyPair dilithiumKeyPair = dilithium.generateKeyPair();
        keys.put("dilithium", dilithiumKeyPair);

        logger.info("Generated JWT signing keys:");
        logger.info("  - Dilithium3 public key: {} bytes", dilithiumKeyPair.getPublic().getEncoded().length);
        logger.info("  - Dilithium3 private key: {} bytes", dilithiumKeyPair.getPrivate().getEncoded().length);

        return keys;
    }

    /**
     * Generate keys for hybrid TLS
     *
     * @return Map containing both "kyber" and "dilithium" KeyPairs
     */
    public static Map<String, KeyPair> generateTLSKeys() throws Exception {
        ensureInitialized();

        Map<String, KeyPair> keys = new HashMap<>();

        // Generate Kyber key pair for key exchange
        KyberKEM kyber = createKEMProvider();
        KeyPair kyberKeyPair = kyber.generateKeyPair();
        keys.put("kyber", kyberKeyPair);

        // Generate Dilithium key pair for authentication
        DilithiumSignature dilithium = createSignatureProvider();
        KeyPair dilithiumKeyPair = dilithium.generateKeyPair();
        keys.put("dilithium", dilithiumKeyPair);

        logger.info("Generated TLS keys:");
        logger.info("  - Kyber1024 public key: {} bytes", kyberKeyPair.getPublic().getEncoded().length);
        logger.info("  - Dilithium3 public key: {} bytes", dilithiumKeyPair.getPublic().getEncoded().length);

        return keys;
    }

    /**
     * Get current configuration
     */
    public static Config getConfig() {
        return config;
    }

    /**
     * Check if integration is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Ensure the integration is initialized
     */
    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                "QSIGN-OQS integration not initialized. Call QSIGNIntegration.initialize() first."
            );
        }
    }

    /**
     * Get supported PQC algorithms
     */
    public static String[] getSupportedAlgorithms() {
        return new String[]{
            "KYBER512", "KYBER768", "KYBER1024",
            "DILITHIUM2", "DILITHIUM3", "DILITHIUM5"
        };
    }

    /**
     * Get algorithm information
     */
    public static String getAlgorithmInfo(String algorithm) {
        switch (algorithm.toUpperCase()) {
            case "KYBER512":
                return "KYBER512 - ML-KEM (Security Level 1, AES-128 equivalent)";
            case "KYBER768":
                return "KYBER768 - ML-KEM (Security Level 3, AES-192 equivalent)";
            case "KYBER1024":
                return "KYBER1024 - ML-KEM (Security Level 5, AES-256 equivalent)";
            case "DILITHIUM2":
                return "DILITHIUM2 - ML-DSA (Security Level 2, AES-128 equivalent)";
            case "DILITHIUM3":
                return "DILITHIUM3 - ML-DSA (Security Level 3, AES-192 equivalent)";
            case "DILITHIUM5":
                return "DILITHIUM5 - ML-DSA (Security Level 5, AES-256 equivalent)";
            default:
                return "Unknown algorithm";
        }
    }
}
