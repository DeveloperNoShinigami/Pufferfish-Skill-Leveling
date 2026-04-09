# CNPC Class NPCs

[< Back to Epic Classes Index](index.md) | [Next: Class Attributes & Scaling >](CLASS_ATTRIBUTES_GUIDE.md)

---

Epic Class-facing NPC flows are now CNPC-owned. There is no separate `job_masters/` datapack path anymore.

To make a CustomNPCs NPC participate in class selection or class advancement, assign the class directly in stored data:

```js
function init(event) {
    event.npc.getStoreddata().put("job_master", "adventurer");
}
```

That value must match the exact custom class id from your Epic Class datapack. The bridge reads the NPC's stored data and opens the correct class UI while CNPC continues to own the visible dialog.

If the same NPC also acts as a quest giver, add a quest role id as well:

```js
function init(event) {
    event.npc.getStoreddata().put("job_master", "adventurer");
    event.npc.getStoreddata().put("quest_npc", "starter_town");
}
```

## Supported Stored Data Keys

| Key | Type | Required | Description |
|---|---|---|---|
| `job_master` | String | No | Exact custom class id for NPCs that open class-related flows. |
| `quest_npc` | String | No | Exact quest role or pool id for NPCs that participate in mirrored CNPC quest flows. |
| `cnpcQuestMappings` | String (JSON) | No | Optional JSON string containing per-quest metadata such as `classId`, `bookCategory`, and `title`. |

## Example Combined Setup

```js
function init(event) {
    event.npc.getStoreddata().put("job_master", "adventurer");
    event.npc.getStoreddata().put("quest_npc", "starter_town");
    event.npc.getStoreddata().put("cnpcQuestMappings", JSON.stringify({
        "1": {
            "title": "Test Quest 2",
            "bookCategory": "job"
        }
    }));
}
```

## Notes

- `job_master` replaces the old `job_master_id` / `job_masters/<id>.json` pipeline.
- `quest_npc` is a string role id, not a boolean flag.
- `cnpcQuestMappings` uses the same object shape as the bridge config and is keyed by the exact CNPC quest id.
- When a custom class is active, the class book uses that custom class's display identity in the job quest area instead of surfacing the Epic Class proxy enum.

---

[< Back to Epic Classes Index](index.md) | [Next: Class Attributes & Scaling >](CLASS_ATTRIBUTES_GUIDE.md)
