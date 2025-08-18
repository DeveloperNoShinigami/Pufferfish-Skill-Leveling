# Pufferfish Skill Leveling Addon

This addon extends Pufferfish's Skills mod with **multi-level skill progression** capabilities.

## 🆕 New Features

### Multi-Level Skills
- **Level-Based Progression**: Skills can have multiple levels (e.g., Level 1, 2, 3, etc.)
- **Per-Level Rewards**: Each level can grant different rewards when reached
- **Dynamic Descriptions**: Tooltips show current level and preview next level
- **Configurable Point Costs**: Set skill points required per level

### Reward System
- **Per-Level Rewards**: Define specific rewards that trigger when reaching each level
- **Initial Unlock Rewards**: Standard rewards applied when first unlocking a skill
- **Custom Reward Type**: New `puffish_skill_leveling:per_level_rewards` reward type
- **Merge Description**: Option to accumulate descriptions across skill levels

### Management
- **Level Tracking**: Track exact skill levels for each player
- **Administrative Commands**: Commands to get, set, advance, and refund skill levels
- **Data Persistence**: Skill levels saved with player data

## 🎮 New Commands

### Addon-Specific Commands
- `/skillleveling get <player> <category> <skill>` - Get the level of a skill
- `/skillleveling set <player> <category> <skill> <level>` - Set the level of a skill
- `/skillleveling advance <player> <category> <skill>` - Advance a skill by one level
- `/skillleveling refund one <player> <category> <skill>` - Refund one level
- `/skillleveling refund multiple <player> <category> <skill> <levels>` - Refund multiple levels
- `/skillleveling refund all <player> <category> <skill>` - Reset a skill to level 1


## ⚙️ Configuration

### New Fields

| Field | Type | Description |
|-------|------|-------------|
| `max_skill_level` | integer | Maximum levels for this skill |
| `points_per_level` | integer | Points required per level |
| `merge_description` | boolean | Accumulate descriptions (default: false) |
| `descriptions` | string[] | Description for each level |
| `extra_descriptions` | string[] | Preview for next level |

### Examples

**Example 1: Individual Descriptions (merge_description: false)**
```json
{
  "stacked_power": {
    "title": "Master Miner",
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
          "skill_id": "19aazycn9ii0lfh1",
          "levels": {
            "1": [
              { "type": "puffish_skills:attribute",
                "data": { "attribute": "generic.attack_damage", "value": 10, "operation": "addition" } }
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

**Example 2: Merged Descriptions (merge_description: true)**
```json
{
  "stacked_power_2": {
    "title": "Master Miner 2",
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
          "skill_id": "ykn32h02xhgaujyr",
          "levels": {
            "1": [
              { "type": "puffish_skills:attribute",
                "data": { "attribute": "generic.attack_damage", "value": 20, "operation": "addition" } }
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
      },
      { 
        "type": "puffish_skills:command",
        "data": { "command": "give @p minecraft:experience_bottle 1" }
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


## � Important Datapack Compatibility Notice 🚨

If you encounter errors like these when loading your datapacks:

```
Unused field `type` at `skill_name`
Unused field `max_skill_level` at `skill_name`
Unused field `points_per_level` at `skill_name`
Unused field `merge_description` at `skill_name`
Unused field `descriptions` at `skill_name`
Expected a valid reward type at `type` at index 0 at `rewards` at `skill_name`
```

See the `example_datapack` folder for a complete working example.

## 📋 Requirements

- **Pufferfish Skills mod** 0.16.3+1.20 or newer (required dependency)
- **Java 17+** 
- **Minecraft 1.20**
- **Fabric ** or **Forge** depending on your platform choice

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
