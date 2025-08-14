# Pufferfish Skills - Standalone Addon

A comprehensive, easily integrable skill leveling system for Minecraft servers and modpacks. This enhanced version transforms the original Pufferfish Skills mod into a plug-and-play addon with extensive configuration, integration, and management capabilities.

## 🚀 Key Features

### Standalone & Integrable
- **Multi-platform support**: Fabric, Forge, NeoForge, and Quilt compatibility
- **Automatic platform detection**: Adapts behavior based on hosting environment
- **Plug-and-play setup**: Works out of the box with minimal configuration
- **Extensive API**: Rich integration points for other mods

### Enhanced Management
- **Hot-reloading**: Update configurations without server restarts
- **Admin commands**: Comprehensive administrative control
- **Data export/import**: Backup and migrate player skill data
- **Version management**: Automatic update checking and compatibility validation

### Developer-Friendly
- **Comprehensive API**: Well-documented integration system
- **Event system**: Custom progression triggers and hooks
- **Testing framework**: Automated validation of functionality
- **Rich documentation**: Installation, configuration, and API guides

## 📦 Installation

### Quick Start

1. **Download** the appropriate version for your platform (Fabric/Forge)
2. **Place** the jar file in your server's `mods` folder
3. **Start** the server to generate initial configuration
4. **Configure** using provided templates or admin commands
5. **Restart** and enjoy!

### Platform Requirements

- **Minecraft**: 1.20+
- **Java**: 17+
- **Fabric API**: Required for Fabric installations
- **Forge**: 46+ required for Forge installations

## ⚙️ Configuration

### Quick Configuration with Templates

Choose from pre-made templates:

```bash
# Copy template to main config
cp config/puffish_skills/templates/basic_config.json config/puffish_skills/config.json
```

Available templates:
- `basic_config.json` - Simple setup (3 categories)
- `full_config.json` - Complete setup (10 categories)
- `pvp_config.json` - PvP-optimized setup

### Hot Reloading

Update configurations without server restart:

```bash
/skillsadmin reload
```

### Admin Commands

- `/skillsadmin info` - Show addon status and information
- `/skillsadmin reload` - Hot-reload all configurations
- `/skillsadmin backup config` - Create configuration backup
- `/skillsadmin backup data` - Backup all player data
- `/skillsadmin export all` - Export all player data
- `/skillsadmin version check` - Check for updates
- `/skillsadmin platform` - Show platform capabilities

## 🔧 Integration

### For Mod Developers

Register integration hooks:

```java
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.integration.IntegrationAPI;

// Register your mod's integration
SkillsAPI.registerIntegrationHook("my_mod", new MyModIntegration());

// Listen for skill events
SkillsAPI.registerIntegrationEventListener("my_listener", event -> {
    // Handle skill progression events
});
```

### For Server Administrators

The addon provides extensive configuration options:

- **Skill categories**: Choose which skill trees to enable
- **Experience sources**: Customize how players gain experience
- **Progression rates**: Adjust leveling speed and difficulty
- **Rewards**: Configure skill unlock benefits

## 📊 Data Management

### Export & Import

Backup and migrate player data:

```bash
# Export all player data
/skillsadmin export all

# Export specific player
/puffish_skills export player <username>

# Import from backup
/skillsadmin import data backup_file.json
```

### Automatic Backups

Configure automatic data backups:

```json
{
    "data_export": {
        "enabled": true,
        "interval": "daily",
        "retain_days": 30
    }
}
```

## 🧪 Testing & Validation

### Built-in Testing Framework

Validate addon functionality:

```java
// Run automated tests
AddonTestFramework testFramework = new AddonTestFramework();
testFramework.runAllTests().thenAccept(results -> {
    if (results.allPassed()) {
        logger.info("All tests passed!");
    }
});
```

### Configuration Validation

Validate configurations before applying:

```bash
/skillsadmin validate config
/skillsadmin validate category combat
```

## 📚 Documentation

Comprehensive guides available in the `docs/` directory:

- [Installation Guide](docs/installation.md) - Platform-specific setup instructions
- [Configuration Guide](docs/configuration.md) - Detailed configuration options
- [API Documentation](docs/api.md) - Integration examples and API reference

## 🔄 Version Management

### Automatic Updates

The addon includes built-in update checking:

```bash
# Check for updates
/skillsadmin version check

# Show current version info
/skillsadmin info
```

### Configuration Migration

Automatic migration between configuration versions:

- **Version validation**: Ensures compatibility
- **Automatic upgrades**: Migrates old configurations
- **Rollback support**: Restore from backups if needed

## 🏗️ Architecture

### Modular Design

```
Common/
├── api/                     # Public API interfaces
│   ├── config/             # Configuration management
│   ├── data/               # Data export/import
│   ├── integration/        # Mod integration hooks
│   ├── platform/           # Platform detection
│   └── version/            # Version management
├── commands/               # Admin commands
├── config/                 # Configuration system
└── test/                   # Testing framework

Resources/
├── config/
│   └── templates/          # Configuration templates
└── docs/                   # Documentation
```

### Backward Compatibility

All enhancements maintain full backward compatibility:

- ✅ Existing configurations work unchanged
- ✅ Original API methods preserved
- ✅ No breaking changes to core functionality
- ✅ Gradual migration path for new features

## 🤝 Contributing

This addon is designed to be easily extensible:

1. **Fork** the repository
2. **Create** feature branches for new functionality
3. **Test** changes using the built-in testing framework
4. **Document** new features and API changes
5. **Submit** pull requests with clear descriptions

## 📄 License

This project maintains the original licensing terms. See `LICENSE.txt` for details.

## 🔗 Links

- [Original Pufferfish Skills](https://github.com/DeveloperNoShinigami/Pufferfish-Skill-Leveling)
- [Installation Guide](docs/installation.md)
- [Configuration Guide](docs/configuration.md)
- [API Documentation](docs/api.md)

---

## Summary of Enhancements

This enhanced version adds:

### 🎯 Core Enhancements
- **Version Management**: Update checking and compatibility validation
- **Platform Detection**: Automatic mod loader detection and adaptation  
- **Configuration Hot-Reloading**: Update configs without server restart
- **Data Export/Import**: Comprehensive backup and migration tools
- **Enhanced API**: Rich integration points for other mods

### 🛠️ Admin Tools
- **Admin Commands**: `/skillsadmin` command suite for server management
- **Configuration Templates**: Pre-made setups for different server types
- **Validation Tools**: Test configurations before applying
- **Backup Systems**: Automated data protection

### 📖 Documentation
- **Installation Guides**: Platform-specific setup instructions
- **Configuration Manual**: Detailed configuration options and examples
- **API Reference**: Complete integration documentation with examples
- **Testing Framework**: Automated validation of functionality

### 🔧 Developer Features
- **Integration Hooks**: Easy mod integration points
- **Event System**: Custom progression triggers and listeners
- **Testing Framework**: Automated functionality validation
- **Rich API**: Comprehensive programmatic access

The result is a truly standalone, plug-and-play addon that maintains all original functionality while providing extensive customization, integration, and management capabilities for both server administrators and modpack creators.