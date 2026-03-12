# Toggle System

[< Back to Core Index](index.md) | [Next: Skill Master System >](Skill_Master_System.md)

---

Complete guide to creating skills that players can switch on and off. Covers all three toggle types — from simple on/off abilities to complex hybrid skills that scale with level.

---

## Table of Contents

- [Overview](#overview)
- [Toggle Types at a Glance](#toggle-types-at-a-glance)
- [Pure Toggle](#-pure-toggle)
- [Basic Toggle](#-basic-toggle)
- [Hybrid Patterns](#-hybrid-patterns)
  - [Global Toggle (Outer)](#pattern-1-global-toggle-outer)
  - [Selective Toggle (Inner)](#pattern-2-selective-toggle-inner)
  - [Nested PLR Gating](#pattern-3-nested-plr-gating)
- [Toggle Fields Reference](#-toggle-fields-reference)
- [The ToggleReward Type](#-the-togglereward-type)
- [Potion Protection](#-potion-protection)
- [Cooldowns](#-cooldowns)
- [Auto-Disabling](#-auto-disabling)
- [Best Practices](#-best-practices)

---

## Overview

Any skill with `"toggle": true` becomes an active ability. Players toggle it using:
- **Left-click** in the skill tree
- **Keybind** (when `keybind_slot` is assigned, keys 1–9)

The toggle state is stored server-side and persists across disconnects and server restarts.

---

## Toggle Types at a Glance

| Type | `toggle` | `max_skill_level` | Levels? | Example |
|------|----------|-------------------|---------|---------|
| **Pure Toggle** | `true` | Omitted (defaults to 0) | No | Simple on/off — Berserker Rage |
| **Basic Toggle** | `true` | `1` | 1 (for imbuing/loot) | Night Vision with loot and imbue support |
| **Hybrid Toggle** | `true` | `2+` | Yes | Berserker Stance — scales with levels |

---

## ⚡ Pure Toggle

The simplest form. No levels, no loot integration — just click to activate.

```json
"berserker_rage": {
    "category_id": "combat",
    "title": "Berserker Rage",
    "description": "Enter a state of reckless fury.",
    "icon": { "type": "item", "data": { "item": "minecraft:netherite_axe" } },
    "toggle": true,
    "keybind_slot": 2,
    "cooldown": 600,
    "rewards": [
        {
            "type": "puffish_skill_leveling:toggle",
            "data": {
                "enable_rewards": [
                    {
                        "type": "puffish_skill_leveling:effect",
                        "data": {
                            "effect": "minecraft:strength",
                            "amplifier": 1,
                            "duration": -1,
                            "persistent": true,
                            "is_protected": true
                        }
                    },
                    {
                        "type": "puffish_skills:attribute",
                        "data": {
                            "attribute": "generic.attack_speed",
                            "value": 0.2,
                            "operation": "multiply_base"
                        }
                    },
                    {
                        "type": "puffish_skills:command",
                        "data": {
                            "command": "tellraw @s {\"text\":\"Rage unleashed!\",\"color\":\"red\"}"
                        }
                    }
                ],
                "disable_rewards": [
                    {
                        "type": "puffish_skills:command",
                        "data": {
                            "command": "tellraw @s {\"text\":\"Rage fades...\",\"color\":\"gray\"}"
                        }
                    }
                ]
            }
        }
    ],
    "metadata": {}
}
```

Key points:
- No `max_skill_level` — defaults to 0, making it a pure binary switch.
- No `loot_mode` — cannot be found as loot or imbued to equipment.
- `points_per_level` controls the purchase cost in the skill tree.

---

## 🔦 Basic Toggle

Like a pure toggle, but with `max_skill_level: 1` and `loot_mode`, enabling loot drops and equipment imbuing.

```json
"night_vision": {
    "category_id": "utility",
    "title": "Night Vision",
    "description": "Toggle Night Vision (Keybind 1)",
    "icon": { "type": "item", "data": { "item": "minecraft:ender_eye" } },
    "loot_mode": "both",
    "max_skill_level": 1,
    "toggle": true,
    "keybind_slot": 1,
    "imbuement_cost": 5,
    "rewards": [
        {
            "type": "puffish_skill_leveling:toggle",
            "data": {
                "enable_rewards": [
                    {
                        "type": "puffish_skill_leveling:effect",
                        "data": {
                            "effect": "minecraft:night_vision",
                            "duration": -1,
                            "amplifier": 0,
                            "persistent": true,
                            "is_protected": true
                        }
                    },
                    {
                        "type": "puffish_skills:command",
                        "data": {
                            "command": "tellraw @s {\"text\":\"Night Vision Enabled\",\"color\":\"green\"}"
                        }
                    }
                ],
                "disable_rewards": [
                    {
                        "type": "puffish_skills:command",
                        "data": {
                            "command": "tellraw @s {\"text\":\"Night Vision Disabled\",\"color\":\"gray\"}"
                        }
                    }
                ]
            }
        }
    ],
    "metadata": {}
}
```

Key points:
- `max_skill_level: 1` — the skill has exactly one level, enabling Skill Tome generation and imbuing.
- `loot_mode: "both"` — tomes can appear in chests and the skill can be imbued to gear.
- `imbuement_cost: 5` — costs 5 XP to imbue manually.

---

## 🔀 Hybrid Patterns

Hybrid toggles combine levels with toggle behavior. Three structural patterns control when the toggle activates relative to levels.

### Pattern 1: Global Toggle (Outer)

The `ToggleReward` wraps a `per_level_rewards` block. The **entire reward output** is toggled — when the skill is disabled, nothing applies regardless of level.

```
Reward Structure:
└── ToggleReward (outer wrapper)
    └── enable_rewards:
        └── PerLevelRewards (levels 1-5)
```

```json
"berserker_stance": {
    "category_id": "combat",
    "title": "Berserker Stance",
    "description": "Enter a state of bloodlust. Power increases with rank.",
    "icon": { "type": "item", "data": { "item": "minecraft:iron_sword" } },
    "max_skill_level": 5,
    "points_per_level": 1,
    "toggle": true,
    "keybind_slot": 3,
    "cooldown": 200,
    "merge_description": true,
    "descriptions": {
        "1": "Rank 1: +1 Attack Damage when active.",
        "2": "Rank 2: +2 Attack Damage when active.",
        "3": "Rank 3: +3 Attack Damage when active.",
        "4": "Rank 4: +4 Attack Damage when active.",
        "5": "Rank 5: +5 Attack Damage and Berserk Focus."
    },
    "rewards": [
        {
            "type": "puffish_skill_leveling:toggle",
            "data": {
                "enable_rewards": [
                    {
                        "type": "puffish_skill_leveling:per_level_rewards",
                        "data": {
                            "skill_id": "berserker_stance",
                            "levels": {
                                "1": [{ "type": "puffish_skills:attribute", "data": { "attribute": "minecraft:generic.attack_damage", "operation": "addition", "value": 1.0 } }],
                                "2": [{ "type": "puffish_skills:attribute", "data": { "attribute": "minecraft:generic.attack_damage", "operation": "addition", "value": 2.0 } }],
                                "3": [{ "type": "puffish_skills:attribute", "data": { "attribute": "minecraft:generic.attack_damage", "operation": "addition", "value": 3.0 } }],
                                "4": [{ "type": "puffish_skills:attribute", "data": { "attribute": "minecraft:generic.attack_damage", "operation": "addition", "value": 4.0 } }],
                                "5": [{ "type": "puffish_skills:attribute", "data": { "attribute": "minecraft:generic.attack_damage", "operation": "addition", "value": 5.0 } }]
                            },
                            "on_disable_levels": {
                                "1": [
                                    { "type": "puffish_skills:command", "data": { "command": "title @s actionbar {\"text\":\"Stance Broken\",\"color\":\"red\"}" } }
                                ]
                            }
                        }
                    }
                ],
                "disable_rewards": [
                    { "type": "puffish_skills:command", "data": { "command": "playsound minecraft:entity.iron_golem.damage player @s ~ ~ ~ 1 0.5" } }
                ]
            }
        }
    ],
    "metadata": {}
}
```

**Behavior:**
- Player buys levels → nothing happens until they toggle ON.
- Toggle ON → level rewards activate (e.g., Level 3 gives +3 attack damage).
- Toggle OFF → all level rewards deactivate, `disable_rewards` fires, `on_disable_levels` fires.
- Buying a new level while toggled ON immediately activates the new rewards.

### Pattern 2: Selective Toggle (Inner)

The `per_level_rewards` is the outer wrapper, and only specific levels contain `ToggleReward` blocks. Lower levels apply passively, and the toggle only gates certain rewards.

```
Reward Structure:
└── PerLevelRewards (levels 1-5)
    ├── Level 1-2: passive attribute rewards
    └── Level 3+: ToggleReward (wraps additional bonuses)
```

```json
"rewards": [
    {
        "type": "puffish_skill_leveling:per_level_rewards",
        "data": {
            "skill_id": "shadow_step",
            "levels": {
                "1": [
                    { "type": "puffish_skills:attribute", "data": { "attribute": "generic.movement_speed", "value": 0.02, "operation": "multiply_base" } }
                ],
                "2": [
                    { "type": "puffish_skills:attribute", "data": { "attribute": "generic.movement_speed", "value": 0.04, "operation": "multiply_base" } }
                ],
                "3": [
                    { "type": "puffish_skills:attribute", "data": { "attribute": "generic.movement_speed", "value": 0.06, "operation": "multiply_base" } },
                    {
                        "type": "puffish_skill_leveling:toggle",
                        "data": {
                            "enable_rewards": [
                                { "type": "puffish_skill_leveling:effect", "data": { "effect": "minecraft:invisibility", "duration": -1, "amplifier": 0, "persistent": true, "is_protected": true } }
                            ],
                            "disable_rewards": []
                        }
                    }
                ]
            }
        }
    }
]
```

**Behavior:**
- Levels 1–2 are **always active** (passive speed boost).
- Level 3 adds a toggleable invisibility effect that can be switched on/off independently.
- The tooltip shows "Ready at Level 3" until the player reaches that level.

### Pattern 3: Nested PLR Gating

The most advanced pattern. A `ToggleReward` wraps a `per_level_rewards` block (like Pattern 1), but the nested PLR is marked as `nested: true` internally. This allows the toggle to gate ALL rewards while the levels scale.

This is functionally identical to Pattern 1 but uses explicit nesting markers. Pattern 1 is the recommended approach for new skills — the addon handles nesting detection automatically.

---

## 📋 Toggle Fields Reference

These fields go in the skill definition (in `definitions.json`):

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `toggle` | boolean | Yes | Set to `true` to enable toggling. |
| `keybind_slot` | integer | No | Assigns a hotkey (1–9) for quick access. |
| `cooldown` | integer | No | Ticks before the skill can be re-enabled after disabling (20 ticks = 1 second). |

---

## 🔧 The ToggleReward Type

The `puffish_skill_leveling:toggle` reward type wraps other rewards in an on/off container.

```json
{
    "type": "puffish_skill_leveling:toggle",
    "data": {
        "enable_rewards": [ ... ],
        "disable_rewards": [ ... ]
    }
}
```

| Field | Description |
|-------|-------------|
| `enable_rewards` | Array of rewards applied when the toggle is turned ON. Removed when turned OFF. |
| `disable_rewards` | Array of rewards fired **once** when the toggle is turned OFF. Think of these as "on-deactivate" triggers. |

### enable_rewards
Supports **all** reward types:
- `puffish_skills:attribute` — added on enable, removed on disable.
- `puffish_skill_leveling:effect` — applied on enable, cleared on disable.
- `puffish_skills:command` — executed on enable only (not on disable).
- `puffish_skill_leveling:per_level_rewards` — nested level-scaled rewards.

### disable_rewards
Only trigger once when the player toggles OFF. Typically used for:
- Visual/audio feedback (commands, particles, sounds).
- Temporary debuffs as a penalty for deactivating.

---

## 🧪 Potion Protection

If a toggle skill grants potion effects, **always** use the addon's enhanced effect type with protection:

```json
{
    "type": "puffish_skill_leveling:effect",
    "data": {
        "effect": "minecraft:night_vision",
        "duration": -1,
        "amplifier": 0,
        "persistent": true,
        "is_protected": true
    }
}
```

### Why is this important?

Without `is_protected: true`:
- **Drinking milk** removes the effect, but the toggle stays ON — the player sees "Enabled" with no actual effect.
- **Dying** clears all effects, but the toggle persists — same desync.

With `is_protected: true`:
- The addon automatically re-applies the effect after milk, death, or any other clear event.
- The effect stays in sync with the toggle state.

### Field Reference

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `effect` | string | **Yes** | — | Potion effect ID (e.g., `"minecraft:night_vision"`). |
| `duration` | integer | **Yes** | — | Duration in ticks. Use `-1` for infinite. |
| `amplifier` | integer | No | `0` | Effect level (0 = Level I). |
| `persistent` | boolean | No | `false` | Survives death when `true`. |
| `is_protected` | boolean | No | `false` | Re-applies automatically if cleared by milk/death. |

---

## ⏱ Cooldowns

Prevent toggle spam with the `cooldown` field. The value is in **ticks** (20 ticks = 1 second).

```json
"cooldown": 600
```

This means after disabling the skill, the player must wait 30 seconds before enabling it again.

### Cooldown Behavior

1. Player toggles OFF the skill.
2. A countdown begins (600 ticks = 30 seconds).
3. The skill tooltip shows "ON COOLDOWN: Xs remaining."
4. After the cooldown expires, the player can toggle ON again.

Cooldowns only apply **after disabling**. The first activation has no delay.

---

## ♻ Auto-Disabling

The addon automatically disables toggle skills in certain situations:

### On Death
When a player dies, all active toggles are disabled. They can re-enable them after respawning (subject to cooldowns).

### On Skill Loss
If a toggle skill requires a level (hybrid or basic toggle) and the player's level drops to 0 (via refunding, admin command, or cleansing), the toggle is automatically disabled.

### On Loot/Imbue Loss
For basic toggles with `loot_mode`, if the player loses the skill entirely (level goes to 0), the toggle is force-disabled during the next reward refresh cycle.

---

## 🧠 Best Practices

### Always Use Protected Effects
Every potion effect inside a toggle should have `"is_protected": true`. Unprotected effects will desync.

### Use Infinite Duration for Toggle Effects
Set `"duration": -1` for effects that should last as long as the toggle is active. Finite durations will expire independently of the toggle state.

### Feedback on Enable AND Disable
Use `puffish_skills:command` in both `enable_rewards` and `disable_rewards` to give clear visual feedback:
```json
"enable_rewards": [
    { "type": "puffish_skills:command", "data": { "command": "title @s actionbar {\"text\":\"Berserker Mode ON\",\"color\":\"red\"}" } }
],
"disable_rewards": [
    { "type": "puffish_skills:command", "data": { "command": "title @s actionbar {\"text\":\"Berserker Mode OFF\",\"color\":\"gray\"}" } }
]
```

### Use Cooldowns for Powerful Abilities
Toggles that grant significant combat advantages (Strength, Resistance, Speed) should have cooldowns. A 10–30 second cooldown (200–600 ticks) prevents abuse without feeling punishing.

### Pattern Choice Guide

| Scenario | Pattern | Why |
|----------|---------|-----|
| Simple on/off ability | Pure Toggle | Minimal config, no levels needed. |
| On/off with loot/imbue support | Basic Toggle | `max_skill_level: 1` enables tome generation. |
| "Everything scales with toggle" | Global Toggle (Outer) | One switch controls all rewards. |
| "Passive base, optional active at high rank" | Selective Toggle (Inner) | Mix passive and active rewards. |

### Keybind Slot Tips
- Players can assign up to 9 toggle keybinds across all categories.
- Assign frequently used toggles to slots 1–3 for easy access.
- Keybinds work globally — not just while the skill screen is open.

---

*For full definition schema, see [Datapack Guide](./DATAPACK_GUIDE.md). For per-level rewards details, see [Datapack Guide — Point F](./DATAPACK_GUIDE.md#-point-f-per-level-rewards).*

---

[< Back to Core Index](index.md) | [Next: Skill Master System >](Skill_Master_System.md)
