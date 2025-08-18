# Development Guide - Pufferfish Skill Leveling Addon

## Quick Start

1. **Setup Environment**
   ```bash
   cd "Minecraft Dev Docker Client"
   ./scripts/setup-environment.sh
   ```

2. **Build the Addon**
   ```bash
   # Build all platforms
   ./scripts/build-and-test.sh
   
   # Build specific platform
   ./scripts/build-and-test.sh fabric
   ./scripts/build-and-test.sh forge
   ```

3. **Test with Minecraft**
   ```bash
   # Start Fabric server
   ./scripts/run-minecraft.sh fabric server
   
   # Start Forge client
   ./scripts/run-minecraft.sh forge client
   ```

4. **Docker Development**
   ```bash
   # Build and run development container
   ./scripts/docker-dev.sh build
   ./scripts/docker-dev.sh run
   
   # Execute commands in container
   ./scripts/docker-dev.sh exec "gradle build"
   ```

## Addon Features

### Per-Level Rewards System
- **Reward Type**: `puffish_skill_leveling:per_level_rewards`
- **Purpose**: Provides different rewards at each skill level
- **Configuration**: Supports merge_description for combined reward text

### Example Usage
```json
{
  "type": "puffish_skill_leveling:per_level_rewards",
  "merge_description": true,
  "rewards": [
    {
      "level": 1,
      "reward": {
        "type": "puffish_skills:command",
        "command": "give @s minecraft:iron_sword"
      }
    },
    {
      "level": 5,
      "reward": {
        "type": "puffish_skills:command", 
        "command": "give @s minecraft:diamond_sword"
      }
    }
  ]
}
```

### Commands Available
- `/skill-leveling get <category> <skill> <player>` - Get skill level
- `/skill-leveling set <category> <skill> <player> <level>` - Set skill level
- `/skill-leveling advance <category> <skill> <player>` - Advance skill level
- `/skill-leveling refund <category> <skill> <player> <amount>` - Refund skill levels

## File Structure
```
Pufferfish-Skill-Leveling/
├── Common/               # Common platform code
├── Fabric/              # Fabric-specific code  
├── Forge/               # Forge-specific code
└── Minecraft Dev Docker Client/
    ├── Dockerfile       # Development container
    ├── config/          # Configuration files
    ├── scripts/         # Development scripts
    └── supervisord.conf # Process management
```

## Development Workflow

### 1. Make Changes
Edit code in `Common/src/main/java/` or platform-specific folders.

### 2. Build and Test
```bash
./scripts/build-and-test.sh
```

### 3. Test in Minecraft
```bash
# Copy built mod to test server
./scripts/run-minecraft.sh fabric server
```

### 4. Debug Issues
Check logs in:
- `fabric-server/logs/latest.log`
- `forge-server/logs/latest.log`

## Configuration Files

### Skills Configuration
Place skill definitions in:
- `fabric-server/config/puffish_skills/categories/`
- `forge-server/config/puffish_skills/categories/`

### Example Skill with Per-Level Rewards
```json
{
  "skills": [
    {
      "id": "combat",
      "name": "Combat",
      "description": "Fighting skills",
      "max_level": 10,
      "rewards": [
        {
          "type": "puffish_skill_leveling:per_level_rewards",
          "merge_description": true,
          "rewards": [
            {
              "level": 1,
              "reward": {
                "type": "puffish_skills:command",
                "command": "give @s minecraft:wooden_sword"
              }
            },
            {
              "level": 5,
              "reward": {
                "type": "puffish_skills:command",
                "command": "give @s minecraft:iron_sword"
              }
            },
            {
              "level": 10,
              "reward": {
                "type": "puffish_skills:command",
                "command": "give @s minecraft:diamond_sword"
              }
            }
          ]
        }
      ]
    }
  ]
}
```

## Troubleshooting

### Common Issues

1. **Java Version Error**
   ```
   Solution: Ensure Java 17 is installed and set as JAVA_HOME
   ```

2. **Mod Not Loading**
   ```
   Check: dependencies.json for correct Skills mod version
   Verify: mod is in correct mods/ directory
   ```

3. **Reward Not Triggering**
   ```
   Check: skill configuration syntax
   Verify: reward type is registered correctly
   Debug: server logs for error messages
   ```

### Debug Commands
```bash
# Check Java version
java -version

# Verify mod is loaded
grep "puffish_skill_leveling" fabric-server/logs/latest.log

# Test commands
/skill-leveling get combat swordplay Player1
```

## Development Tips

1. **Use Docker for Consistency**
   - Ensures same Java version across environments
   - Isolated testing environment
   - Easy to reset and rebuild

2. **Test Both Platforms**
   - Fabric and Forge have different behaviors
   - Test commands and rewards on both

3. **Check Logs Regularly**
   - Server logs show loading issues
   - Client logs show rendering problems

4. **Use Creative Mode**
   - Faster testing of rewards
   - Easy access to debug commands

## Contributing

1. Follow existing code style
2. Test on both Fabric and Forge
3. Update this guide if adding features
4. Include example configurations

## Support Files

- `dev-config.env` - Development configuration
- `supervisord.conf` - Process management
- `Dockerfile` - Container setup
- `scripts/` - Automation scripts
