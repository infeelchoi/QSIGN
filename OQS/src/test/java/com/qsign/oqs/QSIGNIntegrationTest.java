package com.qsign.oqs;

import com.qsign.oqs.provider.QSIGNIntegration;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QSIGN 통합 테스트
 *
 * QSIGN IAM 플랫폼과의 통합 기능을 검증합니다.
 * - JWT 서명 키 생성
 * - TLS 하이브리드 키 생성
 * - 설정 관리
 *
 * Test cases for QSIGN integration
 */
public class QSIGNIntegrationTest {

    /**
     * OQS 초기화 테스트
     *
     * QSIGNIntegration이 정상적으로 초기화되는지 확인합니다.
     * Provider 등록 및 알고리즘 설정이 완료되어야 합니다.
     */
    @Test
    public void testInitialization() {
        QSIGNIntegration.initialize();
        assertTrue(QSIGNIntegration.isInitialized());
    }

    /**
     * JWT 서명 키 생성 테스트
     *
     * Keycloak에서 사용할 JWT 서명용 Dilithium 키를 생성합니다.
     * JWT 토큰을 양자 내성 알고리즘으로 서명할 수 있습니다.
     */
    @Test
    public void testGenerateJWTSigningKeys() throws Exception {
        // QSIGN 통합 초기화
        QSIGNIntegration.initialize();

        // JWT 서명 키 생성
        Map<String, KeyPair> keys = QSIGNIntegration.generateJWTSigningKeys();

        // Dilithium 키가 생성되었는지 확인
        assertNotNull(keys);
        assertTrue(keys.containsKey("dilithium"));

        KeyPair dilithiumKeyPair = keys.get("dilithium");
        assertNotNull(dilithiumKeyPair.getPublic());
        assertNotNull(dilithiumKeyPair.getPrivate());

        // 키 크기 출력
        System.out.println("JWT 서명 키 생성 완료:");
        System.out.println("  Dilithium 공개키: " + dilithiumKeyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("  Dilithium 개인키: " + dilithiumKeyPair.getPrivate().getEncoded().length + " bytes");
    }

    /**
     * TLS 하이브리드 키 생성 테스트
     *
     * Q-TLS에서 사용할 하이브리드 키를 생성합니다.
     * - Kyber: 키 교환용
     * - Dilithium: 인증용
     */
    @Test
    public void testGenerateTLSKeys() throws Exception {
        // QSIGN 통합 초기화
        QSIGNIntegration.initialize();

        // TLS 하이브리드 키 생성
        Map<String, KeyPair> keys = QSIGNIntegration.generateTLSKeys();

        // Kyber와 Dilithium 키가 모두 생성되었는지 확인
        assertNotNull(keys);
        assertTrue(keys.containsKey("kyber"));
        assertTrue(keys.containsKey("dilithium"));

        KeyPair kyberKeyPair = keys.get("kyber");
        KeyPair dilithiumKeyPair = keys.get("dilithium");

        assertNotNull(kyberKeyPair.getPublic());
        assertNotNull(kyberKeyPair.getPrivate());
        assertNotNull(dilithiumKeyPair.getPublic());
        assertNotNull(dilithiumKeyPair.getPrivate());

        // 키 크기 출력
        System.out.println("TLS 하이브리드 키 생성 완료:");
        System.out.println("  Kyber 공개키: " + kyberKeyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("  Dilithium 공개키: " + dilithiumKeyPair.getPublic().getEncoded().length + " bytes");
    }

    /**
     * 사용자 정의 설정 테스트
     *
     * 하이브리드 모드, 로깅 등 사용자 정의 설정이
     * 올바르게 적용되는지 확인합니다.
     */
    @Test
    public void testCustomConfiguration() {
        // 사용자 정의 설정 생성
        QSIGNIntegration.Config config = new QSIGNIntegration.Config()
            .setHybridMode(true)    // 하이브리드 모드 활성화
            .setLogging(true);      // 로깅 활성화

        // 설정을 적용하여 초기화
        QSIGNIntegration.initialize(config);

        // 설정이 올바르게 적용되었는지 확인
        assertTrue(QSIGNIntegration.getConfig().isHybridMode());
        assertTrue(QSIGNIntegration.getConfig().isLoggingEnabled());
    }

    /**
     * 지원 알고리즘 조회 테스트
     *
     * OQS에서 지원하는 모든 PQC 알고리즘 목록과
     * 각 알고리즘의 정보를 확인합니다.
     */
    @Test
    public void testGetSupportedAlgorithms() {
        // 지원하는 알고리즘 목록 조회
        String[] algorithms = QSIGNIntegration.getSupportedAlgorithms();

        assertNotNull(algorithms);
        assertTrue(algorithms.length > 0);

        // 각 알고리즘 정보 출력
        System.out.println("지원하는 양자 안전 알고리즘:");
        for (String algo : algorithms) {
            System.out.println("  - " + algo + ": " + QSIGNIntegration.getAlgorithmInfo(algo));
        }
    }
}
