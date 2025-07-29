# Pufferfish Skill Leveling

This mod provides an API to create skill trees via datapacks. Categories and skills can unlock rewards and gain experience.

## Player skill definitions

Skill definitions describe how a skill looks and what it grants. Datapacks may now define stackable skills with extra fields:

- `type` – identifier of the skill type. Defaults to `puffish_skills:default`.
- `max_levels` – how many times the skill can be unlocked.
- `descriptions` – list of tooltip lines shown for each level.
- `extra_descriptions` – list of extra tooltip lines (displayed while holding Shift).
- `title`, `icon`, `frame`, `size`, and `rewards` work as before.

A basic skill definition might look like this:

```json
{
  "my_skill": {
    "type": "mystackableskills:stackable",
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

The reward registry includes `puffish_skills:per_level_rewards` which lets you specify rewards that depend on the player’s current level. The `levels` object maps level numbers to arrays of nested rewards.

```json
{
  "type": "puffish_skills:per_level_rewards",
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

## Example datapack

The `example-skill-level-template.zip` file contains a datapack demonstrating a stackable skill using per-level rewards. Drop the zip into the `datapacks` folder of a world to test the feature.

