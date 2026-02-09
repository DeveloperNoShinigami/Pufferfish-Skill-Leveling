# Master Datapack Template Breakdown (Technical Parity)

This template serves as the technical gold standard for your modpack, strictly adhering to the **Pufferfish Skills** registry structure and **Skill Leveling Addon** logic.

## 📁 Technical Standards

### 1. Mandatory Attributes for Levels
Every skill definition in this template now uses actual **Minecraft Attribute** rewards for each of the 5 levels. This demonstrates how to create real, tangible progression using the `puffish_skill_leveling:per_level_rewards` type.

### 2. Stackable Reward Logic ("Command + Attribute Scaling")
The template demonstrates the `puffish_skill_leveling:stackable` type:
- **Root Reward (Command)**: A `puffish_skills:command` is used in the root `rewards` list. This benefit triggers every level (as requested to showcase command-based root rewards).
- **Addon Reward (Attributes)**: The `per_level_rewards` block provides the core scaling benefits (e.g., [Armor Plating](file:///c:/path/to/definitions.json) gives specific attribute Milestone bonuses at each level).

### 3. Strict Metadata Positioning
The `metadata` object is positioned at the **bottom** of each skill definition. This follows the official example structure and is required for proper parsing of custom icon assets in the GUI:
```json
"metadata": {
  "icon": "skill_id_icon_id"
}
```

### 4. Deep Skill Integration (5 Truly Listed Levels)
To ensure absolute compatibility with current addon versions, every skill explicitly lists rewards for **Levels 1 through 5**.

---

## 🎖️ Structural Checklist
- [x] Every level (1-5) uses actual Attribute rewards.
- [x] `puffish_skills/config.json` present.
- [x] `metadata` at bottom of all skill entries.
- [x] `level_limit: 10` in all `experience.json` files.
- [x] `connections.json` uses the correct `[ ["skill_a", "skill_b"] ]` pair array format.
