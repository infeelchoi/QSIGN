#!/bin/bash

# OQS-Java Build Script

set -e

echo "======================================================================="
echo "   🛡️  Building OQS-Java"
echo "======================================================================="

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Clean previous build
echo -e "${BLUE}Cleaning previous build...${NC}"
mvn clean

# Compile and package
echo -e "${BLUE}Compiling and packaging...${NC}"
mvn package -DskipTests

# Run tests
echo -e "${BLUE}Running tests...${NC}"
mvn test

# Build with dependencies
echo -e "${BLUE}Building fat JAR with dependencies...${NC}"
mvn assembly:single

echo ""
echo "======================================================================="
echo -e "${GREEN}✅ Build completed successfully${NC}"
echo "======================================================================="
echo ""
echo "Generated artifacts:"
echo "  - target/oqs-java-1.0.0.jar"
echo "  - target/oqs-java-1.0.0-jar-with-dependencies.jar"
echo ""
echo "To deploy to Keycloak:"
echo "  docker cp target/oqs-java-1.0.0-jar-with-dependencies.jar keycloak:/opt/keycloak/providers/"
echo "  docker-compose restart keycloak"
echo ""
