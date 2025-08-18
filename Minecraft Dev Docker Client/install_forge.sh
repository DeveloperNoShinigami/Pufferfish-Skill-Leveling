#!/bin/bash
set -e

echo "Installing Forge Server..."
cd /home/minecraft/forge-server

# Download Forge installer
FORGE_VERSION="47.3.0"
MINECRAFT_VERSION="1.20.1"
curl -OJ "https://maven.minecraftforge.net/net/minecraftforge/forge/${MINECRAFT_VERSION}-${FORGE_VERSION}/forge-${MINECRAFT_VERSION}-${FORGE_VERSION}-installer.jar"

# Run Forge installer
java -jar forge-${MINECRAFT_VERSION}-${FORGE_VERSION}-installer.jar --installServer

# Accept EULA
echo "eula=true" > eula.txt

# Create server.properties
cat > server.properties << EOF
server-port=25566
gamemode=creative
difficulty=peaceful
spawn-protection=0
online-mode=false
enable-command-block=true
op-permission-level=4
max-players=10
motd=Pufferfish Skills Testing Server (Forge)
EOF

# Download required mods
echo "Downloading Pufferfish Skills for Forge from CurseForge..."
curl -L -o mods/puffish-skills.jar "https://edge.forgecdn.net/files/5263/994/puffish_skills-0.16.3%2B1.20-forge.jar"

echo "Forge server setup complete!"
