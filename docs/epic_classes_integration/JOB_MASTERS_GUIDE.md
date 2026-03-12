# Job Masters

[< Back to Epic Classes Index](index.md) | [Next: Class Attributes & Scaling >](CLASS_ATTRIBUTES_GUIDE.md)

---

> [!WARNING]
> **CURRENT LIMITATION / FUTURE ADDITION:** Job Masters are currently listed as a **future addition** and are not actively used natively in the current release. While the configs exist, NPC class changing via dialogue overlaps with native Epic Classes behavior that is pending an upstream overhaul. Rely on the `/skillleveling advanceclass` command or native progression trees for now.

Job Masters are Custom NPCs that allow players to change or select classes. Define them in `data/<namespace>/puffish_skill_leveling/epic_classes/job_masters/`.

## Configuration Syntax

```json
{
  "id": "job_master_gunslinger",
  "marker_block": "minecraft:iron_block",
  "texture": "example_mod:textures/entity/npc/gunslinger.png",
  "name_key": "npc.example_mod.job_master.gunslinger",
  "dialogue_key": "main__gui.epicclassmod.quest.job_master.gunslinger",
  "equipment": {
    "helmet": "minecraft:leather_helmet",
    "chestplate": "minecraft:iron_chestplate",
    "mainhand": "tacz:modern_kinetic_gun{GunId:\"tacz:deagle\"}"
  }
}
```

## Field Explanations

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | String | **Yes** | Unique identifier for your job master config. |
| `marker_block` | String | No | Registry ID of the block that triggers spawning/location marking. |
| `texture` | String | No | Resource location of the NPC's custom texture. |
| `name_key` | String | No | Translation key for the NPC's display name. |
| `dialogue_key` | String | No | Translation key for the overarching NPC dialogue. |
| `equipment` | Object | No | Key-value mapping of the NPC's worn equipment and weapons. Valid keys: `mainhand`, `offhand`, `helmet`, `chestplate`, `leggings`, `boots`. Values are item IDs/NBT strings. |

By assigning this configuration ID to a matching Job Master entity in the world, the system will apply the specified texture, name, equipment, and dialogue to act as a gateway to class changes.

---

[< Back to Epic Classes Index](index.md) | [Next: Class Attributes & Scaling >](CLASS_ATTRIBUTES_GUIDE.md)
