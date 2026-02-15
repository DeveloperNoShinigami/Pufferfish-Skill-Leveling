# Feature Roadmap

## ✅ Completed Features

### Core System
- [x] Multi-Level Skill Progression (`max_skill_level`, `points_per_level`)
- [x] Per-Level Rewards System
- [x] Session-Local Reward Protection (Join-time stability)
- [x] Prerequisite Skills with Level Requirements
- [x] Dynamic Tooltips with Level Info
- [x] Real-Time Attribute Sync

### Skill Tomes & Crafting
- [x] Tome of Progression (advance any skill)
- [x] Tome of Clear Mind (refund 1 level)
- [x] Tome of Greater Clear Mind (reset skill)
- [x] Skill Tome (grants specific skill)
- [x] Blank Tome as a base crafting material
- [x] Full crafting recipes for all tome types

### Equipment Imbuing
- [x] Single-Skill Imbuing via Anvil
- [x] Multi-Skill Slot System (up to 3 per item)
- [x] Sigil of Imbuement (opens slots)
- [x] Tiered Tome of Cleansing (I, II, III) for targeted extraction
- [x] Configurable Costs (`slot_opening_cost`, `cleansing_cost`)
- [x] Correct `loot_mode` preservation on extraction
- [x] Stacked attribute bonuses from multiple skills
- [x] Visual feedback (Enchantment glint on Skill Tomes)

### Integrations
- [x] **Curios API Support**: Dedicated accessory slots for Skill Charms and auto-sync activation.

### Skill Master Villager (Overhauled)
- [x] Workstation registration (Skill Scribe Table)
- [x] **Dynamic trade scaling**: 5-12 slots across 5 tiers.
- [x] **Intelligent level distribution**: Tier-based tome levels (T1-T5).
- [x] **Mastery System**: Reputation and special offers linked to player skill levels.
- [x] **Tome Update Trades**: Emeralds + Tome(L) -> Tome(L+1).
- [x] **Mastery Messaging**: Golden highlight and chat notifications for legendary status.
- [x] **Loot Mode Awareness**: Trades respect `loot_mode` settings (imbue-only vs generic).
- [x] **Admin Tooling**: Commands to force/set/reset the villager.

### World Integration
- [x] Tiered mob drop integration for Blank Tomes and Skill Tomes
- [x] Global structure injection (Village, Dungeon, Nether, End)
- [x] **Skill Master Houses**: Custom Jigsaw-based village buildings.
- [x] **Structural Loot**: Skill Master Barrels with tiered progression loot.

### Advanced Mechanics
- [x] **Hidden Skills**: Visual discovery mode (invisible icons/tooltips until requirements met).
- [x] **Enhanced Prerequisites**: Cross-category locking and specialized bypasses.
- [x] **External Configuration**: Full datapack support for Trading, Reputation, and Loot.
- [x] **Toggle Skills**: Support for 9 custom keybinds to manually activate/deactivate skills.
- [x] **Toggle Stability**: Full state persistence across joins, silent login, and fixed first-click desync.
- [x] **Effect Rewards**: Native Potion Effect support (`puffish_skills:effect`) with infinite duration handling.

---

## 🧪 Implemented (Experimental - Needs Testing)
*These features are implemented but not yet recommended for general use. They may change or be removed.*

- [x] **Dynamic Cost Scaling**: Exponential cost multipliers for skills (`scaling_factor`).
- [x] **Partial Rewards**: Logic to keep rewards active even if prerequisites are lost (`allow_partial_rewards`).
- [x] **Reward Synergies**: Cross-category requirements within specific rewards (`prerequisite_skills` in Rewards).

---

## 🚀 Planned Features (Next Phase)

### Progression Mechanics
- [ ] Tooltip preview for slot opening costs
- [ ] Better visual feedback in anvil when imbuing/extracting
- [ ] **Deactivation Rewards (`locked`)**: Support for rewards that trigger when a skill hits Level 0 (deactivation hook).
- [ ] **Stackable Skill Type**: Restore `puffish_skill_leveling:stackable` to allow base Pufferfish rewards and per-level rewards to scale together.
- [ ] **Progressive Skill Tomes**: (Deferred) Adjust tome levels based on player progression with a config toggle.

### Advanced Imbuing
- [ ] Skill compatibility restrictions (e.g., some skills can't coexist)
- [ ] Slot type restrictions (e.g., armor-only skills)

### World & Loot
- [ ] **Apotheosis-style Skill Loot**: Automatically injecting randomized skills/imbuements into standard loot tables.

### User Interface & Controls
- [ ] **Timed Toggles**: Duration-based activation (`timed_toggle`) with `on_expiry` reward support.
- [ ] **Re-implement Targeted Tome Selection Mode**: Restore functionality for Tomes that require selecting a specific skill (removed due to mixin crash).

### 📚 Documentation & Technical 
- [ ] **Comprehensive Developer Guide**: In-depth technical manual for modders and developers. Covers Java API, internal mixins, and deep-level JSON schema customization for advanced modding.

---

## 🐛 Current Bugs & Known Issues
*All major critical bugs from the v2.5.0 cycle have been resolved.*

- [ ] **Hidden Skill Category Warning**: "Could not determine category for skill..." warnings sometimes appear during initial registry mapping for hidden skills.

---

*Last Updated: 2026-02-11*
