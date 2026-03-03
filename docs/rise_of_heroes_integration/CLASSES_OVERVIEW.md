# Classes Overview

This document provides a summary of all classes currently integrated into the system, including both base *Epic Classes* and custom expansions.

## Base Classes (Epic Classes)
These classes are natively provided by the core mod and have been bridge-linked to the Pufferfish Skill system.

| Class | Combat Style | Key Mechanics |
| :--- | :--- | :--- |
| **Warrior** | Balanced Melee | Uses Tachi/Greatsword, standard stamina. |
| **Paladin** | Tank / Defensive | High armor compatibility, shield-focused skills. |
| **Berserker** | Aggressive Melee | Bonus damage at low health, heavy weapon focus. |
| **Reaper** | Fast / Daggers | Dual-wielding daggers, high mobility. |
| **Sorcerer** | Magic / Ranged | Uses Iron's Spellbooks, high mana pool. |
| **Archer** | Ranged | Bow and Arrow focus, movement buffs. |

---

## Custom Classes
These classes were added specifically for this project to expand the RPG experience.

### Necromancer
*A master of the dark arts and commander of the undead.*
- **Core Attributes**:
    - **Health**: Optimized for medium survival.
    - **Mana**: 150 Base (High).
- **Starting Items**:
    - `irons_spellbooks:cultist_helmet` (or similar dark thematic gear).
    - `irons_spellbooks:blood_staff`.
- **Passives**:
    - **Skeleton Mastery**: Strengthens undead minions.
    - **Magic Conversion**: Converts magic resistance into mana regeneration.
    - **Dark Pact**: Sacrifices healing potential for raw soul power.
    - **Soul Harvest**: Gain extra experience from killing mobs.

---

## How to Add New Classes
New classes can be defined via DataPack JSON files in `data/skillleveling/classes/`.
1. **Define the ID**: Choose a unique string (e.g., `epic_classes:ranger`).
2. **Assign Attributes**: Configure health, mana, and base modifiers.
3. **Register Skills**: Map specific Pufferfish skill categories to the class.
4. **Starter Items**: Add items to the `starting_items` list in the JSON.
