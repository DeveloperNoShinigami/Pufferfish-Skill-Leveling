# Changelog

All notable changes to the **Pufferfish Skill Leveling** mod will be documented in this file.
 
- **Curio Integration Finalization**: Fully integrated Skill Charms with the Curios API, allowing for dedicated charm slots and instant bonus activation. Fixed `CuriosScanner` API usage to correctly iterate and detect charms.
- **Reward Trigger System Restoration**: Reverted the reward trigger system to the stable `stateChanged` logic. Removed the flawed `persistentActivatedLevels` persistence layer in `DataManager` that was causing "backwards" reward behavior (only firing on join). Rewards now fire immediately upon leveling up in a live session.
- **Recursive Refund Fix**: Finalized the Tome of Greater Clear Mind logic to correctly refund all levels down to 0 while maintaining prerequisite integrity.

## [2026-02-06] - Reward Logic Refinement & UI Polish

- **Reward Persistence Stability**: Finalized a robust, session-local initialization guard in `PerLevelRewardsReward`. This completely eliminates join-time re-triggering of commands/chat messages without needing complex global flags.
- **Multi-Level Reward Triggering**: Fixed a critical bug where rewards for Level 2+ were not firing. Implemented manual trigger support in `CategoryDataMixin`.
- **GUI Icon Cleanup**: Removed the "X/Y" level indicator text from skill icons to restore a clean, vanilla-like aesthetic as requested by the user.
- **Hidden Skill Interaction**: Properly blocked mouse clicks on hidden and locked skills in the Skills screen, ensuring they are non-interactive until revealed.
- **0->1 Transition Correctness**: Fixed a bug where Level 1 rewards were sometimes skipped during purchase. Level 1 now triggers exactly once upon unlock or first-time join.

## [2026-02-04] - Visual Discovery & System Consolidation

### Added
- **Hidden Skills Feature**: Skills can now be set as `"hidden": true`. They remain completely invisible (icons, connections, and tooltips) in the UI until their prerequisites are met.
- **Enhanced Prerequisite System**: Added `required_skill` support for cross-category skill requirements and specialized loot-mode bypasses.
- **Datapack-driven Villager Trades**: Replaced hardcoded villager trades with a flexible JSON system (`puffish_skill_leveling/trades/`).
- **Skill Master Reputation Config**: Added `puffish_skill_leveling/reputation.json` to configure trading prices, experience gains, and upgrade chances.
- **Village House Integration**: Added custom Jigsaw-based houses for Skill Master villagers in Plains, Desert, Savanna, Snowy, and Taiga villages.
- **Skill Master Barrels**: New structural loot containers found in Skill Master houses with tiered progression rewards.

### Changed
- **Logging Policy**: Silenced excessive runtime logs. Info logs are now reserved for initialization/registration, while gameplay events use Debug level (disabled by default).
- **Attribute Reward Stability**: Implemented deterministic UUID injection for `AttributeRewards` to ensure modifiers persist correctly through config reloads and server restarts.

### Fixed
- **Tome Progression XP**: Fixed a critical bug where Tomes of Progression would fail to detect current Pufferfish XP, correctly granting the exact amount needed for next level.
- **Hidden Skill Persistence**: Fixed a bug in `ClientSkillLevelStorage` where the hidden flag was lost when a skill's level was reset to 0.
- **Client Prerequisite Caching**: Improved efficiency of client-side prerequisite checks in the UI screen mixins.

---

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
