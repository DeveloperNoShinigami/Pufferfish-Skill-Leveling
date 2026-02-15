# Toggle System & Hybrid Skills

The **Toggle System** (also known as the Trigger System) allows authors to create active abilities that players can enable or disable. When combined with multi-level progression, it creates **Hybrid Skills**.

---

## 🔘 Core Mechanics

A skill becomes a "Toggle Skill" when the `"toggle": true` field is added to its definition.

### 1. Keybind Slots
Each toggle skill is assigned to a **Mastery Key** slot (1-9). Players can configure these keys in their Minecraft "Controls" menu.
- **Field**: `keybind_slot` (Integer).
- **Interaction**: Pressing the key toggles the skill. Players can also **Right-Click** the skill icon in the Skills menu to toggle it.

### 2. Cooldowns
Prevents players from spamming abilities on and off.
- **Field**: `cooldown` (Integer, Ticks).
- **Behavior**: The cooldown triggers when the skill is **disabled**. The play cannot re-enable it until the timer expires.

---

## 🧬 Hybrid Patterns (PLR + Toggle)

The interaction between `PerLevelRewardsReward` (PLR) and `ToggleReward` defines how active and passive progression mix. There are three recommended patterns:

### Pattern 1: Global Toggle (Outer Toggle)
**Use Case**: A skill that provides no benefit unless activated (e.g., "Berserker Rage").

*   **Structure**: The root reward is a `ToggleReward`. It contains a PLR in its `enable_rewards`.
*   **Behavior**:
    *   The player gains no stats/effects until the skill is toggled **ON**.
    *   If the skill is leveled up while **OFF**, the rewards are queued. They trigger for the first time only when the skill is enabled.
*   **Example**:
```json
"berserker_rage": {
  "type": "puffish_skills:default",
  "category_id": "combat",
  "title": "Berserker Rage",
  "description": "Enter a state of reckless fury.",
  "icon": {
    "type": "item",
    "data": { "item": "minecraft:netherite_axe" }
  },
  "toggle": true,
  "keybind_slot": 2,
  "cooldown": 600,
  "max_skill_level": 2,
  "points_per_level": 1,
  "rewards": [
    {
      "type": "puffish_skill_leveling:toggle",
      "data": {
        "enable_rewards": [
          {
            "type": "puffish_skill_leveling:per_level_rewards",
            "data": {
              "skill_id": "berserker_rage",
              "levels": {
                "1": [
                  {
                    "type": "puffish_skills:attribute",
                    "data": {
                      "attribute": "minecraft:generic.attack_damage",
                      "value": 2.0,
                      "operation": "addition"
                    }
                  }
                ],
                "2": [
                  {
                    "type": "puffish_skills:attribute",
                    "data": {
                      "attribute": "minecraft:generic.attack_damage",
                      "value": 4.0,
                      "operation": "addition"
                    }
                  }
                ]
              }
            }
          }
        ]
      }
    }
  ],
  "metadata": {
    "icon": "berserker_rage_icon_id"
  }
}
```

### Pattern 2: Selective Toggle (Inner Toggle / Passive-Active)
**Use Case**: A skill with passive ranks that eventually unlocks an active ability (e.g., "Focus").

*   **Structure**: The root reward is a PLR. A `ToggleReward` is placed *inside* a specific level.
*   **Behavior**:
    *   Lower levels (e.g., Level 1-2) are **Passive** and apply immediately upon leveling.
    *   The high-tier reward (e.g., Level 3) is **Active**. It only applies its effects when the skill is toggled **ON**.
    *   **Configuration**: The root skill must have `"toggle": true` and a `"keybind_slot"` for the keybind to function.
*   **Example (Mining Focus)**:
```json
"mining_focus": {
  "type": "puffish_skills:default",
  "category_id": "utility",
  "title": "Mining Focus",
  "description": "Passive speed, active haste.",
  "icon": {
    "type": "item",
    "data": { "item": "minecraft:diamond_pickaxe" }
  },
  "toggle": true,
  "keybind_slot": 4,
  "max_skill_level": 3,
  "points_per_level": 1,
  "merge_description": true,
  "descriptions": {
    "1": "Level 1: +5% Movement Speed (Passive)",
    "2": "Level 2: +10% Movement Speed (Passive)",
    "3": "Level 3: UNLOCKS DEEP FOCUS (Toggleable Haste)"
  },
  "rewards": [
    {
      "type": "puffish_skill_leveling:per_level_rewards",
      "data": {
        "skill_id": "mining_focus",
        "levels": {
          "1": [
            {
              "type": "puffish_skills:attribute",
              "data": {
                "attribute": "minecraft:generic.movement_speed",
                "value": 0.05,
                "operation": "multiply_base"
              }
            }
          ],
          "2": [
            {
              "type": "puffish_skills:attribute",
              "data": {
                "attribute": "minecraft:generic.movement_speed",
                "value": 0.10,
                "operation": "multiply_base"
              }
            }
          ],
          "3": [
            {
              "type": "puffish_skill_leveling:toggle",
              "data": {
                "enable_rewards": [
                  {
                    "type": "puffish_skill_leveling:effect",
                    "data": {
                      "effect": "minecraft:haste",
                      "amplifier": 0,
                      "duration": -1,
                      "is_protected": true
                    }
                  }
                ]
              }
            }
          ]
        }
      }
    }
  ],
  "metadata": {
    "icon": "mining_focus_icon_id"
  }
}
```

### Pattern 3: Nested PLR Gating (Reliability Fix)
The addon includes internal logic to ensure nested PLRs respect their parent's state.

*   **Initialization Gating**: If a PLR is nested inside a toggle, it initializes with a count of **0** if the toggle is inactive. This prevents players from "burning" level-up rewards (like one-shot commands) before they actually enable the skill.
*   **Toggle Synchronization**: When the parent `ToggleReward` switches state, it sends a specific update to the child PLR to flush or restore its levels.

---

## 🛡️ Advanced Toggle Rewards

### Potion Protection (`puffish_skill_leveling:effect`)
For toggle skills, use the advanced effect reward to ensure reliability.
- **`is_protected: true`**: Recommended for all toggles. Ensures the effect is re-applied if cleared by milk or commands while the skill is ON.
- **`duration: -1`**: Use for infinite duration while the toggle is active.

### Command Execution
Toggle skills can execute commands on both Enable and Disable.
- **Enable**: Run a sound or particle effect when the player activates the reach.
- **Disable**: Clean up entities or send a "Deactivated" message.

---

## 🔄 Auto-Disabling & Loot Modes

Toggle skills interact dynamically with the **Loot System** (Tomes and Imbuements).

*   **Equipment Dependency**: If a toggle skill is `imbue_only` or `both`, and the player unequips the item providing the skill, the skill will **Auto-Disable**.
*   **Safety Reset**: This prevents players from "snapshotting" active effects (like Night Vision) and then switching to better gear while keeping the benefit.
*   **UI Feedback**: When a skill is auto-disabled, the Mastery Slot will darken and the tooltip will notify the player to "Equip item to use".

---

## 🚀 Best Practices
1. **Always use `is_protected`**: Potion effects used in toggles should be protected to prevent accidental removal by players (e.g., drinking milk).
2. **Clear Feedback**: Use `puffish_skills:command` with `tellraw @s` or `title @s actionbar` to send a message when a skill is toggled, so the player knows their input worked.
3. **Recursive Design**: If your toggle skill has multiple levels, use **Pattern 1** (Outer Toggle) for pure active skills, and **Pattern 2** (Inner Toggle) for passive-active hybrids.
