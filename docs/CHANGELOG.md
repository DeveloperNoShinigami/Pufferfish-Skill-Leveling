# Changelog

A record of all notable changes and improvements to the Pufferfish Skill Leveling mod.

## [2026-02-14] - Command Standards & Hybrid Skill Reliability

### Command Syntax Overhaul
- **Minecraft 1.20.1 Standards**: Audited and updated all template datapacks to use standard Minecraft command syntax.
    - **Selectors**: Replaced custom placeholders (`%player%`, `${player}`) with the standard `@s` selector.
    - **Relative Coordinates**: Replaced custom location placeholders (`%player_x/y/z%`) with standard `~ ~ ~` coordinates.
    - **JSON Text Components**: Migrated all `title`, `tellraw`, and `actionbar` commands to proper JSON text formatting (e.g., `{"text":"Hello","color":"red"}`).
- **Documentation Alignment**: Updated `DATAPACK_GUIDE.md` and `FEATURES.md` to reflect these standards.

### Hybrid Skill Reliability
- **Nested Reward Fix**: Resolved a logic conflict where leveling up a disabled hybrid skill would "burn" its reward activation state. Commands and effects now correctly wait until the skill is toggled **ON** to fire for the first time.
- **Login Synchronization**: Ensured that nested rewards correctly respect the toggle state on player join, preventing activation spam or missed triggers on login.
- **Level Triggering**: Improved `triggerLevelRewards` and `deactivateLevelRewards` to be toggle-aware for nested reward instances.

### Toggle System Enhancements
- **Level 0 Toggle Support**: Basic toggle skills (those with `max_skill_level: 1`) can now be toggled immediately at Level 0.
- **Tooltip Refinement**: Added "READY" status to tooltips for Level 0 toggle skills, providing clearer feedback for learners.
- **Recursive Extraction**: Fixed a bug where hybrid skills wouldn't level past 1 by implementing recursive extraction of `maxLevel` and `pointsPerLevel` from nested rewards.

### Documentation Updates
- **Datapack Guide v3**: Comprehensive updates to command examples and technical field descriptions.
- **Feature Reference**: Updated to include new toggle mechanics and command standards.
 
- **Configuration Standardization**: Removed all remaining legacy aliases and fields (`required_skill`, `enchantment_levels`, `imbuement_levels`, `max_levels`) to strictly standardize the configuration schema.
    - **Logic Cleanup**: Removed redundant parsing and fallback paths in Mixins and Reward classes.
    - **Datapack Update**: Standardized all example definitions in the `template_pack` and `Datapack_Example`.
    - **Documentation**: Updated `FEATURES.md` and `DATAPACK_GUIDE.md` to reflect the modern standard exclusively.
 
- **Universal Loot & Imbuement Systems**: Implemented a comprehensive, cross-platform loot injection system for both chests and entities.
    - **Loot Injection**: Added `UniversalLootHandler` to handle persistent injection for mob drops (`LivingDropsEvent`) and Global Loot Modifiers for chests.
    - **Skill Imbuement**: Added dynamic skill imbuing for equipment found in loot, supporting dimension overrides, distance scaling, and exclusion groups.
    - **Vanilla Gear Support**: Ensured that vanilla items (Bows, Armor) dropped by mobs are correctly identified and imbued using any applicable configuration rules.
- **Documentation Overhaul**: Created extensive guides for the new loot systems:
    - `Universal_Loot_System.md`: Configuration schema and injection logic.
    - `Skill_Imbuement_System.md`: Filtering, scaling, and category-specific imbuing rules.
    - Updated `DATAPACK_GUIDE.md`, `FEATURES.md`, and `GETTING_STARTED.md` with integrated links and paths.

- **Curio Integration Finalization**: Fully integrated Skill Charms with the Curios API, allowing for dedicated charm slots and instant bonus activation. Fixed `CuriosScanner` API usage to correctly iterate and detect charms.
- **Reward Trigger System Restoration**: Reverted the reward trigger system to the stable `stateChanged` logic. Removed the flawed `persistentActivatedLevels` persistence layer in `DataManager` that was causing "backwards" reward behavior (only firing on join). Rewards now fire immediately upon leveling up in a live session.
- **Level 1 Reward Fix**: Resolved a critical regression where Level 1 rewards were skipped. Implemented manual reward triggering for the 0->1 transition in `CategoryDataMixin` to bypass Pufferfish's premature firing order.
- **Recursive Refund Fix**: Finalized the Tome of Greater Clear Mind logic to correctly refund all levels down to 0 while maintaining prerequisite integrity.

- **Trigger Skill Stability Finalized**: Resolved a series of critical regressions with Trigger (Toggle) skills. 
    - Fixed "First Click Delay" by unifying `DataManager` caches into an atomic `PlayerCache` object.
    - Resolved "Double Execution" of commands by conditionally silencing `enable_rewards` during disable transitions.
    - Fixed "Persistent Effects/Attributes" by ensuring zero-count updates on all state transitions.
    - Fully restored "World Join Persistence" while maintaining silent login (no activation spam on join).
- **Linter Cleanup**: Resolved all unused imports and defunct methods in `SkillLevelingDataManager` and `ToggleReward`.

## [2026-02-12] - Event-Based Refined Effect Rewards

### Performance & Efficiency
- **Event-Driven Protection**: Migrated the `is_protected` logic from a per-second tick check to a highly efficient event-based system.
    - **Common Mixins**: Hooked into `LivingEntity.onStatusEffectRemoved` and `clearStatusEffects` to trigger instant re-application.
    - **Smart Logic**: Maintained "Smart Overwrite" behavior that respects stronger active potion effects (e.g., waiting for Haste II potion to expire before returning Haste I skill reward).
- **Zero-Tick Overhead**: Removed background ticking checks for protected effects, reducing server load.

### New Features
- **Refined Effect Rewards (`puffish_skills:effect`)**: Significant enhancements to the base effect reward type.
    - **`persistent`**: Boolean flag to make effects immune to milk and curative items (supported natively on Forge).
    - **`is_protected`**: Boolean flag to ensure effects are re-applied if cleared by commands, mods, or death.
- **Cross-Platform Respawn Support**: Added specialized hooks for Forge (`PlayerRespawnEvent`) and Fabric (`AFTER_RESPAWN`) to ensure skill rewards are restored immediately upon death.
- **Platform Abstraction Layer**: Implemented a `Platform` interface and loader-specific implementations (`ForgePlatform`, `FabricPlatform`) to handle technical differences safely.

### Bug Fixes
- **Sticky Effect Removal**: Resolved a race condition where toggling off a skill would trigger its own "protection" and re-apply the effect. Corrected the order of operations to unregister protected effects *before* removal.
- **Night Vision Test Case**: Applied and verified the new fields in the `night_vision` skill template.

## [2026-02-11] - Toggle Skill Refinement & Stability

### New Features
- **Effect Reward (`puffish_skills:effect`)**: Added a native Potion Effect reward type. Supports `amplifier`, `duration`, `ambient`, `show_particles`, and `show_icon`. Ideal for Toggle Skills (use `duration: -1` for infinite effects).

### Toggle Skills
- **Functional Restoration**: Fixed a critical issue where `ToggleReward` instances were not being registered or updated, causing toggle skills to have no effect.
- **Join Event Stability**: Implemented pre-seeding of toggle states on player join. This prevents toggle commands (e.g., chat messages, potion effects) from incorrectly re-triggering every time a player logs in.
- **Command Spams Resolved**: Fixed a logic error in `refreshAllRewards()` that caused toggle skill commands to re-trigger on every sync or equipment change. Toggle rewards now only fire on genuine state changes.
- **Visual State Polish**: Implemented specific visual states for disabled toggle skills based on their `loot_mode`:
    - **Imbue Only**: "DISABLED (Equip item to use)"
    - **Tome Only**: "DISABLED (Read tome to learn)"
    - **Both**: "DISABLED (Equip or Learn to use)"
- **Sync Optimization**: Added `loot_mode` to the `SyncSkillLevelPacket` to ensure client-side tooltips always have the correct context for displaying disabled states.

### General Stability
- **Client Crash Fix**: Resolved a critical `InvalidInjectionException` in `SkillsScreenMixin` by enabling remapping for the `mouseClicked` injection. This crash occurred in non-dev environments where method names are obfuscated.
- **Log Cleanup**: Removed excessive debug logging from `PerLevelRewardsReward`, `ClientSkillLevelStorage`, and `SkillLevelingManager` to reduce console spam.
- **Toggle Skill Default State**: Fixed an issue where toggle skills would automatically activate upon unlocking. Added lazy configuration loading to ensure state checks are always performed, even during the initial learning transaction.
- **Trigger Skill Reload Fix**: Resolved an issue where learned trigger/toggle skills would auto-fire their rewards upon joining the world due to execution order. Moved reward state pre-seeding to occur *before* the initial skill synchronization to prevent race conditions during player join.
- **PerLevelReward State Tracking**: Improved internal state tracking in `PerLevelRewardsReward` to prevent reward re-execution during non-state-changing updates.
- **Null Safety**: Added robust null checks in `SkillLevelingManager` when iterating reward maps to prevent potential crashes.
- **Packet Safety**: Resolved a duplicate field definition in `SyncSkillLevelPacket` that could cause packet decoding errors on some clients.

## [2026-02-10] - Tome Imbuing & Admin Tools

- **Simplified Tome Imbuing (Pathing Checks)**: Implemented flexible ID matching using "pathing checks" (Identifier paths). This ensures that "normally generated" tomes with short IDs (e.g., `vitality`) correctly match namespaced tree skills and gear (e.g., `template:vitality`).
- **Improved Anvil Logic**: Updated the Anvil screen handler to use fuzzy matching for skill upgrades and to consistently resolve short IDs to canonical (namespaced) registry IDs when imbuing new skills onto gear. 
- **New Command: `/skillleveling give tome`**: Added a comprehensive give command with full auto-suggestions for players, categories, skills, loot modes, and levels.
- **ImbuedSkillHelper Refinement**: Added fuzzy matching helpers to `ImbuedSkillHelper` to unify ID handling across Gear and Curios.

## [2026-02-09] - UI Sync & Documentation Restoration

- **UI Sync & Prerequisite Fix**: Resolved a critical issue where using a Skill Tome didn't immediately update the UI state or reveal hidden successor skills until a manual screen refresh.
- **Point A-Z Documentation Flow**: Reorganized the entire repository documentation into a professional, sequential journey (**Getting Started** -> **Features** -> **Datapack Guide** -> **Roadmap**).
- **Exhaustive Datapack Guide (v2)**: Performed a total restoration and expansion of `DATAPACK_GUIDE.md`, moving from foundational structure to "Grandmaster" authorship.
    - **Technical Mechanics**: Restored exhaustive field references for all cost types (`cleansing_cost`, `slot_opening_cost`, etc).
    - **Tome Combining**: Documented the Anvil-based Rank Up logic (Rank X + Rank X = Rank X+1).
    - **Point Bypass System**: Defined the technical "Paid" vs "Granted" level tracking logic.
    - **Specialized Progression Items**: Comprehensive documentation for Blank Tomes, Refund/Reset artifacts, and Skill Charms.
- **Skill Master House & Reputation**: Corrected terminology to "Skill Master House" and added deep-dives into his professional reputation system, tier-gated (T4/T5) special trades, and price scaling.
- **Repository Cleanup**:
    - **Updated `.gitignore`**: Successfully untracked local-only files (`AGENTS.md`, `video_guide/`) while keeping them in the filesystem.
    - **Obsolete Removal**: Deleted `WIKI_TUTORIAL.md`, `STANDARDIZED_DATAPACK_REFERENCE.md`, and `EXAMPLE_SKILLS_README.md` to streamline the repository.
- **Visual & Technical Polish**:
    - **JSON Formatting**: Beautified all guide examples with proper multi-line, indented JSON structures for maximum clarity.
    - **Point Consumption Fix**: Finalized the paid-level tracker bitset integration for Skill Tomes.
- **Roadmap Update**: Added the "Comprehensive Developer Guide" project for future high-level technical/Java API documentation.

## [2026-02-07] - Modpack Integration & Cleanups

- **Fixed**: `InvalidMixinException` in `ServerChatMixin` (Remapped Issue).
- **Fixed**: `InvalidInjectionException` in `SkillsScreenMixin` (Crash Issue).
- **Fixed**: Build errors by correcting `remap` settings for internal mixins (`PlayerDataMixin`, `CategoryDataMixin`).
- **Fixed**: Level 1 reward triggering using manual unlock logic (Fixed "backwards" behavior).
- **Fixed**: State stability in `PerLevelRewardsReward`.
- **Removed**: "Targeted Tome Selection Mode" UI (temporarily) to resolve mixin crashes.

## [2026-02-06] - Reward Logic Refinement & UI Polish

- **Reward Persistence Stability**: Finalized a robust, session-local initialization guard in `PerLevelRewardsReward`. This completely eliminates join-time re-triggering of commands/chat messages without needing complex global flags.
- **Multi-Level Reward Triggering**: Fixed a critical bug where rewards for Level 2+ were not firing. Implemented manual trigger support in `CategoryDataMixin`.
- **GUI Icon Cleanup**: Removed the "X/Y" level indicator text from skill icons to restore a clean, vanilla-like aesthetic as requested by the user.
- **Hidden Skill Interaction**: Properly blocked mouse clicks on hidden and locked skills in the Skills screen, ensuring they are non-interactive until revealed.
- **0->1 Transition Correctness**: Fixed a bug where Level 1 rewards were sometimes skipped during purchase. Level 1 now triggers exactly once upon unlock or first-time join.

## [2026-02-04] - Visual Discovery & System Consolidation

### Added
- **Hidden Skills Feature**: Skills can now be set as `"hidden": true`. They remain completely invisible (icons, connections, and tooltips) in the UI until their prerequisites are met.
- **Enhanced Prerequisite System**: Added `prerequisite_skills` support for cross-category skill requirements and specialized loot-mode bypasses.
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
