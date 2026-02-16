# Universal Loot System

Inject skill-related items (Skill Tomes, Skill Charms, and more) into any chest or mob drop loot table. This system lets you control where, how often, and at what level players find skill items throughout the world.

**Config location:** `data/puffish_skill_leveling/loot_modifiers/universal_loot.json`

---

## Table of Contents

- [How It Works](#how-it-works)
- [Config Structure](#config-structure)
- [Loot Groups](#-loot-groups)
- [Targets](#-targets)
- [Entries](#-entries)
- [Entry Types](#-entry-types)
- [Full Example Config](#-full-example-config)
- [Tips & Troubleshooting](#-tips--troubleshooting)

---

## How It Works

The Universal Loot System hooks into Minecraft's loot generation pipeline. Every time a chest opens or a mob dies, the system:

1. Checks if the loot table matches any configured **targets**.
2. If a target matches, rolls the group's **chance** to see if injection occurs.
3. Rolls a random number between **min** and **max** rolls.
4. For each roll, picks a weighted-random **entry** from the group.
5. Generates the item and adds it to the loot.

This is separate from the [Skill Imbuement System](./Skill_Imbuement_System.md), which imbues skills directly onto equipment. This system **adds new items** to loot pools.

---

## Config Structure

The config has two top-level arrays:

```json
{
    "type": "puffish_skill_leveling:universal_loot",
    "conditions": [],
    "chest_injection_groups": [ ... ],
    "entity_drop_groups": [ ... ]
}
```

| Section | Purpose |
|---------|---------|
| `chest_injection_groups` | Add items to chest loot tables (dungeons, villages, temples, etc.) |
| `entity_drop_groups` | Add items to mob drop loot tables (zombies, bosses, etc.) |

Both arrays use the same group structure — the only difference is which loot tables they target.

---

## 📦 Loot Groups

Each group defines a set of targets, a shared chance, roll count, and a pool of weighted entries.

```json
{
    "targets": [ "minecraft:chests/simple_dungeon" ],
    "chance": 0.4,
    "rolls": { "min": 1, "max": 2 },
    "entries": [ ... ]
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `targets` | string[] | — | Loot table IDs to inject into. |
| `chance` | float | `1.0` | Probability (0.0–1.0) that this group triggers at all. |
| `rolls` | object | `{ "min": 1, "max": 1 }` | How many items to generate per trigger. |
| `rolls.min` | integer | `1` | Minimum rolls. |
| `rolls.max` | integer | `1` | Maximum rolls (inclusive). |

**How rolls work:** If `chance` succeeds, the system rolls a random number between `min` and `max`. Each roll picks one weighted-random entry from the entries array.

---

## 🎯 Targets

Targets are loot table identifiers. They use Minecraft's `namespace:path` format.

### Chest Targets (common examples)

| Target | Location |
|--------|----------|
| `minecraft:chests/simple_dungeon` | Dungeon chests |
| `minecraft:chests/abandoned_mineshaft` | Mineshaft chests |
| `minecraft:chests/stronghold_library` | Stronghold libraries |
| `minecraft:chests/stronghold_corridor` | Stronghold corridors |
| `minecraft:chests/desert_pyramid` | Desert temples |
| `minecraft:chests/jungle_temple` | Jungle temples |
| `minecraft:chests/shipwreck_treasure` | Shipwreck treasures |
| `minecraft:chests/nether_bridge` | Nether fortress chests |
| `minecraft:chests/bastion_treasure` | Bastion remnant treasure |
| `minecraft:chests/end_city_treasure` | End city chests |
| `minecraft:chests/village/village_weaponsmith` | Village weaponsmith |
| `minecraft:chests/village/village_toolsmith` | Village toolsmith |
| `minecraft:chests/village/village_armorer` | Village armorer |
| `minecraft:chests/village/village_temple` | Village temple |

### Entity Drop Targets (common examples)

| Target | Mob |
|--------|-----|
| `minecraft:entities/zombie` | Zombies |
| `minecraft:entities/skeleton` | Skeletons |
| `minecraft:entities/creeper` | Creepers |
| `minecraft:entities/spider` | Spiders |
| `minecraft:entities/enderman` | Endermen |
| `minecraft:entities/piglin` | Piglins |
| `minecraft:entities/pillager` | Pillagers |
| `minecraft:entities/elder_guardian` | Elder Guardians |
| `minecraft:entities/evoker` | Evokers |
| `minecraft:entities/ravager` | Ravagers |
| `minecraft:entities/warden` | Wardens |
| `minecraft:entities/wither` | Withers |
| `minecraft:entities/ender_dragon` | Ender Dragons |

You can target any valid loot table — including modded ones.

---

## 📝 Entries

Each entry describes one possible item that can be generated per roll.

```json
{
    "type": "skill_tome",
    "name": "puffish_skill_leveling:skill_tome",
    "weight": 80,
    "chance": 1.0,
    "min_level": 1,
    "max_level": 3
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `type` | string | `"item"` | Entry type — see [Entry Types](#-entry-types). |
| `name` | string | — | Item registry ID. |
| `weight` | integer | `1` | Relative probability within the group (higher = more likely). |
| `chance` | float | `1.0` | Per-entry drop chance (rolled after the entry is selected). |
| `min_level` | integer | `1` | Minimum level for `skill_tome` entries. |
| `max_level` | integer | `1` | Maximum level for `skill_tome` entries. |
| `nbt` | string | — | Optional NBT data string. |
| `skill` | string | — | Specific skill ID for `skill_tome` entries. |
| `skills` | array | `[]` | Weighted skill pool for `skill_tome` entries. |

---

## 🔧 Entry Types

### `"item"` — Regular Item

Drops a vanilla or modded item:

```json
{
    "type": "item",
    "name": "puffish_skill_leveling:skill_charm",
    "weight": 20
}
```

### `"skill_tome"` — Random Skill Tome

Generates a Skill Tome for a random skill. The skill is chosen from all registered skills with valid `loot_mode`, at a random level between `min_level` and `max_level`.

```json
{
    "type": "skill_tome",
    "name": "puffish_skill_leveling:skill_tome",
    "weight": 100,
    "min_level": 1,
    "max_level": 3
}
```

### `"skill_tome"` with Specific Skill

Force the tome to always be for a particular skill:

```json
{
    "type": "skill_tome",
    "name": "puffish_skill_leveling:skill_tome",
    "skill": "warrior_strength",
    "weight": 50,
    "min_level": 1,
    "max_level": 2
}
```

### `"skill_tome"` with Weighted Skill Pool

Choose from a specific set of skills with custom weights:

```json
{
    "type": "skill_tome",
    "name": "puffish_skill_leveling:skill_tome",
    "weight": 100,
    "min_level": 1,
    "max_level": 3,
    "skills": [
        { "skill": "warrior_strength", "weight": 40 },
        { "skill": "vitality", "weight": 30 },
        { "skill": "arcane_striker", "weight": 30 }
    ]
}
```

---

## 📋 Full Example Config

A complete working config covering common mob tiers and chest locations:

```json
{
    "type": "puffish_skill_leveling:universal_loot",
    "conditions": [],
    "entity_drop_groups": [
        {
            "targets": [
                "minecraft:entities/zombie",
                "minecraft:entities/skeleton",
                "minecraft:entities/creeper",
                "minecraft:entities/spider"
            ],
            "chance": 0.1,
            "rolls": { "min": 1, "max": 1 },
            "entries": [
                {
                    "type": "skill_tome",
                    "name": "puffish_skill_leveling:skill_tome",
                    "weight": 100,
                    "min_level": 1,
                    "max_level": 1
                }
            ]
        },
        {
            "targets": [
                "minecraft:entities/enderman",
                "minecraft:entities/piglin",
                "minecraft:entities/pillager"
            ],
            "chance": 0.25,
            "rolls": { "min": 1, "max": 1 },
            "entries": [
                {
                    "type": "skill_tome",
                    "name": "puffish_skill_leveling:skill_tome",
                    "weight": 80,
                    "min_level": 1,
                    "max_level": 2
                },
                {
                    "type": "item",
                    "name": "puffish_skill_leveling:skill_charm",
                    "weight": 20
                }
            ]
        },
        {
            "targets": [
                "minecraft:entities/elder_guardian",
                "minecraft:entities/ravager",
                "minecraft:entities/evoker"
            ],
            "chance": 0.5,
            "rolls": { "min": 1, "max": 2 },
            "entries": [
                {
                    "type": "skill_tome",
                    "name": "puffish_skill_leveling:skill_tome",
                    "weight": 70,
                    "min_level": 1,
                    "max_level": 3
                },
                {
                    "type": "item",
                    "name": "puffish_skill_leveling:skill_charm",
                    "weight": 15
                },
                {
                    "type": "item",
                    "name": "puffish_skill_leveling:blank_tome",
                    "weight": 15
                }
            ]
        }
    ],
    "chest_injection_groups": [
        {
            "targets": [
                "minecraft:chests/village/village_weaponsmith",
                "minecraft:chests/village/village_toolsmith",
                "minecraft:chests/village/village_armorer"
            ],
            "chance": 0.2,
            "rolls": { "min": 1, "max": 1 },
            "entries": [
                {
                    "type": "skill_tome",
                    "name": "puffish_skill_leveling:skill_tome",
                    "weight": 100,
                    "min_level": 1,
                    "max_level": 2
                }
            ]
        },
        {
            "targets": [
                "minecraft:chests/simple_dungeon",
                "minecraft:chests/abandoned_mineshaft",
                "minecraft:chests/stronghold_library",
                "minecraft:chests/desert_pyramid",
                "minecraft:chests/jungle_temple"
            ],
            "chance": 0.4,
            "rolls": { "min": 1, "max": 2 },
            "entries": [
                {
                    "type": "skill_tome",
                    "name": "puffish_skill_leveling:skill_tome",
                    "weight": 80,
                    "min_level": 1,
                    "max_level": 3
                },
                {
                    "type": "item",
                    "name": "puffish_skill_leveling:skill_charm",
                    "weight": 20
                }
            ]
        },
        {
            "targets": [
                "minecraft:chests/end_city_treasure",
                "minecraft:chests/bastion_treasure"
            ],
            "chance": 0.6,
            "rolls": { "min": 1, "max": 3 },
            "entries": [
                {
                    "type": "skill_tome",
                    "name": "puffish_skill_leveling:skill_tome",
                    "weight": 60,
                    "min_level": 2,
                    "max_level": 5
                },
                {
                    "type": "item",
                    "name": "puffish_skill_leveling:skill_charm",
                    "weight": 20
                },
                {
                    "type": "item",
                    "name": "puffish_skill_leveling:sigil_of_imbuement",
                    "weight": 10
                },
                {
                    "type": "item",
                    "name": "puffish_skill_leveling:blank_tome",
                    "weight": 10
                }
            ]
        }
    ]
}
```

**Design rationale:**
- Common mobs: Low chance (10%), Level 1 tomes only — frequent encounters, rare drops.
- Mid-tier mobs: Moderate chance (25%), Level 1–2, chance for Skill Charms.
- Mini-bosses: High chance (50%), Level 1–3, wider item variety.
- Village chests: Low chance (20%), basic tomes — early game finds.
- Dungeon chests: Moderate chance (40%), better tomes and charms.
- End-game chests: High chance (60%), high-level tomes, sigils, and blanks.

---

## 💡 Tips & Troubleshooting

### Design Tips

- **Tier your drops.** Common mobs should give Level 1 tomes rarely. Bosses should give Level 3–5 tomes more often.
- **Mix entry types.** Don't just drop tomes — include Skill Charms, Blank Tomes, and Sigils of Imbuement for variety.
- **Use multiple groups for the same target.** You can have two groups targeting `minecraft:entities/wither` — one for rare tomes and one for guaranteed crafting materials.
- **Gate by world progression.** Only list Nether chests for Level 3+ and End chests for Level 5.

### Nothing is dropping

1. Verify the `type` field is `"puffish_skill_leveling:universal_loot"`.
2. Check that your `targets` use the correct loot table path format (e.g., `minecraft:entities/zombie`, not `minecraft:zombie`).
3. Ensure `chance` is greater than 0.
4. Check that at least one entry has `weight > 0`.
5. Run `/reload` after making changes.

### Tomes always show Level 1

- Check `max_level` on the entry — it defaults to 1 if not specified.
- Level is clamped to the skill's `max_skill_level`, so a Level 5 roll on a 3-level skill gives Level 3.

### Too many drops

- Lower the group `chance`.
- Reduce `rolls.max`.
- Lower individual entry `weight` values.

### Testing

Use `/loot give @s loot <target>` to test specific loot tables:
```
/loot give @s loot minecraft:chests/end_city_treasure
/loot give @s kill entity @e[type=zombie,limit=1,sort=nearest]
```

---

*For imbuing skills onto equipment in loot, see [Skill Imbuement System](./Skill_Imbuement_System.md). For the full definition schema, see [Datapack Guide](./DATAPACK_GUIDE.md).*
