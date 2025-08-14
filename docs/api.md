# Pufferfish Skills - API Documentation

## Overview

The Pufferfish Skills addon provides a comprehensive API for other mods to integrate with the skill system. This documentation covers all available integration points and provides examples for common use cases.

## Core API Access

### Basic API Usage

```java
import net.puffish.skillsmod.api.SkillsAPI;

// Check if the addon is available
if (SkillsAPI.getCurrentPlatform() != PlatformType.UNKNOWN) {
    // Addon is loaded and ready
}
```

### Platform Detection

```java
import net.puffish.skillsmod.api.platform.PlatformDetector;

// Detect current platform
PlatformDetector.PlatformType platform = SkillsAPI.getCurrentPlatform();

// Check for platform-specific features
boolean supportsMixins = PlatformDetector.supportsFeature(PlatformFeature.MIXINS);
```

## Integration Hooks

### Registering Integration Hooks

Integration hooks allow your mod to respond to skill system events:

```java
public class MyModIntegration implements IntegrationAPI.IntegrationHook {
    @Override
    public String getId() {
        return "my_mod_integration";
    }
    
    @Override
    public boolean isEnabled() {
        return true; // Your mod's configuration
    }
    
    @Override
    public void onPlayerJoin(ServerPlayerEntity player) {
        // Handle player join events
        // Access player skill data, apply bonuses, etc.
    }
    
    @Override
    public void onPlayerLeave(ServerPlayerEntity player) {
        // Handle player leave events
        // Save cached data, cleanup resources, etc.
    }
}

// Register your integration
SkillsAPI.registerIntegrationHook("my_mod", new MyModIntegration());
```

## Event System

### Skill Events

Listen for skill-related events:

```java
import net.puffish.skillsmod.api.integration.IntegrationAPI;

// Register an event listener
SkillsAPI.registerIntegrationEventListener("my_listener", event -> {
    switch (event.getEventType()) {
        case "skill_unlock":
            handleSkillUnlock((IntegrationAPI.SkillUnlockEvent) event);
            break;
        case "skill_lock":
            handleSkillLock((IntegrationAPI.SkillLockEvent) event);
            break;
        case "skill_progression":
            handleProgression((IntegrationAPI.SkillProgressionEvent) event);
            break;
    }
});
```

### Custom Event Triggers

Fire custom progression events:

```java
import java.util.Map;
import java.util.HashMap;

// Create custom event data
Map<String, Object> eventData = new HashMap<>();
eventData.put("source_mod", "my_mod");
eventData.put("trigger_type", "custom_action");
eventData.put("multiplier", 1.5);

// Fire a progression event
IntegrationAPI.fireProgressionEvent(
    player, 
    Identifier.of("puffish_skills", "combat"), 
    "my_mod_action", 
    100, 
    eventData
);
```

## Version Management

### Version Checking

```java
import net.puffish.skillsmod.api.version.VersionManager;

// Get version manager
Optional<VersionManager> versionManager = SkillsAPI.getVersionManager();

versionManager.ifPresent(vm -> {
    // Check current version
    String currentVersion = vm.getCurrentVersion();
    
    // Check for updates
    vm.checkForUpdates().thenAccept(updateAvailable -> {
        if (updateAvailable) {
            // Handle update notification
            String latestVersion = vm.getLatestVersion().orElse("unknown");
            // Notify administrators
        }
    });
    
    // Validate compatibility
    VersionManager.VersionCompatibility compatibility = 
        vm.validateConfigVersion(myConfigVersion);
        
    switch (compatibility) {
        case COMPATIBLE:
            // All good
            break;
        case OUTDATED:
            // Warn about old configuration
            break;
        case TOO_OLD:
        case TOO_NEW:
            // Handle incompatibility
            break;
    }
});
```

## Configuration Management

### Hot Reloading

```java
import net.puffish.skillsmod.api.config.ConfigurationManager;

// Get configuration manager
Optional<ConfigurationManager> configManager = SkillsAPI.getConfigurationManager();

configManager.ifPresent(cm -> {
    // Check if hot reload is enabled
    if (cm.isHotReloadEnabled()) {
        // Trigger a configuration reload
        cm.reloadConfigurations(server).thenAccept(success -> {
            if (success) {
                // Configuration reloaded successfully
                updateMyModConfiguration();
            } else {
                // Handle reload failure
                handleConfigurationError();
            }
        });
    }
    
    // Validate configurations
    cm.validateConfigurations(server).thenAccept(result -> {
        if (result.isValid()) {
            // Configuration is valid
        } else {
            // Handle validation errors
            logger.warn("Configuration validation failed: {}", result.message());
        }
    });
});
```

## Data Management

### Export and Import

```java
import net.puffish.skillsmod.api.data.DataManager;

// Get data manager
Optional<DataManager> dataManager = SkillsAPI.getDataManager();

dataManager.ifPresent(dm -> {
    // Export player data
    dm.exportPlayerData(player).thenAccept(result -> {
        if (result.success()) {
            // Data exported successfully
            Path exportFile = result.exportFile();
            // Process exported data
        }
    });
    
    // Export all player data
    dm.exportAllPlayerData().thenAccept(result -> {
        if (result.success()) {
            // Bulk export completed
            archiveExportedData(result.exportFile());
        }
    });
    
    // Import data
    Path importFile = getImportFile();
    dm.importPlayerData(importFile).thenAccept(result -> {
        if (result.success()) {
            // Data imported successfully
            notifyPlayersOfImport();
        } else {
            // Handle import failure
            logger.error("Import failed: {}", result.message());
        }
    });
});
```

## Experience Sources

### Custom Experience Sources

```java
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceFactory;

public class MyExperienceSource implements ExperienceSource {
    private final String sourceId;
    private final int baseAmount;
    
    public MyExperienceSource(String sourceId, int baseAmount) {
        this.sourceId = sourceId;
        this.baseAmount = baseAmount;
    }
    
    @Override
    public String getId() {
        return sourceId;
    }
    
    @Override
    public int getExperience(ServerPlayerEntity player) {
        // Calculate experience based on player context
        // Apply your mod's multipliers, conditions, etc.
        return baseAmount * getMyModMultiplier(player);
    }
}

// Register the experience source
SkillsAPI.registerExperienceSource(
    Identifier.of("my_mod", "custom_source"),
    new ExperienceSourceFactory() {
        @Override
        public ExperienceSource create(JsonObject config) {
            String sourceId = config.get("source_id").getAsString();
            int baseAmount = config.get("base_amount").getAsInt();
            return new MyExperienceSource(sourceId, baseAmount);
        }
    }
);
```

### Updating Experience

```java
// Update all experience sources for a player
SkillsAPI.updateExperienceSources(player, experienceSource -> {
    // Return experience amount to award
    if (experienceSource instanceof MyExperienceSource) {
        return calculateMyModExperience(player);
    }
    return 0; // No experience for other sources
});

// Update specific type of experience sources
SkillsAPI.updateExperienceSources(player, MyExperienceSource.class, source -> {
    return source.getExperience(player);
});
```

## Rewards

### Custom Rewards

```java
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardFactory;

public class MyCustomReward implements Reward {
    private final String itemId;
    private final int amount;
    
    public MyCustomReward(String itemId, int amount) {
        this.itemId = itemId;
        this.amount = amount;
    }
    
    @Override
    public void apply(ServerPlayerEntity player) {
        // Give custom reward to player
        giveCustomItem(player, itemId, amount);
    }
    
    @Override
    public void remove(ServerPlayerEntity player) {
        // Remove reward if skill is locked
        removeCustomItem(player, itemId, amount);
    }
}

// Register the reward
SkillsAPI.registerReward(
    Identifier.of("my_mod", "custom_item"),
    new RewardFactory() {
        @Override
        public Reward create(JsonObject config) {
            String itemId = config.get("item").getAsString();
            int amount = config.get("amount").getAsInt();
            return new MyCustomReward(itemId, amount);
        }
    }
);
```

## Player Data Access

### Reading Skill Data

```java
import net.puffish.skillsmod.SkillsMod;

// Get player's experience in a category
Optional<Integer> experience = SkillsMod.getInstance()
    .getExperience(player, Identifier.of("puffish_skills", "combat"));

// Get player's points in a category
Optional<Integer> points = SkillsMod.getInstance()
    .getPoints(player, Identifier.of("puffish_skills", "combat"), "combat_kills");

// Check if player has unlocked a skill
boolean hasSkill = checkPlayerHasSkill(player, "combat", "advanced_sword");
```

### Modifying Skill Data

```java
// Add points to a player
SkillsMod.getInstance().addPoints(
    player,
    Identifier.of("puffish_skills", "combat"),
    "my_mod_source",
    10,
    false // not silent
);

// Add experience
SkillsMod.getInstance().addExperience(
    player,
    Identifier.of("puffish_skills", "mining"),
    "my_mod_mining",
    50
);
```

## Testing Integration

### Integration Testing

```java
import net.puffish.skillsmod.test.AddonTestFramework;

// Create test framework
AddonTestFramework testFramework = new AddonTestFramework();

// Run integration tests
testFramework.runAllTests().thenAccept(results -> {
    if (results.allPassed()) {
        logger.info("All integration tests passed!");
    } else {
        logger.warn("Some tests failed: {}/{} passed", 
            results.passed(), results.passed() + results.failed());
        
        // Log failed tests
        results.results().stream()
            .filter(result -> !result.passed())
            .forEach(result -> 
                logger.error("Test failed: {} - {}", 
                    result.testName(), result.message())
            );
    }
});
```

## Best Practices

### Error Handling

```java
// Always check if components are available
SkillsAPI.getDataManager().ifPresentOrElse(
    dataManager -> {
        // Use data manager
    },
    () -> {
        // Handle missing component
        logger.warn("Skills data manager not available");
    }
);
```

### Asynchronous Operations

```java
// Use CompletableFuture for non-blocking operations
CompletableFuture.allOf(
    versionManager.checkForUpdates(),
    configManager.validateConfigurations(server),
    dataManager.exportPlayerData(player)
).thenRun(() -> {
    // All operations completed
    logger.info("All skill operations completed");
});
```

### Resource Cleanup

```java
// Clean up resources when your mod is disabled
public void onDisable() {
    // Unregister integration hooks
    IntegrationAPI.unregisterIntegrationHook("my_mod");
    
    // Cancel pending operations
    cancelPendingOperations();
    
    // Save any cached data
    saveCachedData();
}
```

## Example Integrations

### Combat Mod Integration

```java
public class CombatModIntegration {
    public void onEntityKill(ServerPlayerEntity player, Entity killed) {
        // Award combat experience
        Map<String, Object> data = new HashMap<>();
        data.put("entity_type", killed.getType().toString());
        data.put("weapon_used", getPlayerWeapon(player));
        
        IntegrationAPI.fireProgressionEvent(
            player,
            Identifier.of("puffish_skills", "combat"),
            "entity_kill",
            getKillExperience(killed),
            data
        );
    }
}
```

### Economy Mod Integration

```java
public class EconomyIntegration implements IntegrationAPI.IntegrationHook {
    @Override
    public void onPlayerJoin(ServerPlayerEntity player) {
        // Apply economy bonuses based on skills
        applySkillBasedEconomyBonuses(player);
    }
    
    private void applySkillBasedEconomyBonuses(ServerPlayerEntity player) {
        // Check player's trading skills
        Optional<Integer> tradingLevel = getSkillLevel(player, "trading");
        tradingLevel.ifPresent(level -> {
            // Apply trading bonuses
            setPlayerTradingMultiplier(player, 1.0 + (level * 0.1));
        });
    }
}
```