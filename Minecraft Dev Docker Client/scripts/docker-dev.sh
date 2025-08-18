#!/bin/bash

# Docker Development Environment for Pufferfish Skill Leveling
# Manages Docker container for testing

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

CONTAINER_NAME="minecraft-dev-skillleveling"
IMAGE_NAME="minecraft-dev-skillleveling"

# Function to display usage
usage() {
    echo -e "${BLUE}Docker Development Environment${NC}"
    echo -e "${YELLOW}Usage: $0 [command]${NC}"
    echo ""
    echo -e "${GREEN}Commands:${NC}"
    echo "  build      Build the Docker image"
    echo "  start      Start the development container"
    echo "  stop       Stop the development container"
    echo "  restart    Restart the development container"
    echo "  logs       View container logs"
    echo "  shell      Access container shell"
    echo "  status     Check container status"
    echo "  clean      Remove container and image"
    echo ""
    echo -e "${GREEN}Examples:${NC}"
    echo "  $0 build       # Build the Docker image"
    echo "  $0 start       # Start development servers"
    echo "  $0 logs        # View server logs"
    echo ""
}

# Function to check if Docker is available
check_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        echo -e "${RED}✗ Docker not found. Please install Docker.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Docker found${NC}"
}

# Function to build Docker image
build_image() {
    echo -e "${YELLOW}Building Docker image...${NC}"
    docker build -t $IMAGE_NAME .
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Docker image built successfully${NC}"
    else
        echo -e "${RED}✗ Docker build failed${NC}"
        exit 1
    fi
}

# Function to start container
start_container() {
    # Check if container already exists
    if docker ps -a --format "table {{.Names}}" | grep -q "^$CONTAINER_NAME$"; then
        echo -e "${YELLOW}Container exists. Starting...${NC}"
        docker start $CONTAINER_NAME
    else
        echo -e "${YELLOW}Creating and starting new container...${NC}"
        docker run -d \
            --name $CONTAINER_NAME \
            -p 25565:25565 \
            -p 25566:25566 \
            -v "$(pwd)/../:/workspace" \
            $IMAGE_NAME
    fi
    
    echo -e "${GREEN}✓ Container started${NC}"
    echo -e "${BLUE}Fabric server: localhost:25565${NC}"
    echo -e "${BLUE}Forge server: localhost:25566${NC}"
}

# Function to stop container
stop_container() {
    if docker ps --format "table {{.Names}}" | grep -q "^$CONTAINER_NAME$"; then
        echo -e "${YELLOW}Stopping container...${NC}"
        docker stop $CONTAINER_NAME
        echo -e "${GREEN}✓ Container stopped${NC}"
    else
        echo -e "${YELLOW}Container not running${NC}"
    fi
}

# Function to show logs
show_logs() {
    if docker ps -a --format "table {{.Names}}" | grep -q "^$CONTAINER_NAME$"; then
        echo -e "${YELLOW}Showing container logs (Ctrl+C to exit)...${NC}"
        docker logs -f $CONTAINER_NAME
    else
        echo -e "${RED}Container not found${NC}"
    fi
}

# Function to access shell
access_shell() {
    if docker ps --format "table {{.Names}}" | grep -q "^$CONTAINER_NAME$"; then
        echo -e "${YELLOW}Accessing container shell...${NC}"
        docker exec -it $CONTAINER_NAME bash
    else
        echo -e "${RED}Container not running${NC}"
    fi
}

# Function to check status
check_status() {
    echo -e "${BLUE}=== Container Status ===${NC}"
    
    if docker ps -a --format "table {{.Names}}" | grep -q "^$CONTAINER_NAME$"; then
        docker ps -a --filter "name=$CONTAINER_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        
        if docker ps --format "table {{.Names}}" | grep -q "^$CONTAINER_NAME$"; then
            echo ""
            echo -e "${GREEN}✓ Container is running${NC}"
            echo -e "${BLUE}Available services:${NC}"
            echo -e "  Fabric Server: ${YELLOW}localhost:25565${NC}"
            echo -e "  Forge Server:  ${YELLOW}localhost:25566${NC}"
        else
            echo -e "${YELLOW}Container exists but is not running${NC}"
        fi
    else
        echo -e "${YELLOW}Container not found${NC}"
    fi
}

# Function to clean up
clean_up() {
    echo -e "${YELLOW}Cleaning up Docker resources...${NC}"
    
    # Stop and remove container
    if docker ps -a --format "table {{.Names}}" | grep -q "^$CONTAINER_NAME$"; then
        docker stop $CONTAINER_NAME >/dev/null 2>&1 || true
        docker rm $CONTAINER_NAME
        echo -e "${GREEN}✓ Container removed${NC}"
    fi
    
    # Remove image
    if docker images --format "table {{.Repository}}" | grep -q "^$IMAGE_NAME$"; then
        docker rmi $IMAGE_NAME
        echo -e "${GREEN}✓ Image removed${NC}"
    fi
}

# Main execution
main() {
    if [ $# -eq 0 ]; then
        usage
        exit 0
    fi
    
    check_docker
    
    case $1 in
        build)
            build_image
            ;;
        start)
            start_container
            ;;
        stop)
            stop_container
            ;;
        restart)
            stop_container
            sleep 2
            start_container
            ;;
        logs)
            show_logs
            ;;
        shell)
            access_shell
            ;;
        status)
            check_status
            ;;
        clean)
            clean_up
            ;;
        *)
            echo -e "${RED}Unknown command: $1${NC}"
            usage
            exit 1
            ;;
    esac
}

main "$@"
