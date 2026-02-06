# Datapack Guide

This guide explains how to create custom datapacks that use the Pufferfish Skill Leveling Addon features.

> [!IMPORTANT]
> The addon does **not** ship with a production-ready datapack. You must create your own.

---

## Datapack Structure

```
your_datapack/
├── pack.mcmeta
└── data/
    └── your_namespace/
        └── puffish_skills/
            └── categories/
                └── your_category/
                    ├── category.json
                    ├── definitions.json
                    ├── skills.json
                    └── connections.json
```

### pack.mcmeta
```json
{
    "pack": {
        "pack_format": 15,
        "description": "My Custom Skills"
    }
}
```

---

## Skill Definition Schema

Each skill in `definitions.json` must follow this structure:

```json
{
    "skill_id": {
        "type": "puffish_skills:default",
        "title": "Skill Title",
        "description": "Skill description",
        "icon": {
            "type": "item",
            "data": { "item": "minecraft:diamond" }
        },
        "max_skill_level": 3,
        "points_per_level": 1,
        "descriptions": {
            "1": "Level 1 description",
            "2": "Level 2 description",
            "3": "Level 3 description"
        },
        "rewards": [
            {
                "type": "puffish_skill_leveling:per_level_rewards",
                "data": {
                    "skill_id": "skill_id",
                    "levels": {
                        "1": [ /* rewards */ ],
                        "2": [ /* rewards */ ],
                        "3": [ /* rewards */ ]
                    }
                }
            }
        ],
        "metadata": {}
    }
}
```

> [!WARNING]
> **The `metadata` field is required!** Even if empty, you must include `"metadata": {}` or the skill will fail to load.

---

## Field Reference

### Core Fields (Required)

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | Skill type (see Skill Types below) |
| `title` | string | Display name in the UI |
| `description` | string | Short description |
| `icon` | object | Icon configuration |
| `rewards` | array | Array of reward definitions |
| `metadata` | object | Extra metadata (**required**, can be empty: `{}`) |

### Addon Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `max_skill_level` | integer | 1 | Maximum levels for this skill |
| `points_per_level` | integer | 1 | Skill points cost per level |
| `category_id` | string | — | Category this skill belongs to |
| `loot_mode` | string | — | `"tome_only"` or `"imbue_only"` |
| `hidden` | boolean | false | If true, skill stays invisible until prerequisites are met |
| `merge_description` | boolean | false | Accumulate descriptions across levels |
| `descriptions` | object | — | Level-specific descriptions (keyed by level number) |
| `extra_descriptions` | object | — | Preview text for next level (keyed by level number) |
| `prerequisite_skills` | array | — | Skills required before this icon appears/unlocks (Top-level) |
| `enchantment_levels` | integer/string | 0 | XP level cost for anvil combining. Supports expressions (e.g., `"level * 5"`) |
| `imbuement_cost` | integer/string | — | XP level cost for manual imbuing. Supports expressions. |
| `slot_opening_cost` | integer/string | 0 | XP level cost for opening a skill slot with a Sigil. Supports expressions |
| `cleansing_cost` | integer/string | 0 | XP level cost for extracting a skill. Supports expressions |

---

## Prerequisites & Visual Discovery

The addon provides **two distinct prerequisite systems** for different use cases.

### Quick Comparison

| Feature | `prerequisite_skills` | `required_skill_for_level` |
|---------|----------------------|---------------------------|
| **Location** | Root of skill definition | Root of skill definition |
| **Purpose** | Controls skill unlock/purchase | Gates specific levels |
| **Cross-Category** | ✅ Use `category` field | ✅ Use `category` field |
| **When Checked** | Before skill can be purchased | Before advancing to a level |
| **Use Case** | Lock skills behind others | Advanced skill requires other skills for higher levels |

---

### 1. Prerequisites for Unlock (`prerequisite_skills`)

These control when a skill icon becomes visible and purchasable in the skill tree.

**Key Behaviors**:
- Skill is **locked** (or hidden if `hidden: true`) until ALL prerequisites are met
- Once purchased, the skill **stays purchased** even if prerequisites are later lost
- Supports **cross-category** requirements via the `category` field

#### Basic Example (Same Category)
```json
"advanced_mining": {
    "title": "Advanced Mining",
    "prerequisite_skills": [
        { "skill": "basic_mining", "min_level": 3 }
    ],
    "max_skill_level": 5,
    ...
}
```

#### Cross-Category Example ⭐
Require a skill from a completely different category:

```json
"reinforced_tools": {
    "title": "Reinforced Tools",
    "prerequisite_skills": [
        { "skill": "basic_mining", "min_level": 2 },
        { "skill": "smithing", "min_level": 3, "category": "crafting" }
    ],
    ...
}
```

> [!NOTE]
> **Category ID Format**: Use the **category ID** (folder name under `puffish_skills/categories/`). For example, if your category is at `data/mymod/puffish_skills/categories/crafting/`, use `"category": "crafting"`. No namespace prefix needed.

#### Multiple Prerequisites (All Required)
```json
"master_warrior": {
    "title": "Master Warrior",
    "prerequisite_skills": [
        { "skill": "sword_training", "min_level": 5 },
        { "skill": "shield_proficiency", "min_level": 3 },
        { "skill": "combat_tactics", "min_level": 2, "category": "strategy" }
    ],
    ...
}
```

---

### 2. Prerequisites for Specific Levels (`required_skill_for_level`)

These gate **specific levels** of a skill behind other skill requirements. Use this when you want players to be able to start a skill but require additional prerequisites for higher levels.

**Key Behaviors**:
- Players can unlock and level the skill normally until they hit a gated level
- The gate is checked when attempting to advance to that specific level
- Supports **cross-category** requirements via the `category` field
- Perfect for creating progression dependencies

#### Example: Gate Higher Levels
This skill can be leveled to 2 freely, but level 3 requires another skill:

```json
"advanced_swordsmanship": {
    "title": "Advanced Swordsmanship",
    "max_skill_level": 5,
    "required_skill_for_level": {
        "3": [
            { "skill": "basic_training", "min_level": 3 }
        ],
        "5": [
            { "skill": "weapon_mastery", "min_level": 2 },
            { "skill": "endurance", "min_level": 3, "category": "survival" }
        ]
    },
    ...
}
```

**Behavior**:
- Levels 1-2: Available normally
- Level 3: Requires `basic_training` at level 3 (same category)
- Level 4: No additional requirements
- Level 5: Requires both `weapon_mastery` level 2 AND `endurance` level 3 from survival category

#### Cross-Category Level Gating
```json
"elemental_mastery": {
    "title": "Elemental Mastery",
    "max_skill_level": 3,
    "required_skill_for_level": {
        "2": [
            { "skill": "fire_affinity", "min_level": 2, "category": "magic" }
        ],
        "3": [
            { "skill": "fire_affinity", "min_level": 5, "category": "magic" },
            { "skill": "mana_pool", "min_level": 3, "category": "magic" }
        ]
    },
    ...
}
```

---

### 3. Hidden Skills (`hidden`)

Skills marked as `"hidden": true` are **completely invisible** in the skill tree until their **prerequisite_skills** are met.

**What Gets Hidden**:
- ✅ Skill icon
- ✅ Skill connections (lines to/from)
- ✅ Skill tooltip
- ✅ Any indication the skill exists

**When It Reveals**:
- Automatically appears once ALL `prerequisite_skills` are satisfied
- Creates a "discovery" moment for the player

#### Example: Secret Skill
```json
"hidden_technique": {
    "title": "???",
    "hidden": true,
    "prerequisite_skills": [
        { "skill": "basic_training", "min_level": 5 },
        { "skill": "advanced_training", "min_level": 3 }
    ],
    "descriptions": {
        "0": "A secret technique revealed only to the dedicated.",
        "1": "You have unlocked the hidden power!"
    },
    ...
}
```

> [!TIP]
> **Design Pattern**: Use hidden skills for prestige content, Easter eggs, or reward players who fully explore a branch of the tree.

---

### Combining Both Prerequisite Types

For maximum control, use BOTH types together:

```json
"ultimate_power": {
    "title": "Ultimate Power",
    "hidden": true,
    "max_skill_level": 5,
    "prerequisite_skills": [
        { "skill": "power_training", "min_level": 5 }
    ],
    "required_skill_for_level": {
        "3": [
            { "skill": "mana_well", "min_level": 2, "category": "magic" }
        ],
        "5": [
            { "skill": "mana_well", "min_level": 5, "category": "magic" },
            { "skill": "combat_mastery", "min_level": 3, "category": "combat" }
        ]
    },
    ...
}
```

**Behavior**:
1. Skill is **invisible** until `power_training` reaches level 5 (prerequisite_skills)
2. Once visible, player can **purchase** the skill and level to 2
3. Level 3 requires `mana_well` level 2 from magic category
4. Level 5 requires BOTH `mana_well` level 5 AND `combat_mastery` level 3

---

## Loot Mode System

The `loot_mode` field controls **how a skill can be acquired and used**. This affects both skill tree interaction and Skill Master villager trades.

### Available Modes

| Mode | Skill Tree | Imbuing | Villager Trades | Use Case |
|------|------------|---------|-----------------|----------|
| `"both"` (default) | ✅ Can purchase | ✅ Can imbue | Shows in all trades | Standard skills |
| `"tome_only"` | ✅ Can purchase | ❌ Cannot imbue | Tome trades only | Tree-exclusive skills |
| `"imbue_only"` | ❌ Cannot purchase | ✅ Can imbue | Imbue trades only | Equipment-exclusive skills |

### Example: Imbue-Only Skill (Equipment Exclusive)

This skill can ONLY be acquired through imbuing onto equipment - it won't appear in the skill tree for purchase:

```json
"flame_enchant": {
    "title": "Flame Enchantment",
    "loot_mode": "imbue_only",
    "max_skill_level": 3,
    "points_per_level": 0,
    "descriptions": {
        "1": "Weapon deals +1 fire damage",
        "2": "Weapon deals +2 fire damage",
        "3": "Weapon deals +3 fire damage"
    },
    "rewards": [...]
}
```

### Example: Tome-Only Skill (Tree Exclusive)

This skill can be unlocked in the tree but cannot be imbued onto equipment:

```json
"passive_regen": {
    "title": "Passive Regeneration",
    "loot_mode": "tome_only",
    "max_skill_level": 5,
    "points_per_level": 2,
    "descriptions": {
        "1": "Slowly regenerate health",
        "5": "Rapidly regenerate health"
    },
    "rewards": [...]
}
```

> [!TIP]
> **Villager Behavior**: The Skill Master villager respects loot modes. Imbue-only skills appear in Sigil/equipment trades, while tome-only skills appear in Skill Tome trades.

---

## Datapack Registry Extensions

The addon looks for additional configuration files in these paths:

### 1. Skill Master Reputation Config

**Path**: `data/puffish_skill_leveling/skill_master_reputation/config.json`

Controls Skill Master trading prices, experience gains, and upgrade logic.

```json
{
    "experience_settings": {
        "base_experience_per_trade": 5,
        "bonus_for_both_mode_skills": 3,
        "bonus_for_imbue_only_skills": 5,
        "experience_multiplier_per_villager_tier": 2,
        "experience_bonus_per_mastered_skill": 2,
        "maximum_mastery_experience_bonus": 20
    },
    "static_experience_rewards": {
        "sigil_proxy_trade": 5,
        "tome_of_clear_mind": 10,
        "tome_of_cleansing": 15,
        "sigil_of_imbuement": 50,
        "tome_of_cleansing_upgrade": 25
    },
    "dynamic_trade_settings": {
        "special_upgrade_trade_base_chance": 0.1,
        "special_upgrade_trade_max_chance": 0.25,
        "tome_price_multiplier_at_level_1": 1.2,
        "tome_price_multiplier_at_max_level": 0.5,
        "tome_upgrade_price_multiplier": 0.6
    }
}
```

### 2. Custom Trades (`data/puffish_skill_leveling/trades/*.json`)
Define non-tome trades for the Skill Master.

### 3. Structural Loot (`data/puffish_skill_leveling/loot_tables/chests/skill_master_barrels.json`)
Configure what players find in the Skill Master houses.

### Skill Types

| Type | Description |
|------|-------------|
| `puffish_skills:default` | Standard skill type from base mod |
| `puffish_skill_leveling:stackable` | Combines standard rewards + per-level rewards |

> [!TIP]
> **Base Skill Support**: You can use `puffish_skills:default` skills with the Skill Tome and Imbuement systems! The addon treats standard skills as having a `max_skill_level` of 1, allowing you to imbue base Pufferfish attribute modifiers onto gear.

> [!TIP]
> **Namespace Flexibility**: Skill data is stored in player NBT and matched by category **path** (not full ID). This means you can rename your namespace (e.g., from `mymod:combat` to `newmod:combat`) without losing player progress!

---

## Prerequisite Skills

You can require players to have other skills at specific levels before unlocking a skill:

```json
{
    "advanced_warrior": {
        "title": "Advanced Warrior",
        "prerequisite_skills": [
            {
                "skill": "basic_warrior",
                "min_level": 3
            },
            {
                "skill": "combat_training",
                "min_level": 1,
                "max_level": 5
            }
        ],
        ...
    }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `skill` | string | ID of the required skill |
| `min_level` | integer | Minimum level required (inclusive) |
| `max_level` | integer | Maximum level allowed (optional) |

> [!NOTE]
> **Cross-Category Support**: Per-level prerequisites (`required_skill`) fully support cross-category requirements. You can require levels in one category (e.g., `combat`) to activate rewards in another (e.g., `mining`). Note that Top-Level prerequisites (`prerequisite_skills`) remain restricted to the same category.

---

## Description Merging

### With `merge_description: false` (default)
Each level shows only its own description:
- Level 1: "Current: +1 damage"
- Level 2: "Current: +2 damage"
- Level 3: "Current: +3 damage"

### With `merge_description: true`
Descriptions accumulate across levels:
- Level 1: "+1 damage"
- Level 2: "+1 damage" + "+2 damage"
- Level 3: "+1 damage" + "+2 damage" + "+3 damage"

---

## Extra Descriptions (Next Level Preview)

Use `extra_descriptions` to show what the next level will grant.

> [!IMPORTANT]
> **Levels start at 0!** When a skill is unlocked but not yet leveled, it is at level 0. The `descriptions` key `"0"` shows the inital description (not needed, totally optional), and `extra_descriptions` key `"0"` previews what level 1 will grant.

### Level Numbering Explained:
| Player Level | `descriptions` Key | Shows | `extra_descriptions` Key | Shows |
|--------------|-------------------|-------|-------------------------|-------|
| 0 (unlocked) | `"0"` | Base state | `"0"` | Preview of level 1 |
| 1 | `"1"` | Level 1 benefits | `"1"` | Preview of level 2 |
| 2 | `"2"` | Level 2 benefits | `"2"` | Preview of level 3 |
| 3 (max) | `"3"` | Level 3 benefits | `"3"` | "— MAXED OUT —" |

### Example:
```json
{
    "warrior": {
        "max_skill_level": 3,
        "descriptions": {
            "1": "Level 1: +1 Attack Damage",
            "2": "Level 2: +2 Attack Damage",
            "3": "Level 3: +3 Attack Damage"
        },
        "extra_descriptions": {
            "0": "Next: +1 Attack Damage",
            "1": "Next: +2 Attack Damage",
            "2": "Next: +3 Attack Damage",
            "3": "— MAXED OUT —"
        },
        ...
    }
}
```

---

## Stackable Skill Type

The `puffish_skill_leveling:stackable` type combines standard unlock rewards with per-level rewards:

```json
{
    "master_miner": {
        "type": "puffish_skill_leveling:stackable",
        "title": "Master Miner",
        "max_skill_level": 3,
        "rewards": [
            {
                "type": "puffish_skill_leveling:per_level_rewards",
                "data": {
                    "skill_id": "master_miner",
                    "levels": {
                        "1": [ /* level 1 rewards */ ],
                        "2": [ /* level 2 rewards */ ],
                        "3": [ /* level 3 rewards */ ]
                    }
                }
            },
            {
                "type": "puffish_skills:command",
                "data": { "command": "give @s minecraft:diamond 1" }
            }
        ],
        "metadata": {}
    }
}
```

The command reward triggers on initial unlock, while per-level rewards trigger at each level.

---

## Loot Mode Examples

### Tome-Only Skill
Cannot be purchased in the skill tree; only obtainable via Skill Tomes.

```json
{
    "rare_power": {
        "loot_mode": "tome_only",
        "title": "Rare Power",
        ...
    }
}
```

```json
{
    "imbued_strength": {
        "loot_mode": "imbue_only",
        "title": "Imbued Strength",
        "description": "Apply to equipment for +2 damage",
        "slot_opening_cost": "level * 10",
        "cleansing_cost": 5,
        ...
    }
}
```

---

## Multi-Skill System

Equipment can hold up to 3 different skills simultaneously.

### Skill Slots
Before imbuing a skill, equipment must have an **Open Skill Slot**.
- Use a **Sigil of Imbuement** in an anvil with any piece of gear to open a slot.
- Maximum 3 slots per item.
- Slot opening cost is defined by `slot_opening_cost` in any skill within the category.

### Imbuing & Upgrading
- **Empty Slot + Skill Tome**: Imbues the skill at the tome's level.
- **Matching Skill + Matching Tome**: Upgrades the existing imbued skill by 1 level (up to `max_skill_level`).

### Cleansing (Extraction)
- Use a **Tome of Cleansing (I, II, or III)** in an anvil with imbued gear to extract a skill.
- **Tome of Cleansing (I)** targets **Slot 1** (first added).
- **Tome of Cleansing II** targets **Slot 2** (second added).
- **Tome of Cleansing III** targets **Slot 3** (third added).
- The skill is returned as a **Skill Tome** at its current level (preserving its `loot_mode`).
- The skill slot remains open on the gear.
- Extraction cost is defined by `cleansing_cost` for that specific skill.

---

## Reward Examples

### Attribute Modifier
```json
{
    "type": "puffish_skills:attribute",
    "data": {
        "attribute": "generic.max_health",
        "value": 2.0,
        "operation": "addition"
    }
}
```

### Command Execution
```json
{
    "type": "puffish_skills:command",
    "data": {
        "command": "give @s minecraft:diamond 1"
    }
}
```

### Potion Effect
```json
{
    "type": "puffish_skills:effect",
    "data": {
        "effect": "minecraft:speed",
        "amplifier": 0,
        "ambient": true,
        "show_particles": false
    }
}
```

---

## Common Mistakes

| Mistake | Solution |
|---------|----------|
| Missing `metadata` field | Add `"metadata": {}` to every skill |
| Wrong namespace in `skill_id` | Ensure `skill_id` in rewards matches the skill's key name |
| Invalid item ID in icon | Use valid Minecraft item IDs (e.g., `minecraft:diamond`) |
| Missing level in `levels` object | Define all levels from 1 to `max_skill_level` |

---

## Testing Your Datapack

1. Place your datapack in `.minecraft/saves/YourWorld/datapacks/`
2. Run `/reload` in-game
3. Check for errors in the game log
4. Open the Puffish Skills UI (default: `K` key) to verify skills appear

---

*For more examples, see the [Getting Started Guide](./GETTING_STARTED.md).*
