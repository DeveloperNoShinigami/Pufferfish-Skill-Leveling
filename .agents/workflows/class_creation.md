---
description: How to correctly create and map a new Epic Class to the Pufferfish Skill Leveling system within a datapack.
---

# Epic Class Creation Workflow

This workflow ensures a new Epic Class is perfectly synchronized between the Epic Classes mod UI, the base game's attributes, and the Pufferfish Skill Leveling progression trees.

## Step 1: Define the Epic Class Configuration

Create the class JSON at `data/puffish_skill_leveling/epicclassmod/classes/<class_id>.json`.

### Core Identity
- **`class_name`**: Internal ID (e.g., `marksman`).
- **`epic_class_proxy`**: Base Epic Fight class (e.g., `ARCHER`, `WARRIOR`).
- **`skill_category_id`**: Maps to your Pufferfish category ID.

### Visual Rendering (High-Fidelity)
- **`class_weapon_icon`**: NBT-aware item string (e.g., `tacz:modern_kinetic_gun{GunId:"tacz:deagle"}`).
- **`preview_animation`**: Epic Fight animation path (e.g., `wom:biped/living/enderblaster_onehand_idle`).
- **`preview_mainhand_item`**: Item held in preview.
- **`preview_offhand_item`**: Item held in offhand.

### UI Customization (Bespoke Rendering)
- **`gui_stats`**: List of icons and counts (Hearts, Mana, etc.) displayed in the selection book.
- **`gui_passives`**: Maps Pufferfish Skill IDs to icons for display in the "Passive Skills" section of the book.

## Step 2: Establish the Pufferfish Category

Create the category at `data/puffish_skills/categories/<skill_category_id>/category.json`.
- Set title, icon, and background.
- Ensure the folder structure matches your `skill_category_id`.

## Step 3: Design the Skill Tree Layout

Create `skills.json` and `connections.json` within the category folder.
- Use `x` and `y` coordinates for layout.
- Link nodes logically.

## Step 4: Define Skill Progression & Rewards

Create `definitions.json` and `experience.json`.

> [!IMPORTANT]
> **Reward Match Rule**: You MUST define exactly the same number of reward tiers in the `puffish_skill_leveling:per_level_rewards` block as your `max_skill_level`.

## Step 5: (Optional) Progressive Branching

In your `bridge_config.json`, use the `progressive_class` field to hide advanced classes until their parent class is selected.

## Step 6: Verify and Reload

1. Run `/reload` in-game.
2. Use `/skillleveling advanceclass` to select your new class.
3. Open the Pufferfish Skill menu (`K`) to verify the category unlocked.
