#!/bin/bash

set -e

echo "================================================"
echo "Luna SPI Build Script"
echo "================================================"
echo ""

# Step 1: Install Java
echo "Step 1: Installing Java..."
echo "Running: sudo apt-get install -y openjdk-11-jdk"
echo ""
sudo apt-get update
sudo apt-get install -y openjdk-11-jdk

java -version

# Step 2: Setup environment
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=/tmp/apache-maven-3.9.5/bin:$JAVA_HOME/bin:$PATH

echo ""
echo "JAVA_HOME: $JAVA_HOME"

# Step 3: Extract Maven (already downloaded)
echo ""
echo "Step 2: Setting up Maven..."
cd /tmp
if [ ! -d "apache-maven-3.9.5" ]; then
    tar xzf apache-maven-3.9.5-bin.tar.gz
fi

mvn --version

# Step 4: Build Luna SPI
echo ""
echo "Step 3: Building Luna SPI JAR..."
cd /home/user/QSIGN/apisix-keycloack-Vaultkms-hsm/keycloak-luna-hsm-provider

echo "Cleaning previous builds..."
mvn clean

echo "Building JAR..."
mvn package -DskipTests

# Check result
if [ -f "target/keycloak-spi-luna-keystore-1.1.0.jar" ]; then
    echo ""
    echo "✅ Build Successful!"
    ls -lh target/keycloak-spi-luna-keystore-1.1.0.jar

    # Copy to keycloak-hsm
    echo ""
    echo "Step 4: Copying JAR to keycloak-hsm..."
    cp target/keycloak-spi-luna-keystore-1.1.0.jar /home/user/QSIGN/keycloak-hsm/providers/
    echo "✅ JAR copied"

    # Create symlink for libCryptoki2.so
    echo ""
    echo "Step 5: Creating Luna library symlink..."
    sudo ln -sf /usr/safenet/lunaclient/lib/libCryptoki2_64.so /usr/safenet/lunaclient/lib/libCryptoki2.so
    echo "✅ Symlink created"

    # Restart Keycloak
    echo ""
    echo "Step 6: Restarting Keycloak..."
    kubectl --kubeconfig=/home/user/.kube/config rollout restart deployment/keycloak -n pqc-sso
    kubectl --kubeconfig=/home/user/.kube/config rollout status deployment/keycloak -n pqc-sso --timeout=180s

    echo ""
    echo "================================================"
    echo "✅ All Done!"
    echo "================================================"
    echo ""
    echo "Next: Access Keycloak Admin Console"
    echo "URL: http://192.168.0.11:8080/admin/"
    echo "Login: admin / admin123!@#"
    echo ""
    echo "Then: Realm Settings > Keys > Providers > Add provider"
    echo "      Select: luna-keystore"
    echo ""
else
    echo ""
    echo "❌ Build Failed"
    exit 1
fi
