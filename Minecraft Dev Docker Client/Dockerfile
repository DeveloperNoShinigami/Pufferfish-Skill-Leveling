# Minecraft Server with Fabric/Forge for testing Pufferfish Skill Leveling addon
FROM openjdk:17-jdk-slim

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# Create minecraft user and directories
RUN useradd -m -u 1000 minecraft
WORKDIR /home/minecraft

# Create server directories
RUN mkdir -p /home/minecraft/fabric-server /home/minecraft/forge-server
RUN mkdir -p /home/minecraft/fabric-server/mods /home/minecraft/forge-server/mods
RUN mkdir -p /home/minecraft/fabric-server/config /home/minecraft/forge-server/config

# Copy scripts
COPY install_fabric.sh /home/minecraft/
COPY install_forge.sh /home/minecraft/
COPY copy_latest_mod.sh /home/minecraft/
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Make scripts executable
RUN chmod +x /home/minecraft/*.sh

# Set ownership
RUN chown -R minecraft:minecraft /home/minecraft

# Expose Minecraft server ports
EXPOSE 25565 25566

# Use supervisor to manage processes
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
