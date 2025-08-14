# Pufferfish Skills - Installation Guide

## Overview

The Pufferfish Skills addon is a standalone, easily integrable mod that adds a comprehensive skill leveling system to Minecraft. This guide will walk you through the installation process for both server administrators and modpack creators.

## Supported Platforms

- **Fabric** - Recommended for most users
- **Forge** - For compatibility with Forge-based modpacks
- **NeoForge** - Future compatibility (in development)
- **Quilt** - Alternative to Fabric

## Installation Steps

### For Server Administrators

1. **Download the mod** for your platform (Fabric/Forge)
2. **Place the jar file** in your server's `mods` folder
3. **Start the server** - The mod will create its configuration directory
4. **Configure the mod** using the files in `config/puffish_skills/`
5. **Restart the server** to apply changes

### For Modpack Creators

1. **Add the mod** to your modpack dependencies
2. **Include configuration templates** from `config/templates/`
3. **Test compatibility** with other mods in your pack
4. **Document any custom configurations** for your users

## Configuration Quick Start

The mod creates several configuration files:

- `config.json` - Main mod configuration
- `categories/` - Individual skill category definitions
- `templates/` - Pre-made configuration templates

### Using Templates

Choose a template that matches your server type:

- `basic_config.json` - Simple setup with 3 skill categories
- `full_config.json` - Complete setup with all available skills
- `pvp_config.json` - PvP-focused configuration

Copy your chosen template to `config.json` and restart the server.

## First-Time Setup

1. Start your server with the mod installed
2. Use `/skillsadmin info` to verify the installation
3. Use `/skillsadmin platform` to check platform compatibility
4. Configure your skills using `/puffish_skills` commands

## Admin Commands

- `/skillsadmin info` - Show mod information
- `/skillsadmin reload` - Reload configurations without restart
- `/skillsadmin backup config` - Create configuration backup
- `/skillsadmin backup data` - Backup all player data
- `/skillsadmin platform` - Show platform information

## Integration with Other Mods

The Pufferfish Skills addon provides extensive integration capabilities:

- **API hooks** for other mods to interact with the skill system
- **Event system** for custom progression triggers
- **Data export/import** for migration between servers
- **Platform detection** for automatic compatibility

## Troubleshooting

### Common Issues

1. **Mod won't load**: Check platform compatibility and dependencies
2. **Configuration errors**: Validate your JSON files for syntax errors
3. **Performance issues**: Reduce skill categories or adjust experience sources

### Getting Help

1. Check the configuration validation using `/skillsadmin info`
2. Review server logs for error messages
3. Use `/skillsadmin backup` before making major changes
4. Test configurations on a development server first

## Next Steps

- Read the [Configuration Guide](configuration.md) for detailed setup
- Review [Integration Examples](integration.md) for mod compatibility
- Check [Troubleshooting Guide](troubleshooting.md) for common issues

## Version Compatibility

- **Minecraft**: 1.20+
- **Fabric API**: Required for Fabric installations
- **Forge**: 46+ required for Forge installations
- **Java**: 17+ required for all platforms