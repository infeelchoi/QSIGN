#!/bin/bash

set -e

echo "================================================"
echo "Luna SPI Build and Installation Script"
echo "================================================"
echo ""

# Check if running with proper permissions
if [ "$EUID" -eq 0 ]; then
   echo "Please do not run as root. Run as regular user with sudo access."
   exit 1
fi

# Step 1: Install Maven
echo "Step 1: Installing Maven..."
if ! command -v mvn &> /dev/null; then
    echo "Maven not found. Installing..."
    sudo apt-get update
    sudo apt-get install -y maven
    echo "✓ Maven installed"
else
    echo "✓ Maven already installed"
fi

mvn --version
echo ""

# Step 2: Build Luna SPI JAR
echo "Step 2: Building Luna SPI JAR..."
cd /home/user/QSIGN/apisix-keycloack-Vaultkms-hsm/keycloak-luna-hsm-provider

echo "Cleaning previous builds..."
mvn clean

echo "Building JAR..."
mvn package -DskipTests

if [ -f "target/keycloak-spi-luna-keystore-1.1.0.jar" ]; then
    echo "✓ JAR built successfully"
    ls -lh target/keycloak-spi-luna-keystore-1.1.0.jar
else
    echo "✗ Build failed"
    exit 1
fi

echo ""

# Step 3: Copy JAR to keycloak-hsm providers
echo "Step 3: Copying JAR to keycloak-hsm providers directory..."
cp target/keycloak-spi-luna-keystore-1.1.0.jar /home/user/QSIGN/keycloak-hsm/providers/
echo "✓ JAR copied"

echo ""

# Step 4: Create symlink for libCryptoki2.so
echo "Step 4: Creating symlink for Luna library..."
if [ ! -L /usr/safenet/lunaclient/lib/libCryptoki2.so ]; then
    sudo ln -sf /usr/safenet/lunaclient/lib/libCryptoki2_64.so /usr/safenet/lunaclient/lib/libCryptoki2.so
    echo "✓ Symlink created"
else
    echo "✓ Symlink already exists"
fi

echo ""

# Step 5: Update Keycloak deployment
echo "Step 5: Updating Keycloak deployment to use Luna SPI..."

# Trigger pod restart to pick up new JAR
kubectl --kubeconfig=/home/user/.kube/config rollout restart deployment/keycloak -n pqc-sso

echo "Waiting for rollout to complete..."
kubectl --kubeconfig=/home/user/.kube/config rollout status deployment/keycloak -n pqc-sso --timeout=180s

echo ""
echo "================================================"
echo "✓ Installation Complete!"
echo "================================================"
echo ""
echo "Next steps:"
echo "1. Access Keycloak Admin Console: http://192.168.0.11:8080/admin/"
echo "2. Login with: admin / admin123!@#"
echo "3. Go to: Realm Settings > Keys > Providers"
echo "4. Click: Add provider"
echo "5. Select: luna-keystore"
echo ""
echo "Note: You may need to configure Luna HSM connection settings"
echo "      if this is the first time using the HSM."
echo ""
