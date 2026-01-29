# Pufferfish Skill Leveling Addon

**An addon for Pufferfish's Skills mod that adds multi-level skill progression, skill tomes, and equipment imbuing.**

> [!IMPORTANT]
> **This is an ADDON** — it requires the base [Pufferfish Skills](https://www.curseforge.com/minecraft/mc-mods/pufferfish-skills) mod. All features are optional and designed to work alongside the original mod without conflicts.

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [Getting Started](./docs/GETTING_STARTED.md) | Installation and quick-start tutorial |
| [Features](./docs/FEATURES.md) | Complete list of all addon features |
| [Datapack Guide](./docs/DATAPACK_GUIDE.md) | How to create custom skill datapacks |
| [Roadmap](./docs/ROADMAP.md) | Planned features for future releases |

---

## ✨ Key Features

### Multi-Level Skills
- Skills can have multiple levels (1, 2, 3, ... N)
- Each level grants cumulative or distinct rewards
- Dynamic tooltips show current level and progression

### Skill Tomes
- **Tome of Progression** — Advance any skill by 1 level
- **Tome of Clear Mind** — Refund 1 skill level
- **Skill Tome** — Lootable items that grant specific skills

### Equipment Imbuing
- Apply skills to equipment via anvil
- Bonuses activate when worn, deactivate when removed
- Real-time attribute sync (hearts, damage, speed update instantly)

---

## ⚠️ Important Notes

> [!WARNING]
> **No Production Datapack Included**  
> This addon does not ship with a ready-to-use datapack. You must create your own using the [Datapack Guide](./docs/DATAPACK_GUIDE.md).

> [!NOTE]
> **Metadata Required**  
> Every skill definition **must** include a `"metadata": {}` field (even if empty) or the configuration will fail to load.

---

## 📋 Requirements

- **[Pufferfish Skills](https://www.curseforge.com/minecraft/mc-mods/pufferfish-skills)** v0.17.1+ for MC 1.20
- **Java 17+**
- **Minecraft 1.20**
- **Forge 47.4.10+** or **Fabric** (loader-specific builds)

---

## 📦 Installation

1. Install [Pufferfish Skills](https://www.curseforge.com/minecraft/mc-mods/pufferfish-skills)
2. Install this addon (Pufferfish Skill Leveling)
3. Launch Minecraft — both mods load together
4. Create your own skill datapack (see [Datapack Guide](./docs/DATAPACK_GUIDE.md))

---

## 🎮 Commands

| Command | Description |
|---------|-------------|
| `/skillleveling get <player> <category> <skill>` | View skill level |
| `/skillleveling set <player> <category> <skill> <level>` | Set skill level |
| `/skillleveling advance <player> <category> <skill>` | Advance by 1 level |
| `/skillleveling refund one <player> <category> <skill>` | Refund 1 level |
| `/skillleveling refund all <player> <category> <skill>` | Reset to level 0 |

---

## 🤝 Compatibility

This addon is designed to be **fully compatible** with the base Pufferfish Skills mod:

- **Non-Invasive**: All features are opt-in via datapack
- **Parallel Storage**: Addon data stored separately from base mod
- **Graceful Fallback**: Removing the addon doesn't break base mod functionality
- **Mod-Friendly**: Attribute sync works with any mod using Minecraft's attribute system

---

## 📄 License

This project is open source. See the repository for license details.

---

*For issues, feature requests, or contributions, visit the project repository.*
