#!/bin/bash

# Minecraft Development Runner for Pufferfish Skill Leveling Addon
# Runs Fabric or Forge client/server with proper Java 17 setup

set -e

# Ensure Java 17 is available
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to display usage
usage() {
    echo -e "${BLUE}Minecraft Development Runner${NC}"
    echo -e "${YELLOW}Usage: $0 [platform] [type]${NC}"
    echo ""
    echo -e "${GREEN}Platforms:${NC}"
    echo "  fabric  - Run with Fabric mod loader"
    echo "  forge   - Run with Forge mod loader"
    echo ""
    echo -e "${GREEN}Types:${NC}"
    echo "  client  - Launch Minecraft client (default)"
    echo "  server  - Launch dedicated server"
    echo ""
    echo -e "${GREEN}Examples:${NC}"
    echo "  $0 fabric client    # Launch Fabric client"
    echo "  $0 forge server     # Launch Forge server"
    echo "  $0 fabric          # Launch Fabric client (default)"
    echo ""
    exit 1
}

# Function to check Java version
check_java() {
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        echo -e "${GREEN}✓ Java found: $JAVA_VERSION${NC}"
        
        # Check if Java 17+
        MAJOR_VERSION=$(echo $JAVA_VERSION | cut -d. -f1)
        if [ "$MAJOR_VERSION" -ge 17 ]; then
            echo -e "${GREEN}✓ Java 17+ detected${NC}"
        else
            echo -e "${RED}✗ Java 17+ required. Current: $JAVA_VERSION${NC}"
            echo -e "${YELLOW}Please install Java 17+ or set JAVA_HOME${NC}"
            exit 1
        fi
    else
        echo -e "${RED}✗ Java not found in PATH${NC}"
        exit 1
    fi
}

# Function to check if build exists
check_build() {
    local platform=$1
    local jar_path="../$platform/build/libs/puffish_skill_leveling-*-$platform.jar"
    
    if ls $jar_path 1> /dev/null 2>&1; then
        echo -e "${GREEN}✓ $platform build found${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ $platform build not found. Building...${NC}"
        cd ..
        ./gradlew :$platform:build
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ $platform build completed${NC}"
            cd "Minecraft Dev Docker Client"
            return 0
        else
            echo -e "${RED}✗ $platform build failed${NC}"
            exit 1
        fi
    fi
}

# Function to run Minecraft
run_minecraft() {
    local platform=$1
    local type=${2:-client}
    
    echo -e "${BLUE}Starting Minecraft $platform $type...${NC}"
    echo -e "${YELLOW}Note: First run may take several minutes to download dependencies${NC}"
    echo ""
    
    cd ..
    
    case $type in
        client)
            echo -e "${GREEN}Launching $platform client...${NC}"
            ./gradlew :$platform:runClient
            ;;
        server)
            echo -e "${GREEN}Starting $platform server...${NC}"
            ./gradlew :$platform:runServer
            ;;
        *)
            echo -e "${RED}Unknown type: $type${NC}"
            usage
            ;;
    esac
}

# Main execution
main() {
    echo -e "${BLUE}=== Pufferfish Skill Leveling Development Environment ===${NC}"
    echo ""
    
    # Parse arguments
    if [ $# -eq 0 ]; then
        usage
    fi
    
    PLATFORM=$1
    TYPE=${2:-client}
    
    # Validate platform
    case $PLATFORM in
        fabric|forge)
            ;;
        *)
            echo -e "${RED}Invalid platform: $PLATFORM${NC}"
            usage
            ;;
    esac
    
    # Validate type
    case $TYPE in
        client|server)
            ;;
        *)
            echo -e "${RED}Invalid type: $TYPE${NC}"
            usage
            ;;
    esac
    
    # Run checks and start Minecraft
    check_java
    check_build $PLATFORM
    run_minecraft $PLATFORM $TYPE
}

# Handle Ctrl+C gracefully
trap 'echo -e "\n${YELLOW}Minecraft stopped by user${NC}"; exit 0' INT

# Run main function with all arguments
main "$@"
