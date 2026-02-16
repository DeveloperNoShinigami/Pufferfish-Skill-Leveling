# Changelog

All notable changes to Pufferfish Skill Leveling are documented in this file. Dates are in YYYY-MM-DD format.

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
