# Datapack Guide

A progressive tutorial that starts with the basics and builds to expert-level configurations. Each section adds complexity — skip ahead to what you need or read through for the full journey.

**Prerequisites:** You've followed [Getting Started](./GETTING_STARTED.md) and have a working datapack with at least one skill.

---

## Table of Contents

- [Point A: Datapack Structure](#-point-a-datapack-structure)
- [Point B: The Definition Schema](#-point-b-the-definition-schema)
- [Point C: Loot Modes & Discovery](#-point-c-loot-modes--discovery)
- [Point D: The Gating Systems](#-point-d-the-gating-systems)
- [Point E: Descriptions & Tooltips](#-point-e-descriptions--tooltips)
- [Point F: Per-Level Rewards](#-point-f-per-level-rewards)
- [Point G: Costs & XP Expressions](#-point-g-costs--xp-expressions)
- [Point H: The Anvil & Imbuing Ecosystem](#%EF%B8%8F-point-h-the-anvil--imbuing-ecosystem)
- [Point I: Items & Artifacts](#-point-i-items--artifacts)
- [Point J: The Skill Master](#-point-j-the-skill-master)
- [Best Practices](#-best-practices)
- [Troubleshooting](#-troubleshooting)

---

## 🛠 Point A: Datapack Structure

Your datapack must follow this exact folder hierarchy. Minecraft silently ignores packs with structural errors — no error messages, just missing skills.

### Directory Layout

```
your_datapack/
├── pack.mcmeta
└── data/
    ├── <your_namespace>/
    │   └── puffish_skills/
    │       ├── config.json                 ← Registry of your categories
    │       └── categories/
    │           └── <category_id>/
    │               ├── category.json       ← Category icon, background, gating
    │               ├── definitions.json    ← Skill definitions (rewards, levels, etc.)
    │               ├── skills.json         ← Positions on the skill tree
    │               └── connections.json    ← Lines between skill nodes
    └── puffish_skill_leveling/             ← Addon-specific configs
        ├── loot_modifiers/                 ← Universal loot injection
        ├── skill_imbue_loot/               ← Dynamic imbuing rules
        ├── skill_master_reputation/        ← Villager reputation config
        └── skill_master_trades/            ← Custom trade pools
```

### pack.mcmeta

```json
{
    "pack": {
        "pack_format": 15,
        "description": "My Skill Leveling Pack"
    }
}
```

### config.json

This file tells Pufferfish Skills where to find your categories. It goes at `data/<namespace>/puffish_skills/config.json`:

```json
{
    "categories": ["combat", "utility"]
}
```

Each entry must match a folder name inside `categories/`.

---

## 📋 Point B: The Definition Schema

Every skill lives in `definitions.json` as a JSON object keyed by its **unique ID**. Below is every field the addon recognizes.

### Complete Field Reference

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `type` | string | No | `puffish_skills:default` | Skill type. Omit for standard skills. |
| `category_id` | string | **Yes** | — | Must match the folder name. Needed for tome generation. |
| `title` | string | **Yes** | — | Display name. Should match the JSON key for clarity. |
| `description` | string | No | — | Base tooltip description (before any level info). |
| `icon` | object | **Yes** | — | `{ "type": "item", "data": { "item": "minecraft:..." } }` |
| `max_skill_level` | integer | No | 1 | Maximum number of levels. Omit for pure toggles. |
| `points_per_level` | integer | **Yes** | — | Point cost per level. Use `0` for loot-only skills. |
| `metadata` | object | **Yes** | — | **Required** even if empty (`{}`). Parser fails without it. |
| `loot_mode` | string | No | — | `"both"`, `"tome_only"`, or `"imbue_only"`. |
| `toggle` | boolean | No | `false` | Makes this an on/off ability. |
| `keybind_slot` | integer | No | — | Mastery keybind slot (1–9). |
| `cooldown` | integer | No | — | Ticks before re-enabling after disable. |
| `hidden` | boolean | No | `false` | Invisible until prerequisites are met. |
| `merge_description` | boolean | No | `false` | Stack all previous level tooltips. |
| `descriptions` | object | No | — | Level-based tooltips: `{ "1": "...", "2": "..." }`. |
| `extra_descriptions` | object | No | — | Shift-held previews: `{ "0": "...", "1": "..." }`. |
| `prerequisite_skills` | array | No | — | Skills required before this one appears. |
| `required_skill_for_level` | object | No | — | Per-level gating: `{ "3": [...requirements] }`. |
| `enchantment_cost` | mixed | No | — | XP cost for Anvil use. Scalar, array, or expression. |
| `imbuement_cost` | mixed | No | — | XP cost for manual imbuing. |
| `slot_opening_cost` | mixed | No | — | XP cost for opening gear slots. |
| `cleansing_cost` | mixed | No | — | XP cost for skill extraction. |

### Minimal Valid Skill

```json
{
    "my_skill": {
        "category_id": "combat",
        "title": "My Skill",
        "icon": { "type": "item", "data": { "item": "minecraft:stone" } },
        "points_per_level": 1,
        "metadata": {}
    }
}
```

This creates a 1-level skill with no custom rewards — effectively a standard Pufferfish Skills skill.

---

## 🗡 Point C: Loot Modes & Discovery

### Loot Modes

Control how skills are acquired:

| Mode | In Skill Tree? | Can Imbue to Gear? | Use Case |
|------|---------------|-------------------|----------|
| `"both"` | Yes | Yes | Standard progression skills. |
| `"tome_only"` | Yes | No | Player-only abilities that don't belong on equipment. |
| `"imbue_only"` | **Hidden** | Yes | Hidden from the tree entirely. Equipment-exclusive passives. |

```json
"loot_mode": "both"
```

### Hidden Skills

When `"hidden": true`, the skill is completely invisible until all `prerequisite_skills` are met — no icon, no connection lines, no tooltip. Use this for prestige or discovery content.

```json
"hidden": true,
"prerequisite_skills": [
    { "skill": "master_swordsman", "min_level": 5 }
]
```

---

## 🏹 Point D: The Gating Systems

Three levels of gating control when players can access content.

### 1. Initial Unlock (`prerequisite_skills`)

Defines what a player needs before this skill appears or becomes purchasable. Uses skill-level requirements, optionally across categories.

```json
"prerequisite_skills": [
    { "skill": "basic_archery", "min_level": 5 },
    { "skill": "woodworking", "min_level": 3, "category": "utility" }
]
```

| Field | Required | Description |
|-------|----------|-------------|
| `skill` | Yes | Skill ID to check. |
| `min_level` | Yes | Minimum level the player must have. |
| `category` | No | Category containing the skill. Defaults to current category. |

### 2. Tiered Level Gating (`required_skill_for_level`)

Blocks specific level increments. Perfect for creating "tier gates" where players need mastery elsewhere before reaching the peak of a high-end skill.

```json
"required_skill_for_level": {
    "3": [
        { "skill": "breath_control", "min_level": 3 }
    ],
    "5": [
        { "skill": "breath_control", "min_level": 5 },
        { "skill": "ancient_essence", "min_level": 1, "category": "ancient" }
    ]
}
```

**How to read this:** Level 3 of this skill requires `breath_control` at Level 3. Level 5 also requires `ancient_essence` at Level 1 from the `ancient` category.

### 3. Category Gating (`prerequisite_skills` in `category.json`)

Locks **entire categories** — not just individual skills. Players can't even open the category until requirements are met.

> **Important:** This goes in `category.json`, not `definitions.json`.

```json
{
    "icon": { "type": "item", "data": { "item": "minecraft:nether_star" } },
    "background": "minecraft:textures/block/obsidian.png",
    "prerequisite_skills": [
        { "skill": "warrior_strength", "level": 5, "category": "combat" },
        { "skill": "mana_pool", "level": 3, "category": "magic" }
    ],
    "keep_unlocked": true
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `prerequisite_skills` | array | — | `{ "skill", "level", "category" }` objects. All must pass. |
| `keep_unlocked` | boolean | `false` | If `true`, category stays unlocked permanently once first met. |

**Behaviors:**
- Evaluated on player join and whenever a skill level changes.
- With `keep_unlocked: false`, refunding skills below requirements re-locks the category.
- With `keep_unlocked: true`, once met, the category stays open even if requirements are later lost.

---

## 🧪 Point E: Descriptions & Tooltips

Give players clear feedback about what each level does and what comes next.

### `descriptions` — Current Level Tooltips

Shows what the player currently has. The key is the level number as a string.

```json
"descriptions": {
    "1": "§7+5% Movement Speed",
    "2": "§7+10% Movement Speed",
    "3": "§6+15% Movement Speed §c(MAX)"
}
```

### `extra_descriptions` — Next Rank Previews

Shown when the player holds Shift. Key `"0"` is shown before the first purchase.

```json
"extra_descriptions": {
    "0": "§7Next: +5% Movement Speed",
    "1": "§7Next: +10% Movement Speed",
    "2": "§7Next: +15% Movement Speed (Final)"
}
```

### `merge_description`

Set to `true` to stack all previous level descriptions together — useful for skills that accumulate bonuses rather than replace them.

### Toggle Skill Tooltips

Toggle skills automatically show status text (READY, ENABLED, ON COOLDOWN, DISABLED) without extra configuration. This is generated by the addon based on the skill's state.

---

## 👑 Point F: Per-Level Rewards

The `puffish_skill_leveling:per_level_rewards` reward type is the core of the addon. It maps level numbers to arrays of rewards.

### Basic Example: Attribute Scaling

```json
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
                            "value": 2.0,
                            "operation": "addition"
                        }
                    }
                ],
                "3": [
                    {
                        "type": "puffish_skills:attribute",
                        "data": {
                            "attribute": "generic.attack_damage",
                            "value": 3.0,
                            "operation": "addition"
                        }
                    }
                ]
            }
        }
    }
]
```

### Mixed Rewards Example

Each level can have multiple rewards of different types:

```json
"3": [
    {
        "type": "puffish_skills:attribute",
        "data": {
            "attribute": "generic.attack_damage",
            "value": 3.0,
            "operation": "addition"
        }
    },
    {
        "type": "puffish_skill_leveling:effect",
        "data": {
            "effect": "minecraft:glowing",
            "duration": 100,
            "amplifier": 0,
            "is_protected": true
        }
    },
    {
        "type": "puffish_skills:command",
        "data": {
            "command": "title @s actionbar {\"text\":\"Rank 3 Unlocked!\",\"color\":\"gold\"}"
        }
    }
]
```

### Available Reward Types

| Reward ID | Source | Description |
|-----------|--------|-------------|
| `puffish_skills:attribute` | Base mod | Modify player attributes (damage, health, speed, etc.). |
| `puffish_skills:command` | Base mod | Execute server commands. Supports `@s`, `~ ~ ~` selectors. |
| `puffish_skills:effect` | Base mod | Apply potion effects (basic). |
| `puffish_skill_leveling:effect` | Addon | Enhanced effects with `persistent` and `is_protected`. |
| `puffish_skill_leveling:toggle` | Addon | Wrap rewards in an on/off toggle. See [Toggle System](./Toggle_System.md). |
| `puffish_skill_leveling:per_level_rewards` | Addon | Level-mapped rewards (this section). |

### Toggle + Per-Level Rewards

Toggle rewards can wrap per-level rewards to create abilities that are both level-able and toggle-able. See [Toggle System — Hybrid Patterns](./Toggle_System.md#-hybrid-patterns) for detailed examples.

---

## 💎 Point G: Costs & XP Expressions

All cost fields support three formats:

### 1. Integer (Scalar)
```json
"enchantment_cost": 5
```
Cost = 5 × target level. Level 2 = 10 XP, Level 3 = 15 XP.

### 2. Array (Per-Level)
```json
"enchantment_cost": [5, 10, 20, 35, 50]
```
Exact costs per level. Index 0 = Level 1, Index 1 = Level 2, etc.

### 3. Expression (Math Formula)
```json
"enchantment_cost": {
    "type": "expression",
    "data": { "expression": "level * 5 + 10" }
}
```
Uses the `level` variable. Standard math operators: `+`, `-`, `*`, `/`, `^`.

### Which Costs Apply Where?

| Field | When It's Charged |
|-------|-------------------|
| `enchantment_cost` | Combining Skill Tome + equipment in Anvil |
| `imbuement_cost` | Using Sigil of Imbuing |
| `slot_opening_cost` | Using Sigil of Imbuement to open a slot |
| `cleansing_cost` | Using Tome of Cleansing to extract a skill |

---

## ⚒️ Point H: The Anvil & Imbuing Ecosystem

The Anvil is the workstation for all skill-to-equipment interactions.

### Opening Skill Slots

Equipment starts with 0 skill slots. Use a **Sigil of Imbuement** to open one (up to 3 per item).

| Anvil Input | Result | XP Cost |
|-------------|--------|---------|
| Equipment + Sigil of Imbuement | Equipment with new empty slot | `slot_opening_cost` |

### Applying Skills

| Anvil Input | Result | XP Cost |
|-------------|--------|---------|
| Slotted Equipment + Skill Tome | Skill applied to first empty slot | `enchantment_cost` |

### Upgrading Imbued Skills

| Anvil Input | Result |
|-------------|--------|
| Equipment with Skill Lv1 + Matching Skill Tome | Skill upgrades to Lv2 |

The skill level on gear caps at `max_skill_level`.

### Extracting Skills

| Anvil Input | Result | XP Cost |
|-------------|--------|---------|
| Equipment + Tome of Cleansing I | Skill from Slot 1 returned as Tome | `cleansing_cost` |
| Equipment + Tome of Cleansing II | Skill from Slot 2 returned as Tome | `cleansing_cost` |
| Equipment + Tome of Cleansing III | Skill from Slot 3 returned as Tome | `cleansing_cost` |

The slot remains open after extraction.

### Ranking Up Tomes

Combine two identical Skill Tomes at the same level to create a higher-level version:

| Anvil Input | Result |
|-------------|--------|
| Skill Tome Lv1 + Skill Tome Lv1 | Skill Tome Lv2 |
| Skill Tome Lv3 + Skill Tome Lv3 | Skill Tome Lv4 |

Cannot exceed `max_skill_level`.

### Paid vs. Granted Levels

The addon tracks how each level was earned:

| Source | Type | Refund Value |
|--------|------|-------------|
| Spent skill points | **Paid** | Full point refund |
| Skill Tome / Imbuing | **Granted** | 0 points returned |

This prevents players from "laundering" found tomes into free skill points by leveling up then refunding.

---

## 📜 Point I: Items & Artifacts

| Item | How to Use |
|------|-----------|
| **Blank Tome** | Base crafting material for all specialized tomes. |
| **Skill Tome** | Right-click or combine with gear. Grants +1 level to a specific skill. |
| **Skill Charm** | Curios accessory — imbue it with skills just like armor. |
| **Sigil of Imbuement** | Opens a new skill slot on equipment (Anvil). |
| **Tome of Progression** | Right-click to open a GUI. Select any skill to level up by 1. |
| **Tome of Clear Mind** | Right-click. Select a skill to refund 1 level and recover points. |
| **Tome of Greater Clear Mind** | Right-click. Select a skill to fully reset to Level 0. |
| **Tome of Cleansing (I/II/III)** | Use in Anvil. Extracts a skill from equipment slot 1, 2, or 3. |

---

## 👑 Point J: The Skill Master

The Skill Master is a custom villager profession that trades skill-related items.

### Tier Progression

| Tier | Name | Notable Offers |
|------|------|----------------|
| 1 | Novice | Basic tomes, Blank Tomes |
| 2 | Apprentice | Tome of Clear Mind, Tome of Cleansing, Tome of Progression |
| 3 | Journeyman | Tome Upgrades (lower + emeralds → higher), mid-level tomes |
| 4 | Expert | Advanced skill trades, better pricing |
| 5 | Master | Sigils of Imbuement, highest-level tomes |

### Mastery Pricing

The Skill Master rewards skilled players. As you master more skills across all categories, his prices decrease and his trade pool expands.

### Skill Master Houses

Custom jigsaw-based buildings that spawn in villages across all biome types (Plains, Desert, Savanna, Snowy, Taiga). They contain:
- The Skill Scribe Table workstation
- Tiered loot barrels with progression materials

### Admin Commands

| Command | Description |
|---------|-------------|
| `/skillleveling villager forceProfession` | Convert the villager you're looking at. |
| `/skillleveling villager setTier <1-5>` | Directly set tier. |
| `/skillleveling villager reset` | Reset trades and experience to 0. |

---

## 🧠 Best Practices

### Naming Consistency
Always make your JSON key match your `title` field. This keeps admin commands and server logs readable:
```json
"warrior_strength": {
    "title": "warrior_strength",
    ...
}
```

### Define All Levels
If `max_skill_level` is 5, define rewards for levels 1 through 5. Skipping levels causes inconsistent behavior.

### Use Protected Effects for Toggles
Any potion effect on a toggle skill should use `puffish_skill_leveling:effect` with `is_protected: true`. Without it, milk or death will remove the effect while the toggle is still "on."

### Use Commands for Player Feedback
Add `tellraw` or `title ... actionbar` commands to important level-ups so players know something happened:
```json
{
    "type": "puffish_skills:command",
    "data": {
        "command": "title @s actionbar {\"text\":\"Rank 3 Unlocked!\",\"color\":\"gold\"}"
    }
}
```

### Test with Admin Commands
Use these commands liberally during development to verify your skills work:
```
/skillleveling set @s combat warrior_strength 3
/skillleveling refund @s combat warrior_strength all
/skillleveling info @s combat warrior_strength
```

### Progressive Disclosure for Players
Use `hidden: true` for high-tier skills and let prerequisites reveal them naturally. Don't overwhelm players with a fully visible tree of 50 skills — let them discover as they progress.

### Category Gating for Progression Paths
Use category-level `prerequisite_skills` to create clear advancement tracks:
- **Combat → Advanced Combat** (requires Warrior Strength Lv5)
- **Magic → Arcane Mastery** (requires Mana Pool Lv3 + Basic Spells Lv3)

---

## 🆘 Troubleshooting

### Skills don't appear after `/reload`

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Entire category missing | Folder structure wrong or `config.json` doesn't list the category | Check folder names match exactly |
| Individual skill missing | Missing `metadata: {}` field | Add `"metadata": {}` to the definition |
| Skill shows but no rewards | `skill_id` in `per_level_rewards` doesn't match the definition key | Ensure `skill_id` matches exactly |

### Common Field Mistakes

| Mistake | Result |
|---------|--------|
| No `metadata: {}` | Skill fails to load silently |
| No `category_id` | Tomes won't generate for this skill |
| `points_per_level` missing | Skill can't be leveled in the tree |
| `skill_id` mismatch in rewards | Rewards apply to wrong skill or not at all |

### Useful Debug Commands

```
/reload                                          — Reload all datapacks
/skillleveling get @s <category> <skill>         — Check current level
/skillleveling set @s <category> <skill> 0       — Reset for testing
/skillleveling info @s <category> <skill>        — Full diagnostic
```

### Check Server Logs

If something isn't working, check your server log (`logs/latest.log`). The addon logs warnings for:
- Missing definitions or categories
- Invalid prerequisite references
- Malformed reward configurations

---

*For toggle skill configuration, see [Toggle System](./Toggle_System.md). For loot configuration, see [Universal Loot System](./Universal_Loot_System.md) and [Skill Imbuement System](./Skill_Imbuement_System.md).*
