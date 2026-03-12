# Roadmap

Current development status and future plans for Pufferfish Skill Leveling.

---

## Completed Features

### Core Systems
- [x] Multi-level skill progression (1–999 levels)
- [x] Per-level rewards with nested support
- [x] Level tracking with NBT persistence
- [x] Paid vs granted level distinction for refunds
- [x] Admin commands (`/skillleveling set`, `get`, `refund`, `info`)
- [x] Client/server sync for UI rendering
- [x] Architectury multi-loader (Forge + Fabric)

### Toggle System
- [x] Pure toggles (on/off abilities, maxLevel 0)
- [x] Basic toggles (single-level unlockable toggles)
- [x] Hybrid toggles (multi-level + toggleable)
- [x] Mastery keybinds (slots 1–9)
- [x] Cooldowns (tick-based)
- [x] Auto-disable on death
- [x] Protected effects (survive milk/death)
- [x] ToggleReward type with enable/disable containers

### Prerequisite & Gating
- [x] Prerequisite skills on individual skill definitions
- [x] Cross-category prerequisites
- [x] Required skill for level (tier gates)
- [x] Hidden skills (reveal on prerequisite met)
- [x] Category gating with `prerequisite_skills`
- [x] `keep_unlocked` for permanent category access

### Description System
- [x] Level descriptions (current rank tooltip)
- [x] Extra descriptions (next rank preview on Shift)
- [x] Merge description mode (stacking previous levels)
- [x] Dynamic tooltip injection (level info, toggle status, cooldowns)

### Loot & Acquisition
- [x] Loot mode system (`both`, `tome_only`, `imbue_only`)
- [x] Universal Loot Injection (chest and entity groups)
- [x] Weighted entry selection with per-entry drop chance
- [x] Skill Tome entries (random and targeted)
- [x] Skill Charm entries

### Equipment & Imbuing
- [x] Sigil of Imbuement (slot opening, up to 3)
- [x] Skill Tome anvil application
- [x] Skill upgrading via matching Tome
- [x] Tome of Cleansing (I/II/III) for extraction
- [x] Tome Ranking (combine identical-level Tomes)
- [x] XP cost system (scalar, array, expression)

### Dynamic Skill Imbuement
- [x] Equipment spawns with pre-imbued skills from loot
- [x] Dimension overrides
- [x] Distance scaling with brackets
- [x] Category settings (per-equipment-type tuning)
- [x] Exclusion groups
- [x] Item blacklist/whitelist
- [x] Loot table whitelist
- [x] 16 equipment categories (sword through skill_charm)

### Progression Items
- [x] Blank Tome (base crafting material)
- [x] Skill Tome (grant specific skill level)
- [x] Tome of Progression (choose-any skill level)
- [x] Tome of Clear Mind (refund 1 level)
- [x] Tome of Greater Clear Mind (full skill reset)
- [x] Skill Charm (Curios-compatible accessory)

### Village Integration
- [x] Skill Master villager profession
- [x] Skill Scribe Table workstation
- [x] 5-tier trade progression with mastery pricing
- [x] Village structure generation (Plains, Desert, Savanna, Snowy, Taiga)
- [x] Tiered loot barrels in structures
- [x] Rare Experience Tomes in Skill Master Shop (Level-scaling)

### Epic Classes Integration
- [x] Full Rise of Heroes datapack support
- [x] S2C Network sync for bridge data
- [x] NBT Item icon parsing for class previews
- [x] 10 Multi-file standardized skill categories
- [x] Block interaction gating (Breaking/Right-click)
- [x] Entity interaction/attack gating
- [x] Dimension access gating
- [x] Area/Structure proximity gating
- [x] Environmental gating (in_water, time_of_day)

---

## Experimental / In Development

- [ ] Epic Class Quest gating (require_quest implementation)
- [ ] Expression-based cost formulas for more complex scaling
- [ ] Additional toggle patterns and edge cases

---

## Planned Features

### Short Term
- [ ] Persistent config file system (`config/puffish_skill_leveling/config.json`)
  - [ ] `disable_skill_master_house` — disable Skill Master House structure generation
  - [ ] `require_unlock_for_imbuing` — gate imbued gear bonuses behind base skill unlock
  - [ ] `require_unlock_for_curio_imbuing` — gate curio imbued bonuses behind base skill unlock
  - [ ] `debug_logging` — persistent debug logging toggle (currently runtime-only via command)
- [ ] **Robust Structure Enforcement (Overhaul)**
  - [ ] Implement 40-tick (2s) interval polling for performance
  - [ ] Use `StructureAccessor` for component-level detection
  - [ ] Implement "Teleport Out" fallback for persistent intruders
- [ ] More datapack template categories (magic-focused, ranged-focused)
- [ ] Expanded template pack with demo datapacks covering every feature
- [ ] In-game configuration UI for server admins

### Medium Term
- [ ] Custom Job Master integration (Currently disabled/partly working; NPC class switching will be overhauled in the future)
- [ ] Skill synergy system (bonus effects for specific skill combinations)
- [ ] Party/team skill sharing (share passive bonuses with nearby allies)
- [ ] Achievement-based progression (grant levels from advancement triggers)
- [ ] Custom GUI screens for imbued equipment inspection

### Long Term
- [ ] Skill specializations (branching paths within a single skill)
- [x] Cross-mod integration API for third-party addon support (Epic Classes Addon)
- [ ] Web-based datapack builder tool
- [ ] Skill leaderboards and statistics tracking

---

## Known Limitations

- Toggle keybinds are limited to 9 slots (corresponding to hotbar slots 1–9).
- Curios integration requires the Curios API mod to be installed separately.
- Village structures use jigsaw pooling—structure frequency depends on vanilla village generation.
- Expression-based costs use a custom parser; complex math functions are not yet supported.
