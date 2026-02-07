# Pufferfish Skill Leveling Addon

**An addon for Pufferfish's Skills mod that adds multi-level skill progression, skill tomes, equipment imbuing, and the specialized Skill Master villager.**

> [!IMPORTANT]
> **This is an ADDON** — it requires the base [Pufferfish Skills](https://www.curseforge.com/minecraft/mc-mods/pufferfish-skills) mod. All features are optional and designed to work alongside the original mod without conflicts.

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [Getting Started](./docs/GETTING_STARTED.md) | Installation and quick-start tutorial |
| [Features](./docs/FEATURES.md) | Complete list of all addon features |
| [Datapack Guide](./docs/DATAPACK_GUIDE.md) | How to create custom skill datapacks |
| [Roadmap](./docs/ROADMAP.md) | Planned features and project progress |

---

## ✨ Key Features

### Multi-Level Skills
- Skills can have multiple levels (1 to N).
- Each level grants rewards and contributes to "Mastery".
- Dynamic tooltips show current total level (Base + Bonuses).

### Skill Master Villager
- Dedicated profession for trading skill-related items.
- **Dynamic Trading Pool**: Offers change based on your progression and "Mastery".
- **Reputation System**: Villager tiers up as you trade; higher tiers unlock better wares.
- **Tome Upgrades**: Exchange lower-level tomes and emeralds for higher-level ones.

### Overhauled Skill Tomes
- **Skill Tome** — Primary way to learn and level up specific skills.
- **Sigil of Imbuement** — Rare sigils used to enchant equipment with skills.
- **Tome of Clear Mind** — Refund skill levels and reclaim points.
- **Tome of Cleansing** — Specialized extraction tomes to remove skills from gear.
- **Blank Tome** — The foundation for crafting advanced tomes.

### Equipment Imbuing
- Apply skills to weapons and armor via the Anvil.
- Bonuses update attribute stats (Hearts, Damage, etc.) in real-time.

### Curios Integration
- **Skill Charms**: Dedicated items that provide skill bonuses when placed in Curio slots.
- **Auto-Sync**: Charms activate bonuses immediately upon equipping without needing base skill unlocks.

---

## 🎮 Staff Commands

| Command | Description |
|---------|-------------|
| `/skillleveling get <player> <category> <skill>` | View current base skill level |
| `/skillleveling set <player> <category> <skill> <level>` | Set base level (Admin Override) |
| `/skillleveling refund <player> <category> <skill> [amount\|all]` | Refund levels and return points |
| `/skillleveling villager forceProfession` | Turn a villager into a Skill Master |
| `/skillleveling villager setTier <1-5>` | Directly set a Skill Master's level |
| `/skillleveling villager reset` | Reset looking-at Skill Master to Tier 1 / 0 Exp |
| `/skillleveling info <player> <cat> <skill>` | Detailed level and point breakdown |

---

## ⚠️ Requirements

- **[Pufferfish Skills](https://www.curseforge.com/minecraft/mc-mods/pufferfish-skills)** v0.17.1+
- **Minecraft 1.20.1**
- **Forge** or **Fabric** (latest versions recommended)

---

## 🤝 Compatibility
- **Curios API**: Full support for dedicated accessory slots and skill-imbued charms.
- **Native Attribute Sync**: Works seamlessly with any mod using Minecraft's attributes.
- **Clean NBT Storage**: Uses standard player data paths for maximum reliability.
- **Namespace Agnostic**: Works across any namespace defined in your datapacks.

---

## 📄 License
This project is open source. See the repository for license details.

---
*For issues, feature requests, or contributions, visit the project repository.*
