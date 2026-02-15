# Skill Imbuement System

The Skill Imbuement system dynamically applies random skills to equipment and Skill Charms when they are generated as loot. This system includes advanced filtering and scaling rules to control power progression.

## Configuration

The imbuement logic is configured through a central JSON file.

### Path: `data/puffish_skill_leveling/skill_imbue_loot/config.json`

### Core Features

#### 1. Dimension Overrides
Configure unique imbument rules for different worlds.

```json
"dimension_overrides": {
  "minecraft:overworld": {
    "imbue_chance": 0.2,
    "max_skills": 1,
    "min_level": 1,
    "max_level": 3
  },
  "minecraft:the_nether": {
    "imbue_chance": 0.5,
    "max_skills": 2,
    "min_level": 3,
    "max_level": 5
  }
}
```

- `imbue_chance`: Probability that a valid item will receive ANY skills.
- `max_skills`: Maximum number of skills per item (randomly 1 to N).
- `min_level` / `max_level`: The range of levels for the chosen skills.

#### 2. Distance Scaling
Allows equipment to get progressively stronger as players travel further from spawn.

```json
"distance_scaling": {
  "enabled": true,
  "origin": [0, 0],
  "brackets": [
    { "distance": 1000, "max_level": 1 },
    { "distance": 5000, "max_level": 3 },
    { "distance": 10000, "max_level": 5 }
  ]
}
```
- If an entity dies at distance `X`, its skill levels are capped at the `max_level` of the first bracket it falls within.

#### 3. Category Settings
Fine-tune rules for specific item types.

```json
"category_settings": {
  "bow": {
    "imbue_chance": 1.0,
    "max_skills": 2
  }
}
```

#### 4. Exclusion Groups
Prevent conflicting or overpowered combinations of skills on a single item.

```json
"exclusion_groups": [
  {
    "types": ["offense:sharpened_edge", "offense:blunt_force"]
  }
]
```
- If an item already has `sharpened_edge`, it will never receive `blunt_force`.

#### 5. The "any" Wildcard
To avoid listing every single skill manually, you can use the special `"any"` keyword.

```json
"global": [
  { "skill": "any", "weight": 10 }
]
```
- **Expansion Logic**: When the system encounters `"any"`, it automatically expands it into a list of **all skills** in the mod that have `loot_mode` set to `"both"` or `"imbue_only"`.
- **Weight Consistency**: Each expanded skill inherits the `weight` assigned to the `"any"` entry.
- **Future-Proof**: Any new skills added to the mod or datapacks will be automatically included in loot drops if you use this wildcard.

## Supported Categories

You can define skill pools for specific categories by using the category ID as a top-level key in the config JSON.

| Category ID | Description |
| :--- | :--- |
| `sword` | Any item extending `SwordItem`. |
| `bow` | Any item extending `BowItem`. |
| `crossbow` | Any item extending `CrossbowItem`. |
| `pickaxe` | Any item extending `PickaxeItem`. |
| `axe` | Any item extending `AxeItem`. |
| `shovel` | Any item extending `ShovelItem`. |
| `hoe` | Any item extending `HoeItem`. |
| `helmet` | Items in the HEAD equipment slot. |
| `chestplate` | Items in the CHEST equipment slot. |
| `leggings` | Items in the LEGS equipment slot. |
| `boots` | Items in the FEET equipment slot. |
| `armor` | Any item extending `ArmorItem`. |
| `shield` | Any item extending `ShieldItem`. |
| `trident` | Any item extending `TridentItem`. |
| `skill_charm` | The mod's own `Skill Charm` item. |

## Filtering (Whitelists & Blacklists)

- `item_blacklist`: List of item IDs (e.g., `minecraft:wooden_sword`) that will **never** be imbued.
- `item_whitelist`: If not empty, **only** items in this list will be imbued.
- `loot_table_whitelist`: If not empty, imbuement will only trigger for items originating from these specific tables.
