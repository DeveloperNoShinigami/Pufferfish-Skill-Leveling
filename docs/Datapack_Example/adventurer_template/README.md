# Adventurer Template Datapack

A standalone starter class datapack inspired by **Ragnarok Online's Novice** — the entry point for all class specializations in the **Pufferfish Skill Leveling + Rise of Heroes** system.

## The Adventurer

| Property | Value |
|---|---|
| **Class** | Adventurer |
| **Proxy** | WARRIOR (all-weapon access) |
| **Weapon** | Wooden Sword |
| **Starting Items** | Wooden Sword, 16x Bread |
| **Level Cap** | 10 (fast to max) |
| **XP Source** | +5 XP per kill |

## Skill Tree

```
[Toughen Up] → [Keen Edge] → [Swift Feet] → [Iron Skin] → [★ Second Wind]
   +HP           +ATK          +SPD          +ARMOR        TOGGLE: Regen I
   3 lvls        3 lvls        3 lvls        3 lvls        5s / 30s CD
```

### Skills

1. **Toughen Up** — +1/2/3 Max HP (passive)
2. **Keen Edge** — +0.5/1.0/1.5 Attack Damage (passive)
3. **Swift Feet** — +5%/10%/15% Movement Speed (passive)
4. **Iron Skin** — +1/2/3 Armor (passive)
5. **Second Wind** — Toggle: Regeneration I for 5s, 30s cooldown (Keybind 1)

## Installation

1. Place `adventurer_template/` in your world's `datapacks/` folder
2. Run `/reload`
3. Select the Adventurer class via `/skillleveling advanceclass`
4. Open the skill menu (`K`) to see the category

## File Structure

```
adventurer_template/
├── pack.mcmeta
└── data/
    ├── epic_class/
    │   └── puffish_skills/
    │       ├── config.json
    │       └── categories/
    │           └── adventurer/
    │               ├── category.json
    │               ├── skills.json
    │               ├── connections.json
    │               ├── definitions.json
    │               └── experience.json
    └── puffish_skills_leveling/
        └── epicclassmod/
            └── classes/
                └── adventurer.json
```
