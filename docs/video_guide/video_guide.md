# Video Tutorial Series Guide

A structured outline for creating a video tutorial series covering the Pufferfish Skill Leveling addon, updated for the Curios & Equipment Slots refinement.

---

## Series Overview

**Target Audience**: Minecraft modpack creators and datapack developers  
**Estimated Episodes**: 7-9 videos  
**Episode Length**: 10-20 minutes each

---

## Episode 1: Introduction & Setup
**[Detailed Script](./episode_1_setup.md)**

### Topics
- What the addon does (extend Puffish Skills with multi-level progression)
- Installation requirements (Puffish Skills 0.17.1+, Minecraft 1.20.1)
- **New Requirements**: Curios API (Recommended for Skill Charms)
- Datapack structure overview
- Using the Puffish Skills Web Editor

---

## Episode 2: Creating Your First Multi-Level Skill
**[Detailed Script](./episode_2_multi_level_skills.md)**

### Topics
- `definitions.json` structure
- Key addon fields: `max_skill_level`, `points_per_level`
- `per_level_rewards` reward type
- Level-specific descriptions

---

## Episode 3: Prerequisite Systems Deep Dive
**[Detailed Script](./episode_3_prerequisites.md)**

### Topics
- **Top-Level Prerequisites** (`prerequisite_skills`)
  - Controls skill visibility/purchasability
- **Per-Level Prerequisites** (`required_skill`)
  - Cross-category support
  - Controls reward activation
- **Hidden Skills** (`hidden: true`)
  - Secret progression paths

---

## Episode 4: The Equipment Imbuing System
**[Detailed Script](./episode_4_equipment_imbuing.md)**

### Topics
- **Equipment Slots**: Understanding the new branding
- **Skill Tome**: Grants +1 level to a skill
- **Sigil of Imbuement**: Opens equipment slots (up to 3)
- **Tome of Cleansing**: Extracting skills from gear
- **Terminology**: "Equipment Slots" vs "Skill Slots"

---

## Episode 5: Curios Integration & Skill Charms
**[Detailed Script](./episode_5_curios_skill_charms.md)**

### Topics
- **Skill Charms**: The new dedicated Curio item
- **Curio Slots**: How to configure slots for charms (`curios/slots`, `curios/entities`)
- **Instant Activation**: How Curio charms bypass standard unlock requirements
- **Gating**: Using `requireUnlockForCurioImbuing` to lock/unlock Curio usage

---

## Episode 6: Skill Master Villager
**[Detailed Script](./episode_6_skill_master.md)**

### Topics
- Skill Scribe Table (workstation)
- 5-tier progression system
- Dynamic trade scaling
- Configuration file (`skill_master_reputation/config.json`)

---

## Episode 7: Loot Modes & Acquisition
**[Detailed Script](./episode_7_loot_modes.md)**

### Topics
- `loot_mode` values: `tome_only`, `imbue_only`, `both`
- Creating "Equipment-Only" skills for RPG flavor

---

## Episode 8: Advanced Configuration & Tips
**[Detailed Script](./episode_8_advanced_config.md)**

### Topics
- Expression syntax for costs (`"level * 5"`)
- **Description Merging**: Accumulating bonuses for cleaner tooltips
- **Silenced Logging**: How to find logs for debugging (Search for `[ADDON]`)
- Troubleshooting common JSON mistakes

---

## Recording Tips

### Setup
- Use a clean test world
- Pre-build example datapacks
- **Visuals**: Use the new ✦ star symbol and "Equipment Slots" branding consistently

### Common Mistakes to Show
- Forgetting the `metadata` field
- Setting `max_skill_level` but forgetting to define rewards for all levels
- Misspelling NBT tags (handled by the new robust scanner, but still good to avoid)

---

## Downloadable Resources
- Example datapack with Curios support
- Updated Field Reference Sheet (PDF)
- Template JSON for Skill Charms
