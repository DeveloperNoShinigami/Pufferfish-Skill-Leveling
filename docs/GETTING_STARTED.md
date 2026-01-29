# Getting Started with Pufferfish Skill Leveling

This guide will walk you through setting up and using the **Pufferfish Skill Leveling Addon**. This addon extends the official Pufferfish Skills mod with multi-level progression, skill tomes, and equipment imbuing.

> [!IMPORTANT]
> **This addon requires the base Pufferfish Skills mod.** All features are optional and designed to work alongside the original mod without conflicts.

---

## Installation

1. Install [Pufferfish Skills](https://www.curseforge.com/minecraft/mc-mods/pufferfish-skills) (v0.17.1+ for MC 1.20)
2. Install this addon (Pufferfish Skill Leveling)
3. Launch Minecraft — both mods will load together

> [!NOTE]
> The addon does **not** include a production datapack. You must create your own using the guide below.

---

## Quick Start: Creating Your First Multi-Level Skill

### Step 1: Create a Datapack

Create a new datapack in your world's `datapacks` folder with this structure:

```
my_skills/
├── pack.mcmeta
└── data/
    └── my_skills/
        └── puffish_skills/
            └── categories/
                └── combat/
                    ├── category.json
                    ├── definitions.json
                    ├── skills.json
                    └── connections.json
```

### Step 2: Define a Multi-Level Skill

In `definitions.json`, create a skill with multiple levels:

```json
{
    "warrior_strength": {
        "type": "puffish_skills:default",
        "title": "Warrior's Strength",
        "description": "Increases your attack damage.",
        "icon": {
            "type": "item",
            "data": { "item": "minecraft:iron_sword" }
        },
        "max_skill_level": 3,
        "points_per_level": 1,
        "descriptions": {
            "1": "Level 1: +1 Attack Damage",
            "2": "Level 2: +2 Attack Damage",
            "3": "Level 3: +3 Attack Damage"
        },
        "rewards": [
            {
                "type": "puffish_skill_leveling:per_level_rewards",
                "data": {
                    "skill_id": "warrior_strength",
                    "levels": {
                        "1": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 1.0,
                                    "operation": "addition"
                                }
                            }
                        ],
                        "2": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 1.0,
                                    "operation": "addition"
                                }
                            }
                        ],
                        "3": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 1.0,
                                    "operation": "addition"
                                }
                            }
                        ]
                    }
                }
            }
        ],
        "metadata": {}
    }
}
```

> [!WARNING]
> **The `metadata` field is required!** Even if empty (`"metadata": {}`), it must be present or the configuration will fail to load.

### Step 3: Add to the Skill Tree

In `skills.json`:

```json
{
    "warrior_strength": {
        "definition": "warrior_strength",
        "x": 0,
        "y": 0,
        "root": true
    }
}
```

### Step 4: Test It!

1. Run `/reload` in-game
2. Open the Puffish Skills UI (default: `K` key)
3. Your new skill should appear with level indicators

---

## Feature Overview

### 1. Multi-Level Skills
Skills can have any number of levels, each with distinct rewards. Players unlock levels one at a time, spending skill points per level.

### 2. Skill Tomes
Special items that grant skill progress when used:
- **Tome of Progression** — Advances a skill by 1 level
- **Tome of Clear Mind** — Refunds 1 level of a skill
- **Tome of Greater Clear Mind** — Resets a skill to level 0
- **Skill Tome** — Grants a specific skill (lootable, craftable)

### 3. Equipment Imbuing
Skills with `"loot_mode": "imbue_only"` can be applied to equipment via an anvil. When worn, the equipment grants the skill's bonuses. When removed, the bonuses disappear instantly.

### 4. Real-Time Attribute Sync
All attribute changes (health, damage, speed, armor, etc.) update **instantly** in your UI when equipping or leveling skills. No relogging required.

---

## Loot Modes

| Mode | Description |
|------|-------------|
| `tome_only` | Skill can only be learned via Skill Tomes |
| `imbue_only` | Skill must be imbued onto equipment (cannot be directly learned) |
| (default) | Standard behavior from base Puffish Skills |

---

## Commands

| Command | Description |
|---------|-------------|
| `/skillleveling get <player> <category> <skill>` | View current skill level |
| `/skillleveling set <player> <category> <skill> <level>` | Set skill to specific level |
| `/skillleveling advance <player> <category> <skill>` | Advance skill by 1 level |
| `/skillleveling refund one <player> <category> <skill>` | Refund 1 level |
| `/skillleveling refund all <player> <category> <skill>` | Reset skill to 0 |

---

## Next Steps

- [Features Reference](./FEATURES.md) — Complete list of all addon features
- [Datapack Guide](./DATAPACK_GUIDE.md) — Detailed guide for creating custom datapacks

---

*For issues or feature requests, open an issue on the project repository.*
