# Pufferfish Skill Leveling

This mod provides an API to create skill trees via datapacks. Categories and skills can unlock rewards and gain experience.

## Player skill definitions

Skill definitions describe how a skill looks and what it grants. Datapacks may now define stackable skills with extra fields:

- `type` – identifier of the skill type. Defaults to `puffish_skills:default`.
- `stackable` - Allows the user to use basic rewards along side skill level rewards.
- `per_level_rewards` -Enables the new skill type that gives levels to rewards.
- `levels` - A new  key field that nests the rewards per level, check out the examples below.
 - `max_skill_level` – how many times the skill can be unlocked. This value defines
   the maximum level a skill can reach. When omitted and the skill uses
   `puffish_skills:per_level_rewards`, the highest level is inferred from that reward.
- `descriptions` – list of tooltip lines shown for each level.
- `extra_descriptions` – list of extra tooltip lines (displayed while holding Shift).
- `merge_description` – when `true`, descriptions and extra descriptions accumulate from previous levels starting when level 2 is reached. The tooltip for the very first level is shown on its own. Defaults to `false` when omitted.
- Tooltip lines automatically adjust based on how many times the skill has been unlocked. When hovering a skill, the
  entry matching the player's current level is shown, and holding Shift displays the line for the next level (or a final
  message if the skill is maxed).

- `title`, `icon`, `frame`, `size`, and `rewards` work as before.

A basic skill definition using per-level rewards might look like this:

### Basic skill Definition
```json
{
  "stacked_power": {
      "title": "Master Miner",
      "icon": { "type": "item", "data": { "item": "minecraft:diamond_pickaxe" } },
      "size": 1.0,
      "merge_descriptions": false,
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
          }
      ],
      "metadata": { "icon": "74sqblu8lgizj777" }
  }
}
```

### Stackable Rewards
Here is an example of using another new type called puffish_skills:stackable if you wanted to include standard rewards along side level rewards, this is good if u wanted to run commands or other rewards but dont want to include them in level rewards
```js
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
spent per level instead of the skill's `required_points` value. If the skill
definition omits `max_skill_level`, this field also determines the skill's
maximum level.

All active level rewards stack automatically, so unlocking additional levels increases the total bonus without any extra configuration. When a level is unlocked the category loses `points_per_level` points. A player cannot level beyond `max_skill_level` unless they have enough points to pay for the additional levels.


## Example datapack

The `example-skill-level-template.zip` file contains a datapack demonstrating a stackable skill using per-level rewards. Drop the zip into the `datapacks` folder of a world to test the feature.



## Commands

Administrators can refund skill levels using `/puffish_skills skills refund`.

```
/puffish_skills skills refund <players> <category> <skill> [<count>|all]
```

By default this command refunds one level of the chosen skill. Optionally provide a number of levels to refund or use `all` to remove every level. It reports an error if none of the selected players have any levels to refund.

Refunding removes the reward linked to that level without running any unlock or lock triggers.
