# Features Reference

Complete list of all features in the Pufferfish Skill Leveling Addon.

---

## Core Features

### Multi-Level Skill Progression
Skills can have any number of levels (1 to N), each granting cumulative or distinct rewards.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `max_skill_level` | integer | 1 | Maximum levels for this skill |
| `points_per_level` | integer | 1 | Skill points required per level |
| `category_id` | string | — | Category this skill belongs to |
| `loot_mode` | string | — | `"tome_only"`, `"imbue_only"`, or `"both"` |
| `merge_description` | boolean | false | Accumulate descriptions across levels |
| `hidden` | boolean | false | Completely hide skill from UI until prerequisites met |
| `required_skill` | array | — | Cross-category requirements (object with `skill_id`, `level`, `category_id`) |
| `descriptions` | object | — | Level-specific descriptions |
| `extra_descriptions` | object | — | Preview text for next level |

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

## Equipment Imbuing

- **Application**: Combine a Skill Tome and gear in an anvil.
- **Requirement**: Skill must be defined in the datapack with appropriate `imbue_only` or `both` loot mode.
- **Slots**: Most gear supports up to 3 slots (via Sigils). Skill Charms provide dedicated slots for charms.
- **Stacking**: Multiple imbued skills on one item stack their attribute bonuses.
- **Dynamic Sync**: Attribute changes (Health, Damage, MS) apply immediately when equipped.

---

## Staff Commands

| Command | Description |
|---------|-------------|
| `/skillleveling get <player> <cat> <skill>` | View current base skill level |
| `/skillleveling set <player> <cat> <skill> <level>` | Admin override (sets level directly) |
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

- **Native Attribute Sync**: Compatible with any mod using Minecraft's attribute registry.
- **Namespace Agnostic**: Works across any custom namespace in datapacks.
- **Safe Mixins**: Uses non-intrusive injection points to ensure compatibility with other mods.
- **Base Mod Support**: Fully supports standard Pufferfish Skills (max level 1).

---
*See [Datapack Guide](./DATAPACK_GUIDE.md) for technical configuration details.*
