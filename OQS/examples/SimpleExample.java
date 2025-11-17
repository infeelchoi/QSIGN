import com.qsign.oqs.provider.QSIGNIntegration;
import com.qsign.oqs.crypto.DilithiumSignature;
import com.qsign.oqs.crypto.KyberKEM;

import java.security.KeyPair;
import java.util.Arrays;

/**
 * OQS-Java 사용 예제
 *
 * Dilithium 디지털 서명과 Kyber 키 캡슐화를 시연합니다.
 *
 * 컴파일 및 실행:
 *   javac -cp target/oqs-java-1.0.0-jar-with-dependencies.jar examples/SimpleExample.java
 *   java -cp target/oqs-java-1.0.0-jar-with-dependencies.jar:examples SimpleExample
 *
 * Simple example demonstrating OQS-Java usage
 *
 * Compile and run:
 *   javac -cp target/oqs-java-1.0.0-jar-with-dependencies.jar examples/SimpleExample.java
 *   java -cp target/oqs-java-1.0.0-jar-with-dependencies.jar:examples SimpleExample
 */
public class SimpleExample {

    public static void main(String[] args) throws Exception {
        System.out.println("======================================================================");
        System.out.println("   🛡️  OQS-Java 간단한 예제");
        System.out.println("   🛡️  OQS-Java Simple Example");
        System.out.println("======================================================================");
        System.out.println();

        // OQS Provider 초기화
        // Initialize OQS provider
        QSIGNIntegration.initialize();

        // 예제 1: Dilithium 디지털 서명
        // Example 1: Dilithium Digital Signature
        demonstrateDilithiumSignature();

        System.out.println();

        // 예제 2: Kyber 키 캡슐화
        // Example 2: Kyber Key Encapsulation
        demonstrateKyberKEM();

        System.out.println();
        System.out.println("======================================================================");
        System.out.println("   ✅ 모든 예제가 성공적으로 완료되었습니다");
        System.out.println("   ✅ All examples completed successfully");
        System.out.println("======================================================================");
    }

    /**
     * Dilithium3 디지털 서명 예제
     *
     * 메시지를 서명하고 검증하는 과정을 시연합니다.
     * 변조된 메시지에 대한 서명 검증도 확인합니다.
     */
    private static void demonstrateDilithiumSignature() throws Exception {
        System.out.println("📝 예제 1: DILITHIUM3 디지털 서명");
        System.out.println("📝 Example 1: DILITHIUM3 Digital Signature");
        System.out.println("------------------------------------------");

        // Dilithium3 인스턴스 생성
        DilithiumSignature dilithium = DilithiumSignature.dilithium3();

        // 키 쌍 생성
        System.out.println("Dilithium3 키 쌍 생성 중... (Generating Dilithium3 key pair...)");
        KeyPair keyPair = dilithium.generateKeyPair();
        System.out.println("  ✅ 공개키 (Public key):  " + keyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("  ✅ 개인키 (Private key): " + keyPair.getPrivate().getEncoded().length + " bytes");

        // 메시지 서명
        String message = "안녕하세요, 양자 안전 세상! (Hello, Quantum-Safe World!)";
        System.out.println("\n메시지 서명 중 (Signing message): \"" + message + "\"");
        byte[] signature = dilithium.sign(keyPair.getPrivate(), message.getBytes());
        System.out.println("  ✅ 서명 (Signature): " + signature.length + " bytes");

        // 서명 검증
        System.out.println("\n서명 검증 중... (Verifying signature...)");
        boolean isValid = dilithium.verify(keyPair.getPublic(), message.getBytes(), signature);
        System.out.println("  ✅ 서명이 " + (isValid ? "유효합니다 (VALID)" : "유효하지 않습니다 (INVALID)"));

        // 변조된 메시지로 검증
        String tamperedMessage = "변조된 메시지! (Tampered message!)";
        System.out.println("\n변조된 메시지로 검증 중 (Verifying with tampered message): \"" + tamperedMessage + "\"");
        boolean isInvalid = dilithium.verify(keyPair.getPublic(), tamperedMessage.getBytes(), signature);
        System.out.println("  ✅ 서명이 " + (isInvalid ? "유효합니다 (VALID)" : "유효하지 않습니다 - 예상대로 (INVALID - as expected)"));
    }

    /**
     * Kyber1024 키 캡슐화 예제
     *
     * Alice와 Bob이 양자 안전 키 교환을 통해
     * 동일한 공유 비밀을 얻는 과정을 시연합니다.
     */
    private static void demonstrateKyberKEM() throws Exception {
        System.out.println("🔐 예제 2: KYBER1024 키 캡슐화");
        System.out.println("🔐 Example 2: KYBER1024 Key Encapsulation");
        System.out.println("------------------------------------------");

        // Kyber1024 인스턴스 생성
        KyberKEM kyber = KyberKEM.kyber1024();

        // Alice가 키 쌍 생성
        System.out.println("Alice: Kyber1024 키 쌍 생성 중... (Generating Kyber1024 key pair...)");
        KeyPair aliceKeyPair = kyber.generateKeyPair();
        System.out.println("  ✅ Alice의 공개키 (Alice's public key):  " + aliceKeyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("  ✅ Alice의 개인키 (Alice's private key): " + aliceKeyPair.getPrivate().getEncoded().length + " bytes");

        // Bob이 Alice의 공개키로 공유 비밀을 캡슐화
        System.out.println("\nBob: Alice의 공개키로 공유 비밀 캡슐화 중...");
        System.out.println("Bob: Encapsulating shared secret with Alice's public key...");
        var encapsulated = kyber.encapsulate(aliceKeyPair.getPublic());
        byte[] ciphertext = encapsulated.getEncapsulation();
        byte[] bobSharedSecret = encapsulated.getEncoded();
        System.out.println("  ✅ 암호문 (Ciphertext): " + ciphertext.length + " bytes");
        System.out.println("  ✅ Bob의 공유 비밀 (Bob's shared secret): " + bobSharedSecret.length + " bytes");

        // Alice가 자신의 개인키로 공유 비밀을 복원
        System.out.println("\nAlice: 공유 비밀 역캡슐화 중... (Decapsulating shared secret...)");
        var aliceSharedSecret = kyber.decapsulate(aliceKeyPair.getPrivate(), ciphertext);
        System.out.println("  ✅ Alice의 공유 비밀 (Alice's shared secret): " + aliceSharedSecret.getEncoded().length + " bytes");

        // 두 비밀이 동일한지 확인
        boolean secretsMatch = Arrays.equals(bobSharedSecret, aliceSharedSecret.getEncoded());
        System.out.println("\n공유 비밀 일치 확인 중... (Verifying shared secrets match...)");
        System.out.println("  ✅ 비밀이 " + (secretsMatch ? "일치합니다 (MATCH)" : "일치하지 않습니다 (DO NOT MATCH)"));
    }
}
