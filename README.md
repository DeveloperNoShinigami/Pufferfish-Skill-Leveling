# Pufferfish Skill Leveling

This mod provides an API to create skill trees via datapacks. Categories and skills can unlock rewards and gain experience.

## Player skill definitions

Skill definitions describe how a skill looks and what it grants. Datapacks may now define stackable skills with extra fields:

- `type` – identifier of the skill type. Defaults to `puffish_skill_leveling:default`.
- `max_levels` – how many times the skill can be unlocked.
- `descriptions` – list of tooltip lines shown for each level.
- `extra_descriptions` – list of extra tooltip lines (displayed while holding Shift).
- `merge_description` – when `true`, descriptions and extra descriptions accumulate from earlier levels instead of replacing them. Defaults to `false` when omitted.

- Tooltip lines automatically adjust based on how many times the skill has been unlocked. When hovering a skill, the
  entry matching the player's current level is shown, and holding Shift displays the line for the next level (or a final
  message if the skill is maxed).

- `title`, `icon`, `frame`, `size`, and `rewards` work as before.

A basic skill definition might look like this:

```json
{
  "my_skill": {
    "type": "puffish_skill_leveling:stackable", // unlock the skill multiple times
    "max_levels": 5,
    "title": "Master Miner",
    "icon": { "type": "item", "data": { "item": "minecraft:diamond_pickaxe" } },
    "size": 1.0,
    "descriptions": [
      "Current: +5% mining speed",
      "Current: +10% mining speed",
      "Current: +15% mining speed",
      "Current: +20% mining speed",
      "Current: +25% mining speed"
    ],
    "extra_descriptions": [
      "Next: +10% mining speed",
      "Next: +15% mining speed",
      "Next: +20% mining speed",
      "Next: +25% mining speed",
      "— MAXED OUT —"
    ],
    "rewards": [
      {
        "type": "puffish_skill_leveling:per_level_rewards",
        "data": {
          "skill_id": "my_skill",
          "max_level": 5,
          "points_per_level": 1,
          "levels": {
            "1": [ "puffish_skill_leveling:attribute/mining_speed_i" ],
            "2": [ "puffish_skill_leveling:attribute/mining_speed_i" ],
            "3": [ "puffish_skill_leveling:attribute/mining_speed_i" ],
            "4": [ "puffish_skill_leveling:attribute/mining_speed_i" ],
            "5": [ "puffish_skill_leveling:attribute/mining_speed_i" ]
          }
        }
      }
    ]
  }
}
```

## Per level rewards

The reward registry includes `puffish_skill_leveling:per_level_rewards` which lets you specify rewards that depend on the skill's level. The `levels` object maps level numbers to arrays of nested rewards.

```json
{
  "type": "puffish_skill_leveling:per_level_rewards", // Skill levels can provide different rewards.
  "data": {
    "skill_id": "stacked_power",
    "max_level": 3,
    "points_per_level": 1,
    "levels": {
      "1": [ { "type": "puffish_skill_leveling:attribute", "data": { "attribute": "generic.attack_damage", "value": 1, "operation": "addition" } } ],
      "2": [ { "type": "puffish_skill_leveling:effect", "data": { "effect": "speed", "amplifier": 0, "duration": 200 } } ],
      "3": [ { "type": "puffish_skill_leveling:attribute", "data": { "attribute": "generic.max_health", "value": 2, "operation": "addition" } } ]
    }
  }
}
```

Each nested reward behaves as if it were a normal reward, but it only activates once the player has reached the listed level.

Rewards remain active after being unlocked. If you include the same reward in multiple level entries it will be applied repeatedly, effectively stacking the effect. When a level is unlocked the category loses `points_per_level` points and players cannot level past `max_level` unless they have enough points to pay for each step.


## Example datapack

An example datapack is provided as `example-skill-level-template.zip`. Drop this file (or its extracted folder) into your world's `datapacks` directory and reload the world. The JSON files inside illustrate how to configure a stackable skill with per-level rewards that accumulate as you unlock each tier.

