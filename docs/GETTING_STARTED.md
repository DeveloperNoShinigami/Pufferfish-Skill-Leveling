# Getting Started with Pufferfish Skill Leveling

Welcome! This guide walks you through installing the addon and creating your first multi-level skill. No prior datapack experience is required — just follow along step by step.

> **Requires:** [Pufferfish Skills](https://www.curseforge.com/minecraft/mc-mods/pufferfish-skills) v0.17.1+ for Minecraft 1.20.1 (Forge or Fabric).

---

## Step 1: Install the Mods

1. Download and install [Pufferfish Skills](https://www.curseforge.com/minecraft/mc-mods/pufferfish-skills).
2. Download and install this addon — **Pufferfish Skill Leveling**.
3. Place both `.jar` files in your `mods/` folder and launch Minecraft.

> The addon does **not** ship with a built-in datapack. You create one to define your skills, which gives you full control over your server's progression.

---

## Step 2: Create a Datapack

Every skill you create lives inside a **datapack**. Here's the folder structure you need:

```
<your_world>/datapacks/my_skills/
├── pack.mcmeta
└── data/
    └── my_skills/
        └── puffish_skills/
            └── categories/
                └── combat/
                    ├── category.json      ← How the category looks
                    ├── definitions.json   ← Your skill definitions (the important one!)
                    ├── skills.json        ← Skill positions on the tree
                    └── connections.json   ← Lines between skills
```

Create `pack.mcmeta`:

```json
{
    "pack": {
        "pack_format": 15,
        "description": "My Custom Skills"
    }
}
```

Create `category.json`:

```json
{
    "icon": {
        "type": "item",
        "data": { "item": "minecraft:iron_sword" }
    },
    "background": "minecraft:textures/block/stone.png"
}
```

---

## Step 3: Create Your First Skill

Open `definitions.json` and paste this — a 3-level attack damage skill:

```json
{
    "warrior_strength": {
        "category_id": "combat",
        "title": "Warrior's Strength",
        "description": "Increases your melee attack damage.",
        "icon": {
            "type": "item",
            "data": { "item": "minecraft:iron_sword" }
        },
        "max_skill_level": 3,
        "points_per_level": 1,
        "descriptions": {
            "1": "§7+1 Attack Damage",
            "2": "§7+2 Attack Damage",
            "3": "§6+3 Attack Damage §c(MAX)"
        },
        "rewards": [
            {
                "type": "puffish_skill_leveling:per_level_rewards",
                "data": {
                    "skill_id": "warrior_strength",
                    "levels": {
                        "1": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 1.0,
                                    "operation": "addition"
                                }
                            }
                        ],
                        "2": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 2.0,
                                    "operation": "addition"
                                }
                            }
                        ],
                        "3": [
                            {
                                "type": "puffish_skills:attribute",
                                "data": {
                                    "attribute": "generic.attack_damage",
                                    "value": 3.0,
                                    "operation": "addition"
                                }
                            }
                        ]
                    }
                }
            }
        ],
        "metadata": {}
    }
}
```

> **Common mistake:** Forgetting `"metadata": {}`. It's required even if empty — the parser will fail without it.

---

## Step 4: Place It on the Skill Tree

In `skills.json`:

```json
{
    "warrior_strength": {
        "definition": "warrior_strength",
        "x": 0,
        "y": 0,
        "root": true
    }
}
```

And `connections.json` (empty for now):

```json
{}
```

---

## Step 5: Test It

1. Run `/reload` in-game.
2. Open the skill tree (default key: `K`).
3. Click your skill to level it up — the tooltip updates with each level.

That's it! You've created a multi-level skill.

---

## Understanding What You Just Made

| Field | What It Does |
|-------|--------------|
| `max_skill_level` | How many times players can level this skill. |
| `points_per_level` | Skill points spent per level-up. |
| `descriptions` | Tooltip text for each level. Uses Minecraft `§` color codes. |
| `per_level_rewards` | The actual gameplay effect at each level. Each level replaces the previous one. |
| `metadata` | Required field — use `{}` if empty. |

**How rewards work:** At Level 1 you get +1 damage. When you level to 2, Level 1's reward is removed and Level 2's reward (+2 damage) is applied. You always have exactly the reward matching your current level.

---

## What's Next?

### Quick Wins
- **Add more skills** — duplicate the pattern above with different IDs and rewards.
- **Connect skills** — add entries to `connections.json` to draw lines between skills (creates tree structure).

### Explore Features

| Feature | Difficulty | Guide |
|---------|------------|-------|
| Add tooltip descriptions | Beginner | [Datapack Guide — Descriptions](./DATAPACK_GUIDE.md#-point-e-descriptions--tooltips) |
| Require one skill before another | Beginner | [Datapack Guide — Gating](./DATAPACK_GUIDE.md#-point-d-the-gating-systems) |
| Toggle abilities (Night Vision, Rage) | Intermediate | [Toggle System](./Toggle_System.md) |
| Lock categories behind skill requirements | Intermediate | [Datapack Guide — Category Gating](./DATAPACK_GUIDE.md#3-category-gating-prerequisite_skills-in-categoryjson) |
| Apply skills to equipment via Anvil | Advanced | [Datapack Guide — Anvil & Imbuing](./DATAPACK_GUIDE.md#%EF%B8%8F-point-h-the-anvil--imbuing-ecosystem) |
| Add skill items to loot tables | Advanced | [Universal Loot System](./Universal_Loot_System.md) |
| Dynamic XP cost formulas | Advanced | [Datapack Guide — Costs](./DATAPACK_GUIDE.md#-point-g-costs--xp-expressions) |

### Full Documentation

| Guide | What It Covers |
|-------|----------------|
| [Features Reference](./FEATURES.md) | Complete list of every feature with field tables |
| [Datapack Guide](./DATAPACK_GUIDE.md) | Progressive tutorial from basics to expert configurations |
| [Toggle System](./Toggle_System.md) | Active abilities, keybinds, cooldowns, hybrid patterns |
| [Skill Imbuement System](./Skill_Imbuement_System.md) | Random loot imbuing, dimension scaling |
| [Universal Loot System](./Universal_Loot_System.md) | Adding custom items to any loot table |

---

## Useful Commands

| Command | What It Does |
|---------|--------------|
| `/reload` | Reloads all datapacks. Use after every change. |
| `/skillleveling get <player> <category> <skill>` | Check a player's current level. |
| `/skillleveling set <player> <category> <skill> <level>` | Force-set a level (great for testing). |
| `/skillleveling advance <player> <category> <skill>` | Level up by 1 (respects requirements). |
| `/skillleveling refund <player> <category> <skill> all` | Reset a skill back to 0. |

---

*Having trouble? Check the [Datapack Guide — Troubleshooting](./DATAPACK_GUIDE.md#-troubleshooting) section.*
