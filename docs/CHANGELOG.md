# Changelog

All notable changes to Pufferfish Skill Leveling are documented in this file. Dates are in YYYY-MM-DD format.

## [2026-04-08] — Command Sync Regression Hotfix (RO Refresh + Reset Timing)

### Fixed
- **Per-Click Command Slot Execution Restored**: Attribute slots configured with `command` (including mixed attribute+command slots) now execute their command again on each stat allocation click via `CustomAllocateStatPacket`.
- **`roleveling refresh` Command Compatibility**: Command side-effects are executed with player command context so KubeJS commands that require `ctx.source.player` run correctly during stat allocation.
- **Reset-Time RO Refresh Ordering**: Reset cleanup commands are now deferred to run after reset writes complete, so `roleveling refresh` sees post-reset values instead of stale pre-reset allocations.

### Notes
- This keeps the reset-only command dedupe behavior while restoring real-time click-sync for command-backed stat slots.

## [2026-04-08] — Command Slot Consistency & Reset Sync

### Fixed
- **Client/Server Value Parity for Command Slots**: Class Book display math now matches server-side command execution for shorthand numeric `value` expressions (for example, `"value": "1"`). Numeric values without `points` are now treated as per-point scaling on the client, matching command execution logic on the server.
- **Reset-Time Command Cleanup**: Full stat reset now runs each affected command-backed attribute slot once with `{value}=0`, ensuring command-driven effects are properly cleared when allocated points are removed.
- **Immediate Class Book Reset Refresh**: Confirming a reset now applies an optimistic client-side allocation clear so command-only slots visually reset to 0 immediately without requiring a book reopen.

### Notes
- Reset popup visuals are intentionally unchanged (the current inventory-based diamond display remains by design).

## [2026-04-08] — Skill Tree Unlock Regression Fix

### Fixed
- **Skill Tree Completely Locked (Regression)**: Skills were appearing fully gray/unclickable in the Pufferfish skill tree even when the player had positive skill points. Root cause: three connection-validation checks added to `onCanUnlockSkill` (`CategoryDataMixin`) in the previous session were implemented using `category.connections().normal().getNeighborsFor(skill.id())`, which returns a skill's **children** (outgoing connections), not its **parents**. This caused the parent-connection check to always find 0 unlocked neighbors for every non-root skill (`0 < requiredParents = 1`), making `canUnlockSkill` return `false` for all non-root skills — showing them as `LOCKED` in the UI. The three blocks (exclusive-connections check, exclusive-root check, parent-connection check) have been removed. Pufferfish enforces these constraints natively through its own `canUnlockSkill` logic, which runs unobstructed when our mixin does not cancel early. The native cost safety gate (which guards skills that have a `definition.cost()` but no `PerLevelRewardsReward`) is retained as it remains necessary.

---

## [2026-04-07] — RO Stat Attribute Migration & KubeJS Bridge Rework

### Added
- **`roleveling:*` Real Minecraft Attributes**: STR, AGI, VIT, INT, DEX, and LUK are now registered as actual Minecraft attributes (`roleveling:str` through `roleveling:luk`) via `RoLevelingAttributes`. This means `getValue()` automatically includes all item `AttributeModifier` contributions without any manual gear scanning — the engine handles it for free.
- **`EpicClassSyncHelper.applyCustomAttributes`**: On every server sync, Java reads the player's `alloc_str/agi/vit/int/dex/luk` NBT keys and writes them as base modifiers to the corresponding `roleveling:*` attribute instances. This is the authoritative source of truth for allocated stat points.
- **ForgeEvents Trigger for `applyStats`**: Added a `ForgeEvents.onEvent('net.minecraftforge.event.entity.EntityAttributeModifiedEvent')` listener filtered to `roleveling:*` attributes. Whenever Java writes a stat point allocation, `applyStats` fires immediately — closing the gap where the ClassBook UI allocated points but the KubeJS attribute bridge didn't know until relog or inventory change.
- **HP/SP Recovery System (Standalone)**: `ro_status_system.js` now provides a self-contained HP/SP regeneration tick system using Ragnarok Online formulas. HP recovery scales with VIT and max HP; SP recovery scales with INT, max SP, and an optional modifier from equipment bonus stats. Both are exported for external use.

### Fixed
- **`CustomAllocateStatPacket` Stat Allocation**: The packet handler now correctly reads the incoming stat key, increments the `alloc_*` NBT counter, and calls `applyCustomAttributes` to push the modifier to the live `roleveling:*` attribute — previously the modifier was not being applied until the next full sync.
- **`ro_attribute_bridge.js` Double-Counting Eliminated**: `getRoStat(player, stat)` now reads directly from `player.getAttribute("roleveling:" + stat.toLowerCase()).getValue()`. The old pattern that read base NBT values and added gear bonuses manually caused double-counting when items also had `AttributeModifier` entries.
- **`applyStats` Loop Safety**: The ForgeEvents `EntityAttributeModifiedEvent` listener is safe from recursion — `applyStats` only writes to puffish/vanilla attributes, never back to `roleveling:*`. Firing 6× per allocation (once per attribute in `applyCustomAttributes`) is functionally harmless.
- **Class JSON Attribute Keys**: `adventurer.json` and `swordsman.json` had bare keys (`"str"`, `"agi"`, etc.) that Java was looking up via `ForgeRegistries.ATTRIBUTES` — which requires the full namespaced ID. Fixed to `"roleveling:str"`, `"roleveling:agi"`, etc. The dead `command` fields (`roleveling setbase ... {player}`) have also been removed; that command no longer exists in the reworked system.

### Removed
- **Status Effect Infrastructure in `ro_status_system.js`**: `RO_STATUS_KEYS`, `getPlayerStatuses`, `addStatus`, `removeStatus`, `hasStatus`, `getStatusData`, the per-status tick loop, the `rostatus` command, and all related global exports have been stripped. The status system scope is HP/SP recovery only; full status effects (Poison, Stun, Silence, etc.) are deferred to a future implementation.
- **`initPlayerData` from `ro_attribute_bridge.js`**: The old initialization function that seeded base stat NBT values on login is gone. Stats now live exclusively as Minecraft attribute modifiers — no NBT seeds needed.
- **`addstat`/`setbase` commands from `ro_leveling_system.js`**: These KubeJS commands were the old mechanism for writing stat values. The Java attribute system replaces them entirely.

### UI
- **Starting Items Inline Layout**: In `CustomClassSelectScreen`, starting item icons now render on the same line as the "Starting Items" label rather than on a separate row below it. Label pixel width is measured via `textRenderer.getWidth(siLabel) * globalScale` and the icons begin immediately after with a 6px gap. This frees a full row of vertical space, giving the passives grid more room.

---

## [2026-04-05] — Skill Tree Integrity Fixes

### Fixed
- **Points Overcounting Bug**: Multi-rank skill purchases (rank 1→2+) now correctly mark each level as paid in the `addon_paid_levels` bitmask. Previously, only the first unlock (rank 0→1) set the paid bit, causing `getSpentPoints` to undercount — which inflated `getPointsLeft` and allowed players to spend phantom points on other skills for free.
- **Exclusive Root Enforcement**: Players can no longer unlock multiple root skills in a category with `"exclusive_root": true`. The `canUnlockSkill` gate now checks whether any other root in the same category is already unlocked and blocks the attempt if so. Checks also fall back to pufferfish's native `unlockedSkills` set so natively-tracked first unlocks are caught correctly.
- **Parent Connection Enforcement**: Non-root skills are now blocked unless the required number of connected normal-connection neighbors are already unlocked. Previously, the server-side `canUnlockSkill` gate did not verify parent connectivity, allowing any skill in the tree to be clicked regardless of position. Fallback to `unlockedSkills` added for parity with exclusive root check.
- **Native Cost Gate**: `canAffordLevel` previously returned `true` unconditionally for any skill without a `PerLevelRewardsReward` (e.g. plain attribute-only skills, NYI placeholder nodes, class roots). A final native cost check now enforces `definition.cost()` via `SkillsAPI.getPointsLeft()` before allowing an unlock, closing the gap where players could click and spend skills for free.

---

## [2026-04-01] — Datapack Sync, Class Reset, and Skill Charm Fixes

### Added
- **Datapack-Driven Bridge Reloads**: Epic Class bridge configuration is now driven by datapacks through `BridgeDataLoader`, with `syncBridgeToAll()` pushing refreshed bridge data to connected clients immediately after `/reload`.
- **Passive Display Reloading**: Passive skill titles and descriptions are now reloaded and cached from datapack definitions during bridge reloads so client-facing class and skill text stays current without relogging.
- **Toggle Cooldown Client Sync**: Registered `SyncToggleCooldownPacket` on the Forge channel and bumped the network protocol so cooldown state can be pushed cleanly to clients.

### Fixed
- **Pre-Reset Pufferfish Cleanup**: `/class reset` now clears addon and Pufferfish progression before the previous class/category context is lost, including skill levels, paid-level bookkeeping, category XP, and follow-up lock resync.
- **Skill Charm Tooltip Accuracy**: Skill Charms now read their imbued skill data directly from charm NBT and display the correct skill name, level bonus, and available synced description text in the tooltip.
- **Client Charm Resolution**: Fixed definition-based client skill lookups by replacing flattened `category:skill` parsing with structured mapping and fuzzy matching, allowing equipped charms to resolve namespaced and shorthand ids correctly.
- **Dynamic Point-Cost Detection**: Point-cost lookups now detect nested `PerLevelRewardsReward` entries inside `ToggleReward`, preventing missed dynamic cost handling for toggle-backed skills.
- **Class Screen Stability**: Corrected the class-selection fallback text path so `CustomClassSelectScreen` no longer fails on the broken translation fallback branch.

### UI & Sync
- **Real-Time Datapack UI Updates**: Reloaded bridge config now updates already-connected clients instead of leaving class-selection and bridge-driven UI state stale until relog.
- **Optional-Mod Sync Safety**: Epic Class sync hooks now use reflection-based `SyncClassPacket` handling and `@Pseudo` mixin targets so optional bridge sync paths remain safe when the dependency is absent.
- **Creative Tab Behavior Clarified**: Synced bridge/config updates no longer pretend to rebuild creative tab entries during the current client session; creative-tab contents remain static until restart or a natural tab rebuild.

### Performance & Reliability
- **Skill Master Trade Caching**: Skill Master villager trades are now cached per player per in-game day instead of being rebuilt on every interaction, reducing unnecessary server-side generation work.
- **Trade Pool Cleanup**: Skill Master trade generation now reserves special slots for premium rolls, supports Skill Charm offers, and reduces filler-style outcomes for more consistent shop contents.

---

## [2026-03-16] — Crash Fixes, Robust Conditional Loading & CNPC Quest Integration

### Added
- **Fail-Safe Bridge Integration**: Refactored the Rise of Heroes (Epic Class) bridge to use `@Pseudo` Mixins and reflection. The mod now loads and runs perfectly even if the Rise of Heroes mod is missing.
- **Early-Stage Mod Detection**: Implemented `Class.forName` based mod checking in `BridgeMixinPlugin` for reliable loading during early Forge initialization.
- **CustomNPCs Quest Integration (Soft Dependency)**: Added full CustomNPCs quest tracking as an optional integration. When CustomNPCs is present, the mod bridges into its quest event system via reflection to track accepted, completed, and ready-to-turn-in quests on the client (`CnpcClientQuestState`). All CNPC mixins are gated by `PuffishForgeMixinPlugin` which checks for `noppes.npcs.api.NpcAPI` at mixin load time — the mod runs fully without CustomNPCs installed.
- **Structure Tracker Auto-Clear on Quest Completion**: When a player completes a CNPC quest whose ID matches the currently tracked structure in Epic Classes (`ClientStructureTracker`), the tracker is automatically cleared. This fires on every quest state packet received, not on a tick — entirely event-driven and works via reflection so it does not create a hard dependency.

### Fixed
- **Mod Loading Crash**: Resolved `NoClassDefFoundError` and `ClassNotFoundException` errors caused by hard dependencies on Epic Class classes.
- **Mixin Compatibility**: Cleaned up legacy renderer and entity mixins (`NpcQuestGiverRendererMixin`, `NpcQuestGiverEntityMixin`) that were causing startup failures.
- **Event Subscriber Safety**: Commented out the `CustomJobMasterSpawner` event subscriber to prevent classloading errors when the bridge is inactive.

### UI & Sync
- **Graceful Bridge Degradation**: Bridge-specific UI elements (like the class choice packet handling and dialogue screens) now remain inert and safe when the optional dependency is absent.

---

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
