---
description: How to correctly create and map a new Epic Class to the Pufferfish Skill Leveling system within a datapack.
---

# Epic Class Creation Workflow

This workflow ensures a new Epic Class is perfectly synchronized between the Epic Classes mod UI, the base game's attributes, and the Pufferfish Skill Leveling progression trees. Follow these steps in order to prevent namespace or leveling crashes.

## Step 1: Define the Epic Class Configuration

Create the class JSON at `data/<namespace>/puffish_skill_leveling/epic_classes/classes/<class_id>.json`.

*   **Set `epic_class_proxy`**: Choose an appropriate base Epic Fight class (e.g., `WARRIOR`, `SORCERER`). This governs underlying animations.
*   **Set `skill_category_id`**: This **MUST** map to your intended Pufferfish category.
    *   *Best Practice*: Use the `epic_classes` namespace natively. E.g., `"skill_category_id": "epic_classes:ranger"`.

## Step 2: Establish the Pufferfish Category

Create the matching folder structure for the category defined in Step 1.
If your `skill_category_id` was `epic_classes:ranger`, your path must be:
`data/epic_classes/puffish_skills/categories/ranger/`

Create `category.json` within this folder to define the title, icon, and background.

## Step 3: Design the Skill Tree Layout

Create `skills.json` and `connections.json` within the category folder.
*   Map out your X/Y coordinates for each skill node.
*   Link them logically to ensure players can progress from the starting node outward.

## Step 4: Define Skill Progression & Rewards (CRITICAL)

Create `definitions.json` and `experience.json`. This is where most errors occur.

// turbo-all
1.  **Define `max_skill_level`**: Decide how many ranks the skill has (e.g., 3).
2.  **Define Rewards Array**: You **MUST** define exactly the same number of reward tiers in the `puffish_skill_leveling:per_level_rewards` block.

**BAD:**
```json
"max_skill_level": 3,
"rewards": { "1": [...], "2": [...] } // Missing level 3! Will break progression.
```

**GOOD:**
```json
"max_skill_level": 3,
"rewards": { "1": [...], "2": [...], "3": [...] }
```

## Step 5: (Optional) Map Epic Attributes

If your class needs Pufferfish points to convert directly into raw stats (Health, Mana, Speed), define an attribute scale in `data/<namespace>/puffish_skill_leveling/epic_classes/attributes/<class_id>.json`.

## Step 6: Verify and Reload

1.  Run `/reload` in-game.
2.  Use `/skillleveling advanceclass` to select your new class (Do NOT use the WIP Job Master NPCs for assignment).
3.  Open the Pufferfish Skill menu to verify the category unlocked.
4.  Purchase skills and verify levels increment correctly without exceptions.
