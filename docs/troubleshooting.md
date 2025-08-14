# Pufferfish Skills - Troubleshooting Guide

## Common Issues and Solutions

### Installation Issues

#### Mod Won't Load

**Symptoms:**
- Server fails to start with the mod installed
- "Mod not found" errors in logs
- Platform compatibility warnings

**Solutions:**

1. **Check Platform Compatibility**
   ```bash
   # Use admin command to check platform
   /skillsadmin platform
   ```
   - Ensure you downloaded the correct version (Fabric/Forge)
   - Verify Minecraft version compatibility (1.20+)
   - Check if required dependencies are installed

2. **Verify Dependencies**
   - **Fabric**: Requires Fabric API
   - **Forge**: Requires Forge 46+
   - **Java**: Requires Java 17+

3. **Check Mod Placement**
   - Place mod jar in `mods/` folder
   - Remove any old versions of the mod
   - Ensure file isn't corrupted (re-download if needed)

#### Configuration Not Loading

**Symptoms:**
- Default configuration not created
- Custom configurations ignored
- "Configuration missing" warnings

**Solutions:**

1. **Check Directory Structure**
   ```
   config/puffish_skills/
   ├── config.json
   ├── categories/
   └── templates/
   ```

2. **Validate Configuration Syntax**
   ```bash
   /skillsadmin validate config
   ```

3. **Use Templates**
   ```bash
   # Copy working template
   cp config/puffish_skills/templates/basic_config.json config/puffish_skills/config.json
   ```

### Runtime Issues

#### Skills Not Working

**Symptoms:**
- Players can't unlock skills
- Experience not being awarded
- Skill trees not displaying

**Solutions:**

1. **Check Category Loading**
   ```bash
   /skillsadmin info
   /skillsadmin list categories
   ```

2. **Verify Player Permissions**
   - Ensure players have permission to use `/puffish_skills`
   - Check if skills are properly configured in categories

3. **Reload Configuration**
   ```bash
   /skillsadmin reload
   ```

#### Performance Issues

**Symptoms:**
- Server lag when players open skill interface
- High memory usage
- Slow experience calculation

**Solutions:**

1. **Optimize Configuration**
   - Reduce number of active skill categories
   - Simplify experience source calculations
   - Disable unused features

2. **Check Resource Usage**
   ```bash
   # Monitor server performance
   /skillsadmin info
   ```

3. **Adjust Settings**
   ```json
   {
     "version": 3,
     "show_warnings": false,
     "categories": ["combat", "mining"]
   }
   ```

### Configuration Issues

#### Hot Reload Failures

**Symptoms:**
- Configuration changes not applied
- Errors when using `/skillsadmin reload`
- Players see outdated skill data

**Solutions:**

1. **Check Configuration Syntax**
   ```bash
   /skillsadmin validate config
   ```

2. **Review Error Messages**
   - Check server console for specific errors
   - Fix JSON syntax issues
   - Verify all referenced files exist

3. **Manual Restart**
   - If hot reload fails, restart the server
   - Check that configurations are valid before restart

#### Version Compatibility

**Symptoms:**
- "Configuration outdated" warnings
- "Too new configuration" errors
- Features not working as expected

**Solutions:**

1. **Check Version Compatibility**
   ```bash
   /skillsadmin version check
   ```

2. **Update Configuration Version**
   ```json
   {
     "version": 3,
     // ... rest of config
   }
   ```

3. **Use Migration Tools**
   ```bash
   /skillsadmin backup config
   /skillsadmin migrate config
   ```

### Data Issues

#### Player Data Lost

**Symptoms:**
- Players' skills reset
- Experience progress missing
- Skill unlocks reverted

**Solutions:**

1. **Check for Backups**
   ```bash
   # List available backups
   ls config/puffish_skills/data/
   ```

2. **Restore from Backup**
   ```bash
   /skillsadmin import data backup_file.json
   ```

3. **Export Current Data**
   ```bash
   # Create backup before troubleshooting
   /skillsadmin backup data
   ```

#### Export/Import Failures

**Symptoms:**
- Export commands fail
- Import files corrupted
- Data migration errors

**Solutions:**

1. **Check File Permissions**
   - Ensure server can write to data directory
   - Verify backup files are readable

2. **Validate Export Files**
   - Check that exported JSON is valid
   - Ensure all required fields are present

3. **Use Incremental Exports**
   ```bash
   # Export specific players instead of all data
   /puffish_skills export player <username>
   ```

### Integration Issues

#### Mod Compatibility

**Symptoms:**
- Other mods conflicting with skills
- Features not working with specific mods
- Crashes when certain mods are loaded

**Solutions:**

1. **Check Integration Status**
   ```bash
   /skillsadmin info
   ```

2. **Disable Conflicting Features**
   - Temporarily disable other mods
   - Identify specific conflict sources
   - Use compatibility layers if available

3. **Update Integration Hooks**
   ```java
   // Register proper integration
   SkillsAPI.registerIntegrationHook("my_mod", new MyModIntegration());
   ```

#### API Integration Failures

**Symptoms:**
- Custom experience sources not working
- Events not firing
- Integration hooks not called

**Solutions:**

1. **Verify API Usage**
   ```java
   // Check if components are available
   SkillsAPI.getVersionManager().ifPresentOrElse(
       versionManager -> { /* Use manager */ },
       () -> { /* Handle missing component */ }
   );
   ```

2. **Test Integration**
   ```java
   // Run integration tests
   AddonTestFramework testFramework = new AddonTestFramework();
   testFramework.runAllTests();
   ```

3. **Check Event Registration**
   ```java
   // Ensure events are properly registered
   SkillsAPI.registerIntegrationEventListener("test", event -> {
       logger.info("Event received: {}", event.getEventType());
   });
   ```

## Diagnostic Commands

### Information Commands

```bash
# Show comprehensive addon information
/skillsadmin info

# Display platform capabilities
/skillsadmin platform

# Check version status
/skillsadmin version check

# List loaded categories
/skillsadmin list categories
```

### Validation Commands

```bash
# Validate all configurations
/skillsadmin validate config

# Validate specific category
/skillsadmin validate category combat

# Test configuration reload
/skillsadmin test-reload
```

### Backup Commands

```bash
# Create configuration backup
/skillsadmin backup config

# Create player data backup
/skillsadmin backup data

# Export all data
/skillsadmin export all
```

## Debug Mode

Enable detailed logging for troubleshooting:

```json
{
    "version": 3,
    "show_warnings": true,
    "debug_mode": true,
    "categories": [...]
}
```

This will provide:
- Detailed initialization logs
- Configuration loading information
- API call traces
- Performance metrics

## Getting Additional Help

### Log Analysis

When reporting issues, include:

1. **Server startup logs** - First 100 lines after mod loading
2. **Error messages** - Full stack traces from console
3. **Configuration files** - Your `config.json` and relevant category configs
4. **Platform information** - Output from `/skillsadmin platform`
5. **Addon information** - Output from `/skillsadmin info`

### Community Resources

- Check existing documentation in the `docs/` folder
- Review API examples for integration issues
- Test with minimal configurations to isolate problems

### Reporting Bugs

When reporting issues:

1. **Use minimal reproduction** - Remove unnecessary mods/configs
2. **Include diagnostic output** - Run admin commands and include output
3. **Describe expected behavior** - What should happen vs. what actually happens
4. **List steps to reproduce** - Exact steps to trigger the issue

## Prevention

### Best Practices

1. **Always backup** before making changes
2. **Test configurations** on development servers
3. **Use templates** as starting points
4. **Monitor performance** after changes
5. **Keep documentation updated** for your team
6. **Use version control** for configuration files

### Regular Maintenance

```bash
# Weekly maintenance routine
/skillsadmin backup data
/skillsadmin validate config
/skillsadmin version check
```

This ensures:
- Data is backed up regularly
- Configurations remain valid
- Updates are available when needed