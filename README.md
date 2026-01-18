# Pufferfish Skill Leveling Addon

**An addon for Pufferfish's Skills mod that adds multi-level skill progression.**

> [!IMPORTANT]
> **This is an ADDON** - it works alongside the official Pufferfish Skills mod. Both mods must be installed together.

## 🆕 New Features

### Multi-Level Skills
- **Level-Based Progression**: Skills can have multiple levels (e.g., Level 1, 2, 3, etc.)
- **Per-Level Rewards**: Each level can grant different rewards when reached
- **Dynamic Descriptions**: Tooltips show current level and preview next level
- **Configurable Point Costs**: Set skill points required per level
- **Stackable Type**: New `puffish_skill_leveling:stackable` skill type that combines standard and per-level rewards

### Reward System
- **Per-Level Rewards**: Define specific rewards that trigger when reaching each level
- **Initial Unlock Rewards**: Standard rewards applied when first unlocking a skill
- **Custom Reward Type**: New `puffish_skill_leveling:per_level_rewards` reward type
- **Merge Description**: Option to accumulate descriptions across skill levels
- **Skill Prerequisites**: Require other skills to reach certain levels before unlocking

### Management
- **Level Tracking**: Track exact skill levels for each player
- **Administrative Commands**: Commands to get, set, advance, and refund skill levels
- **Data Persistence**: Skill levels saved with player data
- **Independent Storage**: Addon data stored separately from Skills mod data

## 🎮 New Commands

### Addon-Specific Commands
- `/skillleveling get <player> <category> <skill>` - Get the level of a skill
- `/skillleveling set <player> <category> <skill> <level>` - Set the level of a skill
- `/skillleveling advance <player> <category> <skill>` - Advance a skill by one level
- `/skillleveling refund one <player> <category> <skill>` - Refund one level
- `/skillleveling refund multiple <player> <category> <skill> <levels>` - Refund multiple levels
- `/skillleveling refund all <player> <category> <skill>` - Reset a skill to level 1


## ⚙️ Configuration

### Skill Definition Fields

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | Skill type. Use `puffish_skill_leveling:stackable` to mix normal and per-level rewards |
| `max_skill_level` / `max_levels` | integer | Maximum levels for this skill (inferred from rewards if omitted) |
| `points_per_level` | integer | Points required per level |
| `merge_description` | boolean | Accumulate descriptions (default: false) |
| `descriptions` | string[] | Description for each level |
| `extra_descriptions` | string[] | Preview for next level |

### Reward Type: `puffish_skill_leveling:per_level_rewards`

| Field | Type | Description |
|-------|------|-------------|
| `skill_id` | string | ID of the skill being leveled |
| `levels` | object | Maps level numbers to arrays of rewards |
| `max_skill_level` | integer | Optional override for max level |
| `points_per_level` | integer | Optional points cost per level |

### Examples

**Example 1: Basic Per-Level Skill**
```json
{
  "warrior": {
    "title": "Master Warrior",
    "icon": { "type": "item", "data": { "item": "minecraft:diamond_sword" } },
    "size": 1.0,
    "max_skill_level": 3,
    "points_per_level": 1,
    "merge_description": false,
    "descriptions": [
      "Current: +1 melee damage",
      "Current: +1 Exp Bottle",
      "Current: +2 Max Health"
    ],
    "extra_descriptions": [
      "Next: +1 Exp Bottle",
      "Next: +2 Max Health",
      "— MAXED OUT —"
    ],
    "rewards": [
      {
        "type": "puffish_skill_leveling:per_level_rewards",
        "data": {
          "skill_id": "warrior",
          "levels": {
            "1": [
              { "type": "puffish_skills:attribute",
                "data": { "attribute": "generic.attack_damage", "value": 1, "operation": "addition" } }
            ],
            "2": [
              { "type": "puffish_skills:command",
                "data": { "command": "give @p minecraft:experience_bottle 1" } }
            ],
            "3": [
              { "type": "puffish_skills:attribute",
                "data": { "attribute": "generic.max_health", "value": 2, "operation": "addition" } }
            ]
          }
        }
      }
    ]
  }
}
```

**Example 2: Stackable Skill Type (Combines Normal + Per-Level Rewards)**
```json
{
  "master_miner": {
    "type": "puffish_skill_leveling:stackable",
    "title": "Master Miner",
    "icon": { "type": "item", "data": { "item": "minecraft:diamond_pickaxe" } },
    "size": 1.0,
    "max_skill_level": 3,
    "points_per_level": 1,
    "required_points": 3,
    "merge_description": false,
    "descriptions": [
      "Current: +10% mining speed",
      "Current: +20% mining speed",
      "Current: +30% mining speed"
    ],
    "extra_descriptions": [
      "Next: +20% mining speed",
      "Next: +30% mining speed",
      "— MAXED OUT —"
    ],
    "rewards": [
      {
        "type": "puffish_skill_leveling:per_level_rewards",
        "data": {
          "skill_id": "master_miner",
          "levels": {
            "1": [
              { "type": "puffish_skills:attribute",
                "data": { "attribute": "generic.attack_damage", "value": 1, "operation": "addition" } }
            ],
            "2": [
              { "type": "puffish_skills:command",
                "data": { "command": "give @p minecraft:experience_bottle 1" } }
            ],
            "3": [
              { "type": "puffish_skills:attribute",
                "data": { "attribute": "generic.max_health", "value": 2, "operation": "addition" } }
            ]
          }
        }
      },
      {
        "type": "puffish_skills:command",
        "data": { "command": "give @p minecraft:diamond 1" }
      }
    ]
  }
}
```

**Example 3: Merged Descriptions**
```json
{
  "stacked_power": {
    "title": "Stacked Power",
    "max_skill_level": 3,
    "points_per_level": 1,
    "merge_description": true,
    "descriptions": [
      "+2 melee damage",
      "Obtain +2 Exp Bottles", 
      "+10 Health"
    ],
    "extra_descriptions": [
      "Next: +2 Exp Bottles",
      "Next: +10 Health",
      "— MAXED OUT —"
    ],
    "rewards": [
      {
        "type": "puffish_skill_leveling:per_level_rewards",
        "data": {
          "skill_id": "stacked_power",
          "levels": {
            "1": [
              { "type": "puffish_skills:attribute",
                "data": { "attribute": "generic.attack_damage", "value": 2, "operation": "addition" } }
            ],
            "2": [
              { "type": "puffish_skills:command",
                "data": { "command": "give @p minecraft:experience_bottle 2" } }
            ],
            "3": [
              { "type": "puffish_skills:attribute",
                "data": { "attribute": "generic.max_health", "value": 10, "operation": "addition" } }
            ]
          }
        }
      }
    ]
  }
}
```

### How Merge Description Works

**With `merge_description: false`:**
- Level 1: Shows only "Current: +1 melee damage"
- Level 2: Shows only "Current: +10% mining speed"
- Level 3: Shows only "Current: +15% mining speed"

**With `merge_description: true`:**
- Level 1: Shows "+2 melee damage"
- Level 2: Shows "+2 melee damage" + "+20% mining speed" (accumulated)
- Level 3: Shows all three descriptions accumulated


## 📋 Requirements

> [!IMPORTANT]
> **Both mods are required!** This addon does not work standalone.

- **Pufferfish Skills mod** 0.17.1+1.20 or newer ([Download from CurseForge](https://www.curseforge.com/minecraft/mc-mods/pufferfish-skills))
- **Java 17+** 
- **Minecraft 1.20**
- **Forge 47.4.10+** or **Fabric** depending on your platform choice

## 📦 Installation

1. Install **Pufferfish Skills** mod first
2. Install **Pufferfish Skill Leveling** addon
3. Launch Minecraft - both mods should load together

## 🚨 Compatibility Notice

## 📄 Example Template

See `skill-level-template.zip` for a complete working example demonstrating the addon's multi-level features.

## 🚧 Future Features

The following features are planned for future releases:

### Enhanced Reward System
- **Automatic Reward Stacking**: Higher levels automatically include benefits from all previous levels
- **Progressive Reward Scaling**: Rewards that scale based on current skill level
- **Reward Dependencies**: Rewards that unlock based on other skill levels

### Advanced Skill Management
- **Bulk Operations**: Advance multiple levels at once with single commands
- **Conditional Unlocking**: Skills that require specific levels of other skills as prerequisites
- **Skill Level Dependencies**: Inter-skill level requirements and unlock chains
- **Level-Based Skill Trees**: Complex branching based on skill levels

### Enhanced User Experience
- **GUI Integration**: Visual skill level management interface
- **Progress Bars**: Visual indicators of skill level progression
- **Skill Level Notifications**: Toast notifications for level advancement
- **Experience-Based Leveling**: Skills that level up automatically based on experience gained

### Additional Reward Types
- **Conditional Rewards**: Rewards that trigger based on player state or conditions
- **Temporary Rewards**: Time-limited benefits that refresh per level
- **Economic Integration**: Currency and economy-based rewards per level

### API Extensions
- **Developer API**: More comprehensive programmatic skill level management
- **Event Hooks**: Additional events for skill level changes and milestones
- **Integration APIs**: Better support for integration with other mods

---

*Want to contribute or suggest a feature? Open an issue on the project repository!*
