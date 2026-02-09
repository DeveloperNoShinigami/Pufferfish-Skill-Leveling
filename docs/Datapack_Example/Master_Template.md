# Master Datapack Template Documentation

This template provides a comprehensive starting point for your own modpacks, covering every base Minecraft attribute and demonstrating all advanced features of the **Pufferfish Skill Leveling** addon.

## 📂 Template Contents

### 1. Categories (`master:definitions`)
We've split the skills into three logical categories:
- **Combat**: Defense, health, and resistance.
- **Offense**: Damage, speed, and knockback.
- **Mastery**: Movement, luck, and specialized attributes.
- **Ancient Techniques**: A "Loot-Only" category demonstrating skills that cannot be purchased via points.

### 🛠️ Key Features Demonstrated

#### Multi-Level Optimization
Every skill uses `max_skill_level`. Examples range from simple 5-level skills to massive 50-level scaling trees.

#### Level Gating (Cross-Category)
- Example: `Mastery: Speed` Level 10 requires `Offense: Strength` Level 5.
- This forces players to diversify their builds.

#### Hidden Skills (Visual Discovery)
- Look at `Offense: Dragon's Breath`. It's hidden until you reach Level 10 in all primary combat skills.
- The player won't even know it exists until they meet the requirements!

#### Loot-Only Skills (`imbue_only`)
- Shows skills that don't appear in the menu but can be found as **Skill Tomes** or imbued onto gear.

#### Merged Descriptions
- Demonstrates how to use `merge_description: true` to keep tooltips tidy while showing cumulative bonuses.

---

## 🏗️ Attribute Reference

| Attribute | Skill Name | Category |
|-----------|------------|----------|
| `generic.max_health` | Vitality | Combat |
| `generic.armor` | Hardening | Combat |
| `generic.armor_toughness` | Iron Skin | Combat |
| `generic.knockback_resistance` | Unshakable | Combat |
| `generic.attack_damage` | Strength | Offense |
| `generic.attack_speed` | Reflexes | Offense |
| `generic.attack_knockback` | Brute Force | Offense |
| `generic.movement_speed` | Agility | Mastery |
| `generic.luck` | Fortune | Mastery |
| `generic.follow_range` | Intimidation | Mastery |
| `horse.jump_strength` | Equestrian | Mastery |
| `zombie.spawn_reinforcements` | Call to Arms | Mastery |

---

## 🚀 How to Use
1. Copy the `data` folder into your world's `datapacks` directory.
2. Rename the namespaces (`master`, `ancient`) to match your project.
3. Use `/reload` to apply changes.
4. Modify the values in the JSON files to fit your pack's balance!
