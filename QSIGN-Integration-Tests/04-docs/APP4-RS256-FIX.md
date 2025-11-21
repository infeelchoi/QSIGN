# App4 RS256 서명 알고리즘 수정

## 문제

App4에서 로그인 callback 시 500 에러 발생:
```
unexpected JWT alg received, expected RS256, got: DILITHIUM3
```

## 원인

App4 클라이언트의 서명 알고리즘이 명시적으로 설정되지 않아, Keycloak이 기본값인 DILITHIUM3으로 JWT를 서명하고 있었습니다.

App4는 표준 JWT 라이브러리를 사용하여 RS256 알고리즘만 지원하므로, DILITHIUM3으로 서명된 토큰을 검증할 수 없습니다.

## 해결 방법

### 1. 수동 수정 (Keycloak Admin Console)

1. Keycloak Admin Console 접속: http://192.168.0.11:30181
2. PQC-realm → Clients → app4-client 선택
3. Settings → Advanced settings로 이동
4. 다음 설정 추가:
   - **Access token signature algorithm**: RS256
   - **ID token signature algorithm**: RS256  
   - **User info response signature algorithm**: RS256

### 2. 자동 수정 (스크립트)

```bash
bash /home/user/QSIGN/QSIGN-Integration-Tests/05-scripts/fix-app4-rs256.sh
```

## 설정된 내용

### 서명 알고리즘
- `access.token.signed.response.alg`: RS256
- `id.token.signed.response.alg`: RS256
- `user.info.response.signature.alg`: RS256

### Post Logout Redirect URIs
스크립트가 자동으로 다음 URI도 설정합니다:
- `http://192.168.0.11:30203` (루트)
- `http://192.168.0.11:30203/*` (모든 경로)
- `http://localhost:3003` (로컬 개발)
- `http://localhost:3003/*` (로컬 모든 경로)

## 참고

### App별 알고리즘 선택 기준

| App | 알고리즘 | 이유 |
|-----|---------|------|
| App1 | DILITHIUM3 | PQC 테스트용, liboqs 라이브러리 사용 |
| App2 | DILITHIUM3 | PQC 네이티브 지원 |
| App3 | DILITHIUM3 | PQC JWT 라이브러리 사용 |
| **App4** | **RS256** | 표준 JWT 라이브러리 (jsonwebtoken) 사용 |
| App5 | DILITHIUM3 | PQC 지원 라이브러리 사용 |

App4는 표준 JWT 라이브러리를 사용하므로 RS256을 사용해야 합니다.

## 수정 일시

- 2025-11-21

## 관련 파일

- 스크립트: [QSIGN-Integration-Tests/05-scripts/fix-app4-rs256.sh](../05-scripts/fix-app4-rs256.sh)
- 확인 스크립트: `/tmp/check-app4-signature-algorithm.sh`

## 테스트 방법

1. 브라우저에서 App4 페이지 새로고침
2. 로그인 버튼 클릭
3. Keycloak 로그인 페이지에서 인증
4. 성공적으로 callback 처리되고 토큰 표시 확인

## 추가 정보

Keycloak에서 클라이언트별로 서로 다른 서명 알고리즘을 사용할 수 있습니다:
- PQC 지원 앱: DILITHIUM3, FALCON, SPHINCS+
- 표준 앱: RS256, RS384, RS512, ES256, ES384, ES512

이를 통해 PQC 전환 기간 동안 하이브리드 환경을 지원할 수 있습니다.
