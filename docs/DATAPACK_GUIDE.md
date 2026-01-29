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
| `merge_description` | boolean | false | Accumulate descriptions across levels |
| `descriptions` | object | — | Level-specific descriptions (keyed by level number) |
| `extra_descriptions` | object | — | Preview text for next level (keyed by level number) |
| `prerequisite_skills` | array | — | Skills required before this one can be unlocked |
| `enchantment_levels` | integer | 0 | Experience level cost per level for anvil combining |

### Skill Types

| Type | Description |
|------|-------------|
| `puffish_skills:default` | Standard skill type from base mod |
| `puffish_skill_leveling:stackable` | Combines standard rewards + per-level rewards |

> [!TIP]
> **Base Skill Support**: You can use `puffish_skills:default` skills with the Skill Tome and Imbuement systems! The addon treats standard skills as having a `max_skill_level` of 1, allowing you to imbue base Pufferfish attribute modifiers onto gear.

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
> **Same-Category Only**: Prerequisites currently only work within the same category. Cross-category prerequisites are planned for a future release (see [Roadmap](./ROADMAP.md)).

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

### Imbue-Only Skill
Cannot be directly learned; must be applied to equipment via anvil.

```json
{
    "imbued_strength": {
        "loot_mode": "imbue_only",
        "title": "Imbued Strength",
        "description": "Apply to equipment for +2 damage",
        ...
    }
}
```

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
