# Features Reference

Complete list of all features in the Pufferfish Skill Leveling Addon.

---

## Core Features

### Multi-Level Skill Progression
Skills can have any number of levels (1 to N), each granting cumulative or distinct rewards.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `max_skill_level` | integer | 1 | Maximum levels for this skill. Alias: `max_levels`. |
| `points_per_level`| integer | 1 | Skill points required per level. |
| `category_id` | string | — | Category string (**Mandatory** for Creative Tab items). |
| `loot_mode` | string | — | `"tome_only"`, `"imbue_only"`, or `"both"`. |
| `hidden` | boolean | false | Completely hide skill icon until prerequisites met. |
| `merge_description`| boolean | false | Accumulate tooltips across all attained levels. |
| `descriptions` | object | — | Keyed list of descriptions (e.g., `"1": "Level 1 info"`). |
| `extra_descriptions`| object | — | Keyed list of level-up previews (Use `"0"` for unlock). |

### Technical Cost Mechanics
All XP costs (`enchantment_cost`, `imbuement_cost`, `slot_opening_cost`, `cleansing_cost`) support four high-flexibility formats:

| Format | Example | Behavior |
|--------|---------|----------|
| **Scalar** | `5` | Multiplied by target level (e.g., Level 2 costs 10 XP). |
| **Array** | `[5, 10, 20]` | Index-based lookup (Index = Level - 1). |
| **Expression**| `"level * 2 + 5"`| Evaluates math string with `level` variable. |
| **Object** | `{"type": "expression", "data": {"expression": "..."}}` | Explicitly defined for complex data. |

> [!NOTE]
> **Legacy Support**: `enchantment_levels` and `imbuement_levels` are compatible aliases for `enchantment_cost` and `imbuement_cost`.

---

### Tiered Prerequisite System
The addon uses a multi-layered requirement system to control progression flow.

#### 1. Initial Unlock (`prerequisite_skills`)
- **Field**: `prerequisite_skills` (or legacy `required_skill`).
- **Function**: Controls the first-time appearance and purchase availability.
- **Scope**: Skill-wide. Once unlocked, it stays unlocked.

#### 2. Tiered Level Gating (`required_skill_for_level`)
- **Field**: `required_skill_for_level`.
- **Function**: Blocks specific level increments (e.g., Level 5 requires Sword Mastery 3).
- **Format**: `{"level_number": [{"skill": "id", "min_level": N, "category": "cat"}]}`.
- **Modern Standard**: This field replaces the "old" flat `required_skill` logic for depth-based gating.

### Point Bypass Mechanic (Paid Level Tracker)
The addon tracks which levels were actually purchased with points vs. which were granted by external items.
- **Skill Tomes / Imbuing**: Automatically bypasses point costs. These levels are marked as "Granted".
- **Refunds**: Refunding a "Granted" level returns 0 points. Refunding a "Paid" level returns the full cost.
- **Flexible Matching**: Imbuing and bonus calculation use **Namespace Agnostic Pathing**. This allows "normally generated" tomes with short IDs (e.g., `vitality`) to correctly match gear and tree skills with namespaced IDs (e.g., `template:vitality`).
- **Goal**: Ensures discovery-based progression doesn't interfere with the player's intentional point allocation.

### Visual Discovery (Hidden Skills)

Skills with `"hidden": true` are completely invisible in the skill tree until their prerequisites are met.

**What Gets Hidden**:
- Skill icon
- Connection lines (to/from the skill)
- Tooltip and descriptions
- Any indication the skill exists

**When It Reveals**: Automatically appears once ALL `prerequisite_skills` are satisfied, creating a "discovery" moment.

**Use Cases**: Secret techniques, prestige content, Easter eggs, or rewards for fully exploring a skill branch.

---

### Prerequisite System

The addon provides **three distinct prerequisite layers**:

#### 1. Top-Level Unlock (`prerequisite_skills`)
- **Location**: Root of skill definition.
- **Purpose**: Controls when the skill icon appears/unlocks in the tree.
- **Behavior**: Once purchased, the skill is permanent regardless of prerequisite loss.

#### 2. Level Progression Gates (`required_skill_for_level`)
- **Location**: Root of skill definition.
- **Purpose**: Blocks specific levels (e.g., Level 5 requires Sword Master 3).
- **Behavior**: Prevents leveling up until requirements are met.

> [!TIP]
> Use Top-Level for tree structure, and Progression Gates for mastery chains.

---

### Loot Mode System

Controls how skills can be acquired and used:

| Mode | Skill Tree | Imbuing | Use Case |
|------|------------|---------|----------|
| `"both"` | ✅ | ✅ | Standard skills |
| `"tome_only"` | ✅ | ❌ | Tree-exclusive (no equipment) |
| `"imbue_only"` | ❌ | ✅ | Equipment-exclusive (hidden in tree) |

---

### Skill Master Villager
A new villager profession specialized in trading skill-related items.

- **Workstation**: Skill Scribe Table
- **Village Houses**: Skill Masters spawn in custom jigsaw-integrated houses in all primary village types.
- **Dynamic Trades**: Offers Skill Tomes, Sigils, and Cleansing Tomes based on player progression.
- **Reputation Config**: Trading prices and experience controlled via `skill_master_reputation/config.json`.
- **Mastery Bonuses**: Having many maxed-out skills unlocks cheaper prices and rare offers like Sigils of Imbuement.
- **Tome Upgrades**: Trade lower-level tomes + emeralds for higher-level versions (Tier 3+ villager).

### Professional Tiers
The Skill Master follows standard villager leveling (Novice to Master):
- **Novice (T1)**: Basic tomes and entry-level skill trades.
- **Apprentice (T2)**: Tome of Clear Mind, Tome of Cleansing, and Tome of Progression.
- **Journeyman (T3)**: Tome Upgrades and mid-level skills.
- **Expert (T4)**: Advanced skill trades and better pricing.
- **Master (T5)**: Chance for Sigils of Imbuement and highest-level tomes.

---

## Skill & Progression Items

| Item | Behavior |
|------|----------|
| **Skill Tome** | Grants +1 level to a specific pre-configured skill. |
| **Skill Charm** | Curio item that provides equipment slots for skill imbuing. |
| **Sigil of Imbuement** | Opens a skill slot on equipment (up to 3 slots). |
| **Tome of Progression** | Select and advance any skill by 1 level via GUI. |
| **Tome of Clear Mind** | Refund 1 level of a selected skill via GUI. |
| **Tome of Cleansing** | Extract imbued skills from a specific slot on gear. |
| **Blank Tome** | Crafting ingredient for specialized tomes. |

---

- **Dynamic Sync**: Attribute changes (Health, Damage, MS) apply immediately when equipped.

---

## Technical Costs (XP Expressions)
The addon supports dynamic XP cost calculation using common math expressions (e.g., `level * 5 + 10`).

| Field | Location | Description |
|-------|----------|-------------|
| `enchantment_cost` | Skill | XP cost to combine Tome + Item in Anvil |
| `imbuement_cost` | Skill | XP cost for specialized manual imbuing |
| `slot_opening_cost`| Skill | XP cost to use a Sigil of Imbuement on the item |
| `cleansing_cost` | Skill | XP cost to extract a skill using a Tome of Cleansing |

---

## Staff Commands

| Command | Description |
|---------|-------------|
| `/skillleveling get <player> <cat> <skill>` | View current base skill level |
| `/skillleveling set <player> <cat> <skill> <level>` | Admin override (sets level directly) |
| `/skillleveling give tome <player> <cat> <skill> [mode] [lvl]` | Generate custom Skill Tomes for testing |
| `/skillleveling advance <player> <cat> <skill>` | Advance skill by 1 level (costs points) |
| `/skillleveling refund <player> <cat> <skill> [amount]` | Refund levels and return spent points |
| `/skillleveling refund <player> <cat> <skill> all` | Fully reset a skill to level 0 |
| `/skillleveling info <player> <cat> <skill>` | Detailed level/point/cost breakdown |
| `/skillleveling list <player>` | List all learned skills and levels |
| `/skillleveling categorylevel <player> <cat> <lvl>` | Set category level (calculates XP) |
| `/skillleveling villager forceProfession` | Convert looking-at villager to Skill Master |
| `/skillleveling villager setTier <1-5>` | Set the Skill Master's level |
| `/skillleveling villager reset` | Reset looking-at Skill Master (Tier 1, 0 Exp) |

---

## Compatibility

- **Curios API**: Full support for dedicated accessory slots and skill-imbued charms.
- **Native Attribute Sync**: Compatible with any mod using Minecraft's attribute registry.
- **Namespace Agnostic**: Works across any custom namespace in datapacks.
- **Safe Mixins**: Uses non-intrusive injection points to ensure compatibility with other mods.
- **Base Mod Support**: Fully supports standard Pufferfish Skills (max level 1).

---
*See [Datapack Guide](./DATAPACK_GUIDE.md) for technical configuration details.*
