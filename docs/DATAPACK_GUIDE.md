# Datapack Guide: Point A to Point Z

Welcome to the definitive manual for the **Pufferfish Skill Leveling Addon**. This guide is structured as a journey: it begins with the simple foundation (Point A) and advances through every technical feature, concluding with complex economy configurations (Point Z).

> [!IMPORTANT]
> This addon provides the systems; you provide the content. You must create these files for your skills to exist in the world.

---

## 🛠️ Point A: The Foundation (Structure)

Before you write a single line of logic, your folder hierarchy must be perfect. Minecraft ignores datapacks with even tiny structural errors.

### 1. The Directory Tree
```
your_datapack/
├── pack.mcmeta
└── data/
    └── <your_namespace>/
        └── puffish_skills/
            └── categories/
                └── <category_id>/  <-- e.g., 'combat'
                    ├── category.json
                    ├── definitions.json <-- Technical Logic
                    ├── skills.json      <-- Visual Layout
                    └── connections.json
```

### 2. pack.mcmeta
```json
{
    "pack": {
        "pack_format": 15,
        "description": "My Custom Skill Leveling Pack"
    }
}
```

---

## 📖 Point B: The Schema (Field Reference)

Every skill in `definitions.json` is an object keyed by its **Unique ID**.

### 📋 The Exhaustive Field Reference

| Field | Type | Required? | Description |
| :--- | :--- | :--- | :--- |
| `type` | string | [Opt] | Defaults to `puffish_skills:default`. Can be omitted for standard/toggle skills. Can be omitted for standard/toggle skills. |
| `category_id` | string | **YES** | Must match the folder name. Required for Creative Tab Items. |
| `title` | string | **YES** | **MUST match the ID key** for clear command/data tracking. |
| `points_per_level`| int | **YES** | Point cost per level (Set to `0` for loot-only skills). |
| `metadata` | object | **YES** | Required by parser. Use `{}` if no extra data is needed. |
| `max_skill_level` | int | [Opt] | The level ceiling (Default: 1). Alias: `max_levels`. |
| `loot_mode` | string | [Opt] | Path: `"both"`, `"tome_only"`, or `"imbue_only"`. |
| `hidden` | boolean | [Opt] | If true, icon is invisible until prerequisites are met. |
| `merge_description`| boolean | [Opt] | If true, current-level tooltips list all previous rank bonuses. |
| `descriptions` | map | [Opt] | Map of level-based tooltips (Keyed `"1"`, `"2"`, etc). |
| `extra_descriptions`| map | [Opt] | Map of "Next Rank" previews (Key `"0"` is for first unlock). |
| `prerequisite_skills`| array | [Opt] | **Initial Reveal/Buy** requirements. Alias: `required_skill`.|
| `required_skill_for_level` | object | [Opt] | Gates **specific levels** (e.g., Lvl 5 requires mastery). |
| `enchantment_cost` | mixed | [Opt] | XP cost for Anvil combining. Supports Scalar/Array/Math. |
| `imbuement_cost` | mixed | [Opt] | XP cost for Sigil imbuing. Supports Scalar/Array/Math. |
| `slot_opening_cost`| mixed | [Opt] | XP cost for Opening Slots. Supports Scalar/Array/Math. |
| `cleansing_cost` | mixed | [Opt] | XP cost for Extracting Tomes. Supports Scalar/Array/Math. |

---

## 🗡️ Point C: Visibility & Discovery

Control how players find your skills using specialized acquisition states.

### 1. Loot Modes (`loot_mode`)
| Mode | Discovery Path | Use Case |
| :--- | :--- | :--- |
| `"both"` | Tree Purchase + Gear Imbuing | Standard progression skills. |
| `"tome_only"` | Tree Purchase + Tome Use | Skills that exist ONLY for the player, not gear. |
| `"imbue_only"`| **Hidden from Tree** | Equipment-only enchantments (Item Passives). |

### 2. Hidden Skills (`hidden`)
When a skill is `"hidden": true`, the player cannot see the icon, lines, or text in the tree. It "reveals" (becomes visible) only when ALL **Point D-1 Prerequisites** are satisfied.

---

## 🏹 Point D: The Gating Systems (Mastery Logic)

The addon allows you to link categories and create complex dependencies.

### 1. Initial Unlocks (`prerequisite_skills`)
These control when a skill first appears or becomes purchasable. Use the `category` field for cross-category requirements.

```json
"advanced_fletching": {
    "title": "advanced_fletching",
    "prerequisite_skills": [
        { 
            "skill": "basic_archery", 
            "min_level": 5 
        },
        { 
            "skill": "woodworking", 
            "min_level": 3, 
            "category": "utility" 
        } 
    ]
}
```

### 2. Tiered Gating (`required_skill_for_level`)
This blocks **specific levels** of a skill, forcing players to master other branches before reaching the peak of a high-tier skill.

```json
"dragons_breath": {
    "max_skill_level": 5,
    "required_skill_for_level": {
        "3": [
            { 
                "skill": "breath_control", 
                "min_level": 3 
            }
        ],
        "5": [
            { 
                "skill": "breath_control", 
                "min_level": 5 
            },
            { 
                "skill": "ancient_essence", 
                "min_level": 1, 
                "category": "ancient" 
            }
        ]
    }
}
```

---

## 🧪 Point E: Polish & Descriptions

Manage tooltips to guide players through their multi-rank journey.

### 1. Level 0: The Unlocked State
Players have a "Level 0" state when they can see a skill but haven't bought any levels yet.
- **`descriptions: { "0": "..." }`**: Flavor text for the base skill.
- **`extra_descriptions: { "0": "..." }`**: A preview of what Level 1 will grant.

### 2. Numbering Logic
| State | Current Bonus (`descriptions`) | Next Rank Preview (`extra_descriptions`) |
| :--- | :--- | :--- |
| **Unlocked** | Lvl 0 Text | Lvl 1 Preview |
| **Rank 1** | Lvl 1 Text | Lvl 2 Preview |
| **Maxed** | Max Level Text | "— MAXED OUT —" |

---

---

## 👑 Point F: Technical Rewards (`per_level_rewards`)

This is the core power of the addon. Use it to specify exactly what happens at every single level increment. **Do not skip levels**; if your `max_skill_level` is 5, you must define rewards for levels 1, 2, 3, 4, and 5 in the map for consistent behavior.

#### Example: Continuous Progression (Stackable)
The `puffish_skill_leveling:stackable` type is a **hybrid**. It supports both standard rewards (applied once when the skill is first unlocked) and per-level rewards (applied/incremented at every level).

```json
"champion_seal": {
    "type": "puffish_skill_leveling:stackable",
    "max_skill_level": 5,
    "rewards": [
        {
            "type": "puffish_skills:attribute",
            "data": {
                "attribute": "generic.max_health",
                "value": 2,
                "operation": "addition"
            }
        },
        {
            "type": "puffish_skill_leveling:per_level_rewards",
            "data": {
                "skill_id": "champion_seal",
                "levels": {
                    "1": [
                        {
                            "type": "puffish_skills:command",
                            "data": {
                                "command": "say Rank 1!"
                            }
                        }
                    ],
                    "2": [
                        {
                            "type": "puffish_skills:command",
                            "data": {
                                "command": "say Rank 2!"
                            }
                        }
                    ],
                    "3": [
                        {
                            "type": "puffish_skills:command",
                            "data": {
                                "command": "say Rank 3!"
                            }
                        }
                    ],
                    "4": [
                        {
                            "type": "puffish_skills:command",
                            "data": {
                                "command": "say Rank 4!"
                            }
                        }
                    ],
                    "5": [
                        {
                            "type": "puffish_skills:command",
                            "data": {
                                "command": "say Rank 5!"
                            }
                        }
                    ]
                }
            }
        }
    ],
    "metadata": {}
}
```

---

## 🔘 Point F.1: Toggle Skills

Any skill can be made into a "Toggle Skill" by adding the `toggle` field. These skills are binary (Enabled/Disabled) and can be toggled for free once unlocked.

- **`toggle`**: (Boolean) Set to `true` to enable toggle behavior.
- **`keybind_slot`**: (Integer, 1-9) Assigns the skill to one of the 9 "Mastery Key" slots in the Controls menu.
- **`cooldown`**: (Integer, Ticks) Specifies the cooldown period after a skill is **disabled**. Players cannot enable the skill again until this time has passed.
- **`puffish_skill_leveling:toggle`**: A special reward type that defines what happens when the skill is enabled or disabled. It wraps arrays of other reward types in `enable_rewards` and `disable_rewards`.
- **`puffish_skills:effect`**: A special reward type that applies potion effects to the player.
    - **`effect`**: (String) The identifier of the effect (e.g., `"minecraft:haste"`, `"alexsmobs:speedy_momentum"`). Supports all modded effects.
    - **`amplifier`**: (Integer) The level of the effect (0 = Level I).
    - **`duration`**: (Integer, Ticks) How long the effect lasts. Use `-1` for infinite (ideal for toggles).

#### Example: Comprehensive Toggle Mastery
This example shows a toggle skill with standard Pufferfish fields (`title`, `description`, `icon`) and multiple toggle rewards.

```json
"berserker_rage": {
    "title": "Berserker Rage",
    "description": "Enter a state of reckless fury.",
    "icon": {
        "type": "item",
        "data": { "item": "minecraft:netherite_axe" }
    },
    "toggle": true,
    "keybind_slot": 2,
    "cooldown": 600,
    "rewards": [
        {
            "type": "puffish_skill_leveling:toggle",
            "data": {
                "enable_rewards": [
                    {
                        "type": "puffish_skills:effect",
                        "data": {
                            "effect": "minecraft:strength",
                            "amplifier": 1,
                            "duration": -1
                        }
                    },
                    {
                        "type": "puffish_skills:attribute",
                        "data": {
                            "attribute": "generic.attack_speed",
                            "value": 0.2,
                            "operation": "multiply_base"
                        }
                    }
                ],
                "disable_rewards": [
            }
        }
    ],
    "metadata": {
        "icon": "berserker_rage_icon_id"
    }
}
```

---

## 💎 Point G: Technical Economies (Costs)

All cost fields (`enchantment_cost`, `imbuement_cost`, `slot_opening_cost`, `cleansing_cost`) support three formats:

1.  **Integer (Scalar)**: `5` -> (Target Level * 5 XP Levels).
2.  **Array**: `[5, 10, 15, 20, 30]` -> Specific hardcoded cost per rank.
3.  **Expression**: `"level * 2 + (level^2)"` -> Evaluated math using the `level` variable.

### 4. Comprehensive Example: Arcane Striker
This example combines everything: 5 Levels, Mixed Rewards (Attributes + Effects + Commands), and a Mathematical Enchantment Cost.

```json
"arcane_striker": {
    "title": "Arcane Striker",
    "description": "Infuse your strikes with magic.",
    "icon": {
        "type": "item",
        "data": { "item": "minecraft:amethyst_shard" }
    },
    "max_skill_level": 5,
    "loot_mode": "both",
    "enchantment_cost": {
        "type": "expression",
        "data": { "expression": "level * 5 + 10" }
    },
    "imbuement_cost": 5,
    "rewards": [
        {
            "type": "puffish_skill_leveling:per_level_rewards",
            "data": {
                "skill_id": "arcane_striker",
                "levels": {
                    "1": [
                        { "type": "puffish_skills:attribute", "data": { "attribute": "generic.attack_damage", "value": 1, "operation": "addition" } }
                    ],
                    "2": [
                        { "type": "puffish_skills:attribute", "data": { "attribute": "generic.attack_damage", "value": 2, "operation": "addition" } }
                    ],
                    "3": [
                        { "type": "puffish_skills:attribute", "data": { "attribute": "generic.attack_damage", "value": 3, "operation": "addition" } },
                        { "type": "puffish_skills:effect", "data": { "effect": "minecraft:glowing", "duration": 100, "amplifier": 0 } }
                    ],
                    "4": [
                        { "type": "puffish_skills:attribute", "data": { "attribute": "generic.attack_damage", "value": 4, "operation": "addition" } },
                        { "type": "puffish_skills:effect", "data": { "effect": "minecraft:glowing", "duration": 120, "amplifier": 0 } }
                    ],
                    "5": [
                        { "type": "puffish_skills:attribute", "data": { "attribute": "generic.attack_damage", "value": 5, "operation": "addition" } },
                        { "type": "puffish_skills:effect", "data": { "effect": "minecraft:glowing", "duration": 140, "amplifier": 0 } },
                        { "type": "puffish_skills:command", "data": { "command": "particle minecraft:witch %player_x% %player_y% %player_z% 0 0 0 1 10" } }
                    ]
                }
            }
        }
    ],
    "metadata": { "icon": "arcane_striker_icon" }
}
```

---

## 💰 Point H: The Skill Master (Point Z)

The **Skill Master** is the professional curator of your datapack's ecosystem. He isn't just a merchant; he is a specialist who facilitates the transfer of power between gear and players.

### 1. Reputation & Tiers
The Skill Master has a unique **Reputation System**. As you trade with him, his tier increases (I through V), unlocking more advanced services.
- **What to Lookout For**: At Tier IV and V, the Master begins offering **"Special Upgrade Trades"**. He can transform low-level Skill Tomes into higher ranks for a high XP and resource cost.
- **Trade Variety**: He sells more than just tomes. He is the primarily source for **Sigils of Imbuement** and **Tomes of Cleansing**.

### 2. Reputation & Trade Scaling
The Skill Master values long-term relationships. Authors can configure his professional behavior through internal registry files:
- **Scaling Tiers**: You can control how much experience he gains per trade, making the reach to Master level a true late-game achievement for players.
- **Economic Perks**: At higher tiers, the Master becomes more efficient, offering significant discounts on basic tomes and better exchange rates for resources.

### 3. Structural Presence
The Skill Master isn't just a villager; he has a physical home in the world.
- **Skill Master House**: Custom jigsaw-based buildings that generate in villages, featuring his specialized **Skill Scribe Table** workstation.
- **Loot Reflection**: Players should lookout for any chests and barrels found within the Skill Master House. These containers are the primary source for rare structural skills and progression materials that won't appear in standard dungeon loot.

---

## ⚒️ Point I: The Anvil & Imbuing Ecosystem

The anvil is the primary workstation for modifying both your Tomes and your Equipment.

### 1. Tome Combining (Rank Up)
Authors should note that players can increase the rank of a Skill Tome by combining duplicates.
- **Process**: Place **Skill Tome (Rank X)** + **Skill Tome (Rank X)** in an anvil.
- **Result**: A **Skill Tome (Rank X+1)**.
- **Limit**: Cannot exceed the `max_skill_level` defined in your JSON.

### 2. Imbuing (Applying to Gear)
To apply a skill to gear, players need an **Open Skill Slot** (Max 3).
- **Opening a Slot**: Place gear + **Sigil of Imbuement** in an anvil. Cost: `slot_opening_cost`.
- **Applying a Skill**: Place gear + **Skill Tome** in an anvil with a **Sigil of Imbuing**. Cost: `imbuement_cost`.
- **Lookout Warning**: Do not confuse the **Sigil of Imbuement** (opens slot) with the **Sigil of Imbuing** (applies tome). The slot must be opened *first*.
- **Requirements**: Only skills with `loot_mode: "both"` or `"imbue_only"` can be imbued.

### 3. Upgrading Imbued Gear
Equipment-bound skills can be leveled up directly on the item.
- **Process**: Place imbued gear + **Matching Skill Tome** (any rank) in an anvil.
- **Logic**: If gear has Lvl 1 "Strength" + any "Strength" Tome -> Gear upgrades to Lvl 2.
- **Limit**: Strictly capped by the skill's `max_skill_level`.

### 4. Cleansing (Extraction)
Extraction allows players to recover tomes without destroying the gear or the skill.
- **Process**: Place gear + **Tome of Cleansing (I, II, or III)** in an anvil.
- **Slot Targeting**: Tome I targets Slot 1, Tome II targets Slot 2, etc.
- **Result**: The skill returns as a **Skill Tome** at its current level. The slot remains open.
- **Cost**: Defined by your `cleansing_cost` field.

---

## 📜 Point J: Specialized Progression Items

The addon includes several unique artifacts that authors can use as quest rewards or rare loot.

| Item | Usage |
| :--- | :--- |
| **Blank Tome** | The base crafting material for all specialized tomes. |
| **Tome of Progression**| Allows a player to manually select and advance **any** skill by 1 level via a GUI. |
| **Tome of Clear Mind** | Safely refunds **1 level** of a selected skill, returning any points spent. |
| **Tome of Greater Clear Mind** | Performs a **Full Reset** of a skill, returning all points. |
| **Skill Charm** | A **Curios** accessory that provides portable imbuement slots for skills. |

---

## 🧠 Point K: Point Bypass Logic (Paid vs. Granted)

Technical authors must understand how the addon tracks progression to balance their economies.

1.  **Paid Levels**: Levels purchased by the player using Skill Points. These can be refunded for points.
2.  **Granted Levels**: Levels gained via **Skill Tomes** or **Equipment Imbuing**. These **bypass** point costs.
3.  **Refund Interaction**: If a player refunds a "Granted" level, they receive **0 points**. The level is simply removed. This prevents players from "laundering" finding a Tome into free skill points.

---

## 💎 Point L: Grandmaster Author Tips

Specialized advice for creating high-quality, production-ready packs.

### 1. The "Clean Tree" Technique
- **Hidden Skills**: Use `hidden: true` for "Prestige" skills. Don't clutter the UI with Rank 5 Mastery icons if the player hasn't even mastered Rank 1.
- **Category Linking**: Link your categories! Make a high-level "Mining" skill a prerequisite for a "Heavy Armor" skill in Combat. This forces players to value multiple specializations.

### 2. Consistency is King
- **Lookout**: Always ensure your `title` matches the JSON key. If they differ, server logs and admin commands like `/skillleveling get` will become confusing to read.
- **Metadata**: Never forget `"metadata": {}`. The parser is extremely strict; a missing metadata block is the #1 cause of "Missing Skill" errors in the dev console.

---

## 🆘 Troubleshooting & Admin Logic

### Common Errors
- **Missing Items**: Did you set `category_id`?
- **Crash on Load**: Is `metadata` missing?
- **No Progress**: Did you forget `points_per_level`?

### Developer Commands
- `/reload`: The most important tool. Flushes cache and re-parses all JSON.
- `/skillleveling give tome <player> <category> <skill> [loot_mode] [level]`: Generate custom tomes for testing.
- `/skillleveling get <p> <cat> <skill>`: View exact data values.
- `/skillleveling set <p> <cat> <skill> <lvl>`: Manually force a rank.
- `/skillleveling villager setTier <1-5>`: Force a Skill Master to rank up.

---
*Created for Pufferfish Skill Leveling v2.5.0+*
