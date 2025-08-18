# Pufferfish Skill Leveling Addon

A comprehensive addon for the Pufferfish Skills mod that adds support for multiple levels within individual skills, with level-specific rewards, point costs, and progression systems.

## Features

### Per-Level Skill Rewards
- **Multiple Levels**: Each skill can have multiple upgrade levels beyond the base unlock
- **Level-Specific Rewards**: Different rewards for each level of a skill
- **Progressive Unlocking**: Skills advance through levels with proper requirements
- **Point Integration**: Optional point costs for skill level advancement

### Reward System
- **Per-Level Rewards**: The `per_level_rewards` reward type enables multi-level skills
- **Flexible Configuration**: JSON-based configuration in datapacks
- **Reward Stacking**: Each level can have multiple different reward types
- **Dynamic Application**: Rewards are applied/removed based on current skill level

### Command System
- **Level Management**: Commands to get, set, and advance skill levels
- **Refund System**: Comprehensive refund system for skill levels
- **Admin Tools**: Full administrative control over player skill levels

### Data Persistence
- **Server Integration**: Automatic data saving and loading
- **Player Tracking**: Per-player skill level data persistence
- **Category Organization**: Skills organized by category with proper isolation

## Commands

All commands require operator permission (level 2).

### Basic Level Management
- `/skillleveling get <player> <category> <skill>` - Get current skill level
- `/skillleveling set <player> <category> <skill> <level>` - Set skill to specific level  
- `/skillleveling advance <player> <category> <skill>` - Advance skill by one level

### Refund System
- `/skillleveling refund one <player> <category> <skill>` - Refund one level
- `/skillleveling refund multiple <player> <category> <skill> <levels>` - Refund multiple levels
- `/skillleveling refund all <player> <category> <skill>` - Reset skill to level 1

## Configuration

### Per-Level Rewards in Datapacks

Add the `per_level_rewards` reward type to any skill definition:

```json
{
  "type": "per_level_rewards",
  "skill_id": "mining_efficiency",
  "max_skill_level": 5,
  "points_per_level": 2,
  "merge_description": true,
  "descriptions": {
    "1": "§7Mining Speed: §a+10%",
    "2": "§7Mining Speed: §a+25%", 
    "3": "§7Mining Speed: §a+40%\n§7Bonus: §6Diamond Tools"
  },
  "extra_descriptions": {
    "1": "§8Basic mining improvement",
    "2": "§8Noticeable speed boost",
    "3": "§8Expert-level mining with rewards"
  },
  "levels": {
    "1": [
      {
        "type": "attribute",
        "attribute": "minecraft:generic.attack_damage",
        "operation": "addition",
        "value": 1.0
      }
    ],
    "2": [
      {
        "type": "attribute", 
        "attribute": "minecraft:generic.attack_damage",
        "operation": "addition",
        "value": 2.0
      }
    ],
    "3": [
      {
        "type": "command",
        "command": "give @s minecraft:diamond 1"
      },
      {
        "type": "attribute",
        "attribute": "minecraft:generic.attack_speed", 
        "operation": "addition",
        "value": 0.1
      }
    ]
  }
}
```

### Configuration Options

- **`skill_id`** (optional): Specific skill this reward applies to. If omitted, applies to the skill it's attached to
- **`max_skill_level`**: Maximum level this skill can reach (defaults to highest defined level)
- **`points_per_level`**: Point cost for each level advancement (defaults to 0)
- **`levels`**: Object defining rewards for each level
- **`descriptions`**: Object mapping level numbers to description strings shown in tooltips
- **`extra_descriptions`**: Object mapping level numbers to extra description strings (shown when holding Shift)
- **`merge_description`**: Boolean (defaults to false) - when true, descriptions accumulate from previous levels starting at level 2

### Level Definition

Each level in the `levels` object should contain an array of reward definitions. Each reward follows the standard Pufferfish Skills reward format:

```json
"1": [
  {
    "type": "attribute",
    "attribute": "minecraft:generic.max_health", 
    "operation": "addition",
    "value": 2.0
  },
  {
    "type": "command",
    "command": "tellraw @s {\"text\":\"You reached level 1 of this skill!\",\"color\":\"green\"}"
  }
]
```

### Description Merging Behavior

The `merge_description` feature controls how skill descriptions are displayed in tooltips:

**When `merge_description: false` (default):**
- Each level shows only its own description
- Level 1: "§7Mining Speed: §a+10%"
- Level 2: "§7Mining Speed: §a+25%" 
- Level 3: "§7Mining Speed: §a+40%\n§7Bonus: §6Diamond Tools"

**When `merge_description: true`:**
- Level 1: "§7Mining Speed: §a+10%" (shows only level 1)
- Level 2: "§7Mining Speed: §a+10%\n§7Mining Speed: §a+25%" (shows levels 1-2)
- Level 3: "§7Mining Speed: §a+10%\n§7Mining Speed: §a+25%\n§7Mining Speed: §a+40%\n§7Bonus: §6Diamond Tools" (shows levels 1-3)

This allows players to see their complete progression path and all active benefits when examining higher-level skills.

## Integration

### With Pufferfish Skills
- Fully compatible with existing Skills mod features
- Uses Skills mod command argument types
- Integrates with Skills mod point system
- Respects Skills mod skill unlock requirements

### Event System
- Automatic skill level initialization when skills are unlocked
- Proper cleanup when skills are locked
- Integration with Skills mod event callbacks

### Data Management
- Player data persistence across server restarts
- Category-based skill organization  
- Efficient in-memory caching with periodic saves

## Examples

### Mining Skill with Tool Efficiency
```json
{
  "type": "per_level_rewards",
  "skill_id": "enhanced_mining",
  "max_skill_level": 3,
  "points_per_level": 1,
  "levels": {
    "1": [
      {
        "type": "attribute",
        "attribute": "minecraft:player.block_break_speed",
        "operation": "multiply_total",
        "value": 0.1
      }
    ],
    "2": [
      {
        "type": "attribute", 
        "attribute": "minecraft:player.block_break_speed",
        "operation": "multiply_total",
        "value": 0.25
      }
    ],
    "3": [
      {
        "type": "attribute",
        "attribute": "minecraft:player.block_break_speed", 
        "operation": "multiply_total",
        "value": 0.5
      },
      {
        "type": "command",
        "command": "give @s minecraft:diamond_pickaxe{Enchantments:[{id:\"minecraft:efficiency\",lvl:5}]} 1"
      }
    ]
  }
}
```

### Combat Skill with Progressive Rewards
```json
{
  "type": "per_level_rewards",
  "skill_id": "warrior_training", 
  "max_skill_level": 5,
  "points_per_level": 3,
  "levels": {
    "1": [
      {
        "type": "attribute",
        "attribute": "minecraft:generic.attack_damage",
        "operation": "addition", 
        "value": 1.0
      }
    ],
    "2": [
      {
        "type": "attribute",
        "attribute": "minecraft:generic.attack_damage",
        "operation": "addition",
        "value": 2.0
      }
    ],
    "3": [
      {
        "type": "attribute",
        "attribute": "minecraft:generic.max_health",
        "operation": "addition",
        "value": 4.0
      }
    ],
    "4": [
      {
        "type": "attribute", 
        "attribute": "minecraft:generic.attack_speed",
        "operation": "multiply_total",
        "value": 0.2
      }
    ],
    "5": [
      {
        "type": "command",
        "command": "effect give @s minecraft:strength 300 1 true"
      }
    ]
  }
}
```

## Installation

1. Install Pufferfish Skills mod as a dependency
2. Add this addon to your mods folder
3. Create datapacks with `per_level_rewards` reward definitions
4. Configure skills in your datapack to use the new reward type

## Compatibility

- **Minecraft Version**: 1.20.1+  
- **Pufferfish Skills**: 0.16.3+
- **Mod Loaders**: Fabric, Forge

## Technical Details

### Architecture
- **Addon Design**: Extends Skills mod without modifying core functionality
- **Event-Driven**: Uses Skills mod event system for proper integration
- **Data Separation**: Maintains separate data structures for addon functionality
- **API Integration**: Uses proper Skills mod APIs for all interactions

### Performance
- **Efficient Caching**: In-memory skill level caching with lazy loading
- **Minimal Overhead**: Only processes skills that use per-level rewards
- **Optimized Queries**: Efficient data structures for fast lookups
- **Background Processing**: Non-blocking data persistence operations
