# Episode 3: Prerequisite Systems

**Scene**: A complex skill tree branching out across multiple categories.

---

## 🎙️ Script

**[Intro]**
"Hey everyone! Today we're talking about control. As a modpack creator, you want to guide your players. You don't want them getting ultimate power on day one. In this addon, we have two distinct prerequisite systems: **Unlock Prerequisites** and **Level Gating**."

**[System 1: Unlock Prerequisites]**
"First up: `prerequisite_skills`. This goes in the root of your skill definition. It controls when the skill icon even appears or becomes purchasable. If you want 'Advanced Mining' to require 'Basic Mining' level 3, this is where you put it. 
*Important*: These stay within the same category. They define the shape of your tree."

**[System 2: Level Gating]**
"But what if you want a skill to be easy to start, but hard to master? That's where `required_skill_for_level` comes in. 
This is much more powerful. You can specify a requirement for *individual levels*. For example, 'Warrior Strength' level 5 might require 'Combat Mastery' level 2. 
The best part? These support **cross-category** requirements. You can require a skill from the 'Magic' tree to unlock the final level of a 'Melee' skill."

**[Hidden Skills]**
"And if you want true mystery, use the `hidden: true` tag. The skill will be completely invisible—no icon, no lines, no tooltips—until its `prerequisite_skills` are met. It’s perfect for secret techniques and prestige classes."

**[In-Game Demo]**
"Watch what happens when I lose a prerequisite. If I refund the skill that was fueling my gated level, the bonus turns off instantly. The addon is always checking to make sure players are playing by your rules."

**[Outro]**
"Control your progression, and you control the pace of your pack. Next time, we're taking skills out of the tree and putting them onto your gear with the **Equipment Imbuing** system!"
