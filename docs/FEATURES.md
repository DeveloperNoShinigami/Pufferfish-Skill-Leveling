# Features Reference

Complete list of all features in the Pufferfish Skill Leveling Addon.

---

## Core Features

### Multi-Level Skill Progression
Skills can have any number of levels (1 to N), each granting cumulative or distinct rewards. This systems allows progression beyond the vanilla limit of 1 level per skill.

**Technical ID**: `puffish_skill_leveling:per_level_rewards`

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
| `type` | string | `puffish_skills:default` | Use `puffish_skill_leveling:stackable` to scale base rewards. |

### Stackable Skill Type (Hybrid Scaling)
Restores the ability for base Pufferfish rewards (e.g., attributes) to scale alongside per-level rewards.

**Technical ID**: `puffish_skill_leveling:stackable`

- **Behavior**: When a skill of this type levels up, the addon triggers **ALL** rewards in the definition for the current level.
- **Use Case**: Creating a "Strength" skill where the base `generic.attack_damage` attribute increases at every level (+1, +2, +3...) instead of just being applied once at Level 1.
- **Status**: [Planned Feature] - Intention documented in roadmap; code implementation deferred.

### Toggle Skills (Trigger Skills)
Skills can be configured as binary "On/Off" abilities (e.g., Night Vision, Rage).

**Technical ID**: `puffish_skill_leveling:toggle`

- **Key Mechanics**:
    - **Persistence**: Toggle state (ON/OFF) is saved and persists across world joins.
    - **Silent Join**: Effects persist on login, but one-shot rewards (like "Enabled" messages) do not re-fire.
    - **Cooldowns**: Supports the `cooldown` field (in seconds) to prevent spamming abilities.
- **Configuration**: Set `"toggle": true` and assign a `"keybind_slot"` (1-9).
- **Rewards**: Uses `enable_rewards` and `disable_rewards` blocks to define distinct actions.
- **Visual States**: Disabled toggle skills display specific tooltips based on their `loot_mode`:
    - `"imbue_only"`: **DISABLED (Equip item to use)**
    - `"tome_only"`: **DISABLED (Read tome to learn)**
    - `"both"`: **DISABLED (Equip or Learn to use)**
    - Only skills with `level > 0` (via learning or equipping) can be toggled on.

### Enhanced Reward Types
The addon introduces specialized rewards to support advanced progression.

| ID | Description | Parameters |
|----|-------------|------------|
| `puffish_skills:effect` | Grants potion effects. | `effect`, `amplifier`, `duration`, `show_particles`, `show_icon`, `persistent`, `is_protected`. |
| `puffish_skill_leveling:command` | Executes server-side commands. | `command` (supports placeholder `<player>`). |
| `puffish_skill_leveling:experience` | Grants category-specific XP. | `category`, `amount`. |

---

## Technical Cost Mechanics
All XP costs (`enchantment_cost`, `imbuement_cost`, `slot_opening_cost`, `cleansing_cost`) support high-flexibility formats:

| Format | Example | Behavior |
|--------|---------|----------|
| **Scalar** | `5` | Multiplied by target level (e.g., Level 2 costs 10 XP). |
| **Array** | `[5, 10, 20]` | Index-based lookup (Index = Level - 1). |
| **Expression**| `"level * 2 + 5"`| Evaluates math string with `level` variable. |
| **Object** | `{"type": "expression", "data": {"expression": "..."}}` | Explicitly defined for complex data. |

> [!NOTE]
> **Legacy Support**: `enchantment_levels` and `imbuement_levels` are compatible aliases for `enchantment_cost` and `imbuement_cost`.

---

## Tiered Prerequisite System
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

### Visual Discovery (Hidden Skills)
Skills with `"hidden": true` are completely invisible until ALL `prerequisite_skills` are satisfied.
- **Hidden Elements**: Icon, connection lines, tooltip, and tree placement.
- **Goal**: Creates "Discovery" moments for secret techniques and prestige content.

---

## Equipment & Anvil Mechanics

### Skill Imbuing & Slots
- **Multi-Slot System**: Equipment can hold up to **3 unique skill slots**.
- **Sigil of Imbuement**: Use in an Anvil to open a new slot on gear.
- **Tome Imbuing**: Combine a Skill Tome + Slotted Gear to imbue the skill into an empty slot.
- **Gear Upgrading**: Combine a Skill Tome + Gear already having that skill to upgrade its level (requires same level, results in Level+1).

### Tome Synthesis (Ranking Up)
- **Tome + Tome**: Combine two identical Skill Tomes of the same level (N) at an Anvil to create a **Level N+1** tome.
- **Namespace Agnostic**: Supports "Fuzzy ID" matching. A tome for `vitality` will correctly match gear or tree skills named `modid:vitality`.

### Skill Extraction (Cleansing)
- **Tome of Cleansing**: Specialized extraction tomes targeted at specific slots (I, II, III).
- **Behavior**: Removes the skill from gear and returns it as a Skill Tome.

### Point Bypass Mechanic (Paid Level Tracker)
The addon tracks which levels were actually purchased with points vs. which were granted by external items.
- **Skill Tomes / Imbuing**: Automatically bypasses point costs. These levels are marked as "Granted".
- **Refunds**: Refunding a "Granted" level returns 0 points. Refunding a "Paid" level returns the full cost.
- **Flexible Matching**: Imbuing and bonus calculation use **Namespace Agnostic Pathing**. This allows "normally generated" tomes with short IDs (e.g., `vitality`) to correctly match gear and tree skills with namespaced IDs (e.g., `template:vitality`).
- **Goal**: Ensures discovery-based progression doesn't interfere with the player's intentional point allocation.

---

### Loot Mode System

Controls how skills can be acquired and used:

| Mode | Skill Tree | Imbuing | Use Case |
|------|------------|---------|----------|
| `"both"` | ✅ | ✅ | Standard skills |
| `"tome_only"` | ✅ | ❌ | Tree-exclusive (no equipment) |
| `"imbue_only"` | ❌ | ✅ | Equipment-exclusive (hidden in tree) |

---

## Skill Master Villager
A specialized professional for skill item trade and progression.

- **Infrastructure**: Spawns in custom **Skill Master Houses** with unique tiered loot barrels.
- **Trading**: Offers Skill Tomes, Sigils, and progression artifacts.
- **Mastery Pricing**: Prices scale down as the player masters more skills.
- **Tome Upgrading**: Trade lower-level tomes + emeralds for higher versions (Tier 3+).

### Professional Tiers
The Skill Master follows standard villager leveling (Novice to Master):
- **Novice (T1)**: Basic tomes and entry-level skill trades.
- **Apprentice (T2)**: Tome of Clear Mind, Tome of Cleansing, and Tome of Progression.
- **Journeyman (T3)**: Tome Upgrades and mid-level skills.
- **Expert (T4)**: Advanced skill trades and better pricing.
- **Master (T5)**: Chance for Sigils of Imbuement and highest-level tomes.

---

## Items Reference

| Item | Behavior |
|------|----------|
| **Skill Tome** | Grants +1 level to a specific skill. Can be combined for higher levels. |
| **Skill Charm** | **Curios Slot** item that can be imbued with skills just like armor. |
| **Sigil of Imbuement** | Adds an imbuement slot to an item (Max 3). |
| **Tome of Progression** | Instant +1 Level to any chosen skill via GUI. |
| **Tome of Clear Mind** | Refund 1 level of a chosen skill via GUI. |
| **Tome of Greater Clear Mind** | Fully reset a skill to Level 0 and refund all points. |
| **Tome of Cleansing** | Extract imbued skills from equipment slots (Tiers I-III). |
| **Blank Tome** | Base requirement for crafting specialized artifacts. |

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

## Commands Reference

| Command | Description |
|---------|-------------|
| `/skillleveling get <player> <cat> <skill>` | View current base skill level. |
| `/skillleveling set <player> <cat> <skill> <level>` | Forcefully set a player's base skill level. |
| `/skillleveling give tome <player> <cat> <skill> [mode] [lvl]` | Generate specific Skill Tomes. |
| `/skillleveling advance <player> <cat> <skill>` | Level up (costs points/validates requirements). |
| `/skillleveling refund <player> <cat> <skill> [amount/all]` | Full or partial skill reset with point return. |
| `/skillleveling info <player> <cat> <skill>` | Debug info: Levels, Point Bypass, and Context. |
| `/skillleveling list <player>` | Overview of all learned skill levels. |
| `/skillleveling categorylevel <player> <cat> <lvl>` | Set category level (calculates XP) |
| `/skillleveling villager forceProfession` | Convert looking-at villager to Skill Master |
| `/skillleveling villager setTier <1-5>` | Admin control over Skill Master level. |
| `/skillleveling villager reset` | Reset trades/experience of a Skill Master. |

---

## Compatibility

- **Curios API**: Full support for Skill Charms and accessory imbuing.
- **Native Attribute Sync**: Compatible with any mod using Minecraft's attribute registry.
- **Namespace Agnostic**: Matches short IDs to namespaced IDs automatically.
- **Safe Mixins**: Uses non-intrusive injection points to ensure compatibility with other mods.
- **Base Mod Support**: Fully preserves vanilla Pufferfish Skills (Level 1) behavior.

---
*See [Datapack Guide](./DATAPACK_GUIDE.md) for technical configuration details.*
