# Skill Master System

[< Back to Core Index](index.md) | [Next: Skill Imbuement System >](Skill_Imbuement_System.md)

---

The Skill Master is a custom villager profession that serves as the primary NPC-driven progression path. From early-game Blank Tomes to end-game Sigils of Imbuement, the Skill Master's inventory scales with both his own tier and your mastery across all skill categories.

---

## How It Works

1. **A Skill Master spawns** when an unemployed villager claims a **Skill Scribe Table** as its workstation (like any vanilla profession).
2. **Every time you interact**, the Skill Master regenerates its trade list dynamically based on your current skill levels, mastery count, and the villager's tier.
3. **Trading levels him up** through the standard 5-tier villager progression (Novice → Master). Higher tiers unlock better items and broader skill coverage.
4. **Your mastery count** (how many skills you've maxed across all categories) drives price discounts and increases the chance of rare upgrade trades.

> **Key insight:** Two players talking to the same Skill Master see **different trades** because offers are generated per-player based on individual progression.

---

## Tier Progression

The Skill Master follows Minecraft's standard 5-tier villager leveling. Each tier expands what he can offer:

| Tier | Name | Trade Slots | Static Offers | Dynamic Offers |
|------|------|-------------|---------------|----------------|
| 1 | Novice | 5–7 | Sigil of Imbuement | Introductory skill & exp tomes |
| 2 | Apprentice | 6–8 | Tome of Clear Mind, Tome of Cleansing I | Low–mid skill & exp tomes |
| 3 | Journeyman | 7–9 | Tome of Cleansing II | Tome upgrades + mid-level tomes |
| 4 | Expert | 8–10 | Tome of Cleansing III, Tome of Greater Clear Mind (50%) | High-level tomes |
| 5 | Master | 9–12 | Tome of Progression, Sigil of Imbuement (50%) | Highest-level tomes |

**Trade slot range** is `5 + (tier - 1)` minimum, +2 maximum, with Tier 5 capping at 12.

---

## Trade Types

### Static Trades (from Datapacks)

These are defined in JSON files under `data/<namespace>/skill_master_trades/`. They appear at fixed tiers and have consistent inputs/outputs.

**Built-in static trades** (from [base_tomes.json](../Common/src/main/resources/data/puffish_skill_leveling/skill_master_trades/base_tomes.json)):

| Tier | Input | Output | Max Uses | Chance |
|------|-------|--------|----------|--------|
| 1 | 32 Emeralds + Gold Block | Sigil of Imbuement | 3 | 100% |
| 2 | 16 Emeralds + Book | Tome of Clear Mind | 4 | 100% |
| 2 | 24 Emeralds + Ghast Tear | Tome of Cleansing I | 2 | 100% |
| 3 | 32 Emeralds + Ghast Tear | Tome of Cleansing II | 3 | 100% |
| 4 | 48 Emeralds + Ghast Tear | Tome of Cleansing III | 3 | 100% |
| 4 | 32 Emeralds + Book | Tome of Greater Clear Mind | 2 | 50% |
| 5 | 64 Emeralds + Gold Block | Tome of Progression | 3 | 100% |
| 5 | 32 Emeralds + Gold Block | Sigil of Imbuement | 3 | 50% |

### Dynamic Skill Tome Trades

The remaining trade slots are filled dynamically. The system scans every skill registered in `LeveledConfigStorage` and picks from:

- **Unlearned skills** (Level 0) — 70% selection weight. The Skill Master prioritizes introducing you to new skills.
- **In-progress skills** (Level 1+, not maxed) — 30% selection weight. Offers the next advancement for skills you've already started.

#### Tome-Only Filling Logic

The Skill Master **never** defaults to Blank Tomes as fillers. Instead, every available slot up to the minimum count is filled with a randomized mix:
- **75% Mix**: 1 Experience Tome + 1 Skill Tome.
- **15% Exp Focus**: Both filler slots are Experience Tomes.
- **10% Skill Focus**: Both filler slots are Skill Tomes.

If no Skill Tomes are available for the player (e.g., all skills in that tier's range are maxed), the system gracefully falls back to Experience Tomes to ensure the shop is never empty.

#### Tier-Based Level Ranges

Dynamic trades respect the villager's tier. Each tier covers a proportional segment of a skill's maximum level:

```
Tier 1 → Level 1 to round(maxLevel × 0.2)
Tier 2 → Level round(maxLevel × 0.2)+1 to round(maxLevel × 0.4)
Tier 3 → Level round(maxLevel × 0.4)+1 to round(maxLevel × 0.6)
Tier 4 → Level round(maxLevel × 0.6)+1 to round(maxLevel × 0.8)
Tier 5 → Level round(maxLevel × 0.8)+1 to maxLevel
```

**Example:** A skill with `max_skill_level: 10`:
- Tier 1 Novice offers Levels 1–2
- Tier 3 Journeyman offers Levels 5–6
- Tier 5 Master offers Levels 9–10

If the player already exceeds a tier's range, that skill is skipped for that tier.

#### Standard Purchase

The default dynamic trade format:

```
[Emeralds] + [Book] → [Skill Tome (Level L)]
```

Price is calculated as: `min(level × random(3–10), 64) × priceMultiplier`

The price multiplier scales from `1.2×` at Level 1 down to `0.5×` at max level, meaning early levels cost more relative to their power and later levels become progressively cheaper per level.

#### Upgrade Trade

At Tier 3+, there's a chance the Skill Master offers an upgrade instead of a standard purchase:

```
[Emeralds] + [Skill Tome (Level L)] → [Skill Tome (Level L+1)]
```

- Only offered for skills the player has already learned (Level 1+).
- Price is discounted by the `tome_upgrade_price_multiplier` (default: 60% of standard price).
- Chance starts at 10% and scales to 25% based on mastery count.

### Dedicated Tome Upgrades (Tier 3+)

In addition to the random upgrade chance on dynamic trades, the Skill Master also adds up to **3 dedicated upgrade trades** at Tier 3 and above. These target skills the player currently has at an intermediate level and offer the next level up.

---

## Mastery System

Your **mastery count** is the number of skills you've reached maximum level on, across all categories. Mastering skills directly improves your Skill Master experience:

| Effect | Formula |
|--------|---------|
| **Price reduction** | Tome price multiplier drops from 1.2× to 0.5× as level approaches max |
| **Upgrade trade chance** | `baseChance + (masteryProgress × (maxChance - baseChance))` — 10% at 0 mastered, 25% at 10+ mastered |
| **XP bonus per trade** | `min(masteryCount × 2, 20)` bonus XP added to every trade |
| **Mastery message** | At 3+ mastered skills, the Skill Master acknowledges your dedication with a chat message |

> **Gameplay loop:** Trade → Level up skills → Master them → Prices drop and upgrade chances increase → Better trades available → Repeat.

---

## Reputation Configuration

All Skill Master economic values are data-driven via:

```
data/<namespace>/skill_master_reputation/config.json
```

### Full Schema

```json
{
    "experience_settings": {
        "base_experience_per_trade": 5,
        "bonus_for_both_mode_skills": 3,
        "bonus_for_imbue_only_skills": 5,
        "experience_multiplier_per_villager_tier": 2,
        "experience_bonus_per_mastered_skill": 2,
        "maximum_mastery_experience_bonus": 20
    },
    "static_experience_rewards": {
        "sigil_proxy_trade": 5,
        "tome_of_clear_mind": 10,
        "tome_of_cleansing": 15,
        "sigil_of_imbuement": 50,
        "tome_of_cleansing_upgrade": 25
    },
    "dynamic_trade_settings": {
        "special_upgrade_trade_base_chance": 0.1,
        "special_upgrade_trade_max_chance": 0.25,
        "tome_price_multiplier_at_level_1": 1.2,
        "tome_price_multiplier_at_max_level": 0.5,
        "tome_upgrade_price_multiplier": 0.6
    }
}
```

### Field Reference

#### Experience Settings

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `base_experience_per_trade` | No | 5 | Base villager XP earned per dynamic skill tome trade |
| `bonus_for_both_mode_skills` | No | 3 | Extra XP for trading skills with `loot_mode: "both"` |
| `bonus_for_imbue_only_skills` | No | 5 | Extra XP for trading skills with `loot_mode: "imbue_only"` |
| `experience_multiplier_per_villager_tier` | No | 2 | XP multiplier added per tier (Tier 3 = base + 6) |
| `experience_bonus_per_mastered_skill` | No | 2 | XP bonus per skill the player has mastered |
| `maximum_mastery_experience_bonus` | No | 20 | Cap on mastery XP bonus |

#### Static Experience Rewards

Fixed XP values for specific item trades:

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `sigil_proxy_trade` | No | 5 | XP for the Level 1 proxy trade (Blank Tome) |
| `tome_of_clear_mind` | No | 10 | XP for selling Tome of Clear Mind |
| `tome_of_cleansing` | No | 15 | XP for selling Tome of Cleansing I |
| `sigil_of_imbuement` | No | 50 | XP for selling Sigil of Imbuement |
| `tome_of_cleansing_upgrade` | No | 25 | XP for selling Tome of Cleansing II/III |

#### Dynamic Trade Settings

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `special_upgrade_trade_base_chance` | No | 0.1 | Base probability of offering an upgrade trade (10%) |
| `special_upgrade_trade_max_chance` | No | 0.25 | Maximum upgrade trade probability at 10+ masteries (25%) |
| `tome_price_multiplier_at_level_1` | No | 1.2 | Price multiplier for Level 1 tomes (120% of base) |
| `tome_price_multiplier_at_max_level` | No | 0.5 | Price multiplier for max-level tomes (50% of base) |
| `tome_upgrade_price_multiplier` | No | 0.6 | Price discount for upgrade trades (60% of standard price) |

---

## Custom Trades (Datapack)

You can add your own static trades at any tier by creating JSON files in:

```
data/<namespace>/skill_master_trades/<name>.json
```

Each file is a JSON array of trade templates:

```json
[
    {
        "tier": 2,
        "input1": { "item": "minecraft:emerald", "count": 10 },
        "input2": { "item": "minecraft:book", "count": 1 },
    {
        "tier": 2,
        "input1": { "item": "minecraft:emerald", "count": 10 },
        "input2": { "item": "minecraft:book", "count": 1 },
        "output": { "item": "minecraft:diamond", "count": 1 },
        "maxUses": 8,
        "experience": 5,
        "multiplier": 0.05,
        "chance": 0.75
    }
]
```

### Trade Template Fields

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `tier` | Yes | — | Villager tier (1–5) when this trade appears |
| `input1` | Yes | — | Primary input item (`item` + `count`) |
| `input2` | No | Empty | Secondary input item |
| `output` | Yes | — | Output item |
| `maxUses` | No | 16 | Times the trade can be used before restock |
| `experience` | No | 2 | Villager XP earned per trade |
| `multiplier` | No | 0.05 | Price increase multiplier after each use |
| `chance` | No | 1.0 | Probability (0.0–1.0) that this trade is offered |

---

## Village Structures

### Skill Master Houses

Custom jigsaw-based structures generate naturally in the Overworld. They contain:
- A **Skill Scribe Table** workstation
- **Loot barrels** with progression materials (Blank Tomes, Emeralds, Tomes of Progression, Sigils of Imbuement)

Structure generation is configured at:
```
worldgen/structure_set/skill_master_houses.json
```

Default placement: random spread with **spacing 40** chunks, **separation 16** chunks.

### Structure Loot

Two loot tables are used for the barrels inside Skill Master Houses:

**Standard barrel** (`village_skill_scribe`):
- Blank Tome, Emeralds (1–5), Tome of Progression (rare), Sigil of Imbuement (uncommon)
- Food supplies (cooked chicken, bread, apples)
- Crafting materials (paper, sticks, ink sacs)

**Large barrel** (`village_skill_scribe_large`):
- Higher quantities of materials
- Same item pool with better roll counts

### Disabling Structure Generation

Set `disable_skill_master_house` in `config/puffish_skill_leveling.json` to `true` to fully disable Skill Master Houses.

When enabled, the mod now does both:
- suppresses the Skill Master House structure set for future worldgen
- removes already-generated `puffish_skill_leveling:skill_master_house` structures server-wide during server startup by rewriting saved chunk/region data directly

Important behavior:
- Existing houses are removed using the actual structure-piece footprint, not a full outer-box terrain wipe.
- This affects saved world structure data as well as the placed house blocks, so removed houses stay gone.
- The cleanup pass does not live-load world chunks to perform deletion.
- It does not disable the Skill Master profession, workstation, trades, or villager behavior.

### Configuration

The addon reads `config/puffish_skill_leveling.json` at startup. Current options:

| Option | Description |
|--------|-------------|
| `disable_skill_master_house` | Prevent new Skill Master House generation and remove already-saved houses during server startup |
| `require_unlock_for_imbuing` | Imbued gear bonuses require the base skill to be unlocked |
| `require_unlock_for_curio_imbuing` | Curio (Skill Charm) bonuses require the base skill to be unlocked |
| `debug_logging` | Enable verbose debug logging (persistent) |

`debug_logging` can also be toggled at runtime with:
```
/skillleveling debug true
/skillleveling debug false
```

---

## Profession Behavior

### Workstation Binding

The Skill Master uses the **Skill Scribe Table** as its Point of Interest (POI). Any unemployed villager within range of an unclaimed Skill Scribe Table will adopt the Skill Master profession, just like vanilla professions.

### Duplicate Prevention

A mixin prevents multiple Skill Masters from spawning within 32 blocks of each other. If a villager attempts to claim the Skill Master profession while another already exists nearby, it is blocked.

### Trade Regeneration

Unlike vanilla villagers whose trades are set once, the Skill Master **clears and rebuilds its entire trade list** every time a player interacts with it. This means:
- Trades always reflect the player's current skill state
- Different players see different trade inventories
- Refreshing the trade screen shows updated options

---

## Admin Commands

| Command | Description |
|---------|-------------|
| `/skillleveling villager forceProfession` | Force the villager you're looking at (within 10 blocks) to become a Skill Master |
| `/skillleveling villager setTier <1-5>` | Set the Skill Master's tier (affects trade pool and level range) |
| `/skillleveling villager reset` | Reset a Skill Master villager |

These require permission level 2 (operator).

---

## Early-Game to Late-Game Progression

The Skill Master is designed to serve as a reliable progression backbone across all stages:

### Early Game (Tier 1–2)
- **Blank Tomes** are cheap and readily available — the foundation for all skill acquisition.
- **Introductory skill tomes** let players discover skills they haven't tried yet (70% of dynamic trades target unlearned skills).
- **Tome of Clear Mind** lets players experiment and recover from bad choices without penalty.

### Mid Game (Tier 3)
- **Tome upgrades** begin appearing: trade a lower-level tome + emeralds for a higher-level version. This rewards keeping spare tomes around.
- **Dedicated upgrade trades** (up to 3) specifically target skills the player is actively leveling.
- **Tome of Cleansing II** enables extracting imbued skills from equipment to reconfigure builds.

### Late Game (Tier 4–5)
- **Highest-level tomes** become available, covering the final 20–40% of each skill's level range.
- **Tome of Greater Clear Mind** (50% chance at Tier 4) allows full skill resets for build experimentation.
- **Sigil of Imbuement** (Tier 5) is the gateway to the equipment imbuing system.
- **Tome of Progression** (Tier 5) lets players choose any skill to advance — the ultimate flexibility item.
- **Mastery discounts** mean experienced players pay less for the most powerful trades.

### Mastery Endgame
- Maxing skills further reduces prices and increases upgrade trade frequency.
- The Skill Master's XP bonus from mastery accelerates his own tier progression.
- At 3+ mastered skills, the Skill Master personally acknowledges the player's achievement.

---

## Tips & Best Practices

- **Place Skill Scribe Tables in player hubs** — every base should have one to attract a Skill Master.
- **Trade often at low tiers** — even if you don't need the tomes, trading levels up the villager to unlock better inventory.
- **Keep duplicate tomes** — at Tier 3+ the Skill Master may offer to upgrade them for emeralds, which is cheaper than buying the next level outright.
- **Master easy skills first** — the mastery bonuses apply globally, so maxing cheap 1-level skills improves pricing for expensive multi-level skills.
- **Use the `forceProfession` command** if a village's Skill Scribe Table isn't being claimed. The duplicate-prevention check blocks adoption within 32 blocks of an existing Skill Master.
- **Override the reputation config** via datapack to tune the economy for your server (faster/slower tier progression, cheaper/expensive trades).

---

[< Back to Core Index](index.md) | [Next: Skill Imbuement System >](Skill_Imbuement_System.md)
