import com.qsign.oqs.provider.QSIGNIntegration;
import com.qsign.oqs.crypto.DilithiumSignature;
import com.qsign.oqs.crypto.KyberKEM;

import java.security.KeyPair;
import java.util.Arrays;

/**
 * Simple example demonstrating OQS-Java usage
 *
 * Compile and run:
 *   javac -cp target/oqs-java-1.0.0-jar-with-dependencies.jar examples/SimpleExample.java
 *   java -cp target/oqs-java-1.0.0-jar-with-dependencies.jar:examples SimpleExample
 */
public class SimpleExample {

    public static void main(String[] args) throws Exception {
        System.out.println("======================================================================");
        System.out.println("   🛡️  OQS-Java Simple Example");
        System.out.println("======================================================================");
        System.out.println();

        // Initialize OQS provider
        QSIGNIntegration.initialize();

        // Example 1: Dilithium Digital Signature
        demonstrateDilithiumSignature();

        System.out.println();

        // Example 2: Kyber Key Encapsulation
        demonstrateKyberKEM();

        System.out.println();
        System.out.println("======================================================================");
        System.out.println("   ✅ All examples completed successfully");
        System.out.println("======================================================================");
    }

    private static void demonstrateDilithiumSignature() throws Exception {
        System.out.println("📝 Example 1: DILITHIUM3 Digital Signature");
        System.out.println("------------------------------------------");

        // Create Dilithium3 instance
        DilithiumSignature dilithium = DilithiumSignature.dilithium3();

        // Generate key pair
        System.out.println("Generating Dilithium3 key pair...");
        KeyPair keyPair = dilithium.generateKeyPair();
        System.out.println("  ✅ Public key:  " + keyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("  ✅ Private key: " + keyPair.getPrivate().getEncoded().length + " bytes");

        // Sign a message
        String message = "Hello, Quantum-Safe World!";
        System.out.println("\nSigning message: \"" + message + "\"");
        byte[] signature = dilithium.sign(keyPair.getPrivate(), message.getBytes());
        System.out.println("  ✅ Signature: " + signature.length + " bytes");

        // Verify signature
        System.out.println("\nVerifying signature...");
        boolean isValid = dilithium.verify(keyPair.getPublic(), message.getBytes(), signature);
        System.out.println("  ✅ Signature is " + (isValid ? "VALID" : "INVALID"));

        // Try with tampered message
        String tamperedMessage = "Tampered message!";
        System.out.println("\nVerifying with tampered message: \"" + tamperedMessage + "\"");
        boolean isInvalid = dilithium.verify(keyPair.getPublic(), tamperedMessage.getBytes(), signature);
        System.out.println("  ✅ Signature is " + (isInvalid ? "VALID" : "INVALID (as expected)"));
    }

    private static void demonstrateKyberKEM() throws Exception {
        System.out.println("🔐 Example 2: KYBER1024 Key Encapsulation");
        System.out.println("------------------------------------------");

        // Create Kyber1024 instance
        KyberKEM kyber = KyberKEM.kyber1024();

        // Alice generates key pair
        System.out.println("Alice: Generating Kyber1024 key pair...");
        KeyPair aliceKeyPair = kyber.generateKeyPair();
        System.out.println("  ✅ Alice's public key:  " + aliceKeyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("  ✅ Alice's private key: " + aliceKeyPair.getPrivate().getEncoded().length + " bytes");

        // Bob encapsulates a shared secret using Alice's public key
        System.out.println("\nBob: Encapsulating shared secret with Alice's public key...");
        var encapsulated = kyber.encapsulate(aliceKeyPair.getPublic());
        byte[] ciphertext = encapsulated.getEncapsulation();
        byte[] bobSharedSecret = encapsulated.getEncoded();
        System.out.println("  ✅ Ciphertext: " + ciphertext.length + " bytes");
        System.out.println("  ✅ Bob's shared secret: " + bobSharedSecret.length + " bytes");

        // Alice decapsulates to get the same shared secret
        System.out.println("\nAlice: Decapsulating shared secret...");
        var aliceSharedSecret = kyber.decapsulate(aliceKeyPair.getPrivate(), ciphertext);
        System.out.println("  ✅ Alice's shared secret: " + aliceSharedSecret.getEncoded().length + " bytes");

        // Verify both have the same shared secret
        boolean secretsMatch = Arrays.equals(bobSharedSecret, aliceSharedSecret.getEncoded());
        System.out.println("\nVerifying shared secrets match...");
        System.out.println("  ✅ Secrets " + (secretsMatch ? "MATCH" : "DO NOT MATCH"));
    }
}
