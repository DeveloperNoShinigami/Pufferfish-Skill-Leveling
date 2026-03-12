# Epic Class Creation Guide

[< Back to Epic Classes Index](index.md) | [Next: Item Restrictions & Gating >](ITEM_RESTRICTIONS_GUIDE.md)

---

This guide details how to create and configure custom Epic Classes to tie natively into the Pufferfish Skill Leveling system. By utilizing Minecraft's DataPack system, you can define complete classes, custom GUIs, attribute modifiers, and NPC Job Masters without writing any code.

> [!NOTE]
> All configurations for Epic Classes have moved to native DataPacks in the `data/<namespace>/puffish_skill_leveling/epic_classes/` folder. This ensures absolute compatibility with servers and modpacks.

---

## 1. Directory Structure

A complete integration DataPack uses the following structure:

```text
data/
└── <namespace>/
    ├── puffish_skills/
    │   └── categories/
    │       └── <category_id>/
    │           ├── category.json    (Title, Icon, Background)
    │           ├── definitions.json (Skill levels and rewards)
    │           ├── skills.json      (Tree layout/coordinates)
    │           ├── connections.json (Tree flow/lines)
    │           └── experience.json  (Leveling logic)
    └── puffish_skill_leveling/
        └── epic_classes/
            ├── classes/         (Defines GUI, stats, passives, layout)
            ├── attributes/      (Links Pufferfish skill points to stats)
            └── job_masters/     (Defines NPCs the player can talk to)
```

## 2. Defining an Epic Class

Custom classes are defined in `epic_classes/classes/<class_id>.json`. This file acts as the primary configuration for the class selection UI and starting modifiers.

### Example: Gunslinger Class
Here is a comprehensive example of a full class setup using external weapons (TACZ) and animations.

```json
{
    "class_name": "gunslinger",
    "display_name": "Gunslinger",
    "display_name_key": "class.epicclassmod.gunslinger.title",
    "lore_key": "class.epicclassmod.gunslinger.desc",
    "skill_category_id": "gunslinger",
    "job_master_id": "job_master_gunslinger",
    "epic_class_proxy": "ARCHER",
    "book_lore": "A master of modern ballistics. Relying on agility and precision, they dominate from a distance.",
    "class_weapon_type": "Handguns / Rifles",
    "class_weapon_icon": "tacz:modern_kinetic_gun{GunId:\"tacz:deagle\"}",
    "class_weapon_items": [
        "tacz:modern_kinetic_gun{GunId:\"tacz:deagle\"}",
        "minecraft:iron_sword"
    ],
    "attributes": {
        "minecraft:generic.max_health": {
            "value": 14.0,
            "operation": "BASE"
        },
        "irons_spellbooks:max_mana": {
            "value": 50.0,
            "operation": "BASE"
        }
    },
    "gui_title": "class.epicclassmod.gunslinger.title",
    "gui_description": "Swift and precise marksman.",
    "preview_animation": "wom:biped/living/enderblaster_onehand_idle",
    "preview_armor_base": "minecraft:iron_",
    "preview_mainhand_item": "tacz:modern_kinetic_gun{GunId:\"tacz:deagle\"}",
    "preview_offhand_item": "tacz:modern_kinetic_gun{GunId:\"tacz:deagle\"}",
  "starting_items": [
    "tacz:modern_kinetic_gun{GunId:\"tacz:deagle\", AmmoCount:7}",
    "tacz:ammo{AmmoId:\"tacz:50ae\", Count:64}"
  ],
  "gui_stats": [
    { "label_key": "gui.stat.health", "icon": "epicclassmod:textures/gui/icons/heart.png", "stat_type": "hearts", "count": 7 },
    { "label_key": "gui.stat.speed", "icon": "epicclassmod:textures/gui/icons/boots.png", "stat_type": "number", "count": 105 }
  ],
  "gui_passives": [
    { "pufferfish_skill_id": "quick_draw", "icon": "epicclassmod:textures/gui/icons/sword.png" },
    { "pufferfish_skill_id": "lead_rain", "icon": "epicclassmod:textures/gui/icons/chest.png" },
    { "pufferfish_skill_id": "ghost_step", "icon": "epicclassmod:textures/gui/icons/boots.png" },
    { "pufferfish_skill_id": "killshot", "icon": "epicclassmod:textures/gui/icons/helm.png" }
  ]
}
```

### Core Configuration Fields
| Field | Type | Required | Description |
|---|---|---|---|
| `class_name` | String | **Yes** | Unique identifier for your class. Must match the filename logically. |
| `class_parent` | String | No | The `class_name` of the previous class in the progression tree. Used to define advancement forward from a base class to this class. |
| `display_name` | String | No | Fallback name used if translation keys are missing. |
| `display_name_key` | String | **Yes** | Language key for the title of the class. |
| `description` | String | No | Fallback description text if translation keys are missing. |
| `lore_key` | String | No | Language key for the detailed lore/description. |
| `book_lore` | String | No | Literal string used for detailed lore in the class book. |
| `skill_category_id` | String | **Yes** | **Critical.** The ID of the Pufferfish Skills category to map this class to. The bridge natively tries the `epic_classes:` namespace. Ensure your Pufferfish folders match this exactly. |
| `job_master_id` | String | No | *Future Addition.* The ID of the Job Master NPC config. |
| `epic_class_proxy` | String | **Yes** | **Critical.** Tells Epic Fight which core animation logic to use. Valid: `WARRIOR`, `PALADIN`, `BERSERKER`, `REAPER`, `SORCERER`, `ARCHER`. |

### UI & Presentation Fields
| Field | Type | Required | Description |
|---|---|---|---|
| `gui_title` | String | No | Title specifically shown in the new Class Select screen UI. |
| `gui_description` | String | No | Short description shown in the Class Select screen UI. |
| `gui_notes` | String Array | No | List of bullet points shown in the Class Select screen below the description. |
| `class_weapon_type` | String | No | Text descriptor displayed in the class book. |
| `class_weapon_icon` | String | No | The item ID (supports NBT) used as the icon in the class book. |
| `preview_animation` | String | No | The Epic Fight animation ID to loop in the class select menu dummy. |
| `preview_armor_base` | String | No | The base armor ID prefix to equip on the dummy (e.g. `minecraft:iron_`). |
| `preview_mainhand_item` | String | No | Item in the dummy's mainhand. Supports full NBT strings. |
| `preview_offhand_item` | String | No | Item in the dummy's offhand. Supports full NBT strings. |

### Gameplay Fields
| Field | Type | Required | Description |
|---|---|---|---|
| `class_weapon_items` | String Array | No | List of acceptable weapon IDs for this class. |
| `starting_items` | String Array | No | Items granted to the player the moment they select the class. Supports quantities via `@` (e.g. `bone@10`) or NBT strings. |
| `attributes` | Object | No | The immediate stat changes applied permanently when selecting the class. |
| `gui_stats` | Object Array | No | Visual representation of stats on the UI. Doesn't grant stats, purely for display. |
| `gui_passives` | Object Array | No | Lists Pufferfish Skills to display as "Class Traits" in the UI. Links directly to the `puffish_skills` definitions. |

---

---

[< Back to Epic Classes Index](index.md) | [Next: Item Restrictions & Gating >](ITEM_RESTRICTIONS_GUIDE.md)
