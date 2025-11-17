package com.qsign.oqs;

import com.qsign.oqs.crypto.DilithiumSignature;
import com.qsign.oqs.provider.QSIGNIntegration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dilithium 디지털 서명 테스트
 *
 * DILITHIUM은 NIST 표준화된 양자 내성 디지털 서명 알고리즘입니다.
 * 이 테스트는 키 생성, 서명, 검증 기능을 검증합니다.
 *
 * Test cases for Dilithium digital signatures
 */
public class DilithiumSignatureTest {

    /**
     * 테스트 초기화
     * OQS Provider를 등록하고 QSIGN 통합을 초기화합니다.
     */
    @BeforeAll
    public static void setup() {
        QSIGNIntegration.initialize();
    }

    /**
     * Dilithium3 키 생성 테스트
     *
     * 공개키와 개인키가 정상적으로 생성되는지 확인합니다.
     * - 공개키: 약 1952 bytes
     * - 개인키: 약 4000 bytes
     */
    @Test
    public void testDilithium3KeyGeneration() throws Exception {
        // Dilithium3 인스턴스 생성
        DilithiumSignature dilithium = DilithiumSignature.dilithium3();

        // 키 쌍 생성
        KeyPair keyPair = dilithium.generateKeyPair();

        // 키가 올바르게 생성되었는지 확인
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());

        // 키 크기 출력
        System.out.println("Dilithium3 공개키 크기: " + keyPair.getPublic().getEncoded().length + " bytes");
        System.out.println("Dilithium3 개인키 크기: " + keyPair.getPrivate().getEncoded().length + " bytes");
    }

    /**
     * Dilithium3 서명 및 검증 테스트
     *
     * 메시지에 대한 디지털 서명을 생성하고 검증합니다.
     * 서명 크기는 약 3293 bytes입니다.
     */
    @Test
    public void testDilithium3SignAndVerify() throws Exception {
        // Dilithium3 인스턴스 및 키 쌍 생성
        DilithiumSignature dilithium = DilithiumSignature.dilithium3();
        KeyPair keyPair = dilithium.generateKeyPair();

        // 서명할 메시지
        String message = "Hello, Quantum-Safe World!";
        byte[] messageBytes = message.getBytes();

        // 메시지 서명
        byte[] signature = dilithium.sign(keyPair.getPrivate(), messageBytes);
        assertNotNull(signature);
        System.out.println("서명 크기: " + signature.length + " bytes");

        // 서명 검증
        boolean isValid = dilithium.verify(keyPair.getPublic(), messageBytes, signature);
        assertTrue(isValid, "서명이 유효해야 합니다");
    }

    /**
     * Dilithium3 변조된 메시지 서명 검증 테스트
     *
     * 원본 메시지에 대한 서명이 변조된 메시지에 대해서는
     * 유효하지 않음을 확인합니다.
     */
    @Test
    public void testDilithium3InvalidSignature() throws Exception {
        // Dilithium3 인스턴스 및 키 쌍 생성
        DilithiumSignature dilithium = DilithiumSignature.dilithium3();
        KeyPair keyPair = dilithium.generateKeyPair();

        // 원본 메시지에 서명
        String message = "Original message";
        byte[] messageBytes = message.getBytes();
        byte[] signature = dilithium.sign(keyPair.getPrivate(), messageBytes);

        // 메시지 변조
        String tamperedMessage = "Tampered message";
        byte[] tamperedBytes = tamperedMessage.getBytes();

        // 변조된 메시지에 대한 검증은 실패해야 함
        boolean isValid = dilithium.verify(keyPair.getPublic(), tamperedBytes, signature);
        assertFalse(isValid, "변조된 메시지에 대한 서명은 유효하지 않아야 합니다");
    }

    /**
     * Dilithium2 테스트 (보안 수준 2)
     *
     * Dilithium2는 가장 빠른 성능을 제공하며
     * AES-128과 동등한 보안 수준을 제공합니다.
     */
    @Test
    public void testDilithium2() throws Exception {
        DilithiumSignature dilithium = DilithiumSignature.dilithium2();
        KeyPair keyPair = dilithium.generateKeyPair();

        assertNotNull(keyPair);
        assertEquals(DilithiumSignature.DilithiumVariant.DILITHIUM2, dilithium.getVariant());
    }

    /**
     * Dilithium5 테스트 (보안 수준 5)
     *
     * Dilithium5는 최고 수준의 보안을 제공하며
     * AES-256과 동등한 보안 수준을 제공합니다.
     */
    @Test
    public void testDilithium5() throws Exception {
        DilithiumSignature dilithium = DilithiumSignature.dilithium5();
        KeyPair keyPair = dilithium.generateKeyPair();

        assertNotNull(keyPair);
        assertEquals(DilithiumSignature.DilithiumVariant.DILITHIUM5, dilithium.getVariant());
    }
}
