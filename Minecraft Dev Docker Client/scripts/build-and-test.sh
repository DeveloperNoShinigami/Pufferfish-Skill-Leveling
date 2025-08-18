#!/bin/bash

# Build and Test Script for Pufferfish Skill Leveling Addon
# Builds the addon and optionally runs tests

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Function to display usage
usage() {
    echo -e "${BLUE}Build and Test Runner${NC}"
    echo -e "${YELLOW}Usage: $0 [options]${NC}"
    echo ""
    echo -e "${GREEN}Options:${NC}"
    echo "  --clean         Clean before building"
    echo "  --test          Run tests after building"
    echo "  --fabric-only   Build only Fabric version"
    echo "  --forge-only    Build only Forge version"
    echo "  --run-fabric    Build and run Fabric client"
    echo "  --run-forge     Build and run Forge client"
    echo "  --help          Show this help"
    echo ""
    echo -e "${GREEN}Examples:${NC}"
    echo "  $0                    # Build all platforms"
    echo "  $0 --clean --test     # Clean build with tests"
    echo "  $0 --run-fabric       # Build and launch Fabric"
    echo ""
}

# Default options
CLEAN=false
RUN_TESTS=false
FABRIC_ONLY=false
FORGE_ONLY=false
RUN_FABRIC=false
RUN_FORGE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --clean)
            CLEAN=true
            shift
            ;;
        --test)
            RUN_TESTS=true
            shift
            ;;
        --fabric-only)
            FABRIC_ONLY=true
            shift
            ;;
        --forge-only)
            FORGE_ONLY=true
            shift
            ;;
        --run-fabric)
            RUN_FABRIC=true
            shift
            ;;
        --run-forge)
            RUN_FORGE=true
            shift
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

# Change to project root
cd ..

echo -e "${BLUE}=== Pufferfish Skill Leveling Build Process ===${NC}"
echo ""

# Clean if requested
if [ "$CLEAN" = true ]; then
    echo -e "${YELLOW}Cleaning previous builds...${NC}"
    ./gradlew clean
    echo -e "${GREEN}✓ Clean completed${NC}"
fi

# Determine build targets
BUILD_TARGETS=""
if [ "$FABRIC_ONLY" = true ]; then
    BUILD_TARGETS=":Fabric:build"
elif [ "$FORGE_ONLY" = true ]; then
    BUILD_TARGETS=":Forge:build"
else
    BUILD_TARGETS="build"
fi

# Build
echo -e "${YELLOW}Building addon...${NC}"
echo -e "${BLUE}Targets: $BUILD_TARGETS${NC}"
./gradlew $BUILD_TARGETS

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build completed successfully${NC}"
    echo ""
    echo -e "${GREEN}Generated artifacts:${NC}"
    find */build/libs -name "*.jar" -exec ls -lh {} \;
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Run tests if requested
if [ "$RUN_TESTS" = true ]; then
    echo ""
    echo -e "${YELLOW}Running tests...${NC}"
    ./gradlew test
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ All tests passed${NC}"
    else
        echo -e "${RED}✗ Some tests failed${NC}"
        exit 1
    fi
fi

# Run Minecraft if requested
if [ "$RUN_FABRIC" = true ]; then
    echo ""
    echo -e "${YELLOW}Launching Fabric client...${NC}"
    cd "Minecraft Dev Docker Client"
    ./scripts/run-minecraft.sh fabric client
elif [ "$RUN_FORGE" = true ]; then
    echo ""
    echo -e "${YELLOW}Launching Forge client...${NC}"
    cd "Minecraft Dev Docker Client"
    ./scripts/run-minecraft.sh forge client
fi

echo ""
echo -e "${GREEN}=== Build Process Complete! ===${NC}"
