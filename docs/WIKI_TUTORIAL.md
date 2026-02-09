# Pufferfish Skill Leveling - Complete Tutorial

Welcome to the comprehensive guide for the **Pufferfish Skill Leveling** addon. This mod transforms the base Puffish Skills system into a deep, tiered progression system.

---

## 📖 Table of Contents
1. [Overview & Key Concepts](#overview)
2. [Prerequisites](#prerequisites)
3. [Datapack Architecture](#datapack-architecture)
4. [Creating Your First Skill](#creating-your-first-skill)
5. [Advanced Skill Mechanics](#advanced-skill-mechanics)
    - [Hidden Skills (Visual Discovery)](#hidden-skills)
    - [Loot Mode System](#loot-mode-system)
6. [Prerequisites & Locking Systems](#prerequisites-locking)
7. [The Skill Master & Village Dynamics](#villager-system)
8. [Command Reference](#command-reference)
9. [Troubleshooting & Logs](#troubleshooting)

---

<a name="overview"></a>
## 1. Overview & Key Concepts

The addon extends Puffish Skills by introducing **Levels** to what were previously binary (locked/unlocked) skills.

### Core Addon Logic:
- **Skills are Objects**: Every skill can have `max_skill_level` and `points_per_level`.
- **Rewards are Tiered**: Using `per_level_rewards`, you define exactly what happens at Level 1, Level 2, etc.
- **Visual Discovery**: Skills can be `hidden` until specific cross-category requirements are met.
- **Item Progression**: Players can advance skills via **Skill Tomes** and **Tomes of Progression**.

---

<a name="prerequisites"></a>
## 2. Prerequisites

### Required Mods
- **Puffish Skills** 0.16.3+
- **Pufferfish Skill Leveling** (This Addon)
- **Minecraft 1.20.1** (Forge or Fabric)

### Tools
- **[Puffish Skills Web Editor](https://puffish.net/skillsmod/editor/)**: Highly recommended for designing the basic tree layout and generating unique icon IDs (metadata).

---

<a name="datapack-architecture"></a>
## 3. Datapack Architecture

Your datapack now has two main areas of interest. The standard Puffish Skills folder and the Addon folder for advanced configuration.

```
your_datapack/
└── data/
    ├── your_namespace/
    │   └── puffish_skills/ (Base Mod Tree)
    │       ├── categories/
    │       └── ...
    └── puffish_skill_leveling/ (Addon Advanced Config)
        ├── skill_master_reputation/
        │   └── config.json
        ├── trades/
        └── loot_tables/
```

---

<a name="creating-your-first-skill"></a>
## 4. Creating Your First Skill

### Step 1: define the Definition (`definitions.json`)
This is where the addon's magic happens. We add our custom fields to the standard Pufferfish definition.

```json
"warrior_strength": {
    "title": "Warrior Strength",
    "category_id": "combat",
    "icon": { "type": "item", "data": { "item": "minecraft:iron_sword" } },
    "max_skill_level": 5,
    "points_per_level": 1,
    "merge_description": false,
    "descriptions": {
        "1": "+2 Attack Damage",
        "5": "+10 Attack Damage (MASTER)"
    },
    "rewards": [
        {
            "type": "puffish_skill_leveling:per_level_rewards",
            "data": {
                "skill_id": "sword_1",
                "levels": {
                    "1": [{ "type": "puffish_skills:attribute", "data": { "attribute": "generic.attack_damage", "value": 2, "operation": "addition" } }],
                    "5": [{ "type": "puffish_skills:attribute", "data": { "attribute": "generic.attack_damage", "value": 10, "operation": "addition" } }]
                }
            }
        }
    ],
    "metadata": { "icon": "unique_id_123" }
}
```

> [!IMPORTANT]
> **The `skill_id` field inside the reward data must match the key used in `skills.json`, not the definition name!**

---

<a name="advanced-skill-mechanics"></a>
## 5. Advanced Skill Mechanics

<a name="hidden-skills"></a>
### Hidden Skills (Visual Discovery)

Toggle visibility with `"hidden": true`.

**What Gets Hidden**:
- The skill icon
- Connection lines to/from the skill
- Tooltips and descriptions
- Any indication the skill exists

**When It Reveals**: The skill appears when ALL **Top-Level Prerequisites** (`prerequisite_skills`) are met.

**Use Cases**: Secret techniques, prestige rewards, Easter eggs.

---

<a name="loot-mode-system"></a>
### Loot Mode System

The `loot_mode` field controls how skills can be acquired:

| Mode | Skill Tree | Imbuing | Use Case |
|------|------------|---------|----------|
| `"both"` (default) | ✅ | ✅ | Standard skills |
| `"tome_only"` | ✅ | ❌ | Tree-exclusive skills |
| `"imbue_only"` | ❌ | ✅ | Equipment-only enchantments |

**Villager Behavior**: The Skill Master villager respects loot modes when generating trades.

---

<a name="prerequisites-locking"></a>
## 6. Prerequisites & Locking Systems

There are two distinct types of prerequisites:

### 1. Top-Level (`prerequisite_skills`)
Defined at the level of the skill definition.
- **Controls**: Purchase availability and "Unlocking".
- **Interaction**: If the skill is `hidden`, these control when it appears.
- **Scope**: Same category only.

<a name="villager-system"></a>
## 7. The Skill Master & Village Dynamics

The Skill Master is a professional villager found in custom **Skill Scribe Houses** across all major village types.

### Key Features:
- **Workstation**: Skill Scribe Table
- **Reputation**: Trading with Skill Masters improves your standing.
- **Dynamic Pricing**: Prices scale based on your total "Mastery" (number of maxed skills) and `skill_master_reputation/config.json`.
- **Tome Upgrades**: Large Skill Masters (Tier 3+) allow you to upgrade tomes from Level N to N+1 via trading.
- **Barrels**: Skill Master houses contain structural loot barrels with tiered rewards.

### Customizing Trades:
You can add your own trades to the Skill Master via `data/puffish_skill_leveling/trades/`.

---

<a name="command-reference"></a>
## 8. Command Reference

| Command | Usage |
|---------|-------|
| `/skillleveling get` | View player level & progress bar |
| `/skillleveling set` | Admin override for level |
| `/skillleveling info` | Detailed cost/reward breakdown |
| `/skillleveling refund` | Remove level & return points |
| `/skillleveling villager setTier` | Level up a Skill Master |
| `/skillleveling villager reset` | Wipe a Skill Master's memory |

---

<a name="troubleshooting"></a>
## 9. Troubleshooting & Logs

### Log Policy
We silence heavy gameplay logs to prevent server spam.
- **Initialization**: Info-level (visible by default).
- **Gameplay (Leveling/Sync/Trades)**: Debug-level (hidden by default).

### Common Fixes:
1. **Attribute Modifier Mismatch**: Ensure you use the exact Minecraft attribute ID (e.g. `generic.max_health`).
2. **Missing Metadata**: Every definition must have `"metadata": {}`.
3. **Skill ID Error**: Double check that your reward `skill_id` matches `skills.json`.
4. **Missing Creative Items**: Ensure `category_id` is set to the category folder name in your definitions.

---
*Created for Pufferfish Skill Leveling v2.4.0+*
