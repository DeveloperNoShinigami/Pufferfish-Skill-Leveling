# Episode 2: Creating Your First Multi-Level Skill

**Scene**: Split screen — JSON editor on the left, Minecraft on the right.

---

## 🎙️ Script

**[Intro]**
"Welcome back! In Episode 1, we got the addon installed. Now, let’s make some magic happen. We're going to create a skill called 'Warrior's Strength' that has 5 levels, with attack damage increasing at every step."

**[JSON Structure]**
"Open your `definitions.json`. In the base mod, a skill is usually just 'on' or 'off'. With the addon, we add two key fields to the root of the skill:
- `max_skill_level`: How many times a player can upgrade this.
- `points_per_level`: How many skill points each click costs."

**[The Reward Type]**
"Now for the most important part: the reward. Instead of the usual attribute reward, we use `puffish_skill_leveling:per_level_rewards`. Inside the `data` block, we create a `levels` object. 
Each number here—1, 2, 3—represents a level. Inside those levels, you put the actual rewards, like attack damage or move speed. The mod handles all the math and syncing automatically."

**[Descriptions]**
"To keep your UI looking clean, use the `descriptions` field. You can define a specific line for every level. 
*Tip*: If you want the tooltips to pile up, set `merge_description` to true. This will list out every single bonus the player has earned so far in a nice bulleted list."

**[In-Game Check]**
"Save your file, run `/reload`, and open the skill tree. You'll see the skill now has a 'Level 0/5' indicator. Click it, spend a point, and watch your stats update instantly in the UI. No logout needed!"

**[Outro]**
"Now you know the basics of multi-leveling. Next time, we're going to talk about **Prerequisites**—how to lock these powerful levels behind other skills or even other skill categories!"
