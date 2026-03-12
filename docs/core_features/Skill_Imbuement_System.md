# Skill Imbuement System

[< Back to Core Index](index.md) | [Next: Universal Loot System >](Universal_Loot_System.md)

---

Automatically imbue skills onto equipment that appears as loot. This system turns chests, mob drops, and any loot table into a source of skill-enhanced gear — creating an RPG loot experience.

**Config location:** `data/puffish_skill_leveling/skill_imbue_loot/config.json`

---

## Table of Contents

- [How It Works](#how-it-works)
- [Config Overview](#config-overview)
- [Dimension Overrides](#-dimension-overrides)
- [Distance Scaling](#-distance-scaling)
- [Skill Pools](#-skill-pools)
- [Category Settings](#-category-settings)
- [Exclusion Groups](#-exclusion-groups)
- [Filtering](#-filtering)
- [Supported Equipment Categories](#-supported-equipment-categories)
- [Full Example Config](#-full-example-config)
- [Tips & Troubleshooting](#-tips--troubleshooting)

---

## How It Works

1. Any time loot generates (chests, mob drops, fishing, etc.), eligible equipment items are checked.
2. The system looks up the current dimension to determine imbue chance, level range, and max skills per item.
3. If the roll succeeds, a skill is chosen from the weighted pool and applied at a random level within the allowed range.
4. Equipment receives auto-opened skill slots to hold the imbued skill(s).

Only skills with `"loot_mode": "both"` or `"loot_mode": "imbue_only"` can appear via this system.

---

## Config Overview

The config is a single JSON file with these top-level sections:

| Section | Purpose |
|---------|---------|
| `dimension_overrides` | Per-dimension chance, level range, and max skills. |
| `distance_scaling` | Cap skill level based on distance from spawn. |
| `global` | Skill entries that can appear on any equipment. |
| `<category_name>` | Skill entries restricted to a specific equipment type. |
| `category_settings` | Per-equipment-type overrides for chance, level, and skill count. |
| `exclusion_groups` | Prevent conflicting skills from appearing on the same item. |
| `item_blacklist` | Items that should never receive imbued skills. |
| `item_whitelist` | If set, ONLY these items can receive imbued skills. |
| `loot_table_whitelist` | If set, imbuing only runs on these loot tables. |

---

## 🌍 Dimension Overrides

Control imbue behavior per dimension. **If a dimension is not listed, no imbuing occurs there.**

```json
"dimension_overrides": {
    "minecraft:overworld": {
        "imbue_chance": 0.25,
        "max_skills": 1,
        "min_level": 1,
        "max_level": 2
    },
    "minecraft:the_nether": {
        "imbue_chance": 0.5,
        "max_skills": 2,
        "min_level": 2,
        "max_level": 5
    },
    "minecraft:the_end": {
        "imbue_chance": 0.75,
        "max_skills": 3,
        "min_level": 999,
        "max_level": 999
    }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `imbue_chance` | float | **Yes** | Probability (0.0–1.0) that an eligible item gets imbued. |
| `max_skills` | integer | **Yes** | Maximum number of skills per item. |
| `min_level` | integer | **Yes** | Minimum skill level to roll. |
| `max_level` | integer | **Yes** | Maximum skill level to roll. Clamped to skill's `max_skill_level`. |

**Tip:** Use `min_level: 999` and `max_level: 999` to always grant max level — the system clamps to each skill's actual cap.

---

## 📏 Distance Scaling

Cap skill levels based on how far the loot generates from a central point. Encourages exploration — the farther you go, the better the loot.

```json
"distance_scaling": {
    "enabled": true,
    "origin": [0, 0],
    "brackets": [
        { "distance": 5000, "max_level": 1, "chance_mult": 1.0 },
        { "distance": 25000, "max_level": 3, "chance_mult": 1.0 },
        { "distance": 50000, "max_level": 5, "chance_mult": 1.0 }
    ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `enabled` | boolean | No | Enables/disables distance scaling (Default: false). |
| `origin` | [x, z] | **Yes** | Center point for distance calculation. |
| `brackets` | array | **Yes** | Sorted by distance. First matching bracket sets the cap. |
| `brackets[].distance` | integer | **Yes** | Distance threshold in blocks. |
| `brackets[].max_level` | integer | **Yes** | Maximum skill level within this radius. |
| `brackets[].chance_mult` | float | No | Multiplier on imbue chance at this distance (reserved for future use). |

**How it works:** The system calculates the X/Z distance from `origin` to where the loot was generated. It finds the first bracket where `distance > actual_distance` and caps the level there.

**Example with the config above:**
- Chest at X=2000, Z=1000 (distance ~2236) → max level 1.
- Chest at X=10000, Z=10000 (distance ~14142) → max level 3.
- Chest at X=40000, Z=30000 (distance ~50000) → max level 5.

Distance scaling applies **after** dimension overrides. The final level is the minimum of both caps.

---

## 🎯 Skill Pools

Skills are drawn from weighted pools. There are two types: **global** and **category-specific**.

### Global Pool

Skills in the `global` array can appear on **any** eligible equipment:

```json
"global": [
    { "skill": "any", "weight": 20 }
]
```

### Category Pool

Skills in named arrays only appear on matching equipment types:

```json
"sword": [
    { "skill": "warrior_strength", "weight": 30 },
    { "skill": "arcane_striker", "weight": 15 }
],
"armor": [
    { "skill": "vitality", "weight": 25 }
]
```

### Entry Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `skill` | string | **Yes** | Skill ID or `"any"`. |
| `weight` | integer | **Yes** | Relative probability. Higher = more likely. |

### The `"any"` Wildcard

Setting `"skill": "any"` expands to **all registered skills** that have a valid `loot_mode` (`"both"` or `"imbue_only"`). This is the easiest way to set up imbuing without listing every skill manually.

```json
"global": [
    { "skill": "any", "weight": 20 }
]
```

When a specific skill ID is also listed alongside `"any"`, it gets additional weight from both entries. For example:

```json
"global": [
    { "skill": "any", "weight": 10 },
    { "skill": "vitality", "weight": 40 }
]
```

Here, `vitality` has weight 10 (from `any` expansion) + 40 (explicit) = 50 total, making it much more likely.

---

## ⚙ Category Settings

Override imbue parameters for specific equipment types. These take priority over dimension overrides for matching items.

```json
"category_settings": {
    "sword": {
        "imbue_chance": 0.5,
        "min_level": 1,
        "max_level": 3,
        "max_skills": 2
    },
    "armor": {
        "imbue_chance": 0.3,
        "min_level": 1,
        "max_level": 2,
        "max_skills": 1
    }
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `imbue_chance` | float | No | -1 (use dimension) | Override imbue probability. |
| `min_level` | integer | No | -1 (use dimension) | Override minimum level. |
| `max_level` | integer | No | -1 (use dimension) | Override maximum level. |
| `max_skills` | integer | No | -1 (use dimension) | Override max skills per item. |

A value of `-1` means "fall back to the dimension override."

---

## 🚫 Exclusion Groups

Prevent conflicting skills from appearing on the same item. Each group lists skill IDs or category IDs — at most one skill from each group can be imbued per item.

```json
"exclusion_groups": [
    {
        "types": ["warrior_strength", "arcane_striker"]
    },
    {
        "types": ["speed", "vitality"]
    }
]
```

**How it works:** When placing a second (or third) skill on an item, the system checks whether the new skill shares an exclusion group with any already-applied skill. If so, it's skipped and a different skill is rolled.

You can also use category IDs to exclude entire skill categories from appearing together.

---

## 🔍 Filtering

### Item Blacklist

Specific items that should **never** receive imbued skills:

```json
"item_blacklist": [
    "minecraft:elytra",
    "minecraft:shield"
]
```

### Item Whitelist

If this array has entries, **only** listed items can receive imbued skills. Everything else is excluded. Leave empty to allow all eligible equipment.

```json
"item_whitelist": [
    "minecraft:diamond_sword",
    "minecraft:netherite_chestplate"
]
```

### Loot Table Whitelist

Restricts imbuing to specific loot tables. Useful if you only want imbued gear from certain structures.

```json
"loot_table_whitelist": [
    "minecraft:chests/end_city_treasure",
    "minecraft:chests/stronghold_corridor"
]
```

Leave empty to allow all loot tables.

### Priority Order

1. Item blacklist → reject immediately.
2. Item whitelist → if non-empty, item must be listed.
3. Loot table whitelist → if non-empty, loot table must match.
4. Equipment category → item must be valid equipment (or Skill Charm).

---

## 🛡 Supported Equipment Categories

Use these IDs in `category_settings`, exclusion groups, and skill pool keys:

| Category ID | Matches |
|-------------|---------|
| `sword` | Swords |
| `bow` | Bows |
| `crossbow` | Crossbows |
| `pickaxe` | Pickaxes |
| `shovel` | Shovels |
| `axe` | Axes |
| `hoe` | Hoes |
| `helmet` | Helmets (any material) |
| `chestplate` | Chestplates (any material) |
| `leggings` | Leggings (any material) |
| `boots` | Boots (any material) |
| `armor` | Any armor piece |
| `shield` | Shields |
| `trident` | Tridents |
| `skill_charm` | Skill Charms (addon item) |

Items not matching any category are ignored by the imbue system.

---

## 📋 Full Example Config

```json
{
    "item_whitelist": [],
    "item_blacklist": [],
    "loot_table_whitelist": [],
    "distance_scaling": {
        "enabled": true,
        "origin": [0, 0],
        "brackets": [
            { "distance": 5000, "max_level": 1, "chance_mult": 1.0 },
            { "distance": 25000, "max_level": 3, "chance_mult": 1.0 },
            { "distance": 50000, "max_level": 5, "chance_mult": 1.0 }
        ]
    },
    "dimension_overrides": {
        "minecraft:overworld": {
            "imbue_chance": 0.25,
            "max_skills": 1,
            "min_level": 1,
            "max_level": 2
        },
        "minecraft:the_nether": {
            "imbue_chance": 0.5,
            "max_skills": 2,
            "min_level": 2,
            "max_level": 5
        },
        "minecraft:the_end": {
            "imbue_chance": 0.75,
            "max_skills": 3,
            "min_level": 999,
            "max_level": 999
        }
    },
    "category_settings": {},
    "exclusion_groups": [],
    "global": [
        { "skill": "any", "weight": 20 }
    ]
}
```

This config:
- Enables imbuing in all three vanilla dimensions with escalating rarity.
- Uses distance scaling from spawn — better skills further out.
- Uses the `"any"` wildcard to include all registered imbuable skills.
- No filtering — all valid equipment can receive skills.

---

## 💡 Tips & Troubleshooting

### Nothing is getting imbued
1. Check that the dimension is listed in `dimension_overrides`.
2. Verify the `imbue_chance` is greater than 0.
3. Ensure at least one skill has `"loot_mode": "both"` or `"loot_mode": "imbue_only"`.
4. Check that the item matches a valid equipment category.

### Skills always appear at level 1
- Your `distance_scaling` brackets might be too restrictive. Check the first bracket isn't covering your test area at max_level 1.
- `category_settings` might be overriding with `"max_level": 1`.

### Too many skills on gear
- Lower `max_skills` in `dimension_overrides` or `category_settings`.

### Specific skills appearing too often
- Lower their `weight` relative to other entries.
- Remove explicit entries if using `"any"` and rely on equal weighting.

### Testing Imbued Loot
Use `/loot give @s loot minecraft:chests/end_city_treasure` to generate loot from specific tables and check for imbued skills.

---

*For loot injection (adding items to chests/mob drops), see [Universal Loot System](./Universal_Loot_System.md). For the anvil and manual imbuing workflow, see [Datapack Guide — Point H](./DATAPACK_GUIDE.md#%EF%B8%8F-point-h-the-anvil--imbuing-ecosystem).*

---

[< Back to Core Index](index.md) | [Next: Universal Loot System >](Universal_Loot_System.md)
