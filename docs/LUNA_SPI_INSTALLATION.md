# Luna SPI Installation Guide

## ✅ 현재 상태
- ✅ libstdc++.so.6 설치 완료
- ✅ LunaProvider.jar 복사 완료
- ✅ Luna K7 HSM (PCI 장치) 연결 확인
- ❌ Luna SPI JAR 빌드 필요

---

## 🚀 설치 단계

### 1. Maven 설치 (터미널에서 실행)

```bash
sudo apt-get update
sudo apt-get install -y maven
```

확인:
```bash
mvn --version
```

---

### 2. Luna SPI JAR 빌드

```bash
cd /home/user/QSIGN/apisix-keycloack-Vaultkms-hsm/keycloak-luna-hsm-provider
./build.sh
```

또는 수동으로:
```bash
mvn clean package -DskipTests
```

빌드 결과 확인:
```bash
ls -lh target/keycloak-spi-luna-keystore-1.1.0.jar
```

---

### 3. JAR 복사

```bash
cp target/keycloak-spi-luna-keystore-1.1.0.jar /home/user/QSIGN/keycloak-hsm/providers/
```

---

### 4. Luna 라이브러리 심볼릭 링크 생성

```bash
sudo ln -sf /usr/safenet/lunaclient/lib/libCryptoki2_64.so /usr/safenet/lunaclient/lib/libCryptoki2.so
```

---

### 5. Keycloak Pod 재시작

```bash
kubectl --kubeconfig=/home/user/.kube/config rollout restart deployment/keycloak -n pqc-sso
kubectl --kubeconfig=/home/user/.kube/config rollout status deployment/keycloak -n pqc-sso
```

---

### 6. 확인

Keycloak에 Luna SPI가 로드되었는지 확인:

```bash
kubectl --kubeconfig=/home/user/.kube/config exec -n pqc-sso deployment/keycloak -- ls -la /opt/keycloak/providers/ | grep luna
```

예상 출력:
```
-rw-r--r-- 1 root root 842223 Nov 12 09:03 LunaProvider.jar
-rw-r--r-- 1 root root XXXXXX Nov 12 XX:XX keycloak-spi-luna-keystore-1.1.0.jar
```

---

## 🌐 Keycloak Admin UI에서 확인

1. 브라우저에서 접속: **http://192.168.0.11:8080/admin/**

2. 로그인:
   - Username: `admin`
   - Password: `admin123!@#`

3. **Realm Settings** > **Keys** 이동

4. **Providers** 탭 선택

5. **Add provider** 클릭

6. 드롭다운에서 **luna-keystore** 선택 가능 확인

---

## 📝 Luna Keystore Provider 설정

luna-keystore를 선택한 후 다음 정보 입력:

| 필드 | 값 | 설명 |
|------|-----|------|
| **Priority** | `100` | 최우선 순위 |
| **Keystore** | `/opt/lunastore` | Luna keystore 파일 경로 |
| **Keystore Password** | `[파티션 비밀번호]` | Luna HSM 파티션 암호 |
| **Key Alias** | `keycloak-key` | HSM에 생성된 키 별칭 |
| **Key Password** | `[키 비밀번호]` | 키 암호 |

---

## 🔧 Luna HSM 파티션 확인 (선택사항)

Luna HSM 파티션이 준비되었는지 확인:

```bash
export LD_LIBRARY_PATH=/usr/safenet/lunaclient/lib:$LD_LIBRARY_PATH
/usr/safenet/lunaclient/bin/lunacm
```

lunacm에서:
```
lunacm> slot list
lunacm> quit
```

---

## ⚠️ 문제 해결

### Maven 빌드 실패 시

Keycloak 버전 불일치 오류가 발생하면, pom.xml에서 Keycloak 버전을 23.0으로 수정:

```bash
cd /home/user/QSIGN/apisix-keycloack-Vaultkms-hsm/keycloak-luna-hsm-provider
sed -i 's/<keycloak.version>.*<\/keycloak.version>/<keycloak.version>23.0.0<\/keycloak.version>/' pom.xml
mvn clean package -DskipTests
```

### Luna HSM 연결 실패 시

1. PCI 장치 확인:
```bash
lspci | grep Luna
```

2. 디바이스 파일 확인:
```bash
ls -la /dev/k7pf0
```

3. Chrystoki.conf 설정 확인:
```bash
cat /etc/Chrystoki.conf
```

---

## 📞 지원

문제 발생 시:
- Luna Client 로그: `/var/log/luna/`
- Keycloak 로그: `kubectl logs -n pqc-sso -l app=keycloak`
- Thales 지원 포털: https://supportportal.thalesgroup.com

---

## 🎯 다음 단계

Luna keystore가 Admin UI에 나타나면:
1. Luna HSM에서 키 생성
2. Keycloak realm에서 Luna keystore provider 설정
3. JWT 토큰이 Luna HSM 키로 서명되는지 확인

---

**작성일**: 2025-11-12
**Keycloak 버전**: 23.0
**Luna Client 버전**: 10.9.1
