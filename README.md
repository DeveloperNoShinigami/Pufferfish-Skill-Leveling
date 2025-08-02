# Pufferfish Skill Leveling

This mod provides an API to create skill trees via datapacks. Categories and skills can unlock rewards and gain experience.

## Player skill definitions

Skill definitions describe how a skill looks, how it unlocks, and what it grants.

Supported root fields:

- `type` – identifier of the skill type. Defaults to `puffish_skills:default`. Use `puffish_skills:stackable` to mix normal and per-level rewards.
- `title` – display name of the skill.
- `icon` – icon descriptor shown on the tree.
- `frame` – frame style for the icon.
- `size` – size of the icon.
- `max_skill_level` / `max_levels` – how many times the skill can be unlocked. This value defines the maximum level a skill can reach. When omitted and the skill uses `puffish_skills:per_level_rewards`, the highest level is inferred from that reward.
- `descriptions` – list of tooltip lines shown for each level.
- `extra_descriptions` – list of extra tooltip lines (displayed while holding Shift).
- `merge_description` – when `true`, descriptions and extra descriptions accumulate from previous levels starting when level 2 is reached. The tooltip for the very first level is shown on its own. Defaults to `false` when omitted.
- `rewards` – array of reward objects granted when the skill or its levels are unlocked. Include `puffish_skills:per_level_rewards` entries to define level-specific rewards.
- `cost` – category points spent to unlock the skill.
- `required_skills` – list of other skills that must be unlocked first.
- `required_points` – minimum unspent points needed in the category.
- `required_spent_points` – minimum points already spent in the category.
- `required_exclusions` – list of skill IDs that must remain locked.

Tooltip lines automatically adjust based on how many times the skill has been unlocked. When hovering a skill, the entry matching the player's current level is shown, and holding Shift displays the line for the next level (or a final message if the skill is maxed).

A basic skill definition using per-level rewards might look like this:

### Basic skill Definition
```json
{
  "stacked_power": {
      "title": "Master Miner",
      "icon": { "type": "item", "data": { "item": "minecraft:diamond_pickaxe" } },
      "size": 1.0,
      "max_levels": 3,
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
          }
      ],
      "metadata": { "icon": "74sqblu8lgizj777" }
  }
}
```

### Stackable Rewards
Use the `puffish_skills:stackable` skill type when you want to combine standard rewards with per-level rewards. This lets you run commands or grant other bonuses without adding them to the per-level configuration.
```js
{
  "stacked_power": {
      "type": "puffish_skills:stackable",
      "title": "Master Miner",
      "icon": { "type": "item", "data": { "item": "minecraft:diamond_pickaxe" } },
      "size": 1.0,
      "max_levels": 3,
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

The reward registry includes `puffish_skills:per_level_rewards` which lets you specify rewards that depend on the skill's level.

Supported fields:

- `skill_id` – ID of the skill being leveled.
- `max_skill_level` – highest level obtainable through the reward.
- `points_per_level` – category points consumed for each level.
- `levels` – maps level numbers to arrays of nested rewards.

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

The fields `skill_id`, `max_skill_level` and `points_per_level` are used only by `puffish_skills:per_level_rewards`. They define which skill is leveled, the highest level obtainable through the reward, and how many category points are spent per level instead of the skill's `required_points` value. If the skill definition omits `max_levels`/`max_skill_level`, this field also determines the skill's maximum level.

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
