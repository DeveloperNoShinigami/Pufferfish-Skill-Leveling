# Episode 1: Introduction & Setup

**Scene**: Minecraft Main Menu / Player standing in a vibrant RPG world.

---

## 🎙️ Script

**[Intro]**
"Hey everyone! Welcome to the first episode of our deep dive into the **Pufferfish Skill Leveling** addon. If you've used the base Pufferfish Skills mod before, you know it's a fantastic tool for adding skill trees to your modpacks. But what if you wanted more? What if you wanted skills with 5, 10, or even 100 levels? What if you wanted to imbue those skills onto your sword or armor? That's exactly what this addon allows you to do."

**[What is the Addon?]**
"Pufferfish Skill Leveling isn't a replacement for the base mod—it’s an expansion. It introduces multi-level progression, a robust imbuing system, Curios integration, and a specialized Skill Master villager. Today, we're getting everything set up so you can start building your own RPG experience."

**[Installation]**
"First things first: Requirements. You’ll need Minecraft **1.20.1** on Forge. 
1. Install the base **Pufferfish Skills** mod (v0.17.1 or higher).
2. Install this addon: **Pufferfish Skill Leveling**.
3. *Recommended*: Install the **Curios API**. While not strictly mandatory for the core skills, it’s required if you want to use the new Skill Charms, which we'll cover in a later episode."

**[Datapack Structure]**
"Everything in this mod is driven by datapacks. No coding required. If you look at your world's datapack folder, you'll see the standard Pufferfish structure under `data/your_namespace/puffish_skills/`. 
The addon looks for its own configurations in the `definitions.json` and `categories.json` files within those folders. One thing to remember: **The `metadata` field is mandatory.** Even if it's empty, you must include it in your JSON or the skill won't load."

**[Web Editor]**
"Pro tip: Use the official Pufferfish Skills web editor to layout your tree visually, then we'll jump into the JSON files to add the multi-level magic that this addon provides."

**[Outro]**
"In the next episode, we’re going to build our very first multi-level skill from scratch. See you there!"
