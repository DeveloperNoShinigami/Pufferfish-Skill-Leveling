# Pufferfish Skills - Configuration Guide

## Overview

This guide provides detailed information about configuring the Pufferfish Skills addon for your server or modpack. The addon uses a JSON-based configuration system that's both powerful and easy to understand.

## Configuration Structure

The mod's configuration is split across several files:

```
config/puffish_skills/
├── config.json              # Main configuration
├── categories/               # Skill category definitions
│   ├── combat/
│   ├── mining/
│   └── ...
└── templates/               # Pre-made templates
    ├── basic_config.json
    ├── full_config.json
    └── pvp_config.json
```

## Main Configuration (config.json)

The main configuration file controls which skill categories are loaded:

```json
{
    "version": 3,
    "show_warnings": true,
    "categories": [
        "combat",
        "mining",
        "woodcutting"
    ]
}
```

### Configuration Options

- **version**: Configuration format version (must be 3 for current mod version)
- **show_warnings**: Whether to display configuration warnings in logs
- **categories**: List of skill categories to load

## Hot Reloading

The addon supports hot-reloading of configurations without server restart:

```bash
/skillsadmin reload
```

This will:
1. Validate all configuration files
2. Reload skill categories
3. Update player data structures
4. Notify all online players of changes

## Configuration Templates

### Basic Template

Suitable for small servers or simple setups:

```json
{
    "version": 3,
    "show_warnings": true,
    "categories": ["combat", "mining", "woodcutting"]
}
```

### Full Template

Includes all available skill categories:

```json
{
    "version": 3,
    "show_warnings": false,
    "categories": [
        "combat", "mining", "woodcutting", "farming",
        "crafting", "building", "enchanting", "fishing",
        "archery", "magic"
    ]
}
```

### PvP Template

Optimized for PvP servers:

```json
{
    "version": 3,
    "show_warnings": true,
    "categories": ["pvp_combat", "survival"]
}
```

## Skill Categories

Each skill category is defined in its own directory with these files:

- `category.json` - General category settings
- `definitions.json` - Skill definitions and requirements
- `skills.json` - Individual skill configurations
- `connections.json` - Skill tree connections
- `experience.json` - Experience sources (optional)

## Advanced Configuration

### Version Management

The addon automatically validates configuration versions:

- **Version 1-2**: Legacy formats (automatically migrated)
- **Version 3**: Current format with full feature support
- **Future versions**: Will be backward compatible

### Platform-Specific Settings

The addon automatically adapts to your platform:

- **Fabric**: Uses standard `config/` directory
- **Forge**: Uses `serverconfig/` for server-side configs
- **Auto-detection**: Platform is detected automatically

### Performance Tuning

For large servers, consider:

1. **Reducing categories**: Only load needed skill trees
2. **Disabling warnings**: Set `show_warnings` to `false`
3. **Experience caching**: Enabled automatically for large player counts

## Configuration Validation

Use admin commands to validate your configuration:

```bash
# Show current configuration status
/skillsadmin info

# Validate without applying changes
/skillsadmin validate

# Test reload without committing
/skillsadmin test-reload
```

## Backup and Migration

### Creating Backups

```bash
# Backup configuration files
/skillsadmin backup config

# Backup all player data
/skillsadmin backup data
```

### Migrating Between Versions

1. Create a backup of your current setup
2. Update the mod to the new version
3. Check for configuration warnings
4. Update version number in config.json if prompted
5. Test thoroughly before going live

## Common Configuration Patterns

### Adding Custom Categories

1. Create a new directory in `categories/`
2. Add the required JSON files
3. Include the category name in `config.json`
4. Reload configurations

### Modifying Experience Sources

Experience sources can be customized per category:

```json
{
    "sources": {
        "kill_entity": {
            "multiplier": 1.5,
            "entities": ["minecraft:zombie", "minecraft:skeleton"]
        },
        "break_block": {
            "multiplier": 1.0,
            "blocks": ["minecraft:stone", "minecraft:iron_ore"]
        }
    }
}
```

### Skill Prerequisites

Configure skill unlock requirements:

```json
{
    "skills": {
        "advanced_combat": {
            "requires": ["basic_combat"],
            "cost": 5,
            "max_level": 10
        }
    }
}
```

## Integration Configuration

### API Integration

Other mods can register with the skills system:

```java
// Register an integration hook
SkillsAPI.registerIntegrationHook("my_mod", new MyModIntegration());

// Listen for skill events
SkillsAPI.registerIntegrationEventListener("my_listener", event -> {
    // Handle skill events
});
```

### Data Export Configuration

Configure automatic data exports:

```json
{
    "data_export": {
        "enabled": true,
        "interval": "daily",
        "format": "json",
        "compress": true
    }
}
```

## Troubleshooting Configuration Issues

### Common Problems

1. **Syntax Errors**: Use a JSON validator to check file syntax
2. **Version Mismatch**: Update version number after mod updates
3. **Missing Categories**: Ensure all referenced categories exist
4. **Performance Issues**: Reduce active categories or optimize experience sources

### Debug Mode

Enable detailed logging:

```json
{
    "version": 3,
    "show_warnings": true,
    "debug_mode": true,
    "categories": [...]
}
```

### Validation Commands

```bash
# Check configuration validity
/skillsadmin validate config

# Test specific category
/skillsadmin validate category combat

# Show loaded categories
/skillsadmin list categories
```

## Best Practices

1. **Always backup** before making changes
2. **Test changes** on a development server first
3. **Use templates** as starting points
4. **Document customizations** for your team
5. **Monitor performance** after configuration changes
6. **Keep configurations in version control** for team environments