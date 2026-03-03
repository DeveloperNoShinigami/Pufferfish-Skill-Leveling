# Epic Classes Datapack Guide

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
| Field | Type | Description |
|---|---|---|
| `class_name` | String | Unique identifier for your class. Must match the filename logically. |
| `display_name` | String | Fallback name used if translation keys are missing. |
| `display_name_key` | String | Language key for the title of the class. |
| `lore_key` | String | Language key for the detailed lore/description. |
| `skill_category_id` | String | The exact ID of the Pufferfish Skills category to map this class to. |
| `job_master_id` | String | The ID of the Job Master NPC config that allows players to pick this class. |
| `epic_class_proxy` | String | **Critical.** Tells Epic Fight which core animation logic to use. Valid: `WARRIOR`, `PALADIN`, `BERSERKER`, `REAPER`, `SORCERER`, `ARCHER`. |

### UI & Presentation Fields
| Field | Type | Description |
|---|---|---|
| `class_weapon_type` | String | Text descriptor displayed in the class book. |
| `class_weapon_icon` | String | The item ID (supports NBT) used as the icon. |
| `preview_animation` | String | The Epic Fight animation ID to loop in the class select menu. |
| `preview_armor_base` | String | The base armor ID prefix to equip on the dummy (e.g. `minecraft:iron_` wraps to `iron_helmet`, etc). |
| `preview_mainhand_item` | String | Item in the dummy's mainhand. Supports full NBT strings. |

### Gameplay Fields
| Field | Type | Description |
|---|---|---|
| `class_weapon_items` | String Array | List of acceptable weapon IDs for this class. |
| `starting_items` | String Array | Items granted to the player the moment they select the class. Supports quantities via `@` (e.g. `bone@10`) or NBT strings. |
| `attributes` | Object | The immediate stat changes applied permanently when selecting the class. |
| `gui_stats` | Object Array | Visual representation of stats on the UI. Doesn't grant stats, purely for display. |
| `gui_passives` | Object Array | Lists Pufferfish Skills to display as "Class Traits" in the UI. Links directly to the `puffish_skills` definitions. |

---

## 3. Configuring Class Attributes

This system bridges the gap between Pufferfish points and Epic Fight stats. Place these in `epic_classes/attributes/<class_id>.json`.

> [!TIP]
> This config evaluates mathematical expressions. You can scale stats exactly how you want.

```json
[
  {
    "id": "gunslinger_page",
    "slots": [
      {
        "id": "movement_speed",
        "name": "gui.epicclassmod.speed",
        "icon": "minecraft:feather",
        "attribute_id": "minecraft:generic.movement_speed",
        "value": "points * 0.01",
        "operation": "ADDITION",
        "max_points": 10,
        "description": "Increases your movement speed."
      }
    ]
  }
]
```
* **`value`**: The math expression defining the stat increase. `points` refers to points invested.
* **`attribute_id`**: The raw Minecraft or Epic Fight attribute to modify.

---

## 4. Defining Job Masters

Job Masters are Custom NPCs that allow players to change or select classes. Define them in `epic_classes/job_masters/`.

> [!WARNING]
> While Job Masters correctly load and can be targeted, some interactions with the native Epic Classes NPC dialogues are currently listed as **partly working** and are pending upstream updates.

```json
{
  "id": "job_master_gunslinger",
  "npc_id": "village_gunslinger",
  "name_key": "npc.example_mod.job_master.gunslinger",
  "texture": "example_mod:textures/entity/npc/gunslinger.png",
  "dialogue_key": "main__gui.epicclassmod.quest.job_master.gunslinger",
  "marker_block": "minecraft:iron_block",
  "equipment": {
    "HEAD": "minecraft:leather_helmet",
    "CHEST": "minecraft:iron_chestplate",
    "MAINHAND": "tacz:modern_kinetic_gun{GunId:\"tacz:deagle\"}"
  }
}
```

By placing an NPC entity in the world with the matching `npc_id`, the system will automatically inject this equipment and register it as the gateway to the `gunslinger` class.
