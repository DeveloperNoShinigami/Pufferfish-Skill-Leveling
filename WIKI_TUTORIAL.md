# Pufferfish Skill Leveling - Complete Tutorial

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Setting Up Your First Skill](#setting-up-your-first-skill)
4. [Understanding Skill Definitions](#understanding-skill-definitions)
5. [Configuring Per-Level Rewards](#configuring-per-level-rewards)
6. [Testing Your Skills](#testing-your-skills)
7. [Command Reference](#command-reference)
8. [Advanced Configuration](#advanced-configuration)
9. [Real-Time Features](#real-time-features)
10. [Troubleshooting](#troubleshooting)

---

## Overview

The Pufferfish Skill Leveling addon extends the official Puffish Skills mod with a comprehensive leveling system. It allows you to create skills that players can advance through multiple levels, each with their own rewards, descriptions, and requirements.

### Key Features
- **Multi-level skills** with configurable max levels
- **Per-level rewards** including attributes, commands, and more
- **Dynamic descriptions** that change based on player's current level
- **Real-time synchronization** with immediate visual feedback
- **Complete command suite** for managing skill levels
- **Merge descriptions** for cumulative vs. per-level display
- **Progress visualization** with progress bars and level indicators

---

## Prerequisites

### Required Mods
1. **Puffish Skills 0.16.3** (base mod)
2. **Pufferfish Skill Leveling** (this addon)
3. **Fabric** or **Forge** (depending on your setup)

### Recommended Tools
- **[Puffish Skills Web Editor](https://puffish.net/skillsmod/editor/)** - Visual skill tree designer that generates proper metadata and file structure

### Folder Structure
Your datapack should follow this structure:
```
your_datapack/
└── data/
    └── your_namespace/
        └── puffish_skills/
            ├── config.json
            └── categories/
                └── your_category/
                    ├── category.json
                    ├── skills.json
                    ├── definitions.json
                    ├── connections.json
                    └── experience.json
```

---

## Setting Up Your First Skill

### Step 1: Create the Category Configuration

First, create your category structure. Edit `config.json`:

```json
{
    "version": 3,
    "categories": [
        "combat"
    ]
}
```

### Step 2: Define Your Category

Create `categories/combat/category.json`:

```json
{
    "title": "Combat Skills",
    "icon": {
        "type": "item",
        "data": {
            "item": "iron_sword"
        }
    },
    "background": "textures/gui/advancements/backgrounds/stone.png"
}
```

### Step 3: Add Skills to the Tree

Create `categories/combat/skills.json`:

```json
{
    "combat_skill_001": {
        "definition": "warrior_strength",
        "x": 0,
        "y": 0,
        "root": true
    },
    "combat_skill_002": {
        "definition": "berserker_rage",
        "x": 64,
        "y": 0,
        "connections": ["combat_skill_001"]
    }
}
```

**⚠️ IMPORTANT:** The keys in this file (`combat_skill_001`, `combat_skill_002`) are the **actual skill IDs** that the game uses. These are what you'll reference in commands and the `skill_id` field in your reward configurations.

---

## Understanding Skill Definitions

### Critical: Skill ID vs Definition Name

Before diving into skill definitions, it's crucial to understand the difference between:

- **Skill ID** (from `skills.json`): The actual unique identifier the game uses (`combat_skill_001`)
- **Definition Name** (from `definitions.json`): A human-readable reference name (`warrior_strength`)

**Example Mapping:**
```json
// skills.json
{
    "combat_skill_001": {  // ← This is the REAL skill ID
        "definition": "warrior_strength"  // ← This references the definition
    }
}

// definitions.json  
{
    "warrior_strength": {  // ← This is just a definition name
        "title": "Warrior Strength",
        "rewards": [{
            "data": {
                "skill_id": "combat_skill_001"  // ← Must use the REAL skill ID
            }
        }]
    }
}
```

### Step 4: Create Skill Definitions

Create `categories/combat/definitions.json` with your skill definitions. **Remember: This follows Puffish Skills format first, then our addon enhances it**.

```json
{
    "warrior_strength": {
        "title": "Warrior Strength",
        "icon": {
            "type": "item",
            "data": {
                "item": "minecraft:iron_sword"
            }
        },
        "size": 1.0,
        "max_skill_level": 5,
        "points_per_level": 1,
        "merge_description": false,
        "descriptions": [
            "Level 1: +2 attack damage",
            "Level 2: +4 attack damage", 
            "Level 3: +6 attack damage",
            "Level 4: +8 attack damage",
            "Level 5: +10 attack damage"
        ],
        "extra_descriptions": [
            "Next: +4 attack damage",
            "Next: +6 attack damage",
            "Next: +8 attack damage", 
            "Next: +10 attack damage",
            "— MAXED OUT —"
        ],
        "rewards": [
            {
                "type": "puffish_skill_leveling:per_level_rewards",
                "data": {
                    "skill_id": "combat_skill_001",
                    "levels": {
                        "1": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 2,
                                    "operation": "addition"
                                }
                            }
                        ],
                        "2": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 4,
                                    "operation": "addition"
                                }
                            }
                        ],
                        "3": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 6,
                                    "operation": "addition"
                                }
                            }
                        ],
                        "4": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 8,
                                    "operation": "addition"
                                }
                            }
                        ],
                        "5": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 10,
                                    "operation": "addition"
                                }
                            }
                        ]
                    }
                }
            }
        ],
        "metadata": {"icon": "unique_icon_id_12345"}
    }
}
```

### Key Configuration Options

#### **max_skill_level**
- Defines how many levels this skill can be advanced to
- Range: 1-999 (practical limit)
- Example: `"max_skill_level": 5` allows levels 1-5

#### **points_per_level**
- Cost in skill points to advance one level
- Must have enough skill points to level up
- Example: `"points_per_level": 2` costs 2 points per level

#### **merge_description**
- `false`: Shows only current level description
- `true`: Shows cumulative description of all levels up to current

#### **descriptions[]**
- Array of descriptions for each level (1-indexed)
- Shows what the player currently has at this level
- Example: `"Level 3: +6 attack damage"`

#### **extra_descriptions[]**
- Array of descriptions for what the next level provides
- Shows what the player will get by advancing
- Use `"— MAXED OUT —"` for final level

### Critical Fields Explanation

#### Puffish Skills Required Fields (Base Mod)
- **`metadata`**: **REQUIRED** - Contains icon ID for skill tree positioning
  ```json
  "metadata": {"icon": "unique_icon_id_12345"}
  ```
  **Note:** The metadata field is automatically generated when you use the [Puffish Skills Web Editor](https://puffish.net/skillsmod/editor/). The editor creates unique icon IDs for proper skill tree positioning and visual management.
- **`title`**: Display name shown in skill tree
- **`icon`**: Visual representation in the skill tree
- **`size`**: Scale factor for the skill icon (typically 1.0)

#### Skill Leveling Enhancement Fields (Our Addon)
- **`max_skill_level`**: How many levels this skill can reach
- **`points_per_level`**: Skill points required per advancement
- **`skill_id`**: Must match the UUID from `skills.json` exactly
- **`rewards` type**: Must be `"puffish_skill_leveling:per_level_rewards"` for our addon to detect it

**Flow Summary:**
1. **Create Puffish Skills foundation** (metadata, title, icon, size)
2. **Enhance with leveling system** (max_skill_level, per_level_rewards)
3. **Our addon detects and manages** skills with our reward type

### 💡 Pro Tip: Use the Web Editor

For easier skill tree creation, use the [Puffish Skills Web Editor](https://puffish.net/skillsmod/editor/):

1. **Visual skill tree design** - Drag and drop skills to position them
2. **Automatic metadata generation** - No need to manually create icon IDs
3. **Export ready-to-use files** - Downloads complete datapack structure (this is only for the base puffish skill structure not this addon's new structure.)
4. **Then enhance with our addon** - Add `max_skill_level` and `per_level_rewards` to exported definitions

The web editor handles all the complex positioning and metadata, so you can focus on designing your leveling system!

---

## Configuring Per-Level Rewards

### Reward Types

#### 1. Attribute Rewards
```json
{
    "type": "puffish_skills:attribute",
    "data": {
        "attribute": "generic.attack_damage",
        "value": 5,
        "operation": "addition"
    }
}
```

**Common Attributes:**
- `generic.attack_damage` - Melee damage
- `generic.max_health` - Maximum health
- `generic.movement_speed` - Movement speed
- `generic.armor` - Armor points
- `generic.luck` - Luck level

**Operations:**
- `addition` - Adds flat value
- `multiply_base` - Multiplies base value
- `multiply_total` - Multiplies total value

#### 2. Command Rewards
```json
{
    "type": "puffish_skills:command",
    "data": {
        "command": "give @p minecraft:diamond 1"
    }
}
```

#### 3. Multiple Rewards Per Level
```json
"levels": {
    "3": [
        {
            "type": "puffish_skills:attribute",
            "data": {
                "attribute": "generic.attack_damage",
                "value": 6,
                "operation": "addition"
            }
        },
        {
            "type": "puffish_skills:command",
            "data": {
                "command": "give @p minecraft:experience_bottle 1"
            }
        }
    ]
}
```

### Advanced Example: Berserker Rage

```json
"berserker_rage": {
    "title": "Berserker Rage",
    "icon": {
        "type": "item",
        "data": {
            "item": "minecraft:golden_axe"
        }
    },
    "size": 1.0,
    "max_skill_level": 3,
    "points_per_level": 2,
    "merge_description": true,
    "descriptions": [
        "Gain +10% attack speed when below 50% health",
        "Gain +20% attack speed when below 50% health",
        "Gain +30% attack speed when below 50% health + damage immunity for 3s after kill"
    ],
    "extra_descriptions": [
        "Next: +20% attack speed when below 50% health",
        "Next: +30% attack speed + 3s damage immunity after kills",
        "— MAXED OUT —"
    ],
    "rewards": [
        {
            "type": "puffish_skill_leveling:per_level_rewards",
            "data": {
                "skill_id": "berserker_rage",
                "levels": {
                    "1": [
                        {
                            "type": "puffish_skills:command",
                            "data": {
                                "command": "effect give @p minecraft:haste 1 0 true"
                            }
                        }
                    ],
                    "2": [
                        {
                            "type": "puffish_skills:command",
                            "data": {
                                "command": "effect give @p minecraft:haste 1 1 true"
                            }
                        }
                    ],
                    "3": [
                        {
                            "type": "puffish_skills:command",
                            "data": {
                                "command": "effect give @p minecraft:haste 1 2 true"
                            }
                        },
                        {
                            "type": "puffish_skills:command",
                            "data": {
                                "command": "effect give @p minecraft:resistance 60 4 true"
                            }
                        }
                    ]
                }
            }
        }
    ]
}
```

---

## Testing Your Skills

### Step 5: Test Your Configuration

1. **Start your server/world** with the datapack loaded
2. **Unlock the skill** using the base Puffish Skills commands:
   ```
   /skills unlock <player> <category> <skill>
   ```
3. **Give skill points**:
   ```
   /skills points add <player> <category> <amount>
   ```

### Step 6: Use Leveling Commands

Once skills are unlocked, use the leveling addon commands:

#### Check Current Level
```
/skillleveling get <player> <category> <skill_id>
```
**Example:**
```
/skillleveling get Steve combat combat_skill_001
```
**Output:**
```
═══ Skill Information ═══
Player: Steve
Warrior Strength: Level 2/5
■■□□□□□□□□ 40%
```

#### Advance to Next Level
```
/skillleveling advance <player> <category> <skill_id>
```
**Example:**
```
/skillleveling advance Steve combat combat_skill_001
```
**Output:**
```
═══ Skill Advanced ═══
Player: Steve
⬆ Warrior Strength advanced from level 2 to level 3!
■■■□□□□□□□ 60%
```

#### Set Specific Level
```
/skillleveling set <player> <category> <skill_id> <level>
```
**Example:**
```
/skillleveling set Steve combat combat_skill_001 3
```

#### View Detailed Information
```
/skillleveling info <player> <category> <skill_id>
```
**Example:**
```
/skillleveling info Steve combat combat_skill_001
```

---

## Command Reference

### Complete Command List

| Command | Description | Example |
|---------|-------------|---------|
| `/skillleveling get <player> <category> <skill_id>` | Show current skill level | `/skillleveling get Steve combat combat_skill_001` |
| `/skillleveling set <player> <category> <skill_id> <level>` | Set skill to specific level | `/skillleveling set Steve combat combat_skill_001 3` |
| `/skillleveling advance <player> <category> <skill_id>` | Advance skill by one level | `/skillleveling advance Steve combat combat_skill_001` |
| `/skillleveling refund <player> <category> <skill_id>` | Reduce skill by one level | `/skillleveling refund Steve combat combat_skill_001` |
| `/skillleveling info <player> <category> <skill_id>` | Show detailed skill information | `/skillleveling info Steve combat combat_skill_001` |
| `/skillleveling list <player>` | List all skills for player | `/skillleveling list Steve` |
| `/skillleveling max <player> <category> <skill_id>` | Set skill to maximum level | `/skillleveling max Steve combat combat_skill_001` |

### Command Permissions

All commands require operator permissions by default. You can configure this in your server's permission system.

---

## Advanced Configuration

### Merge Description Examples

#### With `merge_description: false`
Shows only current level:
```
Current: Level 3: +6 attack damage
Next: +8 attack damage
```

#### With `merge_description: true`
Shows cumulative effect:
```
Current: +6 attack damage (+2 from level 1, +2 from level 2, +2 from level 3)
Next: +8 attack damage
```

### Complex Skill Trees

#### Prerequisites and Connections
```json
{
    "basic_combat_001": {
        "definition": "basic_combat",
        "x": 0,
        "y": 0,
        "root": true
    },
    "sword_mastery_001": {
        "definition": "sword_mastery", 
        "x": -64,
        "y": 64,
        "connections": ["basic_combat_001"]
    },
    "axe_mastery_001": {
        "definition": "axe_mastery",
        "x": 64, 
        "y": 64,
        "connections": ["basic_combat_001"]
    },
    "weapon_expert_001": {
        "definition": "weapon_expert",
        "x": 0,
        "y": 128,
        "connections": ["sword_mastery_001", "axe_mastery_001"]
    }
}
```

### Dynamic Reward Scaling

```json
"levels": {
    "1": [
        {
            "type": "puffish_skills:attribute",
            "data": {
                "attribute": "generic.attack_damage",
                "value": 1,
                "operation": "addition"
            }
        }
    ],
    "2": [
        {
            "type": "puffish_skills:attribute", 
            "data": {
                "attribute": "generic.attack_damage",
                "value": 3,
                "operation": "addition"
            }
        }
    ],
    "3": [
        {
            "type": "puffish_skills:attribute",
            "data": {
                "attribute": "generic.attack_damage",
                "value": 6,
                "operation": "addition"
            }
        }
    ]
}
```

This creates scaling where:
- Level 1: +1 damage (total: +1)
- Level 2: +3 damage (total: +3) 
- Level 3: +6 damage (total: +6)

---

## Real-Time Features

### Action Bar Notifications

When players advance skills, they see immediate feedback:

```
⬆ Warrior Strength advanced to level 3!
```

### Progress Visualization

Commands show visual progress bars:
```
■■■□□□□□□□ 60%
```

### Player Join Synchronization

When players join the server, their skill data is immediately synchronized, ensuring they see current information without delays.

### Client-Side Display Enhancements

- **Progress bars** for visual level tracking
- **Level-up messages** with celebration arrows
- **Formatted tooltips** with enhanced information
- **Consistent styling** across all interfaces

---

## Troubleshooting

### Common Issues

#### Skill ID Mismatch
**Problem:** Commands say "skill not found" or leveling doesn't work
**Solution:**
1. Check that your `skill_id` in the reward configuration matches the key from `skills.json`
2. Use the actual skill ID (like `combat_skill_001`), not the definition name (like `warrior_strength`)
3. Example:
   ```json
   // skills.json
   "combat_skill_001": {"definition": "warrior_strength"}
   
   // definitions.json - CORRECT
   "skill_id": "combat_skill_001"
   
   // definitions.json - WRONG  
   "skill_id": "warrior_strength"
   ```

#### Skills Not Showing Up
**Problem:** Skills don't appear in the skills GUI
**Solution:** 
1. Check your JSON syntax with a JSON validator
2. Ensure the skill is properly connected in `skills.json`
3. Verify the category is listed in `config.json`

#### Rewards Not Working
**Problem:** Level rewards don't apply when advancing
**Solution:**
1. Check the reward type is spelled correctly
2. Verify attribute names match Minecraft's attribute registry
3. Ensure the `skill_id` in rewards matches the definition key

#### Commands Not Working
**Problem:** `/skillleveling` commands return errors
**Solution:**
1. Ensure the player has the skill unlocked first using `/skills unlock`
2. Check that the player has enough skill points for advancement
3. Verify the addon is properly installed and loaded

#### Level Not Saving
**Problem:** Skill levels reset when player rejoins
**Solution:**
1. Check server has proper write permissions for data files
2. Ensure the server saves properly before shutdown
3. Verify no conflicting mods are interfering

### Debug Commands

Use these commands to diagnose issues:

```bash
# Check if skill is unlocked (use the actual skill ID from skills.json)
/skills info <player> <category> <skill_id>

# Example with correct skill ID
/skills info Steve combat combat_skill_001

# Check player's skill points
/skills points get <player> <category>

# View current addon version
/skillleveling info <player> <category> <skill_id>
```

### Log Analysis

Check your server logs for error messages:
```
[ERROR] SkillLevelingMod: Failed to load skill definition...
[WARN] PerLevelRewardsReward: Invalid reward configuration...
```

### Configuration Validation

Ensure your JSON follows this schema:

```json
{
    "type": "puffish_skill_leveling:per_level_rewards",
    "data": {
        "skill_id": "string (must match skill ID from skills.json)",
        "max_skill_level": "number (1-999)",
        "points_per_level": "number (1+)",
        "merge_description": "boolean",
        "descriptions": ["array of strings"],
        "extra_descriptions": ["array of strings"],
        "levels": {
            "1": ["array of reward objects"],
            "2": ["array of reward objects"]
        }
    }
}
```

**⚠️ CRITICAL:** The `skill_id` field must match the key from your `skills.json` file, NOT the definition name!

---

## Best Practices

### 1. Start Simple
Begin with basic skills that have 2-3 levels before creating complex trees.

### 2. Balance Point Costs
- Early skills: 1 point per level
- Mid-tier skills: 2-3 points per level  
- Master skills: 5+ points per level

### 3. Clear Descriptions
Write descriptions that clearly explain what each level provides:
```
"Level 3: +15% mining speed (total: +45% from all levels)"
```

### 4. Test Thoroughly
Always test your skills with different scenarios:
- Advancing through all levels
- Refunding levels
- Player disconnect/reconnect
- Multiple players simultaneously

### 5. Backup Configurations
Keep backups of working configurations before making changes.

---

## Example Datapack

Here's a complete example for a mining skill category:

### config.json
```json
{
    "version": 3,
    "categories": ["mining"]
}
```

### categories/mining/category.json
```json
{
    "title": "Mining Mastery",
    "icon": {
        "type": "item",
        "data": {
            "item": "diamond_pickaxe"
        }
    },
    "background": "textures/gui/advancements/backgrounds/stone.png"
}
```

### categories/mining/skills.json
```json
{
    "mining_001": {
        "definition": "efficient_mining",
        "x": 0,
        "y": 0,
        "root": true
    }
}
```

### categories/mining/definitions.json
```json
{
    "efficient_mining": {
        "title": "Efficient Mining",
        "icon": {
            "type": "item",
            "data": {
                "item": "minecraft:diamond_pickaxe"
            }
        },
        "size": 1.0,
        "max_skill_level": 4,
        "points_per_level": 1,
        "merge_description": false,
        "descriptions": [
            "Level 1: +10% mining speed",
            "Level 2: +20% mining speed",
            "Level 3: +30% mining speed", 
            "Level 4: +40% mining speed + Fortune III effect"
        ],
        "extra_descriptions": [
            "Next: +20% mining speed",
            "Next: +30% mining speed",
            "Next: +40% mining speed + Fortune III",
            "— MAXED OUT —"
        ],
        "rewards": [
            {
                "type": "puffish_skill_leveling:per_level_rewards",
                "data": {
                    "skill_id": "mining_001",
                    "levels": {
                        "1": [
                            {
                                "type": "puffish_skills:command",
                                "data": {
                                    "command": "effect give @p minecraft:haste 300 0 true"
                                }
                            }
                        ],
                        "2": [
                            {
                                "type": "puffish_skills:command",
                                "data": {
                                    "command": "effect give @p minecraft:haste 300 1 true"
                                }
                            }
                        ],
                        "3": [
                            {
                                "type": "puffish_skills:command",
                                "data": {
                                    "command": "effect give @p minecraft:haste 300 2 true"
                                }
                            }
                        ],
                        "4": [
                            {
                                "type": "puffish_skills:command",
                                "data": {
                                    "command": "effect give @p minecraft:haste 300 3 true"
                                }
                            },
                            {
                                "type": "puffish_skills:command",
                                "data": {
                                    "command": "enchant @p minecraft:fortune 3"
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

This complete example creates a mining skill that:
- Has 4 levels of progression
- Costs 1 skill point per level
- Provides increasing haste effects
- Gives Fortune III enchantment at max level
- Shows clear progression in descriptions

---

## Conclusion

You now have a complete understanding of the Pufferfish Skill Leveling system! This addon provides powerful tools for creating engaging progression systems while maintaining compatibility with the base Puffish Skills mod.

### Next Steps
1. Create your first simple skill following this tutorial
2. Test it thoroughly in a development environment
3. Gradually add complexity as you become comfortable
4. Share your creations with the community!

### Support
If you encounter issues not covered in this tutorial:
1. Check the troubleshooting section
2. Review your server logs for error messages
3. Validate your JSON configuration
4. Test with minimal configurations to isolate problems

Happy skill crafting! 🎯
