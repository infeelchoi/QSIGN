package com.qsign.oqs;

import com.qsign.oqs.crypto.DilithiumSignature;
import com.qsign.oqs.provider.QSIGNIntegration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for Dilithium digital signatures
 */
public class DilithiumSignatureTest {

    @BeforeAll
    public static void setup() {
        QSIGNIntegration.initialize();
    }

    @Test
    public void testDilithium3KeyGeneration() throws Exception {
        DilithiumSignature dilithium = DilithiumSignature.dilithium3();
        KeyPair keyPair = dilithium.generateKeyPair();

        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());

        System.out.println("Dilithium3 Public Key Size: " + keyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("Dilithium3 Private Key Size: " + keyPair.getPrivate().getEncoded().length + " bytes");
    }

    @Test
    public void testDilithium3SignAndVerify() throws Exception {
        DilithiumSignature dilithium = DilithiumSignature.dilithium3();
        KeyPair keyPair = dilithium.generateKeyPair();

        String message = "Hello, Quantum-Safe World!";
        byte[] messageBytes = message.getBytes();

        // Sign the message
        byte[] signature = dilithium.sign(keyPair.getPrivate(), messageBytes);
        assertNotNull(signature);
        System.out.println("Signature Size: " + signature.length + " bytes");

        // Verify the signature
        boolean isValid = dilithium.verify(keyPair.getPublic(), messageBytes, signature);
        assertTrue(isValid, "Signature should be valid");
    }

    @Test
    public void testDilithium3InvalidSignature() throws Exception {
        DilithiumSignature dilithium = DilithiumSignature.dilithium3();
        KeyPair keyPair = dilithium.generateKeyPair();

        String message = "Original message";
        byte[] messageBytes = message.getBytes();

        byte[] signature = dilithium.sign(keyPair.getPrivate(), messageBytes);

        // Tamper with the message
        String tamperedMessage = "Tampered message";
        byte[] tamperedBytes = tamperedMessage.getBytes();

        // Verification should fail
        boolean isValid = dilithium.verify(keyPair.getPublic(), tamperedBytes, signature);
        assertFalse(isValid, "Signature should be invalid for tampered message");
    }

    @Test
    public void testDilithium2() throws Exception {
        DilithiumSignature dilithium = DilithiumSignature.dilithium2();
        KeyPair keyPair = dilithium.generateKeyPair();

        assertNotNull(keyPair);
        assertEquals(DilithiumSignature.DilithiumVariant.DILITHIUM2, dilithium.getVariant());
    }

    @Test
    public void testDilithium5() throws Exception {
        DilithiumSignature dilithium = DilithiumSignature.dilithium5();
        KeyPair keyPair = dilithium.generateKeyPair();

        assertNotNull(keyPair);
        assertEquals(DilithiumSignature.DilithiumVariant.DILITHIUM5, dilithium.getVariant());
    }
}
