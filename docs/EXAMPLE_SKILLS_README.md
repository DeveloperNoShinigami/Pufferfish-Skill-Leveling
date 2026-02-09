# Example Skills - Cross-Category Prerequisites & Level-Gating

This document explains the new example skills that demonstrate the refactored prerequisite system.

---

## Example 1: Cross-Category Prerequisites

### `test_magic_swordsman`
**Location**: `example` category  
**Demonstrates**: Cross-category unlock prerequisites

```json
"prerequisite_skills": [
  {
    "skill": "test_warrior",
    "min_level": 1
  },
  {
    "skill": "vibrant_luck",
    "min_level": 1,
    "category": "additional_skills"
  }
]
```

**Behavior**:
- Requires `test_warrior` level 1 (same category)
- Requires `vibrant_luck` level 1 from `additional_skills` category
- **Has `"hidden": true`** - Skill is completely invisible until both prerequisites are met
- Once prerequisites are met, skill appears and can be purchased

---

## Example 2: Level-Gating with Cross-Category

### `test_advanced_warrior`
**Location**: `example` category  
**Demonstrates**: Level-gating prerequisites with cross-category support

#### Unlock Prerequisites
```json
"prerequisite_skills": [
  {
    "skill": "test_warrior",
    "min_level": 2
  }
]
```

#### Level-Gating
```json
"required_skill_for_level": {
  "3": [
    {
      "skill": "test_mage",
      "min_level": 1
    }
  ],
  "5": [
    {
      "skill": "test_mage",
      "min_level": 2
    },
    {
      "skill": "imbued_vitality",
      "min_level": 2,
      "category": "additional_skills"
    }
  ]
}
```

**Behavior**:
1. **Unlock**: Requires `test_warrior` level 2 (skill is **visible but locked**)
2. **Levels 1-2**: Can be leveled freely once unlocked
3. **Level 3**: Blocked until `test_mage` reaches level 1
4. **Level 4**: No additional requirements
5. **Level 5**: Blocked until BOTH:
   - `test_mage` level 2 (same category)
   - `imbued_vitality` level 2 from `additional_skills` category

**Rewards**: +2/4/6/8/10 Max Health at levels 1-5

---

## Testing the Examples

### In-Game Testing Steps

1. **Load the datapack** and run `/reload`
2. **Open skills UI** (default: `K` key)

#### Test Cross-Category Prerequisites (`test_magic_swordsman`)
1. Notice `test_magic_swordsman` is **completely invisible** (has `"hidden": true`)
2. Level `test_warrior` to 1 → Still invisible
3. Switch to `additional_skills` category
4. Level `vibrant_luck` to 1
5. Return to `example` category → `test_magic_swordsman` now **appears**!
6. Can now purchase it

#### Test Level-Gating (`test_advanced_warrior`)
1. Notice `test_advanced_warrior` is **visible but locked** (no `hidden` field)
2. Level `test_warrior` to 2 → `test_advanced_warrior` unlocks
3. Purchase and level to 1, then 2 → Works normally
4. Try to level to 3 → **BLOCKED** (requires `test_mage` 1)
5. Level `test_mage` to 1 → Can now advance to level 3
6. Try to level to 5 → **BLOCKED** (requires `test_mage` 2 AND `imbued_vitality` 2)
7. Level `test_mage` to 2 → Still blocked
8. Switch to `additional_skills` and level `imbued_vitality` to 2
9. Return to `example` → Can now advance to level 5!

> [!NOTE]
> If you want these example skills to appear in the Creative Tab as Tomes/Charms, you must add `"category_id": "example"` to their definitions in `definitions.json`.

---

## Key Differences from Old System

| Feature | Old System | New System |
|---------|-----------|------------|
| **Cross-Category** | Only in `per_level_rewards` | Now in `prerequisite_skills` too |
| **Level-Gating** | N/A | New `required_skill_for_level` |
| **Category Format** | Full namespace | Path only (e.g., `"additional_skills"`) |
| **Use Case** | Reward activation | Skill unlock AND level progression |

---

## JSON Schema Reference

### Cross-Category Prerequisite
```json
{
  "skill": "skill_name",
  "min_level": 1,
  "category": "category_path"
}
```

### Level-Gating
```json
"required_skill_for_level": {
  "level_number": [
    {
      "skill": "skill_name",
      "min_level": 1,
      "category": "category_path"  // optional, same category if omitted
    }
  ]
}
```
