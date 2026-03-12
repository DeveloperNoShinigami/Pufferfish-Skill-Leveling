# Global Bridge Configuration

[< Back to Epic Classes Index](index.md) | [Next: Datapack Reference >](DATAPACK_REFERENCE.md)

---

The main bridge configuration file connects Pufferfish Skill Leveling and Epic Classes together at a global level.

**Config location:** `config/pufferfish_epic_classes_bridge.json` (or via Server Configs depending on your setup)

## Features

The Bridge config allows you to specify:
1. **Enable/Disable the bridge:** You can toggle the integration entirely.
2. **Category Mappings:** Specify which Pufferfish Skill category corresponds to the main classes.
3. **Stat Conversions:** Configure global conversion rates for attributes.

## Configuration Sub-Schemas

The Bridge config file uses the following schema:

| Field | Type | Default | Description |
|---|---|---|---|
| `enabled` | Boolean | `true` | Master toggle to enable or disable the Bridge entirely. |
| `classToCategoryMap` | Map<String, List<String>> | (Base classes) | Maps an Epic Fight/Classes class ID to a list of Pufferfish category IDs. |
| `categorySyncEnabled` | Map<String, Boolean> | (Base classes) | Toggles automatic level synchronization for specific Pufferfish categories. |
| `statToSkillMap` | Map<String, String> | Empty | Maps an Epic Fight stat ID directly to a Pufferfish Skill node. |
| `classPassiveToSkillMap` | Map<String, String> | (Base classes) | Maps UI passive slots (`CLASSNAME_0`) to Pufferfish Skill nodes. |
| `classPassiveToLevelMap` | Map<String, Integer> | (Base classes) | Level requirements for UI passives (`CLASSNAME_0`). |
| `autoActivateCategory` | Boolean | `true` | Automatically unlocks and switches to the mapped Pufferfish category when a player changes classes. |
| `lockOtherCategories` | Boolean | `true` | Automatically locks all non-mapped Pufferfish categories when a player changes classes. |
| `syncOnLogin` | Boolean | `true` | Re-syncs levels and attributes when a player joins the server. |
| `disableBaseClasses` | Boolean | `false` | Disables selection of the base Epic Classes via traditional menus if utilizing custom class progression. |

### Synchronization Stability (March 11 Update)

The Bridge now implements a **Strict Authoritative Sync** model. Pufferfish Skills is treated as the source of truth for all mapped XP categories. 

- **Recursion Guard**: Uses a depth-counter (`SYNC_DEPTH`) to ensure that even if multiple internal methods are called during an XP transaction, only a single, precise synchronization packet is sent to Epic Classes.
- **Notification Passthrough**: The bridge calculates the exact delta of XP gained and passes it to the Epic Classes HUD, ensuring consistent "EXP Gain" toasts without duplication.

---


[< Back to Epic Classes Index](index.md) | [Next: Datapack Reference >](DATAPACK_REFERENCE.md)
