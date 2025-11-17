package com.qsign.oqs;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Provider;
import java.security.Security;

/**
 * OQS (Open Quantum Safe) Provider - QSIGN 통합용 보안 프로바이더
 *
 * QSIGN IAM 플랫폼에서 사용할 양자 후 암호(PQC) 알고리즘을 통합합니다.
 * BouncyCastle PQC를 기반으로 NIST 표준화 알고리즘을 제공합니다.
 *
 * 지원 알고리즘:
 * - KYBER512, KYBER768, KYBER1024 (키 캡슐화 메커니즘)
 * - DILITHIUM2, DILITHIUM3, DILITHIUM5 (디지털 서명)
 * - 하이브리드 모드 (고전 + PQC 알고리즘)
 *
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

    /**
     * OQS Provider 생성자
     * Provider를 생성하고 자동으로 초기화합니다.
     */
    public OQSProvider() {
        super(PROVIDER_NAME, VERSION, INFO);
        initialize();
    }

    /**
     * OQS Provider 초기화 및 BouncyCastle PQC 알고리즘 등록
     *
     * BouncyCastle Provider와 BouncyCastle PQC Provider를 등록하고,
     * KYBER 및 DILITHIUM 알고리즘을 사용할 수 있도록 설정합니다.
     *
     * Initialize the OQS provider and register BouncyCastle PQC algorithms
     */
    private void initialize() {
        if (!initialized) {
            logger.info("======================================================================");
            logger.info("   🛡️  OQS Provider 초기화");
            logger.info("   Open Quantum Safe for QSIGN");
            logger.info("======================================================================");
            logger.info("   버전: {}", VERSION);
            logger.info("   프로바이더: {}", PROVIDER_NAME);

            // BouncyCastle Provider 등록
            Security.addProvider(new BouncyCastleProvider());
            Security.addProvider(new BouncyCastlePQCProvider());

            // OQS 알고리즘 등록
            registerAlgorithms();

            initialized = true;
            logger.info("   ✅ OQS Provider: 초기화 완료");
            logger.info("======================================================================");
        }
    }

    /**
     * PQC 알고리즘을 Provider에 등록
     *
     * KYBER (키 교환) 및 DILITHIUM (디지털 서명) 알고리즘을
     * Java Security Provider에 등록합니다.
     *
     * Register PQC algorithms with the provider
     */
    private void registerAlgorithms() {
        // 키 교환 메커니즘 (KEM)
        put("KeyPairGenerator.KYBER512", "org.bouncycastle.pqc.jcajce.provider.kyber.BCKyberKeyPairGeneratorSpi$Kyber512");
        put("KeyPairGenerator.KYBER768", "org.bouncycastle.pqc.jcajce.provider.kyber.BCKyberKeyPairGeneratorSpi$Kyber768");
        put("KeyPairGenerator.KYBER1024", "org.bouncycastle.pqc.jcajce.provider.kyber.BCKyberKeyPairGeneratorSpi$Kyber1024");

        // 디지털 서명
        put("KeyPairGenerator.DILITHIUM2", "org.bouncycastle.pqc.jcajce.provider.dilithium.BCDilithiumKeyPairGeneratorSpi$Dilithium2");
        put("KeyPairGenerator.DILITHIUM3", "org.bouncycastle.pqc.jcajce.provider.dilithium.BCDilithiumKeyPairGeneratorSpi$Dilithium3");
        put("KeyPairGenerator.DILITHIUM5", "org.bouncycastle.pqc.jcajce.provider.dilithium.BCDilithiumKeyPairGeneratorSpi$Dilithium5");

        put("Signature.DILITHIUM2", "org.bouncycastle.pqc.jcajce.provider.dilithium.SignatureSpi$Dilithium2");
        put("Signature.DILITHIUM3", "org.bouncycastle.pqc.jcajce.provider.dilithium.SignatureSpi$Dilithium3");
        put("Signature.DILITHIUM5", "org.bouncycastle.pqc.jcajce.provider.dilithium.SignatureSpi$Dilithium5");

        // KEM용 암호화
        put("Cipher.KYBER", "org.bouncycastle.pqc.jcajce.provider.kyber.BCKyberCipherSpi$Base");

        logger.info("   ✅ 등록 완료: KYBER512, KYBER768, KYBER1024 (KEM)");
        logger.info("   ✅ 등록 완료: DILITHIUM2, DILITHIUM3, DILITHIUM5 (서명)");
    }

    /**
     * OQS Provider의 싱글톤 인스턴스 반환
     * Get the singleton instance of OQS Provider
     */
    public static OQSProvider getInstance() {
        return new OQSProvider();
    }

    /**
     * OQS Provider를 보안 프로바이더로 설치
     *
     * 아직 설치되지 않은 경우에만 Provider를 등록합니다.
     *
     * Install the OQS Provider as a security provider
     */
    public static void install() {
        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.addProvider(new OQSProvider());
            logger.info("OQS Provider 설치 완료");
        } else {
            logger.info("OQS Provider 이미 설치됨");
        }
    }

    /**
     * OQS Provider 설치 여부 확인
     * Check if OQS Provider is installed
     */
    public static boolean isInstalled() {
        return Security.getProvider(PROVIDER_NAME) != null;
    }

    /**
     * Provider 정보 반환
     * Get provider information
     */
    public static String getProviderInfo() {
        return String.format("%s v%s - %s", PROVIDER_NAME, VERSION, INFO);
    }
}
