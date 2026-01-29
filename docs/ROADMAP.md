# Roadmap

Planned features for future releases of the Pufferfish Skill Leveling Addon.

---

## 🏪 Skill Master Villager

A custom villager profession that trades skill-related items.

### Planned Features
- **Dynamic Skill Book Trading**: Trade offerings based on player progress:
  - Offers levels the player **hasn't reached yet** (not just unlearned skills)
  - Example: Player at Warrior Level 2 sees trades for Level 3, 4, 5...
  - Higher levels become **rarer** as player progresses in that skill
- **Rarity Scaling**: The more a player levels a skill, the harder it becomes to find higher-level tomes for that skill in trades
- **Standard Tome Pricing**: Fixed emerald costs for base tomes:
  - Tome of Progression
  - Tome of Clear Mind
  - Tome of Greater Clear Mind
- **Reputation System**: Better prices and access to rarer skill books as reputation increases
- **Custom Workstation**: Optional POI block for the Skill Master profession

---

## 📦 Auto-Generated Skill Tomes

Automatically generate Skill Tome variants based on `max_skill_level`.

### Planned Features
- **Creative Tab Population**: When a skill has `max_skill_level: 3`, automatically create:
  - Skill Tome (Level 1)
  - Skill Tome (Level 2)
  - Skill Tome (Level 3)
- **Loot Pool Ready**: All generated tomes are ready for use in loot tables
- **Dynamic Updates**: Adding/removing skills from datapacks automatically updates available tomes

### Example
If `imbued_vitality` has `max_skill_level: 5`, the creative tab will show:
- Imbued Vitality Tome (+1)
- Imbued Vitality Tome (+2)
- Imbued Vitality Tome (+3)
- Imbued Vitality Tome (+4)
- Imbued Vitality Tome (+5)

---

## 📦 Global Loot Tables

Inject skill-related items into vanilla and modded loot tables.

### Planned Features
- **Mob Drops**: Chance for enemies to drop Skill Tomes
  - Configurable drop rates per mob type
  - Boss-specific rare skill drops
- **Chest Loot**: Skill Tomes appear in dungeon, mineshaft, and structure chests
  - Weighted by structure rarity
  - Datapack-configurable loot pools
- **Fishing Loot**: Rare skill books as treasure catches

---

## 🔗 Cross-Category Prerequisites

Expand prerequisite system to support skills from different categories.

### Current Limitation
Prerequisites currently only work within the **same category**.

### Planned Enhancement
```json
"prerequisite_skills": [
    {
        "category": "combat_skills",
        "skill": "basic_warrior",
        "min_level": 3
    },
    {
        "category": "magic_skills",
        "skill": "mana_control",
        "min_level": 1
    }
]
```

---

## 📚 Skill Book Combining

Combine identical Skill Tomes to create higher-level versions, similar to enchantment book combining.

### Planned Features
- **Anvil Combining**: Place two Skill Tomes of the **same level** in an anvil
- **Enchantment-Style Progression**: Two level N books combine into one level N+1 book
- **Stack Limits**: Define maximum combinable level per skill
- **Experience Cost**: Optional XP cost for combining
- **Visual Feedback**: Clear indication of resulting level

### Combining Rules
| Input | Output |
|-------|--------|
| Level 1 + Level 1 | Level 2 |
| Level 2 + Level 2 | Level 3 |
| Level 3 + Level 3 | Level 4 |
| Level 1 + Level 2 | ❌ Wont work (must be same level) |

### Example Flow
1. Skill Tome (+1 Warrior) + Skill Tome (+1 Warrior) = Skill Tome (+2 Warrior)
2. Skill Tome (+2 Warrior) + Skill Tome (+2 Warrior) = Skill Tome (+3 Warrior)
3. Player needs 2× Level 1, 2× Level 2, 2× Level 3 to reach Level 4

---

## 🎯 Priority Order

| Priority | Feature | Status |
|----------|---------|--------|
| 1 | Skill Master Villager | 🔜 Planned |
| 2 | Auto-Generated Skill Tomes | 🔜 Planned |
| 3 | Global Loot Tables | 🔜 Planned |
| 4 | Cross-Category Prerequisites | 🔜 Planned |
| 5 | Skill Book Combining | 🔜 Planned |

---

*Want to suggest a feature or contribute? Open an issue on the repository!*
