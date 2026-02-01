# Changelog

All notable changes to the **Pufferfish Skill Leveling** mod will be documented in this file.
 
## [Unreleased] - Skill Master & Crafting Overhaul
 
### Added
- **Blank Tome**: New base crafting material registered to help structure tome progression.
- **Crafting Recipes**: Full progression path for all base tomes (Progression, Clear Mind, Cleansing, Sigil) using Blank Tomes.
- **Dynamic Villager Scaling**: Skill Master trades now scale from 5 to 12 slots based on tier.
- **Intelligent Tome Levels**: Skill tomes offered by villagers now have levels that scale with the villager's tier (e.g., Tier 5 offers Max Level).
- **Global Loot Tables**: Skill Tomes and Blank Tomes integrated into mob drops (Zombies, Wither Skeletons, etc.) and structure chests.
- **Mastery Priority**: Villagers now prioritize offering skills the player hasn't learned yet.
- **Tome Trade-In**: Players can now upgrade tomes through specialized villager trades.
- **Mastery Feedback**: Added unique villager messages for players who have mastered available skills.
 

## [2026-01-30] - Multi-Skill Cleansing & Visual Polish

### Added
- **Tiered Tome of Cleansing (I, II, III)**: New items that target specific equipment slots (1, 2, or 3) for targeted skill extraction.
- **Skill Tome Enchantment Glint**: All Skill Tomes now feature a visual enchantment shimmer for better item recognition.
- **Multi-Skill Slot System**: Equipment can now hold up to 3 different imbued skills simultaneously using Sigils of Imbuement.
- **Sigil of Imbuement**: New item that opens skill slots on equipment (max 3 per item).
- **Configurable Costs**: Added `slot_opening_cost` and `cleansing_cost` fields for datapack configuration.
- **New Sample Skills**: Added `imbued_haste`, `imbued_protection`, `imbued_toughness`, and `imbued_swiftness` for testing.

### Fixed
- **Refund Tome State**: Extracted skills now correctly preserve their original `loot_mode` (e.g., `imbue_only`).
- **Double Consumption**: Fixed a bug where Tome of Cleansing consumed two items instead of one.
- **Attribute Bonuses**: Multi-skill equipment now correctly applies bonuses from all imbued skills.
- **UI Bug**: Removed misplaced level indicators from the Skills Screen that were floating in the wrong position.

---

## [2026-01-30] - Feature Completion & Visual Polish

### Added
- **Separate Experience Costs**: Introduced specific configuration fields for different anvil actions.
  - `enchantment_levels` (or `enchantment_cost`): Controls the cost for combining two tomes.
  - `imbuement_levels` (or `imbuement_cost`): Controls the cost for imbuing a tome onto a weapon/piece of gear.
  - Supports `scalar`, `array`, and `expression` types for both.
- **Configured Sample Skills**: Updated `definitions.json` with differentiated costs (imbuement set to 50% of combination cost).
- **Global Visual State Engine**: Completely refactored the skills screen highlighting logic to be robust and data-driven.
  - Skills at max level (Base + Gear) now consistently show a Green/Gold border.
  - Loot-only skills (`imbue_only`, `tome_only`) no longer show as Yellow "Affordable".
- **Advanced Networking**: Enhanced `SyncSkillDescriptionsPacket` to sync full `loot_mode` metadata to the client, ensuring UI consistency.
- **Enhanced Prerequisite Validation**: Improved the skill screen's ability to check prerequisites across levels.

### Fixed
- **Skill Tome Learning Failure**: Resolved a critical bug where skills with certain namespace formats (e.g., `Lootable Berserker`) could not be learned from tomes.
- **Visual Desync**: Fixed an issue where max-level skills with gear bonuses were incorrectly flagged as "Affordable" (yellow border).
- **Anvil Stack Consumption**: Fixed a bug where imbuing a tome from a stack would consume the entire stack instead of just one item.
- **Metadata Leaks**: Fixed issues in `ClientDescriptionStorage` where removing or clearing data left stale loot mode entries.

---

## [2026-01-22] - Tome & Imbuing Core Implementation

### Added
- **Skill Tomes**: New item type that allows learning or imbuing specific skills.
- **Anvil Imbuing**: Support for imbuing skills onto legitimate equipment (Swords, Bows, Armor).
- **Anvil Combining**: Support for combining two tomes of the same level to create a higher-level tome.
- **Dynamic Config Storage**: Centralized `LeveledConfigStorage` for accessing addon-specific skill metadata.

### Fixed
- **Base Level Reward Logic**: Fixed a bug in `PerLevelRewardsReward` where rewards were not correctly calculated for the base level 0.

---

## [2026-01-17] - Foundation & Multi-Level Support

### Added
- **Multi-Level Skill Definitions**: Support for `puffish_skill_leveling:default` and `puffish_skill_leveling:stackable` types.
- **Point-Based Leveling**: Skills can now require multiple points per level through `points_per_level`.
- **Merged Descriptions**: Option to merge descriptions across levels for cleaner UI.
- **Dynamic Tooltips**: Client-side tooltips now accurately reflect the next level's bonuses.
