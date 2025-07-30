# Pufferfish Skill Leveling

This mod provides an API to create skill trees via datapacks. Categories and skills can unlock rewards and gain experience.

## Player skill definitions

Skill definitions describe how a skill looks and what it grants. Datapacks may now define stackable skills with extra fields:

- `type` – identifier of the skill type. Defaults to `puffish_skills:default`.
- `max_levels` – how many times the skill can be unlocked.
- `descriptions` – list of tooltip lines shown for each level.
- `extra_descriptions` – list of extra tooltip lines (displayed while holding Shift).
- `parent` – id of another definition to inherit from.
- `merge_description` – when `true`, descriptions and extra descriptions from the parent are appended to this definition's own lists. Defaults to `false` when omitted. You only want to use this when you are adding the total number of rewards together (such as origins skill types, effect, or command even), other wise you will want to write what the progression of the reward would look like.

- Tooltip lines automatically adjust based on how many times the skill has been unlocked. When hovering a skill, the
  entry matching the player's current level is shown, and holding Shift displays the line for the next level (or a final
  message if the skill is maxed).

- `title`, `icon`, `frame`, `size`, and `rewards` work as before.

A basic skill definition might look like this:

```json
{
  "my_skill": {
    "type": "puffish_skills:stackable", //allows a new type that lets you utlize level based skill types and standard rewards.
    "max_levels": 5,
    "title": "Master Miner",
    "icon": { "type": "item", "data": { "item": "minecraft:diamond_pickaxe" } },
    "frame": { /* your frame config */ },
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
    "cost": 1,
    "required_skills": 0,
    "required_points": 0,
    "required_spent_points": 0,
    "required_exclusions": 0,
    "rewards": [
      "puffish_skills:attribute/mining_speed_i"
    ]
  }
}
```

## Per level rewards

The reward registry includes `puffish_skills:per_level_rewards` which lets you specify rewards that depend on the skill's level. The `levels` object maps level numbers to arrays of nested rewards.

```json
{
  "type": "puffish_skills:per_level_rewards", // Skill levels can provide different rewards.
  "data": {
    "skill_id": "stacked_power",
    "max_level": 3,
    "points_per_level": 1,
    "levels": {
      "1": [ { "type": "puffish_skills:attribute", "data": { "attribute": "generic.attack_damage", "value": 1, "operation": "addition" } } ],
      "2": [ { "type": "puffish_skills:effect", "data": { "effect": "speed", "amplifier": 0, "duration": 200 } } ],
      "3": [ { "type": "puffish_skills:attribute", "data": { "attribute": "generic.max_health", "value": 2, "operation": "addition" } } ]
    }
  }
}
```

Each nested reward behaves as if it were a normal reward, but is only active when the player's skill level is at least the specified level.

All active level rewards stack automatically, so unlocking additional levels increases the total bonus without any extra configuration. When a level is unlocked the category loses `points_per_level` points. A player cannot level beyond `max_level` unless they have enough points to pay for the additional levels.


## Example datapack

The `example-skill-level-template.zip` file contains a datapack demonstrating a stackable skill using per-level rewards. Drop the zip into the `datapacks` folder of a world to test the feature.

