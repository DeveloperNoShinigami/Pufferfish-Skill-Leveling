# Pufferfish Skill Leveling Addon

This addon extends Pufferfish's Skills mod with **advanced multi-level progression** and **per-level reward stacking** capabilities.

## 🆕 New Features

### Enhanced Multi-Level Skills
- **Stackable Progression**: Skills can now have multiple levels (e.g., Level 1, 2, 3, etc.)
- **Progressive Rewards**: Each level grants additional benefits that stack with previous levels
- **Dynamic Descriptions**: Tooltips automatically update to show current and next level benefits
- **Flexible Point Costs**: Configure different point costs per level

### Advanced Reward System
- **Per-Level Rewards**: Define specific rewards for each skill level
- **Reward Stacking**: Higher levels automatically include benefits from all previous levels
- **Custom Reward Types**: New reward types specifically designed for leveled progression
- **Conditional Unlocking**: Skills can require specific levels of other skills
- **Merge Description**: Enhanced tooltip system that accumulates descriptions across skill levels

### Enhanced Management
- **Level Tracking**: Track exact skill levels for each player
- **Bulk Operations**: Advance multiple levels at once
- **Level Refunding**: Administrative commands to refund specific skill levels
- **Progress Persistence**: Skill levels saved with player data

## 🔄 Project Structure

This is a **standalone addon** that extends the base Skills mod without modifying it:

- **Package**: `net.bluelotuscoding.skillleveling`
- **Mod ID**: `puffish_skill_leveling`
- **Type**: Clean addon that depends on `puffish_skills`

## 🏗️ New Addon Components

### Core Classes
- **`SkillLevelingMod`** - Main addon integration
- **`SkillLevelingManager`** - Multi-level skill progression logic
- **`LeveledSkill`** - Enhanced skill wrapper with level tracking
- **`PerLevelReward`** - New reward type for level-specific benefits
- **`SkillLevelingEventListener`** - Lifecycle event handling

### New APIs
```java
// Check if player has specific skill level
addon.hasSkillLevel(player, categoryId, skillId, level);

// Get current skill level
int currentLevel = addon.getSkillLevel(player, categoryId, skillId);

// Advance to next level
addon.advanceSkillLevel(player, categoryId, skillId);
```

## 📦 Installation

1. Install the **Pufferfish's Skills** core mod (dependency)
2. Install this **Skill Leveling** addon
3. Addon automatically integrates on server startup

## 🎮 New Commands

### Addon-Specific Commands
- `/skillleveling info` - Display addon version and loaded features
- `/skillleveling check <player> <category> <skill>` - Check specific skill level
- `/skillleveling advance <player> <category> <skill> [levels]` - Advance skill levels
- `/skillleveling refund <player> <category> <skill> [levels]` - Refund skill levels
- `/skillleveling list <player>` - List all skill levels for a player

### Enhanced Core Commands
The addon extends existing Skills mod commands with level-aware functionality.

## ⚙️ New Datapack Configuration

### Enhanced Skill Definitions

The addon introduces new configuration options for multi-level skills:

#### Merge Description Feature

The addon enhances the `merge_description` functionality for better tooltip management:

```json
{
    "progressive_skill": {
        "merge_description": true,
        "descriptions": [
            "+1 melee damage",
            "+10% mining speed", 
            "+15% mining speed"
        ]
    }
}
```

**How it works:**
- **Level 1:** Shows only `"+1 melee damage"`
- **Level 2:** Shows `"+1 melee damage"` + `"+10% mining speed"` (accumulated)
- **Level 3:** Shows all three descriptions accumulated

When `merge_description` is `false` (default), each level shows only its individual description.

#### Stackable Skills with Per-Level Rewards
```json
{
    "enhanced_mining": {
        "type": "puffish_skills:stackable",
        "title": "Enhanced Mining",
        "max_skill_level": 5,
        "points_per_level": 2,
        "merge_description": true,
        "descriptions": [
            "Level 1: +10% mining speed",
            "Level 2: +20% mining speed", 
            "Level 3: +30% mining speed",
            "Level 4: +40% mining speed",
            "Level 5: +50% mining speed + Fortune"
        ],
        "rewards": [
            {
                "type": "puffish_skills:per_level_rewards",
                "data": {
                    "skill_id": "enhanced_mining_id",
                    "levels": {
                        "1": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "player.block_break_speed",
                                    "value": 0.1,
                                    "operation": "multiply_base"
                                }
                            }
                        ],
                        "2": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "player.block_break_speed",
                                    "value": 0.1,
                                    "operation": "multiply_base"
                                }
                            }
                        ],
                        "3": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "player.block_break_speed",
                                    "value": 0.1,
                                    "operation": "multiply_base"
                                }
                            }
                        ],
                        "4": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "player.block_break_speed",
                                    "value": 0.1,
                                    "operation": "multiply_base"
                                }
                            }
                        ],
                        "5": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "player.block_break_speed",
                                    "value": 0.1,
                                    "operation": "multiply_base"
                                }
                            },
                            {
                                "type": "puffish_skills:command",
                                "data": {
                                    "command": "enchant @p fortune 1"
                                }
                            }
                        ]
                    }
                }
            }
        ]
    }
}
```

### New Configuration Options
- **`max_skill_level`** - Maximum levels this skill can reach (addon feature)
- **`points_per_level`** - Points consumed for each additional level (addon feature)  
- **`merge_description`** - Whether level descriptions accumulate (addon enhancement)
- **`level_requirements`** - Require specific levels of other skills (addon feature)

## 🚀 Quick Start

### 1. Installation
```bash
# Install Dependencies
# 1. Install Pufferfish Skills mod (required)
# 2. Install this addon

# Development Setup
git clone <repository-url>
cd Pufferfish-Skill-Leveling
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew build
```

### 2. Test the Addon
```bash
# Fabric
./gradlew :Fabric:runClient

# Forge  
./gradlew :Forge:runClient
```

### 3. Create Multi-Level Skills
1. Extract `example-skill-level-template_new.zip` to your world's `datapacks` folder
2. Modify skill definitions to use `"type": "puffish_skills:stackable"`
3. Add `"max_skill_level"` and `"points_per_level"` to your skills
4. Use `"puffish_skills:per_level_rewards"` for level-specific benefits
5. Restart server or `/reload`

### 4. Test Your Multi-Level Skills
```bash
# Open skills GUI
/puffish_skills open <category>

# Check specific skill level
/skillleveling check <player> <category> <skill>

# Advance a skill level (admin)
/skillleveling advance <player> <category> <skill>
```

### 5. Addon APIs for Developers
```java
// Get addon instance
SkillLevelingMod addon = SkillLevelingMod.getInstance();

// Check skill level
boolean hasLevel3 = addon.hasSkillLevel(player, categoryId, skillId, 3);

// Get current level
int level = addon.getSkillLevel(player, categoryId, skillId);

// Advance level programmatically
addon.advanceSkillLevel(player, categoryId, skillId);
```

## 🔧 Development Benefits

### What This Addon Adds
- ✅ **Multi-level skill progression** - Skills can now have 2-10+ levels
- ✅ **Stacking reward system** - Each level adds to previous level benefits  
- ✅ **Dynamic progression costs** - Configure different point costs per level
- ✅ **Level-aware commands** - New commands for managing skill levels
- ✅ **Enhanced tooltips** - Show current level and next level previews
- ✅ **Flexible skill requirements** - Require specific levels of prerequisite skills

### Addon Architecture Benefits
- ✅ **Non-intrusive**: Extends without modifying the base Skills mod
- ✅ **Compatible**: Works with existing Skills mod datapacks
- ✅ **Modular**: Can be enabled/disabled independently
- ✅ **Future-proof**: Updates with new Skills mod versions

### Recent API Integration (August 2025)
- ✅ **Complete Skills mod API integration** with latest version (0.16.3+1.20)
- ✅ **Platform-specific implementations** for Fabric and Forge
- ✅ **Proper ServerPlatform interface** implementation 
- ✅ **Event-driven integration** through ServerEventListener
- ✅ **Factory-pattern reward registration** for custom reward types

## 📋 Requirements

- **Pufferfish Skills mod** 0.16.3+1.20 or newer (required dependency)
- **Java 17+** 
- **Minecraft 1.20**
- **Fabric Loader** or **Forge** depending on your platform choice

## 📄 Example Template

See `example-skill-level-template_new.zip` for a complete working example demonstrating the addon's multi-level features.
