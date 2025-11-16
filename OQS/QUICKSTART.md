# OQS-Java Quick Start Guide

## 🚀 Getting Started

### Prerequisites

Make sure you have the following installed:

```bash
# Check Java version (required: 17+)
java -version

# Check Maven version (required: 3.6+)
mvn -version
```

If Maven is not installed:

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install maven

# macOS
brew install maven

# Or use Docker (see below)
```

### Option 1: Build with Maven

```bash
cd /home/user/QSIGN/OQS

# Clean and build
mvn clean package

# Run tests
mvn test

# Build fat JAR with all dependencies
mvn assembly:single
```

### Option 2: Build with Docker

If Maven is not installed locally, use Docker:

```bash
cd /home/user/QSIGN/OQS

# Build using Maven Docker container
docker run --rm -v "$(pwd)":/app -w /app maven:3.9-eclipse-temurin-17 mvn clean package

# Generated files will be in target/
ls -lh target/*.jar
```

### Option 3: Use Build Script

```bash
cd /home/user/QSIGN/OQS

# Make script executable (if not already)
chmod +x build.sh

# Run build script
./build.sh
```

## 📦 Build Artifacts

After successful build, you'll find:

```
target/
├── oqs-java-1.0.0.jar                        # Main JAR
├── oqs-java-1.0.0-jar-with-dependencies.jar  # Fat JAR (use this for deployment)
└── test-classes/                              # Compiled tests
```

## 🔧 Deploy to QSIGN / Keycloak

```bash
# Copy to Keycloak providers directory
docker cp target/oqs-java-1.0.0-jar-with-dependencies.jar \
    keycloak:/opt/keycloak/providers/

# Restart Keycloak to load the provider
cd ../Q-SIGN
docker-compose restart keycloak

# Check Keycloak logs
docker-compose logs -f keycloak | grep OQS
```

You should see:

```
======================================================================
   🛡️  Initializing OQS Provider
   Open Quantum Safe for QSIGN
======================================================================
   Version: 1.0.0
   Provider: OQS
   ✅ Registered KYBER512, KYBER768, KYBER1024 (KEM)
   ✅ Registered DILITHIUM2, DILITHIUM3, DILITHIUM5 (Signature)
   ✅ OQS Provider: INITIALIZED
======================================================================
```

## ✅ Verify Installation

### Test 1: Run JUnit Tests

```bash
mvn test
```

Expected output:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.qsign.oqs.DilithiumSignatureTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Running com.qsign.oqs.QSIGNIntegrationTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

### Test 2: Run Simple Example

```bash
# Compile example
javac -cp target/oqs-java-1.0.0-jar-with-dependencies.jar \
    examples/SimpleExample.java

# Run example
java -cp target/oqs-java-1.0.0-jar-with-dependencies.jar:examples \
    SimpleExample
```

Expected output:

```
======================================================================
   🛡️  OQS-Java Simple Example
======================================================================

📝 Example 1: DILITHIUM3 Digital Signature
------------------------------------------
Generating Dilithium3 key pair...
  ✅ Public key:  1952 bytes
  ✅ Private key: 4000 bytes

Signing message: "Hello, Quantum-Safe World!"
  ✅ Signature: 3293 bytes

Verifying signature...
  ✅ Signature is VALID

...
```

## 🧪 Integration Testing with Q-SIGN

### 1. Update Keycloak PQC Provider

Edit `../Q-SIGN/keycloak-pqc-provider/pom.xml`:

```xml
<dependency>
    <groupId>com.qsign</groupId>
    <artifactId>oqs-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Use OQS in Keycloak Provider

Edit your Keycloak provider Java code:

```java
import com.qsign.oqs.provider.QSIGNIntegration;
import com.qsign.oqs.crypto.DilithiumSignature;

public class Dilithium3SignatureProvider implements SignatureProvider {

    @Override
    public void init() {
        // Initialize OQS
        QSIGNIntegration.initialize();

        // Create Dilithium provider
        DilithiumSignature dilithium = QSIGNIntegration.createSignatureProvider();

        // Use for JWT signing
        // ...
    }
}
```

### 3. Rebuild and Deploy

```bash
# In Q-SIGN/keycloak-pqc-provider
mvn clean package

# Copy to Keycloak
docker cp target/keycloak-pqc-provider-1.0.0.jar \
    keycloak:/opt/keycloak/providers/

# Restart
docker-compose restart keycloak
```

## 🔍 Troubleshooting

### Maven Not Found

```bash
# Install Maven
sudo apt-get install maven

# Or use Docker
docker run --rm -v "$(pwd)":/app -w /app maven:3.9-eclipse-temurin-17 mvn clean package
```

### Java Version Issues

```bash
# Check Java version
java -version

# Should be 17 or higher
# If not, install Java 17:
sudo apt-get install openjdk-17-jdk

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### Build Errors

```bash
# Clean Maven cache
mvn clean

# Force update dependencies
mvn clean package -U

# Skip tests if needed (not recommended)
mvn package -DskipTests
```

### Dependency Issues

```bash
# Verify BouncyCastle is available
mvn dependency:tree | grep bouncycastle

# Force re-download
mvn dependency:purge-local-repository -DactTransitively=false -DreResolve=false
```

## 📖 Next Steps

1. **Read the full documentation**: [README.md](README.md)
2. **Explore examples**: Check `examples/` directory
3. **Run tests**: `mvn test`
4. **Integrate with Q-SIGN**: Follow integration guide above
5. **Deploy to production**: See deployment guide in README

## 🆘 Getting Help

- **Documentation**: [README.md](README.md)
- **Issues**: Create an issue on GitHub
- **Q-SIGN Docs**: See `../Q-SIGN/README.md`
- **API Reference**: JavaDoc in `target/apidocs/`

## 🎯 Common Use Cases

### Use Case 1: JWT Signing for Keycloak

```java
QSIGNIntegration.initialize();
Map<String, KeyPair> keys = QSIGNIntegration.generateJWTSigningKeys();
KeyPair dilithiumKeyPair = keys.get("dilithium");
// Use for JWT signing
```

### Use Case 2: TLS Key Generation

```java
QSIGNIntegration.initialize();
Map<String, KeyPair> keys = QSIGNIntegration.generateTLSKeys();
KeyPair kyberKP = keys.get("kyber");
KeyPair dilithiumKP = keys.get("dilithium");
// Use for Q-TLS
```

### Use Case 3: Digital Signatures

```java
DilithiumSignature dilithium = DilithiumSignature.dilithium3();
KeyPair keyPair = dilithium.generateKeyPair();
byte[] signature = dilithium.sign(privateKey, message);
boolean valid = dilithium.verify(publicKey, message, signature);
```

---

**OQS-Java** - Ready for quantum-safe QSIGN integration! 🛡️
