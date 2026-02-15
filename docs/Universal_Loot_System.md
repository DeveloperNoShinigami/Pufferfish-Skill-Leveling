# Universal Loot Injection System

The Universal Loot Injection system allows you to add custom items, including Skill Charms, to existing loot tables (chests and mob drops) across both Forge and Fabric platforms.

## Overview

- **Chest Loot**: Handled via Global Loot Modifiers (GLM).
- **Entity Drops**: Handled by a custom `LivingDropsEvent` (Forge) ensuring that injected items are correctly persisted and imbued even if the standard loot system is bypassed.

## Configuration

Loot injections are configured via JSON files in your datapack.

### Path: `data/puffish_skill_leveling/loot_modifiers/universal_loot.json`

This file defines the groups of items to be injected.

```json
{
  "conditions": [
    {
      "condition": "minecraft:alternative",
      "terms": [
        { "condition": "minecraft:location_check", "predicate": { "dimension": "minecraft:overworld" } }
      ]
    }
  ],
  "chest_injection_groups": [
    {
      "targets": ["minecraft:chests/village/village_toolsmith", "minecraft:chests/abandoned_mineshaft"],
      "chance": 0.3,
      "rolls": { "min": 1, "max": 2 },
      "entries": [
        {
          "type": "item",
          "name": "puffish_skill_leveling:skill_charm",
          "weight": 50,
          "chance": 1.0
        }
      ]
    }
  ],
  "entity_drop_groups": [
    {
      "targets": ["minecraft:entities/skeleton", "minecraft:entities/zombie"],
      "chance": 0.1,
      "rolls": { "min": 1, "max": 1 },
      "entries": [
        {
          "type": "item",
          "name": "puffish_skill_leveling:skill_charm",
          "weight": 10
        }
      ]
    }
  ]
}
```

### JSON Fields Breakdown

| Field | Type | Description |
| :--- | :--- | :--- |
| `targets` | `List<String>` | List of Loot Table IDs (e.g. `minecraft:chests/desert_pyramid` or `minecraft:entities/skeleton`). Supports `#` prefix for tags. |
| `chance` | `Float` | The overall probability (0.0 to 1.0) that this group will trigger for a matching table. |
| `rolls` | `Object` | Defines how many items from the `entries` list will be picked. Contains `min` and `max`. |
| `entries` | `List<Object>` | The pool of items to choose from based on weights. |

#### Entry Fields

| Field | Type | Description |
| :--- | :--- | :--- |
| `type` | `String` | Type of entry (currently only `item` is supported). |
| `name` | `String` | The Resource Location of the item (e.g. `minecraft:iron_ingot`). |
| `weight` | `Integer` | Used for weighted random selection among entries in the same group. |
| `chance` | `Float` | Individual chance for this specific item to be picked (tested after weight selection). |
| `count` | `Object` | (Optional) `min`/`max` for the number of items in the stack. |
| `skill` | `String` | (Optional) Forces a specific skill and level (e.g. `combat:arcane_striker:3`). |

## Persistent Mob Drops
Unlike standard GLMs which can sometimes fail to apply NBT or be skipped by complex mob-death logic, our system hooks directly into the Living Entity drops. This ensures that injected Skill Charms are **recorded and imbued** reliably before hitting the ground.
