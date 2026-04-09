# CNPC Quest Mode

[< Back to Epic Classes Index](index.md) | [Next: Global Bridge Configuration >](GLOBAL_BRIDGE_CONFIG.md)

---

CNPC quest mode lets **CustomNPCs** stay in charge of quest and dialog presentation while **Pufferfish Skill Leveling** remains in charge of class progression, Pufferfish category sync, and future mirrored book data.

## Enable CNPC Quest Mode

Set this in `data/<namespace>/puffish_skills_leveling/epicclassmod/pufferfish_skills_bridge.json`:

```json
{
  "useCnpcQuests": true
}
```

## NPC Stored Data Markers

Use CustomNPCs `storeddata` to mark NPC roles:

```js
function init(event) {
    event.npc.getStoreddata().put("job_master", "adventurer");
    event.npc.getStoreddata().put("quest_npc", "starter_town");
}
```

Supported keys:

- `job_master`: exact class ID for class-capable NPCs
- `quest_npc`: exact quest role or pool ID for general quest NPCs

An NPC can define either key or both.

## Quest Mapping

Use `cnpcQuestMappings` to tell the bridge where a CustomNPCs quest should appear in Epic Class-facing UI:

```json
{
  "useCnpcQuests": true,
  "cnpcQuestMappings": {
    "1": {
      "bookCategory": "main"
    },
    "2": {
      "classId": "epicclassmod:adventurer",
      "bookCategory": "job"
    },
    "3": {
      "classId": "epicclassmod:warrior",
      "bookCategory": "job",
      "trackStructure": "cataclysm:burning_arena"
    }
  }
}
```

The key in `cnpcQuestMappings` must be the **exact numeric CNPC quest ID** (find it in the CNPC quest editor — it is always a number).

### Quest Mapping Fields

| Field | Type | Required | Description |
|---|---|---|---|
| `bookCategory` | String | No | Where the quest appears in the ECM book: `"main"`, `"job"`, or `"sub"` |
| `classId` | String | No | Class this quest belongs to (e.g. `"epicclassmod:warrior"`). Required for `"job"` quests to filter correctly per class. |
| `title` | String | No | Human-readable label — not used by the UI, only for your own reference |
| `trackStructure` | String | No | Structure registry ID to enable the Track button for this quest (e.g. `"cataclysm:burning_arena"`). Omit to hide the track button entirely. |

By default, no structure tracking marker appears on CNPC quests. Set `trackStructure` only on quests where the player needs to hunt down a specific structure.

You can also define the same object on an NPC through CustomNPCs `storeddata` if you want script-driven setup instead of datapack-only setup:

```js
function init(event) {
    event.npc.getStoreddata().put("cnpcQuestMappings", JSON.stringify({
        "3": {
            "classId": "epicclassmod:warrior",
            "bookCategory": "job",
            "trackStructure": "cataclysm:burning_arena"
        }
    }));
}
```

Rules:

- the stored-data key name stays `cnpcQuestMappings`
- the real lookup key is still the exact numeric quest id
- `title` is optional and human-readable only
- datapack `cnpcQuestMappings` still take priority if both sources define the same quest id

## Runtime Behavior and Edge Cases

### Soft Dependency Behavior

- If CustomNPCs is not installed, CNPC bridge hooks stay inert and the addon continues running.
- `useCnpcQuests` does not hard-require CNPC classes at runtime.

### Missing Mapping Behavior

- If a CNPC quest ID has no mapping, the quest still works in CustomNPCs.
- It just will not be mirrored into Epic Class-facing quest UI categories.
- This is expected and useful for side quests you do not want shown in class progression views.

### Invalid `classId` on Job Category

- `bookCategory: "job"` should include a valid `classId`.
- If `classId` is omitted or wrong, filtering can hide that quest from the intended class context.

### Structure Tracker Auto-Clear

- If a mapping defines `trackStructure`, the tracker can be cleared automatically when the mapped quest is completed.
- This prevents stale tracking arrows/targets after the quest objective is done.
- Behavior is event-driven off quest state updates (not a polling tick loop).

### Mapping Source Priority

- Datapack mapping is authoritative when a quest ID exists in both datapack config and NPC storeddata.
- Storeddata mappings are best for local scripted overrides when a datapack entry does not exist.

### ID Format Reminder

- Quest mapping keys must be numeric CNPC quest IDs as strings in JSON object keys (for example, `"3"`).
- Do not use quest names or internal display labels as keys.

## Recommended Workflow

1. Author the visible dialog and quest flow in CustomNPCs.
2. Tag the NPC with `job_master`, `quest_npc`, or both.
3. Add `cnpcQuestMappings` entries only for quests that should appear in the Epic Class book/overlay.
4. Use a small helper script to call the bridge when a quest milestone should open class selection or refresh bridge state.

---

[< Back to Epic Classes Index](index.md) | [Next: Global Bridge Configuration >](GLOBAL_BRIDGE_CONFIG.md)
