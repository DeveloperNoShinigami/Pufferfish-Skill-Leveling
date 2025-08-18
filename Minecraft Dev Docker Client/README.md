# Minecraft Development Server with Docker

This Docker setup provides a complete testing environment for the Pufferfish Skill Leveling addon.

## Features

- **Dual Server Setup**: Both Fabric and Forge servers for comprehensive testing
- **Auto-Dependency Download**: Automatically downloads required mods (Pufferfish Skills, Fabric API)
- **Development Integration**: Copies your latest builds to test servers
- **Easy Management**: Supervisor handles server processes

## Quick Start

### 1. Build the Docker Image
```bash
cd "Minecraft Dev Docker Client"
docker build -t minecraft-dev-server .
```

### 2. Run the Development Server
```bash
# Mount your workspace to automatically copy latest builds
docker run -d \
  --name minecraft-test \
  -p 25565:25565 \
  -p 25566:25566 \
  -v "$(pwd)/../:/workspace" \
  minecraft-dev-server
```

### 3. Check Server Status
```bash
# View logs
docker logs minecraft-test

# Connect to container
docker exec -it minecraft-test bash
```

## Server Details

### Fabric Server
- **Port**: 25565
- **Includes**: Fabric API + Pufferfish Skills + Your Addon
- **GameMode**: Creative (for easy testing)

### Forge Server  
- **Port**: 25566
- **Includes**: Pufferfish Skills + Your Addon
- **GameMode**: Creative (for easy testing)

## Testing Your Addon

1. **Build your mod**: `./gradlew build`
2. **Restart container**: `docker restart minecraft-test`
3. **Connect to server**: Minecraft client → Direct Connect → `localhost:25565` (Fabric) or `localhost:25566` (Forge)
4. **Test commands**: `/puffish_skills` and `/skillleveling` commands should be available

## Troubleshooting

```bash
# View server logs
docker logs minecraft-test -f

# Access server console
docker exec -it minecraft-test supervisorctl status
docker exec -it minecraft-test tail -f /home/minecraft/fabric-server/logs/latest.log
docker exec -it minecraft-test tail -f /home/minecraft/forge-server/logs/latest.log

# Restart specific server
docker exec -it minecraft-test supervisorctl restart fabric-server
docker exec -it minecraft-test supervisorctl restart forge-server
```

## Manual Mod Testing

If you want to test with different mod versions:

```bash
# Copy specific jar to test
docker cp your-addon.jar minecraft-test:/home/minecraft/fabric-server/mods/
docker exec minecraft-test supervisorctl restart fabric-server
```
