# Changelog

All notable changes to Pufferfish Skill Leveling are documented in this file. Dates are in YYYY-MM-DD format.

## [2026-03-13] — XP Sync & Data Cleanup Fixes

### Fixed
- **XP Bar Synchronisation**: Completely eliminated the "100 XP" fallback bug. The bridge now utilizes the authoritative Pufferfish API `Experience.getRequired(level)` for both initial login sync and real-time XP changes (kills, etc.). This ensures the Epic Classes XP bar always matches the Pufferfish HUD.
- **Gear Requirement Inheritance**: Resolved an issue where child classes (e.g., advanced classes) failed to inherit gear/item restrictions from their parent classes.
- **Client HUD Resilience**: Implemented a high fallback value for `xpNeeded` on the client during initialization to prevent the XP bar from visually "overflowing" before server data arrives.

### Added
- **Data Cleanup Command**: Added `/skillleveling cleanup <player>` to safely remove all addon-specific NBT data and reset Epic Class state. This ensures safe mod removal without risking player or world corruption.

---

## [2026-03-12] — Skill Master Trade Refinement

### Fixed
- **Tome-Only Shop Logic**: Removed all hardcoded Blank Tome fillers from the Skill Master's trade pool. The shop now dynamically populates all slots with meaningful Experience and Skill Tomes.
- **Robust Filler Fallback**: Implemented a "Tome-only" filling sequence that ensures no empty or "fake" slots (e.g., Blank Tomes) appear, even at Novice tier or when player skills are maxed.
- **Dynamic Trade Distribution**: Fixed the probability distribution for random tome slots (15% 2x Exp, 10% 2x Skill, 75% Mix).
- **Infinite Tome Cycling**: Updated Experience Tome generation to cycle through available types if requested slots exceed unique tome types, ensuring constant variety.

---

## [2026-03-11] — XP Sync Refinement & Experience Tomes

### Added
- **Experience Tomes System**: Introduced configurable items that grant Pufferfish XP and synchronize with Epic Classes. Includes bulk consumption and chat-based selection UI.
- **Experience Tome DataPack Schema**: New JSON schema for defining custom tomes under `tome_config/`.
- **Shop Expansion**: Skill Master now offers rare Experience Tomes in the dynamic trade pool, scaling with player progression and villager tier.
- **Advanced World Gating**: Expanded restrictions to include Blocks, Entities, Dimensions, and Structure proximity.
- **Environmental Gating**: Added `in_water` and `time_of_day` conditions to all restriction types.

### Fixed
- **SYNC_DEPTH Mechanism**: Replaced boolean recursion guards with a depth counter in `PufferfishExperienceMixin`. This accurately captures the XP delta across nested method calls (e.g., `addExperience` calling `setExperience`) and ensures exactly one sync packet is sent per transaction.
- **HUD Notification Restoration**: Reverted aggressive suppression that was disabling EXP toasts and Level Up animations. Both are now correctly rendered as the authoritative UI for Pufferfish-originated XP gains.
- **Silent Login Sync**: Refined `ClientLevelStateMixin` to exclusively suppress Level Up animations during initial world connection, preventing noisy login notifications while maintaining normal gameplay feedback.
- **XP Delta Correction**: Added missing hooks to `setExperienceInternal` to ensure accurate before/after total comparisons for sync packets.

### UI & Stability
- **Class Selection Safety**: Added bounds checks to `CustomClassSelectScreen` to prevent `IndexOutOfBoundsException` when no valid classes are available for selection.
- **Improved Environmental Checks**: Refined `in_water` restriction to use `isTouchingWater()` for better compatibility with standing/surface water detection.
- **Resource Path Reliability**: Fixed a pathing bug in `ItemRequirementsManager` ensuring restrictions load correctly from datapacks regardless of folder nesting.

---

## [2026-03-10] — Category Locking & Namespace Resilience Fixes

### Fixed
- **Category Locking Logic**: Prevented parent classes from locking their own categories when child classes exist sharing the same category ID. The bridge now correctly checks if a shared category belongs to the "safe" class list before locking it.
- **Namespace-Resilient Sync**: Fixed a critical bug where class progress was reset to 0 on login. The `ForgePlatform` level retrieval now implements a fallback mechanism that checks alternative namespaces (e.g., `epic_classes` if `epic_class` fails) when attempting to sync Pufferfish progress.
- **Hierarchy Awareness**: Restored parent category unlocking in `onClassChanged`, ensuring progression trees stay accessible.
- **Advancement Gating**: Fixed `/skillleveling advanceclass` to correctly gate progress based on obtaining all class skills for mastery, rather than simple point thresholds.

---

## [2026-03-06] — Universal Proxy Passive Blocking & Category Enforcement

### Added
- **Universal Proxy Passive Suppression:** New `ClassTypeUtilMixin` intercepts `ClassTypeUtil.getType()` to return `null` for custom classes with `epic_class_proxy` set. This blocks ALL proxy passives (mana regen, damage reduction, debuff shortening, dashes) across all 6 base classes with a single mixin — no per-class mixins needed.
- **Locked Category XP Rejection:** `SkillsModExperienceMixin` now guards `addExperience` and `setExperience` at `HEAD`, cancelling any XP gain for locked Pufferfish categories.
- **State-Based Quest Suppression:** `ClientPacketHandlersMixin` now checks `ClientClassState.selectedType` to reliably block the initial quest dialog when a player already has a class (e.g., during advancement).

### Fixed
- **Proxy passives leaking to custom classes:** Custom classes (e.g., Necromancer proxying SORCERER) no longer receive the proxy's event-based passive effects on the server. Previously, `PassiveUnlocksMixin` only intercepted slot-based abilities — the actual event handlers in `SorcererPassives` bypassed it entirely.

### Removed
- `SorcererPassivesMixin.java` — replaced by universal `ClassTypeUtilMixin`.
- `shouldBlockProxyPassive()` and `hasCustomPassives()` from `EpicClassBridge` — redundant with upstream blocking.

---
## [2026-03-05] — Advanced Class Sync & Quest Fixes

### Added
- **Login Category Sync:** `syncOnPlayerLogin` repopulates the player's active Pufferfish category from NBT on login, fixing level syncing after server restarts.
- **Dynamic Advancement Titles:** Class advancement screen now shows "Choose your 2nd class", "Choose your 3rd class", etc., dynamically based on class tree depth.
- **Quest Dialog Suppression:** `ClientPacketHandlersMixin` intercepts and blocks the initial quest dialog from re-triggering during class advancement (session-scoped flag).

### Fixed
- **Level syncing to wrong category:** `getCategoryForClass` now checks `RESOLVED_MAPPINGS` first instead of blindly returning the first array element. This fixes advanced classes (e.g., Lich) incorrectly syncing to their parent's level (Necromancer).
- **Advancement gate bypass:** Added null guard in `/skillleveling advanceclass` so the max-level check blocks (instead of silently passing) when a class definition is not found.
- **Mixin annotation mismatch:** `ClientPacketHandlersMixin` changed from direct class reference (`@Mixin(ClientPacketHandlers.class)`) to string-based target (`@Mixin(targets = "...")`) matching the project's existing pattern, fixing silent mixin application failure.

---

## [2026-03-03] — Epic Classes Bridge & DataPack Restructure

### Added
- **Native DataPack Support:** Epic Class definitions, Attributes, and Job Masters can now be loaded dynamically via datapacks (`data/puffish_skills_leveling/rise_of_heros/`).
- **NBT Parsing Support:** Added `PuffishItemHelper` to correctly parse and render item NBT (e.g., custom attributes, tacz attachments) in class selection preview menus.
- **Client Synchronization:** Implemented S2C packet (`SyncBridgeContentPacket`) to ensure custom DataPack configurations render correctly on the client side UI.
- **Multi-File Category Standard:** Upgraded all 10 base class skill trees (Warrior, Archer, Berserker, Paladin, Sorcerer, Necromancer, Reaper, Shadowmancer, Gunslinger, Executioner) to a structured multi-file format (category.json, definitions.json, skills.json, connections.json, experience.json).
- **MMORPG Attribute Scaling:** Introduced `epic_class_attributes.json` mapping, allowing fine-tuned progression scaling aligned with the expected MMORPG stat curve.

### Fixed
- **Forge Build Compilation:** Resolved missing import errors in `CustomClassSelectScreen` and `PuffishItemHelper`.
- **Class Start Items:** Starting items logic now natively mirrors Epic Fight/Epic Classes standard granting behavior, correctly applying NBT metadata to granted items.

---

## [2026-02-16] — Effect Amplifier & Reload Fixes

### Fixed
- **Effect amplifier not updating after datapack reload:** Fixed a race condition where changing an effect's `amplifier` value in a skill's JSON definition and running `/reload` would not update the applied effect. The issue was caused by the protected effect registry being updated AFTER effect removal, causing the mixin to re-apply the old amplifier. Now updates the protected effect map BEFORE removing the old effect.
- **Protected effects not clearing on reload:** Fixed `onServerReload()` to properly clear `toggleRewards` and `protectedEffects` maps, preventing stale reward instances from persisting after datapack changes.

---

## [2026-02-15] — Toggle System Bug Fixes

### Fixed
- **Toggle prerequisite false positive:** Pure toggles (maxLevel = 0) and basic toggles (maxLevel = 1) were incorrectly blocked by the prerequisite check with "Toggle Prerequisites not met" message. Changed level check from `== 1` to `<= 1`.
- **Toggle skills showing mastery state:** Toggle skills with `max_skill_level: 1` (e.g., night_vision) would display gold/mastery state (UNLOCKED) instead of staying toggleable (AVAILABLE). Added toggle exclusion to the Priority 2 mastery check in both server and client skill state logic.
- **Cooldown UI missing for pure toggles:** Pure toggles (maxLevel = 0) didn't display cooldown timers or status text. Fixed by broadening the basic toggle identification check from `maxLevel == 1` to `maxLevel <= 1` and adding cooldown display logic inside the basic toggle branch.

---

## [2026-02-14] — Category Gating & Client Safety

### Added
- **Category Gating System:** Categories can now define `prerequisite_skills` in `category.json` to lock entire categories behind skill requirements. Supports `keep_unlocked` flag to permanently unlock once met.
- **SideSafeClient rewrite:** Complete rewrite using inner-class pattern to safely handle client-only Minecraft API calls on dedicated servers. Prevents `NoClassDefFoundError` crashes.

### Fixed
- **Fabric CloseSkillScreenPacket:** Implemented the stub that was missing, ensuring the skill screen closes properly on Fabric dedicated servers after toggle prerequisite failures.
- **Skill screen not closing on dedicated server:** Fixed by routing screen-close commands through the new SideSafeClient architecture.

---

## [2026-02-13] — Equipment Imbuing & Loot Overhaul

### Added
- **Dynamic Skill Imbuement System:** Equipment found as loot can now spawn with pre-imbued skills. Configurable via `skill_imbue_loot/config.json`.
- **Dimension Overrides:** Different dimensions can have different imbue chances, level ranges, and skill caps.
- **Distance Scaling:** Skill levels on imbued loot scale based on distance from spawn origin.
- **Category Settings:** Per-equipment-type overrides for imbue chance, level range, and max skills.
- **Exclusion Groups:** Prevent conflicting skills from appearing on the same equipment.
- **Item Blacklist/Whitelist:** Control which items can receive imbued skills.
- **Loot Table Whitelist:** Restrict imbuing to specific loot table sources.

---

## [2026-02-12] — Universal Loot System

### Added
- **Universal Loot Injection:** New system to inject Skill Tomes, Skill Charms, and other items into any chest or mob drop loot table.
- **Chest Injection Groups:** Target specific chest loot tables (villages, dungeons, temples, etc.).
- **Entity Drop Groups:** Target specific mob loot tables with tiered drop rates.
- **Weighted Entry System:** Entries chosen by weighted random selection with per-entry drop chance.
- **Skill Tome Entries:** Generate random Skill Tomes with configurable level ranges and optional skill targeting.

---

## [2026-02-10] — Skill Master & Village Integration

### Added
- **Skill Master Villager:** Custom profession with 5-tier trade progression.
- **Skill Scribe Table:** Custom workstation block for the Skill Master.
- **Mastery Pricing:** Trade prices decrease as the player masters more skills.
- **Village Structures:** Jigsaw-based Skill Master Houses spawn in Plains, Desert, Savanna, Snowy, and Taiga villages.
- **Tiered Loot Barrels:** Village structures contain progression materials.

---

## [2026-02-08] — Toggle System & Keybinds

### Added
- **Toggle Skills:** Skills with `"toggle": true` become active on/off abilities.
- **ToggleReward Type:** New `puffish_skill_leveling:toggle` reward wraps other rewards in an enable/disable container.
- **Mastery Keybinds:** Assign toggle skills to hotkeys (slots 1–9) via `keybind_slot`.
- **Cooldowns:** `cooldown` field prevents toggle spam with configurable tick-based delays.
- **Protected Effects:** `puffish_skill_leveling:effect` with `is_protected: true` survives milk and death.
- **Auto-Disable on Death:** Active toggles are automatically disabled on player death.
- **Hybrid Toggle Patterns:** Toggle + per_level_rewards combinations for multi-level toggleable skills.

### Fixed
- **Toggle state persistence:** Toggle states now survive server restarts and player disconnects.

---

## [2026-02-06] — Anvil & Equipment System

### Added
- **Sigil of Imbuement:** Opens skill slots on equipment (up to 3 per item).
- **Skill Tome Application:** Combine Skill Tome + slotted equipment to apply skills.
- **Skill Upgrading:** Combine equipment with matching Skill Tome to upgrade imbued skill level.
- **Tome of Cleansing (I/II/III):** Extract imbued skills from equipment slots as Skill Tomes.
- **Tome Ranking:** Combine two identical-level Skill Tomes to create a higher-level version.
- **XP Cost System:** `enchantment_cost`, `imbuement_cost`, `slot_opening_cost`, `cleansing_cost` fields with scalar, array, and expression support.
- **Paid vs Granted Level Tracking:** Distinguishes between point-spent and tome-granted levels for fair refund behavior.

---

## [2026-02-04] — Progression Items

### Added
- **Blank Tome:** Base crafting material for specialized tomes.
- **Skill Tome:** Grants +1 level to a specific skill on right-click or anvil use.
- **Tome of Progression:** Right-click GUI to choose any skill to level up.
- **Tome of Clear Mind:** Right-click to select and refund 1 level from any skill.
- **Tome of Greater Clear Mind:** Right-click to fully reset any skill to Level 0.
- **Skill Charm:** Curios-compatible accessory that can hold imbued skills.

---

## [2026-02-01] — Prerequisite & Gating Systems

### Added
- **Prerequisite Skills:** `prerequisite_skills` array on skill definitions blocks access until requirements are met.
- **Cross-Category Prerequisites:** Reference skills from other categories with the `category` field.
- **Required Skill for Level:** `required_skill_for_level` creates tier gates at specific level thresholds.
- **Hidden Skills:** `hidden: true` makes skills invisible until prerequisites are met.

---

## [2026-01-29] — Description System

### Added
- **Level Descriptions:** `descriptions` field shows current-rank tooltip text.
- **Extra Descriptions:** `extra_descriptions` field shows next-rank preview on Shift-hold.
- **Merge Description:** `merge_description: true` stacks all previous level descriptions.
- **Dynamic Tooltip Injection:** Mixin into Pufferfish Skills' tooltip rendering for level info, toggle status, and cooldown display.

---

## [2026-01-26] — Multi-Level Foundation

### Added
- **Per-Level Rewards:** `puffish_skill_leveling:per_level_rewards` reward type maps specific rewards to each level.
- **Multi-Level Progression:** `max_skill_level` supports 1–999 levels per skill.
- **Level Tracking:** Server-side level storage with NBT persistence.
- **Refund System:** Full point refund for paid levels, 0 for granted levels.
- **Admin Commands:** `/skillleveling set`, `/skillleveling get`, `/skillleveling refund`, `/skillleveling info`.

---

## [2026-01-22] — Initial Release

### Added
- **Addon Framework:** Mixin-based addon for Pufferfish Skills v0.17.1.
- **Architectury Multi-Loader:** Forge and Fabric support from a shared Common module.
- **Loot Mode System:** `loot_mode` field (`"both"`, `"tome_only"`, `"imbue_only"`) controls skill acquisition paths.
- **Client/Server Sync:** Skill levels synced to client for UI rendering.
