#!/bin/bash
set -e

echo "Installing Fabric Server..."
cd /home/minecraft/fabric-server

# Download Fabric installer
FABRIC_VERSION="0.15.11"
MINECRAFT_VERSION="1.20.1"
curl -OJ "https://meta.fabricmc.net/v2/versions/loader/${MINECRAFT_VERSION}/${FABRIC_VERSION}/1.0.1/server/jar"

# Accept EULA
echo "eula=true" > eula.txt

# Create server.properties
cat > server.properties << EOF
server-port=25565
gamemode=creative
difficulty=peaceful
spawn-protection=0
online-mode=false
enable-command-block=true
op-permission-level=4
max-players=10
motd=Pufferfish Skills Testing Server (Fabric)
EOF

# Download required mods
echo "Downloading Fabric API..."
curl -L -o mods/fabric-api.jar "https://edge.forgecdn.net/files/5098/696/fabric-api-0.95.4%2B1.20.1.jar"

echo "Downloading Pufferfish Skills from CurseForge..."
curl -L -o mods/puffish-skills.jar "https://edge.forgecdn.net/files/5263/993/puffish_skills-0.16.3%2B1.20-fabric.jar"

echo "Fabric server setup complete!"
