# Global Bridge Configuration

[< Back to Epic Classes Index](index.md) | [Next: Datapack Reference >](DATAPACK_REFERENCE.md)

---

The main bridge configuration file connects Pufferfish Skill Leveling and Epic Classes together at a global level.

**Bridge datapack location:** `data/<namespace>/epicclassmod/pufferfish_skills_bridge.json`

## Features

The Bridge config allows you to specify:
1. **Enable/Disable the bridge:** You can toggle the integration entirely.
2. **Category Mappings:** Specify which Pufferfish Skill category corresponds to the main classes.
3. **Stat Conversions:** Configure global conversion rates for attributes.
4. **Weapon Restriction Ownership:** Choose whether bridge-owned class weapon restrictions replace ECM's legacy weapon restriction system.

## Configuration Sub-Schemas

The Bridge config file uses the following schema:

| Field | Type | Default | Description |
|---|---|---|---|
| `enabled` | Boolean | `true` | Master toggle to enable or disable the Bridge entirely. |
| `classToCategoryMap` | Map<String, List<String>> | (Base classes) | Maps an Epic Fight/Classes class ID to a list of Pufferfish category IDs. |
| `categorySyncEnabled` | Map<String, Boolean> | (Base classes) | Toggles automatic level synchronization for specific Pufferfish categories. |
| `statToSkillMap` | Map<String, String> | Empty | Maps an Epic Fight stat ID directly to a Pufferfish Skill node. |
| `classPassiveToSkillMap` | Map<String, String> | (Base classes) | Maps UI passive slots (`CLASSNAME_0`) to Pufferfish Skill nodes. |
| `classPassiveToLevelMap` | Map<String, Integer> | (Base classes) | Level requirements for UI passives (`CLASSNAME_0`). |
| `autoActivateCategory` | Boolean | `true` | Automatically unlocks and switches to the mapped Pufferfish category when a player changes classes. |
| `lockOtherCategories` | Boolean | `true` | Automatically locks all non-mapped Pufferfish categories when a player changes classes. |
| `syncOnLogin` | Boolean | `true` | Re-syncs levels and attributes when a player joins the server. |
| `disableBaseClasses` | Boolean | `false` | Disables selection of the base Epic Classes via traditional menus if utilizing custom class progression. |
| `enableAutoClassWeaponRestrictions` | Boolean | `true` | `true` enables bridge-owned class weapon restrictions and disables ECM's legacy job weapon restrictions. `false` disables bridge auto class weapon checks and leaves ECM's legacy job weapon restrictions enabled. |
| `useCnpcQuests` | Boolean | `false` | Enables CNPC-first quest mode. CustomNPCs stays authoritative for quest/dialog flows while the addon remains authoritative for class and Pufferfish state. |
| `cnpcQuestMappings` | Map<String, Object> | Empty | Optional integration metadata keyed by CustomNPCs quest ID. Used to mark quests as `general`, `job`, or `advancement`, attach a `classId`, route a mirrored `bookCategory`, and trigger bridge actions such as `open_class_select`. |
| `stat_points_per_level` | Integer | `1` | Global default for how many ECM stat points a player earns per Pufferfish level gained. Can be overridden per-class using `stat_points_per_level` in the class JSON. |

### Weapon Restriction Mode

When `enableAutoClassWeaponRestrictions` is enabled:
- the bridge reads `class_weapon_items` and `class_weapon_tags`
- parent classes contribute inherited weapon rules through `class_parent`
- ECM's original job-weapon restriction system is turned off in favor of the bridge system

When `enableAutoClassWeaponRestrictions` is disabled:
- bridge auto class-weapon checks are skipped
- ECM's original job-weapon restriction system remains active

### Synchronization Stability (March 11 Update)

The Bridge now implements a **Strict Authoritative Sync** model. Pufferfish Skills is treated as the source of truth for all mapped XP categories. 

- **Recursion Guard**: Uses a depth-counter (`SYNC_DEPTH`) to ensure that even if multiple internal methods are called during an XP transaction, only a single, precise synchronization packet is sent to Epic Classes.
- **Notification Passthrough**: The bridge calculates the exact delta of XP gained and passes it to the Epic Classes HUD, ensuring consistent "EXP Gain" toasts without duplication.

### Stat Point Carry-Over

The bridge now preserves previously unspent ECM stat points when class sync recalculates the player's level-backed progression. This prevents spare points from being lost during class transitions or sync refreshes.

---


[< Back to Epic Classes Index](index.md) | [Next: Datapack Reference >](DATAPACK_REFERENCE.md)
