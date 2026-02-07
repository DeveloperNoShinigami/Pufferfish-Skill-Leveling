# Episode 8: Advanced Configuration & Troubleshooting

**Scene**: Fast-paced montage of complex JSON files and quick in-game testing.

---

## 🎙️ Script

**[Intro]**
"We've made it! In this final episode, we're covering the 'Pro Tips' that will take your modpack from good to great. We're talking cost expressions, description merging, and how to fix errors fast."

**[Cost Expressions]**
"Don't settle for static numbers. Fields like `enchantment_levels`, `slot_opening_cost`, and `cleansing_cost` support full expressions. 
Instead of '10', use `"level * 5"`. This makes costs scale dynamically with the power of the skill. It keeps your economy balanced from the early game to the endgame."

**[Description Merging & Previews]**
"To keep your tooltips clean, use `merge_description`. 
Also, don't forget `extra_descriptions`. This lets you preview the *next* level's benefits while the player is looking at their current level. It builds anticipation—players see exactly what they're working towards."

**[Troubleshooting]**
"JSON is picky. If your skills aren't loading, check your `latest.log`. We’ve silenced all the debug spam, so if you see `[ADDON]` followed by an error, that's where your problem is. 
Common mistakes?
- Forgetting the mandatory `metadata` field.
- Using a `skill_id` that doesn't match the folder name.
- Misspelling the NBT tags in your loot tables."

**[Command Reference]**
"Always keep the `/skillleveling` commands in your back pocket. 
- Use `/skillleveling get` to check NBT state.
- Use `/skillleveling set` or `advance` to bypass the XP requirements for testing.
- And `/skillleveling refund all` is your best friend when you need a clean slate."

**[Closing]**
"That’s it! You now have all the tools to build a truly unique, multi-layered RPG experience in Minecraft. I can't wait to see what you create with the **Pufferfish Skill Leveling** addon. Happy modding!"
