# Pufferfish Skill Leveling

This mod provides an API to create skill trees via datapacks. Categories and skills can unlock rewards and gain experience.

## Player skill definitions

Skill definitions describe how a skill looks and what it grants. Datapacks may now define stackable skills with extra fields:

- `type` – identifier of the skill type. Defaults to `puffish_skills:default`.
- `max_skill_level` – how many times the skill can be unlocked. This value defines
  the maximum level a skill can reach.
- `descriptions` – list of tooltip lines shown for each level.
- `extra_descriptions` – list of extra tooltip lines (displayed while holding Shift).
- `merge_description` – when `true`, descriptions and extra descriptions start accumulating from level 2 onward instead of replacing them. The first level's tooltip is shown on its own for smoother progression. Defaults to `false` when omitted.
- Tooltip lines automatically adjust based on how many times the skill has been unlocked. When hovering a skill, the
  entry matching the player's current level is shown, and holding Shift displays the line for the next level (or a final
  message if the skill is maxed).

- `title`, `icon`, `frame`, `size`, and `rewards` work as before.

A basic skill definition might look like this:

```json
{
  "stacked_power": {
      "type": "puffish_skills:stackable",
      "title": "Master Miner",
      "icon": { "type": "item", "data": { "item": "minecraft:diamond_pickaxe" } },
      "size": 1.0,
      "required_points": 3,
      "merge_description": false,
      "descriptions": [
          "Current: +1 melee damage",
          "Current: +10% mining speed",
          "Current: +15% mining speed"
      ],
      "extra_descriptions": [
          "Next: +10% mining speed",
          "Next: +15% mining speed",
          "— MAXED OUT —"
      ],
      "rewards": [
          {
              "type": "puffish_skills:per_level_rewards",
              "data": {

                    "skill_id": "19aazycn9ii0lfh1", 
                    "max_skill_level": 3,
                    "points_per_level": 1,
                    "levels": {
                        "1": [
                            { "type": "puffish_skills:attribute",
                              "data": { "attribute": "generic.attack_damage",
                                        "value": 10,
                                        "operation": "addition" } }
                        ],
                        "2": [
                            {"type": "puffish_skills:command",
                              "data": { "command": "give @p minecraft:experience_bottle 1" } 
                            }
                        ],
                        "3": [
                            { "type": "puffish_skills:attribute",
                              "data": { "attribute": "generic.max_health",
                                        "value": 2,
                                        "operation": "addition" } }
                        ]
                    }
                }
          },
          { 
              "type": "puffish_skills:command",
              "data": { "command": "give @p minecraft:experience_bottle 1" }
          }
      ],
      "metadata": { "icon": "74sqblu8lgizj777" }
  }
}
```

## Per level rewards

The reward registry includes `puffish_skills:per_level_rewards` which lets you specify rewards that depend on the skill's level. The `levels` object maps level numbers to arrays of nested rewards.

```json
{
  "type": "puffish_skills:per_level_rewards", // Skill levels can provide different rewards.
  "data": {
    "skill_id": "19aazycn9ii0lfh1",
    "max_skill_level": 3,
    "points_per_level": 1,
    "levels": {
      "1": [ { "type": "puffish_skills:attribute", "data": { "attribute": "generic.attack_damage", "value": 1, "operation": "addition" } } ],
      "2": [ { "type": "puffish_skills:command", "data": {"command": "give @p minecraft:experience_bottle 1"} } ],
      "3": [ { "type": "puffish_skills:attribute", "data": { "attribute": "generic.max_health", "value": 2, "operation": "addition" } } ]
    }
  }
}
```

Each nested reward behaves as if it were a normal reward, but is only active when the player's skill level is at least the specified level.

The fields `skill_id`, `max_skill_level` and `points_per_level` are used only by
`puffish_skills:per_level_rewards`. They define which skill is leveled, the
highest level obtainable through the reward, and how many category points are
spent per level.

All active level rewards stack automatically, so unlocking additional levels increases the total bonus without any extra configuration. When a level is unlocked the category loses `points_per_level` points. A player cannot level beyond `max_skill_level` unless they have enough points to pay for the additional levels.


## Example datapack

The `example-skill-level-template.zip` file contains a datapack demonstrating a stackable skill using per-level rewards. Drop the zip into the `datapacks` folder of a world to test the feature.



## Commands

Administrators can refund skill levels using `/puffish_skills skills refund`.

```
/puffish_skills skills refund <players> <category> <skill> [all]
```

Running without `all` refunds a single level. Adding `all` removes every level of the chosen skill. The command can be repeated as needed and reports an error if none of the selected players have any levels to refund.
