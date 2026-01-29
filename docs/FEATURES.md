# Features Reference

Complete list of all features in the Pufferfish Skill Leveling Addon.

---

## Core Features

### Multi-Level Skill Progression
Skills can have any number of levels (1, 2, 3, ... N), each granting cumulative or distinct rewards.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `max_skill_level` | integer | 1 | Maximum levels for this skill |
| `points_per_level` | integer | 1 | Skill points required per level |
| `category_id` | string | — | Category this skill belongs to |
| `loot_mode` | string | — | `"tome_only"` or `"imbue_only"` |
| `merge_description` | boolean | false | Accumulate descriptions across levels |
| `descriptions` | object | — | Level-specific descriptions (e.g., `"1": "Level 1 desc"`) |
| `extra_descriptions` | object | — | Preview text for next level |
| `prerequisite_skills` | array | — | Required skills before unlocking |
| `enchantment_levels` | integer | 0 | XP cost per level for combining tomes |

### Skill Types

| Type | Description |
|------|-------------|
| `puffish_skills:default` | Standard skill type from base mod |
| `puffish_skill_leveling:stackable` | Combines standard unlock rewards + per-level rewards |

### Prerequisite Skills
Require players to have other skills at specific levels:

```json
"prerequisite_skills": [
    { "skill": "basic_warrior", "min_level": 3 },
    { "skill": "combat_training", "min_level": 1, "max_level": 5 }
]
```

### Per-Level Rewards
Define exactly what happens at each level using the custom reward type:

```json
{
    "type": "puffish_skill_leveling:per_level_rewards",
    "data": {
        "skill_id": "your_skill_id",
        "levels": {
            "1": [ /* rewards for level 1 */ ],
            "2": [ /* rewards for level 2 */ ],
            "3": [ /* rewards for level 3 */ ]
        }
    }
}
```

Each level's rewards can include any standard Puffish Skills reward type:
- `puffish_skills:attribute` — Modify player attributes
- `puffish_skills:command` — Execute commands
- `puffish_skills:effect` — Apply potion effects

### Real-Time Attribute Sync
All attribute modifiers (health, damage, speed, armor, etc.) update **instantly** in the player's UI:
- When equipping/unequipping imbued gear
- When leveling up or refunding skills
- No relogging or menu reopening required

---

## Skill Tomes

### Tome of Progression
- **Function**: Opens a GUI to select and advance any skill by 1 level
- **Item ID**: `puffish_skill_leveling:tome_of_progression`

### Tome of Clear Mind
- **Function**: Refunds 1 level of a selected skill
- **Item ID**: `puffish_skill_leveling:tome_of_clear_mind`

### Tome of Greater Clear Mind
- **Function**: Resets a skill completely to level 0
- **Item ID**: `puffish_skill_leveling:tome_of_greater_clear_mind`

### Skill Tome
- **Function**: Grants +1 level to a specific pre-configured skill
- **Item ID**: `puffish_skill_leveling:skill_tome`
- **NBT**: Must contain `CategoryId` and `SkillId`

---

## Equipment Imbuing

### Overview
Skills marked as `"loot_mode": "imbue_only"` cannot be directly learned. Instead, they must be applied to equipment using an anvil.

### How It Works
1. Obtain a Skill Tome for an imbue-only skill
2. Place equipment in an anvil's left slot
3. Place the Skill Tome in the right slot
4. The equipment gains the skill's bonuses when worn

### Behavior
- **Equip**: Skill bonuses apply immediately
- **Unequip**: Skill bonuses are removed immediately
- **Stack**: Multiple imbued items stack their bonuses
- **Persistence**: Imbued data is saved on the item via NBT

---

## Loot Modes

| Mode | Behavior |
|------|----------|
| `tome_only` | Skill can only be learned via Skill Tomes (not purchasable in the skill tree) |
| `imbue_only` | Skill must be imbued onto equipment; cannot be directly learned |
| *(default)* | Standard Puffish Skills behavior |

---

## Commands

All commands require operator permissions.

| Command | Description |
|---------|-------------|
| `/skillleveling info` | Display addon information |
| `/skillleveling get <player> <category> <skill>` | Get current skill level |
| `/skillleveling set <player> <category> <skill> <level>` | Set skill to specific level |
| `/skillleveling advance <player> <category> <skill>` | Advance skill by 1 level |
| `/skillleveling refund one <player> <category> <skill>` | Refund 1 level |
| `/skillleveling refund multiple <player> <category> <skill> <count>` | Refund multiple levels |
| `/skillleveling refund all <player> <category> <skill>` | Reset skill to level 0 |

---

## Compatibility

### Design Philosophy
This addon is designed to be **fully compatible** with the base Pufferfish Skills mod:

- **Non-Invasive**: All features are opt-in via datapack configuration
- **Parallel Storage**: Addon data is stored separately from base mod data
- **Graceful Fallback**: If the addon is removed, base mod functionality continues
- **Mixin Safety**: All mixins use `require = 0` to prevent crashes if methods don't exist

### With Other Mods
The real-time attribute sync works with any mod that registers attributes through Minecraft's standard attribute system.

### Pufferfish Base Skills & Attribute Unlocking
The system is fully compatible with standard Pufferfish Skills that do not use the leveling system:
- **Base Skill Support**: Any standard skill (max level of 1) can be placed into a Skill Tome or imbued onto equipment.
- **Attribute Unlocking**: Imbuing a base Pufferfish skill that grants attribute modifiers will correctly "unlock" and apply those attributes when the item is equipped.
- **Automatic Scaling**: Standard skills default to Level 1, meaning they grant their full benefit immediately when used or equipped via the addon system.

---

*See [Datapack Guide](./DATAPACK_GUIDE.md) for creating custom skill configurations.*
