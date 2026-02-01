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
| `descriptions` | object | — | Level-specific descriptions |
| `extra_descriptions` | object | — | Preview text for next level |
| `prerequisite_skills` | array | — | Required skills before unlocking |

### Skill Master Villager
A new villager profession specialized in trading skill-related items.

- **Dynamic Trades**: Offers Skill Tomes, Sigils, and Cleansing Tomes.
- **Progression-Linked**: Wares update based on your current skill levels and "Mastery".
- **Mastery Bonuses**: Having many maxed-out skills unlocks cheaper prices and rare offers like Sigils of Imbuement.
- **Tome Upgrades**: Allows trading lower-level tomes + emeralds for higher-level versions (Tier 3+ villager).
- **Fallback Logic**: If no specific progression trades are available, the villager provides starter tomes.

### Professional Tiers
The Skill Master follows standard villager leveling (Novice to Master):
- **Novice (T1)**: Basic tomes and entry-level skill trades.
- **Apprentice (T2)**: Tome of Clear Mind and Tome of Cleansing.
- **Journeyman (T3)**: Tome Upgrades and mid-level skills.
- **Expert (T4)**: Advanced skill trades and better pricing.
- **Master (T5)**: Chance for Sigils of Imbuement and highest-level tomes.

---

## Skill & Progression Items

| Item | Behavior |
|------|----------|
| **Skill Tome** | Grants +1 level to a specific pre-configured skill. |
| **Sigil of Imbuement** | Opens a skill slot on equipment (up to 3 slots). |
| **Tome of Progression** | Select and advance any skill by 1 level via GUI. |
| **Tome of Clear Mind** | Refund 1 level of a selected skill via GUI. |
| **Tome of Cleansing** | Extract imbued skills from a specific slot on gear. |
| **Blank Tome** | Crafting ingredient for specialized tomes. |

---

## Equipment Imbuing

- **Application**: Combine a Skill Tome and gear in an anvil.
- **Requirement**: Skill must be defined in the datapack with appropriate `imbue_only` or `both` loot mode.
- **Stacking**: Multiple imbued skills on one item stack their attribute bonuses.
- **Dynamic Sync**: Attribute changes (Health, Damage, MS) apply immediately when equipped.

---

## Staff Commands

| Command | Description |
|---------|-------------|
| `/skillleveling get <player> <cat> <skill>` | View current base skill level |
| `/skillleveling set <player> <cat> <skill> <level>` | Admin override to set a specific level |
| `/skillleveling refund <player> <cat> <skill> <qty\|all>` | Refund levels and return spent points |
| `/skillleveling info <player> <cat> <skill>` | Detailed level/point/cost breakdown |
| `/skillleveling list <player>` | List all learned skills and levels |
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
