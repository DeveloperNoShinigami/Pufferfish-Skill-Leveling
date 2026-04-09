# Item Restrictions & Gating

[< Back to Epic Classes Index](index.md) | [Next: CNPC Class NPCs >](JOB_MASTERS_GUIDE.md)

---

The integration allows you to restrict the usage of powerful items, weapons, and tools based on a player's class and skill levels. This ensures that only players who have invested in the correct progression paths can wield specific equipment.

## 1. Bridge-Owned Class Weapon Restrictions

The recommended system uses `class_weapon_items` and/or `class_weapon_tags` in your Epic Class `classes/<name>.json` definition.

Behavior:
1. The bridge reads the class's allow-list directly from the class JSON.
2. Child classes inherit parent weapon rules automatically through `class_parent`.
3. If `enableAutoClassWeaponRestrictions` is `true`, the bridge system is authoritative and ECM's legacy job weapon restriction system is disabled.
4. If `enableAutoClassWeaponRestrictions` is `false`, the bridge skips these automatic class-weapon checks and ECM's legacy system stays active.

### Direct Item List Example

```json
"class_weapon_items": [
    "tacz:modern_kinetic_gun{GunId:\"tacz:deagle\"}",
    "minecraft:diamond_sword"
]
```

### Class Tag Example

```json
"class_weapon_tags": [
    "puffish_skills_leveling:class_weapons/gunslinger"
]
```

### Recommended Tag Layout

Use one item tag per class and put the actual item IDs inside the tag file.

`data/puffish_skills_leveling/tags/items/class_weapons/gunslinger.json`

```json
{
  "replace": false,
  "values": [
    "tacz:modern_kinetic_gun{GunId:\"tacz:deagle\"}",
    "minecraft:crossbow"
  ]
}
```

This is the preferred workflow for custom classes because the tag file becomes the single source of truth for that class's allowed weapons.

## 2. Advanced Item Requirements Datapack

For fine-grained control, you can create dedicated requirement files in `data/<namespace>/epicclassmod/item_restrictions/`. This system allows you to configure specific item restrictions based on skills, attributes, other worn equipment, or active quests.

### Example Configuration

You can define either a single requirement object, or a multi-entry file using an array.

**`data/example_namespace/epicclassmod/item_restrictions/heavy_swords.json`**
```json
{
  "restrictions": [
    {
      "item": ["minecraft:iron_sword", "minecraft:diamond_sword"],
      "require_class": "warrior",
      "require_level": {
        "category": "warrior",
        "min": 10
      }
    },
    {
      "item": "irons_spellbooks:blood_staff",
      "require_attribute": {
        "attribute": "irons_spellbooks:max_mana",
        "min": 100.0
      },
      "require_worn": ["minecraft:iron_chestplate"]
    }
  ]
}
```

### Available Requirements
- `require_class`: Requires a specific Epic Class ID. Flexible (`Lich` and `epic_classes:lich` both work).
- `require_level`: Requires a minimum level in a specific Pufferfish category (e.g. `{"category": "archery", "min": 5}`).
- `require_attribute`: Requires a minimum entity attribute total (e.g. `{"attribute": "minecraft:generic.max_health", "min": 20.0}`).
- `require_held`: Player must hold one of these items in Main/Offhand.
- `require_worn`: Player must be wearing one of these armor items.
- `require_effect`: Player must have ALL specified status effect IDs active.
- `require_quest`: Player must have accepted ALL specified Epic Class Quest IDs.

### Interaction With Class Weapon Restrictions

- `item_restrictions` and bridge-owned class weapon restrictions are separate systems.
- Explicit `item_restrictions` entries are still the right choice for special-case gating such as level, attribute, armor, quest, or effect checks.
- If an explicit item restriction already uses `require_class`, that explicit class rule stays authoritative for that item instead of stacking the bridge auto class-weapon message on top.

### 3. Non-Item Restrictions

The system also supports restricting interactions with the world itself. You can use the same JSON format but specify targets other than `item`.

#### Block Restrictions (`blocks`)
Restricts breaking or right-clicking specific blocks.
```json
{
  "blocks": ["minecraft:obsidian", "minecraft:diamond_ore"],
  "require_level": { "category": "mining", "min": 20 }
}
```

#### Entity Restrictions (`entities`)
Restricts attacking or interacting with specific entities.
```json
{
  "entities": ["minecraft:villager", "minecraft:iron_golem"],
  "require_class": "rogue"
}
```

#### Dimension Gating (`dimensions`)
Restricts entry into entire dimensions. If a player enters without meeting requirements, they are teleported back to the Overworld spawn.
```json
{
  "dimensions": ["minecraft:the_nether"],
  "require_level": { "category": "defense", "min": 15 }
}
```

#### Area/Structure Restrictions (`structures`, `dungeons`)
Restricts movement within specific structures or dungeons. Players are knocked back if they attempt to enter without meeting requirements.
```json
{
  "structures": ["minecraft:bastion_remnant", "minecraft:fortress"],
  "require_class": "paladin"
}
```

### 4. Environmental Gating

Any restriction (Item, Block, etc.) can also include environmental conditions:

- `in_water`: `true` or `false`.
- `time_of_day`: An object with `min` and `max` (0-24000 ticks).

**Example: Nocturnal Weapon**
```json
{
  "item": "minecraft:netherite_sword",
  "time_of_day": { "min": 13000, "max": 23000 }
}
```

---

[< Back to Epic Classes Index](index.md) | [Next: CNPC Class NPCs >](JOB_MASTERS_GUIDE.md)
