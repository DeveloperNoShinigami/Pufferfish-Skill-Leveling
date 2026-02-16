# Features Reference

A complete reference of every feature in the Pufferfish Skill Leveling addon. Each section explains what the feature does, how to configure it, and when to use it.

---

## Table of Contents

1. [Multi-Level Skill Progression](#multi-level-skill-progression)
2. [Toggle Skills](#toggle-skills)
3. [Skill Prerequisites](#skill-prerequisites)
4. [Category Gating](#category-gating)
5. [Loot Modes](#loot-modes)
6. [Descriptions & Tooltips](#descriptions--tooltips)
7. [Effect Rewards](#effect-rewards)
8. [Cost Expressions](#cost-expressions)
9. [Equipment Imbuing & Anvil](#equipment-imbuing--anvil)
10. [Universal Loot Injection](#universal-loot-injection)
11. [Dynamic Loot Imbuing](#dynamic-loot-imbuing)
12. [Skill Tomes & Items](#skill-tomes--items)
13. [Skill Master Villager](#skill-master-villager)
14. [Commands](#commands)
15. [Compatibility](#compatibility)

---

## Multi-Level Skill Progression

The core feature of this addon. Skills can have any number of levels (1 to N), each with distinct rewards that replace the previous level's effects.

**How it works:** Wrap your rewards inside a `puffish_skill_leveling:per_level_rewards` block and define what happens at each level.

### Definition Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `max_skill_level` | integer | 1 | Maximum number of levels for this skill. |
| `points_per_level` | integer | 1 | Skill points required per level-up. Set to `0` for loot-only skills. |
| `category_id` | string | — | **Required** for Creative Tab items and tome generation. |
| `metadata` | object | — | **Required.** Use `{}` if empty. |

### Reward Type

| ID | Description |
|----|-------------|
| `puffish_skill_leveling:per_level_rewards` | Defines a map of level → reward arrays. Each level replaces the previous. |

### Example

```json
"max_skill_level": 3,
"rewards": [
    {
        "type": "puffish_skill_leveling:per_level_rewards",
        "data": {
            "skill_id": "my_skill",
            "levels": {
                "1": [ /* Level 1 rewards */ ],
                "2": [ /* Level 2 rewards */ ],
                "3": [ /* Level 3 rewards */ ]
            }
        }
    }
]
```

> **Best Practice:** Always define rewards for every level from 1 to `max_skill_level`. Skipping levels can cause unexpected behavior.

---

## Toggle Skills

Skills with `"toggle": true` become active abilities that players turn on and off. They support keybinds, cooldowns, and can be combined with multi-level progression for powerful hybrid designs.

There are three types of toggle skills:

| Type | Has Levels? | Example |
|------|-------------|---------|
| **Pure Toggle** | No (`max_skill_level` omitted or 0) | A simple on/off ability like Berserker Rage |
| **Basic Toggle** | Limited (`max_skill_level`: 1) | Night Vision with imbue/loot support |
| **Hybrid Toggle** | Yes (`max_skill_level`: 2+) | Abilities that grow stronger with levels |

### Toggle Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `toggle` | boolean | `false` | Enables toggle behavior. |
| `keybind_slot` | integer | — | Assigns the skill to a Mastery Keybind (1–9). |
| `cooldown` | integer | — | Ticks before the skill can be re-enabled after disabling. |

### Key Behaviors

- **Keybind & Right-Click:** Players toggle via their assigned Mastery Key or by right-clicking the skill in the tree.
- **State Persistence:** Toggle states are saved and restored across sessions.
- **Auto-Disable:** If a toggle skill depends on equipped gear (via imbuing), removing the gear auto-disables the skill.
- **Cooldowns** activate when a skill is **disabled**, preventing spam.

> **Full Guide:** See [Toggle System](./Toggle_System.md) for hybrid patterns, nesting strategies, and best practices.

---

## Skill Prerequisites

Control when skills become visible and purchasable using a two-tier requirement system.

### Tier 1: Initial Unlock (`prerequisite_skills`)

Gates the first-time appearance and purchase of a skill. Once unlocked, it stays unlocked.

```json
"prerequisite_skills": [
    { "skill": "basic_archery", "min_level": 5 },
    { "skill": "woodworking", "min_level": 3, "category": "utility" }
]
```

| Field | Required | Description |
|-------|----------|-------------|
| `skill` | Yes | The skill ID to check. |
| `min_level` | Yes | Minimum level required. |
| `category` | No | Category of the required skill. Defaults to the current category. |

### Tier 2: Level Gating (`required_skill_for_level`)

Blocks specific level increments — useful for forcing players to master other branches at higher tiers.

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

### Hidden Skills

Set `"hidden": true` to make a skill completely invisible (icon, connections, tooltip — everything) until all `prerequisite_skills` are met. This creates discovery moments for secret techniques and prestige content.

---

## Category Gating

Lock entire skill categories behind prerequisite skills from other categories. This creates a natural progression path — players must advance in foundational categories before accessing advanced ones.

> **Important:** Category prerequisites go in `category.json`, not `definitions.json`. Don't confuse with skill-level `prerequisite_skills`.

### Configuration (in `category.json`)

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

### Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `prerequisite_skills` | array | — | Each entry: `{ "skill", "level", "category" }`. All must be satisfied. |
| `keep_unlocked` | boolean | `false` | If `true`, once unlocked the category stays open permanently. |

### Behavior

| Event | `keep_unlocked: false` | `keep_unlocked: true` |
|-------|------------------------|-----------------------|
| Prerequisites met | Unlocks | Unlocks |
| Skill refunded below requirement | Re-locks | Stays unlocked |
| Player joins server | Re-evaluated | Stays unlocked if ever met |

---

## Loot Modes

Controls how a skill can be acquired by players.

| Mode | Skill Tree | Equipment Imbuing | When to Use |
|------|------------|-------------------|-------------|
| `"both"` | Yes | Yes | Standard skills — learnable and imbuable. |
| `"tome_only"` | Yes | No | Player-only skills that can't go on gear. |
| `"imbue_only"` | Hidden | Yes | Equipment-exclusive passives (hidden from tree). |

Set via the `loot_mode` field on your skill definition. Skills without a `loot_mode` behave as standard skills (similar to `"both"`).

---

## Descriptions & Tooltips

Provide tooltip text that changes with each level, giving players clear feedback on what they have and what comes next.

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `descriptions` | object | Current-level tooltips. Key = level number as string. |
| `extra_descriptions` | object | "Next rank" preview shown when holding Shift. Key `"0"` = preview before first purchase. |
| `merge_description` | boolean | If `true`, shows all previous levels' descriptions stacked together. |

### Example

```json
"descriptions": {
    "1": "§7+5% Movement Speed",
    "2": "§7+10% Movement Speed",
    "3": "§6+15% Movement Speed §c(MAX)"
},
"extra_descriptions": {
    "0": "§7Next: +5% Movement Speed",
    "1": "§7Next: +10% Movement Speed",
    "2": "§7Next: +15% Movement Speed (Final)"
}
```

### How It Looks

| Player State | Main Tooltip (`descriptions`) | Shift Tooltip (`extra_descriptions`) |
|-------------|-------------------------------|--------------------------------------|
| Not purchased | Original skill description | Key `"0"` preview |
| Level 1 | Key `"1"` text | Key `"1"` preview |
| Max Level | Max level text | "MAXED OUT" message |

---

## Effect Rewards

The addon provides an enhanced effect reward type with protection features for reliable potion effects.

### Comparison

| Reward ID | Source | Extra Features |
|-----------|--------|----------------|
| `puffish_skills:effect` | Base mod | Standard potion effects. |
| `puffish_skill_leveling:effect` | This addon | Adds `persistent` and `is_protected`. |

### Enhanced Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `effect` | string | — | Potion effect ID (e.g., `"minecraft:night_vision"`). |
| `amplifier` | integer | 0 | Effect strength (0 = Level I). Changes take effect immediately when datapack is reloaded. |
| `duration` | integer | — | Duration in ticks. Use `-1` for infinite (toggle skills). |
| `persistent` | boolean | `false` | Makes the effect survive milk and curative items (Forge native). |
| `is_protected` | boolean | `false` | Re-applies the effect if cleared by commands, death, or other mods. |

### Example

```json
{
    "type": "puffish_skill_leveling:effect",
    "data": {
        "effect": "minecraft:night_vision",
        "duration": -1,
        "amplifier": 0,
        "persistent": true,
        "is_protected": true
    }
}
```

> **Best Practice:** Always use `puffish_skill_leveling:effect` with `is_protected: true` for toggle skills. Without it, drinking milk or dying will remove the effect while the toggle is still "on".

---

## Cost Expressions

All XP cost fields support three formats — from simple to dynamic:

### Formats

| Format | Example | How It Works |
|--------|---------|-------------|
| **Integer** | `5` | Flat cost multiplied by level. Level 2 = 10 XP. |
| **Array** | `[5, 10, 20]` | Exact cost per level. Index = Level - 1. |
| **Expression** | `"level * 2 + 5"` | Math formula evaluated with the `level` variable. |

### Cost Fields

| Field | Used For |
|-------|----------|
| `enchantment_cost` | Combining a Skill Tome + item in an Anvil. |
| `imbuement_cost` | Applying a skill via Sigil of Imbuing. |
| `slot_opening_cost` | Using a Sigil of Imbuement to open a gear slot. |
| `cleansing_cost` | Extracting a skill using a Tome of Cleansing. |

### Example

```json
"enchantment_cost": {
    "type": "expression",
    "data": { "expression": "level * 5 + 10" }
},
"imbuement_cost": 5,
"slot_opening_cost": [10, 20, 40],
"cleansing_cost": 15
```

---

## Equipment Imbuing & Anvil

Players can apply, upgrade, and extract skills from equipment using the Anvil.

### Slot System
- Equipment can hold up to **3 skill slots**.
- Slots must be opened first using a **Sigil of Imbuement**.

### Operations

| Operation | Left Slot | Right Slot | Result |
|-----------|-----------|------------|--------|
| Open a slot | Equipment | Sigil of Imbuement | Equipment with new empty slot |
| Apply a skill | Slotted equipment | Skill Tome | Equipment with skill in empty slot |
| Upgrade a skill | Equipment with Skill Lv N | Matching Skill Tome | Skill upgrades to Lv N+1 |
| Extract a skill | Imbued equipment | Tome of Cleansing (I/II/III) | Skill returned as Tome |
| Rank up a tome | Skill Tome Lv N | Skill Tome Lv N (same) | Skill Tome Lv N+1 |

### Requirements
- Only skills with `loot_mode: "both"` or `"imbue_only"` can be imbued onto gear.
- Skill level on gear cannot exceed `max_skill_level`.
- **Namespace Agnostic:** A tome for `vitality` will correctly match tree skills named `template:vitality`.

### Point Bypass
Levels gained through imbuing/tomes bypass point costs — they're tracked as "Granted" levels. Refunding a granted level returns 0 points, preventing exploitation.

---

## Universal Loot Injection

Add custom items (Skill Charms, tomes, etc.) to any chest or mob loot table with a single JSON config.

- **Path:** `data/puffish_skill_leveling/loot_modifiers/universal_loot.json`
- **Cross-Platform:** Identical behavior on Forge and Fabric.
- **Reliable Drops:** Entity drops use a dedicated hook to ensure imbued items are always applied, even when standard loot modifiers are bypassed.

> **Full Guide:** [Universal Loot System](./Universal_Loot_System.md)

---

## Dynamic Loot Imbuing

Automatically applies random skills to equipment and Skill Charms found in loot.

- **Path:** `data/puffish_skill_leveling/skill_imbue_loot/config.json`
- **Dimension Scaling:** Different worlds can have different imbue chances and level ranges.
- **Distance Scaling:** Equipment gets stronger the further from spawn it's found.
- **Exclusion Groups:** Prevent conflicting skills on the same item.

> **Full Guide:** [Skill Imbuement System](./Skill_Imbuement_System.md)

---

## Skill Tomes & Items

| Item | What It Does |
|------|--------------|
| **Skill Tome** | Grants +1 level to a specific skill. Combine two identical tomes in an Anvil to rank up. |
| **Skill Charm** | Curios accessory that can be imbued with skills, just like armor or weapons. |
| **Sigil of Imbuement** | Opens a new skill slot on equipment (max 3 per item). |
| **Tome of Progression** | Select any skill from a GUI and advance it by 1 level. |
| **Tome of Clear Mind** | Refund 1 level of a chosen skill and recover spent points. |
| **Tome of Greater Clear Mind** | Fully reset a skill to Level 0 and refund all points. |
| **Tome of Cleansing (I/II/III)** | Extract a skill from equipment slot 1, 2, or 3. Returns as a Skill Tome. |
| **Blank Tome** | Base crafting material for specialized tomes. |

---

## Skill Master Villager

A specialized villager profession that serves as the primary NPC-driven progression path, from early-game Blank Tomes to end-game Sigils of Imbuement.

> **Full guide:** [Skill Master System](Skill_Master_System.md) — complete coverage of trades, tiers, mastery pricing, reputation config, custom trades, village structures, and progression strategy.

### Key Features
- **Dynamic Trading:** Every interaction regenerates the trade list based on the individual player's skill levels and mastery count.
- **5 Tiers:** Novice → Apprentice → Journeyman → Expert → Master. Higher tiers unlock better items and broader skill coverage.
- **Tome Upgrades:** At Tier 3+, trade a lower-level Skill Tome + emeralds for a higher-level version.
- **Mastery Pricing:** Prices scale down and upgrade trade chances increase as the player masters more skills.
- **Sigil Source:** Primary vendor for Sigils of Imbuement (higher tiers).
- **Datapack-Configurable:** Static trades, reputation values, and XP settings are all defined in JSON.

### Tier Progression

| Tier | Name | Trade Slots | Notable Offers |
|------|------|-------------|----------------|
| 1 | Novice | 5–7 | Blank Tomes, Sigil of Imbuement, introductory skill tomes |
| 2 | Apprentice | 6–8 | Tome of Clear Mind, Tome of Cleansing I |
| 3 | Journeyman | 7–9 | Tome of Cleansing II, tome upgrades, mid-level skill tomes |
| 4 | Expert | 8–10 | Tome of Cleansing III, Tome of Greater Clear Mind (50%) |
| 5 | Master | 9–12 | Tome of Progression, Sigil of Imbuement (50%), highest-level tomes |

### World Presence
- **Skill Master Houses:** Custom jigsaw-based buildings generate in villages (spacing: 40 chunks, separation: 16 chunks) with a Skill Scribe Table workstation and tiered loot barrels. Can be disabled in the mod config.

---

## Commands

### Player Commands

| Command | Description |
|---------|-------------|
| `/skillleveling get <player> <category> <skill>` | View a player's current base level. |
| `/skillleveling set <player> <category> <skill> <level>` | Force-set a skill level. |
| `/skillleveling advance <player> <category> <skill>` | Level up (respects point costs and requirements). |
| `/skillleveling refund <player> <category> <skill> [amount]` | Refund specific number of levels. |
| `/skillleveling refund <player> <category> <skill> all` | Reset skill to 0 and refund all points. |
| `/skillleveling info <player> <category> <skill>` | Detailed breakdown: levels, paid/granted, context. |
| `/skillleveling list <player>` | Overview of all learned skills and levels. |

### Item Commands

| Command | Description |
|---------|-------------|
| `/skillleveling give tome <player> <category> <skill> [mode] [level]` | Generate a Skill Tome. |

### Villager Commands

| Command | Description |
|---------|-------------|
| `/skillleveling villager forceProfession` | Convert the villager you're looking at to a Skill Master. |
| `/skillleveling villager setTier <1-5>` | Set a Skill Master's tier directly. |
| `/skillleveling villager reset` | Reset a Skill Master's trades and experience. |

---

## Compatibility

| Integration | Details |
|-------------|---------|
| **Curios API** | Full support for Skill Charms in dedicated accessory slots. |
| **Attribute Sync** | All attribute changes (health, damage, speed) update instantly in the UI. |
| **Namespace Agnostic** | Short IDs (e.g., `vitality`) automatically match namespaced IDs (e.g., `template:vitality`). |
| **Base Mod Safe** | All original Pufferfish Skills functionality is preserved. The addon uses non-intrusive injection points. |
| **Multi-Platform** | Identical behavior on both Forge and Fabric. |

---

*See [Datapack Guide](./DATAPACK_GUIDE.md) for hands-on configuration tutorials.*
