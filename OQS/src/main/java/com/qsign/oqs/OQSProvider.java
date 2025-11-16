package com.qsign.oqs;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Provider;
import java.security.Security;

/**
 * OQS (Open Quantum Safe) Provider for QSIGN Integration
 *
 * This provider integrates Post-Quantum Cryptography algorithms
 * for use with QSIGN IAM platform.
 *
 * Supported Algorithms:
 * - KYBER512, KYBER768, KYBER1024 (KEM)
 * - DILITHIUM2, DILITHIUM3, DILITHIUM5 (Signature)
 * - Hybrid modes (Classical + PQC)
 */
public class OQSProvider extends Provider {

    private static final Logger logger = LoggerFactory.getLogger(OQSProvider.class);
    private static final String PROVIDER_NAME = "OQS";
    private static final String VERSION = "1.0.0";
    private static final String INFO = "Open Quantum Safe Provider for QSIGN";

    private static boolean initialized = false;

    public OQSProvider() {
        super(PROVIDER_NAME, VERSION, INFO);
        initialize();
    }

    /**
     * Initialize the OQS provider and register BouncyCastle PQC algorithms
     */
    private void initialize() {
        if (!initialized) {
            logger.info("======================================================================");
            logger.info("   🛡️  Initializing OQS Provider");
            logger.info("   Open Quantum Safe for QSIGN");
            logger.info("======================================================================");
            logger.info("   Version: {}", VERSION);
            logger.info("   Provider: {}", PROVIDER_NAME);

            // Register BouncyCastle providers
            Security.addProvider(new BouncyCastleProvider());
            Security.addProvider(new BouncyCastlePQCProvider());

            // Register OQS algorithms
            registerAlgorithms();

            initialized = true;
            logger.info("   ✅ OQS Provider: INITIALIZED");
            logger.info("======================================================================");
        }
    }

    /**
     * Register PQC algorithms with the provider
     */
    private void registerAlgorithms() {
        // Key Exchange Mechanisms (KEM)
        put("KeyPairGenerator.KYBER512", "org.bouncycastle.pqc.jcajce.provider.kyber.BCKyberKeyPairGeneratorSpi$Kyber512");
        put("KeyPairGenerator.KYBER768", "org.bouncycastle.pqc.jcajce.provider.kyber.BCKyberKeyPairGeneratorSpi$Kyber768");
        put("KeyPairGenerator.KYBER1024", "org.bouncycastle.pqc.jcajce.provider.kyber.BCKyberKeyPairGeneratorSpi$Kyber1024");

        // Digital Signatures
        put("KeyPairGenerator.DILITHIUM2", "org.bouncycastle.pqc.jcajce.provider.dilithium.BCDilithiumKeyPairGeneratorSpi$Dilithium2");
        put("KeyPairGenerator.DILITHIUM3", "org.bouncycastle.pqc.jcajce.provider.dilithium.BCDilithiumKeyPairGeneratorSpi$Dilithium3");
        put("KeyPairGenerator.DILITHIUM5", "org.bouncycastle.pqc.jcajce.provider.dilithium.BCDilithiumKeyPairGeneratorSpi$Dilithium5");

        put("Signature.DILITHIUM2", "org.bouncycastle.pqc.jcajce.provider.dilithium.SignatureSpi$Dilithium2");
        put("Signature.DILITHIUM3", "org.bouncycastle.pqc.jcajce.provider.dilithium.SignatureSpi$Dilithium3");
        put("Signature.DILITHIUM5", "org.bouncycastle.pqc.jcajce.provider.dilithium.SignatureSpi$Dilithium5");

        // Cipher for KEM
        put("Cipher.KYBER", "org.bouncycastle.pqc.jcajce.provider.kyber.BCKyberCipherSpi$Base");

        logger.info("   ✅ Registered KYBER512, KYBER768, KYBER1024 (KEM)");
        logger.info("   ✅ Registered DILITHIUM2, DILITHIUM3, DILITHIUM5 (Signature)");
    }

    /**
     * Get the singleton instance of OQS Provider
     */
    public static OQSProvider getInstance() {
        return new OQSProvider();
    }

    /**
     * Install the OQS Provider as a security provider
     */
    public static void install() {
        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.addProvider(new OQSProvider());
            logger.info("OQS Provider installed successfully");
        } else {
            logger.info("OQS Provider already installed");
        }
    }

    /**
     * Check if OQS Provider is installed
     */
    public static boolean isInstalled() {
        return Security.getProvider(PROVIDER_NAME) != null;
    }

    /**
     * Get provider information
     */
    public static String getProviderInfo() {
        return String.format("%s v%s - %s", PROVIDER_NAME, VERSION, INFO);
    }
}
