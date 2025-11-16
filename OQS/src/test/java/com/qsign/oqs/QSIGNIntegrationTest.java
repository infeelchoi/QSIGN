package com.qsign.oqs;

import com.qsign.oqs.provider.QSIGNIntegration;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for QSIGN integration
 */
public class QSIGNIntegrationTest {

    @Test
    public void testInitialization() {
        QSIGNIntegration.initialize();
        assertTrue(QSIGNIntegration.isInitialized());
    }

    @Test
    public void testGenerateJWTSigningKeys() throws Exception {
        QSIGNIntegration.initialize();

        Map<String, KeyPair> keys = QSIGNIntegration.generateJWTSigningKeys();

        assertNotNull(keys);
        assertTrue(keys.containsKey("dilithium"));

        KeyPair dilithiumKeyPair = keys.get("dilithium");
        assertNotNull(dilithiumKeyPair.getPublic());
        assertNotNull(dilithiumKeyPair.getPrivate());

        System.out.println("JWT Signing Keys Generated:");
        System.out.println("  Dilithium Public Key: " + dilithiumKeyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("  Dilithium Private Key: " + dilithiumKeyPair.getPrivate().getEncoded().length + " bytes");
    }

    @Test
    public void testGenerateTLSKeys() throws Exception {
        QSIGNIntegration.initialize();

        Map<String, KeyPair> keys = QSIGNIntegration.generateTLSKeys();

        assertNotNull(keys);
        assertTrue(keys.containsKey("kyber"));
        assertTrue(keys.containsKey("dilithium"));

        KeyPair kyberKeyPair = keys.get("kyber");
        KeyPair dilithiumKeyPair = keys.get("dilithium");

        assertNotNull(kyberKeyPair.getPublic());
        assertNotNull(kyberKeyPair.getPrivate());
        assertNotNull(dilithiumKeyPair.getPublic());
        assertNotNull(dilithiumKeyPair.getPrivate());

        System.out.println("TLS Keys Generated:");
        System.out.println("  Kyber Public Key: " + kyberKeyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("  Dilithium Public Key: " + dilithiumKeyPair.getPublic().getEncoded().length + " bytes");
    }

    @Test
    public void testCustomConfiguration() {
        QSIGNIntegration.Config config = new QSIGNIntegration.Config()
            .setHybridMode(true)
            .setLogging(true);

        QSIGNIntegration.initialize(config);

        assertTrue(QSIGNIntegration.getConfig().isHybridMode());
        assertTrue(QSIGNIntegration.getConfig().isLoggingEnabled());
    }

    @Test
    public void testGetSupportedAlgorithms() {
        String[] algorithms = QSIGNIntegration.getSupportedAlgorithms();

        assertNotNull(algorithms);
        assertTrue(algorithms.length > 0);

        System.out.println("Supported Algorithms:");
        for (String algo : algorithms) {
            System.out.println("  - " + algo + ": " + QSIGNIntegration.getAlgorithmInfo(algo));
        }
    }
}
