# Pufferfish Skill Leveling

A multi-level progression addon for [Pufferfish Skills](https://modrinth.com/mod/puffish-skills) on Minecraft 1.20.1. Extends the base skill tree system with leveling, toggleable abilities, equipment imbuing, loot injection, and more — all driven by datapacks.

Supports **Forge** and **Fabric** from a shared codebase via Architectury.

---

## Documentation

### Block 1: Pufferfish Skills Leveling Core
This core documentation covers everything that works independently of Epic Classes.

| Guide | Description |
|-------|-------------|
| [Core Index](docs/core_features/index.md) | **Start Here:** The main navigation hub for core features |
| [Getting Started](docs/core_features/GETTING_STARTED.md) | First-time setup, creating your first datapack and skill |
| [Features Guide](docs/core_features/FEATURES.md) | Complete core feature list and explanations |
| [Datapack Guide](docs/core_features/DATAPACK_GUIDE.md) | Full datapack schema, field reference, and best practices |
| [Toggle System](docs/core_features/Toggle_System.md) | Pure, basic, and hybrid toggle skills with examples |
| [Skill Master](docs/core_features/Skill_Master_System.md) | Villager profession, trades, tiers, and mastery pricing |
| [Skill Imbuement](docs/core_features/Skill_Imbuement_System.md) | Dynamic skill imbuing on loot equipment |
| [Universal Loot](docs/core_features/Universal_Loot_System.md) | Injecting Skill Tomes and items into loot tables |

### Block 2: Epic Class / Rise of Heroes Integration
This block provides a comprehensive guide to bridging Pufferfish Skills with the Epic Classes mod.

| Guide | Description |
|-------|-------------|
| [Integration Index](docs/epic_classes_integration/index.md) | **Start Here:** The main navigation hub for integration features |
| [Global Bridge Config](docs/epic_classes_integration/GLOBAL_BRIDGE_CONFIG.md) | Setting up the main bridge behavior and class-to-category mapping |
| [Epic Class Creation Guide](docs/epic_classes_integration/EPIC_CLASS_CREATION_GUIDE.md) | Creating, inheriting, and configuring custom Epic Classes |
| [Job Masters Guide](docs/epic_classes_integration/JOB_MASTERS_GUIDE.md) | Outfitting and configuring NPC Job Masters |
| [Class Attributes Guide](docs/epic_classes_integration/CLASS_ATTRIBUTES_GUIDE.md) | UI layout, custom math expressions, and max points |
| [Item Restrictions Guide](docs/epic_classes_integration/ITEM_RESTRICTIONS_GUIDE.md) | Datapack-driven item usage and attack restriction rules |
| [Datapack Reference](docs/epic_classes_integration/DATAPACK_REFERENCE.md) | Raw JSON cheat sheet for all fields across the integration |

### Project Updates
| Document | Description |
|----------|-------------|
| [Changelog](docs/CHANGELOG.md) | Version history and recent fixes |
| [Roadmap](docs/ROADMAP.md) | Completed, planned, and experimental features |

A working template datapack is included at `docs/Datapack_Example/epic_class_template/`.

---

## Key Features

- **Multi-level skills** — Up to 999 levels per skill with per-level rewards
- **Toggle abilities** — On/off skills with cooldowns, keybinds, and auto-disable on death
- **Category gating** — Lock entire skill categories behind prerequisite requirements
- **Equipment imbuing** — Sigils open slots; Skill Tomes apply, upgrade, and extract skills via anvil
- **Dynamic imbuement** — Loot equipment spawns with pre-imbued skills based on dimension, distance, and category
- **Universal loot injection** — Drop Skill Tomes and Charms from chests and mobs
- **Skill Master villager** — Custom profession with mastery-scaled trades and village structures
- **Curios integration** — Skill Charms work as wearable accessories
- **Epic Classes Bridge** — Native datapack support for `Epic Classes`, bridging job masters, starting NBT items, and multi-file MMORPG skill trees.
- **Datapack-driven** — Every skill, reward, cost, loot rule, and epic class is defined in JSON

---

## Admin Commands

All commands require permission level 2 (operator).

| Command | Description |
|---------|-------------|
| `/skillleveling set <player> <category> <skill> <level>` | Set a skill to a specific level |
| `/skillleveling get <player> <category> <skill>` | View a player's current skill level |
| `/skillleveling refund <player> <category> <skill>` | Refund all paid points from a skill |
| `/skillleveling info <player> <category> <skill>` | Show detailed skill state (level, paid/granted, toggle) |

---

## Requirements

- **Minecraft** 1.20.1
- **Pufferfish Skills** v0.17.1
- **Architectury API** (matching your loader version)
- **Forge** or **Fabric** (with Fabric API)
- **Curios API** *(optional, for Skill Charm support)*

---

## Building

```bash
./gradlew build
```

Output JARs are placed in `Forge/build/libs/` and `Fabric/build/libs/`.

---

## License

See [LICENSE.txt](LICENSE.txt) for code and [LICENSE-RESOURCES.txt](LICENSE-RESOURCES.txt) for assets.
