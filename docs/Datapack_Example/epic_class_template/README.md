# Epic Class Template Datapack

This datapack provides 6 class-based skill categories that align with the **Epic Class Mod** class system for Minecraft 1.20.1.

## Categories

### 1. **Warrior** (Iron Sword Icon)
Focuses on defensive combat and durability.

**Skills:**
- **Shield Bash**: +2-10 Armor (5 levels)
- **Fortitude**: +2-10 Max HP (5 levels)
- **Battle Cry**: +1-5 Attack Damage (5 levels)
- **Iron Will**: +10%-50% Knockback Resistance (5 levels)
- **Warrior Mastery**: +5%-15% Attack Speed (3 levels)

### 2. **Paladin** (Golden Sword Icon)
Holy warrior combining healing, defense, and righteous fury.

**Skills:**
- **Holy Light**: +2-10 Max HP (5 levels)
- **Sanctuary**: +2-10 Armor (5 levels)
- **Divine Shield**: +1-5 Armor Toughness (5 levels)
- **Righteous Fury**: +1-5 Attack Damage (5 levels)
- **Paladin Mastery**: +5%-15% Movement Speed (3 levels)

### 3. **Berserker** (Diamond Axe Icon)
Aggressive melee specialist with high damage and speed.

**Skills:**
- **Blood Rage**: +2-10 Attack Damage (5 levels)
- **Savage Strike**: +5%-25% Attack Speed (5 levels)
- **Wild Fury**: +5%-25% Movement Speed (5 levels)
- **Relentless**: +2-10 Armor (5 levels)
- **Berserker Mastery**: +2-6 Max HP (3 levels)

### 4. **Reaper** (Netherite Hoe Icon)
Death-themed class with life steal and shadow abilities.

**Skills:**
- **Soul Harvest**: +1-5 Attack Damage (5 levels)
- **Death Touch**: +5%-25% Attack Speed (5 levels)
- **Shadow Walk**: +5%-25% Movement Speed (5 levels)
- **Life Steal**: +2-10 Max HP (5 levels)
- **Reaper Mastery**: +2-6 Armor (3 levels)

### 5. **Sorcerer** (Enchanted Book Icon)
Magic-oriented class with arcane power and mystical defenses.

**Skills:**
- **Arcane Power**: +1-5 Attack Damage (5 levels)
- **Mana Shield**: +2-10 Armor (5 levels)
- **Spell Mastery**: +5%-25% Attack Speed (5 levels)
- **Mystic Knowledge**: +2-10 Max HP (5 levels)
- **Sorcerer Mastery**: +5%-15% Movement Speed (3 levels)

### 6. **Archer** (Bow Icon)
Ranged combat specialist with precision and agility.

**Skills:**
- **Eagle Eye**: +1-5 Attack Damage (5 levels)
- **Swift Draw**: +5%-25% Attack Speed (5 levels)
- **Piercing Shot**: +1-5 Armor Toughness (5 levels)
- **Agility**: +5%-25% Movement Speed (5 levels)
- **Archer Mastery**: +2-6 Max HP (3 levels)

## Installation

1. Place this datapack folder in your world's `datapacks/` directory
2. Reload with `/reload` or restart the server
3. Skills will automatically be available in the `/skills` GUI

## Epic Class Mod Integration

When used with the **Pufferfish Skill Leveling Bridge** (Tier 1):
- Categories automatically unlock/lock based on player's Epic Class selection
- Class changes are detected and synchronized automatically
- Works seamlessly on dedicated servers with polling-based detection

## Datapack Structure

```
epic_class_template/
├── pack.mcmeta
└── data/
    └── epic_class/
        └── puffish_skills/
            ├── config.json
            └── categories/
                ├── warrior/
                ├── paladin/
                ├── berserker/
                ├── reaper/
                ├── sorcerer/
                └── archer/
```

Each category folder contains:
- `category.json` - Category display settings (title, icon, background)
- `skills.json` - Skill positioning on the skill tree
- `connections.json` - Visual connections between skills
- `definitions.json` - Complete skill definitions with rewards
- `experience.json` - Experience requirements and sources

## Customization

To modify skills:
1. Edit the `definitions.json` in each category folder
2. Adjust attribute values, max levels, or rewards
3. Run `/reload` to apply changes

## Experience System

All categories use the same experience formula:
- **Level Cap**: 10
- **Formula**: `min(level ^ 1.5 + 10, 500)`
- **Source**: +5 XP per entity kill

## Compatibility

- **Minecraft**: 1.20.1
- **Pufferfish Skills**: 0.17+
- **Pufferfish Skill Leveling**: 0.17.2+
- **Epic Class Mod**: 1.7.8+ (optional, for bridge integration)
- **Mod Loader**: Forge 46+

## Notes

- Category IDs match Epic Class Mod enum values (lowercase)
- All skills use vanilla Minecraft attributes for maximum compatibility
- Skills are designed to be balanced for dedicated server gameplay
- Bridge configuration can be customized in `config/pufferfish_skills_bridge.json`

---

**Created for**: Pufferfish Skill Leveling addon system  
**Version**: 1.0  
**Author**: BlueLotus Coding  
**License**: See main project LICENSE
