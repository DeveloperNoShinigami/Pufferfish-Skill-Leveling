#!/bin/bash

# Environment Setup for Pufferfish Skill Leveling Development
# Configures Java, dependencies, and development tools

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Setting up Minecraft Development Environment ===${NC}"

# Set Java 17
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

# Update environment in current session
echo -e "${YELLOW}Setting Java 17 environment...${NC}"
if [ -d "$JAVA_HOME" ]; then
    echo -e "${GREEN}✓ Java 17 found at $JAVA_HOME${NC}"
    
    # Add to bashrc for persistence
    if ! grep -q "JAVA_HOME.*java-17" ~/.bashrc; then
        echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> ~/.bashrc
        echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc
        echo -e "${GREEN}✓ Java 17 environment added to ~/.bashrc${NC}"
    fi
else
    echo -e "${RED}✗ Java 17 not found. Please install openjdk-17-jdk${NC}"
    exit 1
fi

# Install necessary GUI packages for Minecraft client (if not in Docker)
if [ ! -f /.dockerenv ]; then
    echo -e "${YELLOW}Installing GUI dependencies for Minecraft client...${NC}"
    sudo apt-get update > /dev/null 2>&1
    sudo apt-get install -y \
        libgl1-mesa-glx \
        libgl1-mesa-dri \
        xorg \
        openbox \
        > /dev/null 2>&1
    echo -e "${GREEN}✓ GUI dependencies installed${NC}"
fi

# Set Gradle properties
echo -e "${YELLOW}Configuring Gradle...${NC}"
GRADLE_USER_HOME="${HOME}/.gradle"
mkdir -p "$GRADLE_USER_HOME"

cat > "$GRADLE_USER_HOME/gradle.properties" << EOF
# Java setup
org.gradle.java.home=$JAVA_HOME
org.gradle.jvmargs=-Xmx4G -XX:MaxMetaspaceSize=512m

# Performance
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true

# Minecraft development
minecraft.runDir=run
EOF

echo -e "${GREEN}✓ Gradle configured${NC}"

# Create run directories
echo -e "${YELLOW}Creating run directories...${NC}"
cd ..
mkdir -p Fabric/run Forge/run
echo -e "${GREEN}✓ Run directories created${NC}"

# Check current builds
echo -e "${YELLOW}Checking current builds...${NC}"
if ls */build/libs/*.jar 1> /dev/null 2>&1; then
    echo -e "${GREEN}✓ Found existing builds:${NC}"
    find */build/libs -name "*.jar" -exec basename {} \;
else
    echo -e "${YELLOW}ℹ No builds found. Run './gradlew build' to create them${NC}"
fi

echo ""
echo -e "${GREEN}=== Environment Setup Complete! ===${NC}"
echo -e "${BLUE}Available scripts:${NC}"
echo -e "  ${YELLOW}./scripts/run-minecraft.sh fabric client${NC}  - Run Fabric client"
echo -e "  ${YELLOW}./scripts/run-minecraft.sh forge server${NC}   - Run Forge server"
echo -e "  ${YELLOW}./scripts/build-and-test.sh${NC}              - Build and test addon"
echo -e "  ${YELLOW}./scripts/docker-dev.sh${NC}                  - Docker development environment"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "1. Run ${YELLOW}../gradlew build${NC} to build the addon"
echo -e "2. Use ${YELLOW}./scripts/run-minecraft.sh fabric client${NC} to test"
echo -e "3. Check ${YELLOW}README.md${NC} for detailed instructions"
