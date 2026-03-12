# Datapack Reference

[< Back to Epic Classes Index](index.md)

---

This reference page serves as a cheat-sheet for all JSON schemas and fields used by the Epic Classes Integration. Refer back to this when creating your custom classes and attributes.

## Epic Class Schema (`epic_classes/classes/<id>.json`)

The main configuration file for Epic Classes.

| Field | Type | Required | Description |
|---|---|---|---|
| `class_name` | String | **Yes** | Unique identifier for your class. Must match filename logically. |
| `class_parent` | String | No | The `class_name` of the previous class in the progression tree. Used to define advancement forward from a base class to the next class. |
| `display_name` | String | No | Fallback name used if translation keys are missing. |
| `display_name_key` | String | **Yes** | Language key for the title of the class. |
| `description` | String | No | Fallback description text used if translation keys are missing. |
| `lore_key` | String | No | Language key for the detailed lore/description. |
| `book_lore` | String | No | Literal string used for detailed lore in the class book if key is missing. |
| `skill_category_id` | String | **Yes** | The Pufferfish Skills category ID mapped to this class. |
| `job_master_id` | String | No | The Job Master NPC config ID. |
| `epic_class_proxy` | String | **Yes** | Epic Fight animation root (`WARRIOR`, `PALADIN`, `BERSERKER`, `REAPER`, `SORCERER`, `ARCHER`). |
| `gui_title` | String | No | Language key for the title specifically shown in the new Class Select screen. |
| `gui_description` | String | No | Short description shown in the Class Select screen. |
| `gui_notes` | String[] | No | List of bullet points shown in the Class Select screen below the description. |
| `class_weapon_type` | String | No | Text descriptor displayed in the class book. |
| `class_weapon_icon` | String | No | Item ID (supports NBT) used as the icon in the class book. |
| `class_weapon_items` | String[] | No | Acceptable weapon IDs for this class (Item Restrictions). |
| `preview_animation` | String | No | Epic Fight animation ID to loop in the class select menu. |
| `preview_armor_base` | String | No | Base armor ID prefix to equip on the dummy (e.g. `minecraft:iron_`). |
| `preview_mainhand_item` | String | No | Item in the dummy's mainhand. Supports full NBT strings. |
| `preview_offhand_item` | String | No | Item in the dummy's offhand. Supports full NBT strings. |
| `starting_items` | String[] | No | Items granted when selecting the class. Supports quantities via `@` or NBT. |
| `attributes` | Object | No | Immediate stats applied permanently when selecting the class. Keys are attribute IDs, values are `{"value": X, "operation": "..."}` objects. |
| `gui_stats` | Object[] | No | Visual representation of stats on the UI. See Stat UI Schema below. |
| `gui_passives` | Object[] | No | Pufferfish Skills to display as "Class Traits" in the UI. See Passive UI Schema below. |

## Stat UI Schema (`gui_stats` items)

| Field | Type | Required | Description |
|---|---|---|---|
| `label_key` | String | **Yes** | Translation key for the stat label. |
| `icon` | String | No | Valid item ID or texture path. |
| `stat_type` | String | No | `hearts` or `number`. Defaults to `hearts`. |
| `count` | Integer | **Yes** | Number of hearts to display or numeric value to show. |
| `unit` | String | No | Text appended to numeric stats (e.g. `HP`) when using `number` type. |

## Passive UI Schema (`gui_passives` items)

| Field | Type | Required | Description |
|---|---|---|---|
| `pufferfish_skill_id` | String | No | The ID of the Pufferfish skill to link to. Overrides name/desc keys if found. |
| `icon` | String | No | Valid item ID or texture path. |
| `name_key` | String | No | Translation key for the passive's name (if not using Pufferfish ID). |
| `desc_key` | String | No | Translation key for the passive's description (if not using Pufferfish ID). |

## Attribute Schema (`epic_classes/attributes/<id>.json`)

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | String | **Yes** | Unique ID within the file. |
| `name` | String | **Yes** | Translation key for the attribute. |
| `icon` | String | No | Item ID used as icon. |
| `attribute_id` | String | **Yes** | Raw Minecraft/Epic Fight attribute to modify. |
| `value` | String | **Yes** | Math expression (uses `points`). |
| `operation` | String | **Yes** | Combine operation (`ADDITION`, `MULTIPLY_BASE`, etc). |
| `max_points` | Integer | No | Cap the stat to this maximum point investment. |
| `description` | String | No | Tooltip description. |

## Job Master Schema (`epic_classes/job_masters/<id>.json`)

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | String | **Yes** | Unique identifier for your job master config. |
| `marker_block` | String | No | Registry ID of the block that triggers spawning/location marking. |
| `texture` | String | No | Resource location of the NPC's custom texture. |
| `name_key` | String | No | Translation key for the NPC's display name. |
| `dialogue_key` | String | No | Translation key for the overarching NPC dialogue. |
| `equipment` | Object | No | Key-value mapping of the NPC's worn equipment and weapons. Valid keys: `mainhand`, `offhand`, `helmet`, `chestplate`, `leggings`, `boots`. Values are item IDs/NBT strings. |

## Global Bridge Schema (`config/pufferfish_epic_classes_bridge.json`)

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

## Item Restriction Schema (`epicclassmod/item_restrictions/<id>.json`)

Defines advanced item usage requirements. Supports both a single object format and a multi-entry format `{"restrictions": [...]}`.

| Field | Type | Required | Description |
|---|---|---|---|
| `item` | String or Array | **Yes** | The item ID(s) to restrict. |
| `require_class` | String | No | The specific Epic Class ID required to use the item. |
| `require_level` | Object | No | Pufferfish level requirement. Keys: `"category"` (String), `"min"` (Integer). |
| `require_attribute` | Object | No | Entity attribute requirement. Keys: `"attribute"` (String), `"min"` (Double). |
| `require_held` | String Array | No | The player must hold at least one of these item IDs in either hand. |
| `require_worn` | String Array | No | The player must wear at least one of these armor piece item IDs. |
| `require_effect` | String Array | No | The player must have ALL of these status effect IDs active. |
| `require_quest` | String Array | No | The player must have accepted ALL of these Epic Class Quest IDs. |

---

## Experience Tome Definition (`data/[namespace]/tome_config/<id>.json`)

Defines custom Experience Tomes that can be used to grant raw Pufferfish XP.

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | String | **Yes** | Internal identifier for the tome definition. |
| `name` | String | No | Default display name (overridden by NBT). |
| `rarity` | String | No | Color formatting (`COMMON`, `UNCOMMON`, `RARE`, `EPIC`, etc). |
| `max_levels` | Integer | No | Maximum level support for the variant. |
| `experience_per_level` | Object | **Yes** | Logic for XP granted per level. |

### Experience Per Level Sub-Object

| Field | Type | Required | Description |
|---|---|---|---|
| `type` | String | **Yes** | `values` or `expression`. |
| `data` | Object | **Yes** | If `values`: `{"values": [100, 200, ...]}`. If `expression`: `{"expression": "level * 500"}`. |

---

[< Back to Epic Classes Index](index.md)
