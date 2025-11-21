# App3 로그아웃 URI 수정

## 문제

App3에서 로그아웃 시 "Invalid redirect uri" 에러 발생

## 원인

App3 클라이언트의 `post.logout.redirect.uris` 속성이 빈 값("+" 만 설정됨)으로 설정되어 있음

## 해결 방법

### 1. 수동 수정 (Keycloak Admin Console)

1. Keycloak Admin Console 접속: http://192.168.0.11:30181
2. PQC-realm → Clients → app3-client 선택
3. Settings → Advanced settings로 이동
4. "Valid post logout redirect URIs" 필드에 다음 값 입력 (## 구분자 사용):
   ```
   http://localhost:3002##http://localhost:3002/*##http://192.168.0.11:30202##http://192.168.0.11:30202/*##http://localhost:4202##http://localhost:4202/*
   ```

### 2. 자동 수정 (스크립트)

```bash
bash /home/user/QSIGN/QSIGN-Integration-Tests/05-scripts/fix-app3-logout-uri.sh
```

## 설정된 URI

로그아웃 후 다음 URI로 리다이렉트 가능:

1. `http://localhost:3002` - 로컬 개발 환경 (루트)
2. `http://localhost:3002/*` - 로컬 개발 환경 (모든 경로)
3. `http://192.168.0.11:30202` - 배포 환경 (루트)
4. `http://192.168.0.11:30202/*` - 배포 환경 (모든 경로)
5. `http://localhost:4202` - 대체 로컬 포트 (루트)
6. `http://localhost:4202/*` - 대체 로컬 포트 (모든 경로)

## 참고

- Keycloak에서 여러 URI는 `##` 구분자로 연결
- 와일드카드 `/*`를 사용하면 모든 하위 경로 허용
- `post.logout.redirect.uris`는 클라이언트 속성(attributes)에 저장됨

## 수정 일시

- 2025-11-21

## 관련 파일

- 스크립트: [QSIGN-Integration-Tests/05-scripts/fix-app3-logout-uri.sh](../05-scripts/fix-app3-logout-uri.sh)
