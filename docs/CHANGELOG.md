# Changelog

All notable changes to the **Pufferfish Skill Leveling** mod will be documented in this file.
 
## [2026-02-01] - Skill Master & Registry Overhaul

### Added
- **Skill Master reset command**: Added `/skillleveling villager reset` to reset a villager's tier and experience for testing.
- **Dedicated Creative Tabs Classes**: Refactored creative tab registration into `ForgeCreativeTabs` and `FabricCreativeTabs` for better platform isolation.
- **Blank Tome Texture Fix**: Added the missing `blank_tome.json` item model, resolving the broken texture issue.
- **Tome Upgrade System**: Emeralds + Tome Level N -> Tome Level N+1 (available at Tier 3+ villagers).
- **Mastery System**: Villager pricing and offers now scale with the player's total number of mastered skills.
- **Fallback Trading**: Villagers now offer a diverse pool of starter tomes if no advanced progression trades are available.

### Changed
- **Unified Villager Logic**: Re-implemented the Skill Master trade selection to prioritize unlearned skills and intelligent tier-based scaling.
- **Standardized Registration**: All platform-specific registration calls (items, villagers, tabs) are now unified in a cleaner format in `ForgeMain` and `FabricMain`.
- **Command Overhaul**: Standardized command naming and added better user feedback via colored text.

### Fixed
- **Creative Tab Bug (Forge)**: Resolved an issue where the Puffish Skill Leveling tabs were missing on the Forge platform.
- **Fabric Namespace Leak**: Fixed accidental inclusion of Forge-specific imports in the Fabric module.
- **Registry Cleanup**: Removed redundant registration and unused imports across the entire project.

---

## [2026-01-30] - Multi-Skill Cleansing & Visual Polish

### Added
- **Tiered Tome of Cleansing (I, II, III)**: Targeted extraction of equipment slots.
- **Skill Tome Enchantment Glint**: Visual shimmer for all specific skill tomes.
- **Multi-Skill Slot System**: Up to 3 unique skill slots per equipment item.
- **Sigil of Imbuement**: Consumable item to unlock additional skill slots.

### Fixed
- **Anvil Stack Consumption**: Fixed a bug where imbuing a tome consumed the entire stack.
- **UI Highlighting**: Corrected the "Affordable" (yellow) border logic to respect loot-only skills and max levels.
- **Loot Table Integration**: Verified correctly functioning mob drops and structural loot for all new items.

---

## [2026-01-22] - Tome & Imbuing Core Implementation
... (previous entries preserved)
