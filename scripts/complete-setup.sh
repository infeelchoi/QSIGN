#!/bin/bash

set -e

echo "============================================"
echo "Complete Keycloak Luna HSM Setup"
echo "============================================"

# Step 1: Build Luna SPI JAR
echo ""
echo "Step 1: Building Luna SPI JAR..."
cd /home/user/QSIGN/apisix-keycloack-Vaultkms-hsm/keycloak-luna-hsm-provider

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven not found. Installing Maven..."
    sudo apt-get update
    sudo apt-get install -y maven
fi

# Build the JAR
./build.sh

# Copy JAR to keycloak-hsm providers
cp target/keycloak-spi-luna-keystore-1.1.0.jar /home/user/QSIGN/keycloak-hsm/providers/

echo "✓ Luna SPI JAR built and copied"

# Step 2: Build Docker image with libstdc++
echo ""
echo "Step 2: Building custom Keycloak image with libstdc++..."
cd /home/user/QSIGN/keycloak-hsm

docker build -f Dockerfile.fixed -t keycloak-luna-hsm:23.0 .

echo "✓ Docker image built"

# Step 3: Update Kubernetes deployment
echo ""
echo "Step 3: Updating Keycloak deployment..."
kubectl --kubeconfig=/home/user/.kube/config set image deployment/keycloak -n pqc-sso keycloak=keycloak-luna-hsm:23.0

echo "✓ Deployment updated"

# Step 4: Wait for rollout
echo ""
echo "Step 4: Waiting for deployment to complete..."
kubectl --kubeconfig=/home/user/.kube/config rollout status deployment/keycloak -n pqc-sso

echo ""
echo "============================================"
echo "✓ Setup Complete!"
echo "============================================"
echo ""
echo "Next steps:"
echo "1. Access Keycloak Admin Console: http://192.168.0.11:8080/admin/"
echo "2. Go to Realm Settings > Keys > Add provider"
echo "3. You should now see 'luna-keystore' option"
echo ""
