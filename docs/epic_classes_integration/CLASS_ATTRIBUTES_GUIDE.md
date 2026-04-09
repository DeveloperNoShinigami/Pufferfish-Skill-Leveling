# Class Attributes & Scaling

[< Back to Epic Classes Index](index.md) | [Next: Global Bridge Configuration >](GLOBAL_BRIDGE_CONFIG.md)

---

This system bridges the gap between Pufferfish points and Epic Fight stats. Place your configuration in `data/<namespace>/epicclassmod/epic_class_attributes.json`.

> [!TIP]
> This config evaluates mathematical expressions. You can scale stats exactly how you want.

## Configuration Syntax

```json
{
  "attributes_by_class": {
    "gunslinger": [
      {
        "id": "gunslinger_page",
        "slots": [
          {
            "id": "movement_speed",
            "name": "gui.epicclassmod.speed",
            "icon": "minecraft:feather",
            "attribute_id": "minecraft:generic.movement_speed",
            "format": "+%.2f",
            "value": "points * 0.01",
            "operation": "ADDITION",
            "max_points": 10,
            "description": "Increases your movement speed."
          }
        ]
      }
    ]
  }
}
```

## Field Explanations

* **`attributes_by_class`** *(Required)*: The root object containing a map of class IDs to their attribute pages.
* **`id`** (Page) *(Required)*: A unique identifier for the attribute page.
* **`slots`** *(Required)*: The list of attribute definitions on this page.

### Slot Fields

* **`value`** *(Required)*: The math expression defining the stat increase. `points` refers to points invested.
* **`attribute_id`** *(Required)*: The fully-qualified Minecraft attribute ID to modify. Standard vanilla attributes (`minecraft:generic.max_health`, etc.) and RO stat attributes (`roleveling:str`, `roleveling:agi`, `roleveling:vit`, `roleveling:int`, `roleveling:dex`, `roleveling:luk`) are both valid. The `roleveling:*` attributes are real Minecraft attributes — their `getValue()` automatically includes any item `AttributeModifier` contributions without manual gear scanning.
* **`operation`** *(Required)*: The operation applied (`ADDITION`, `MULTIPLY_BASE`, `MULTIPLY_TOTAL`).
* **`id`** *(Required)*: Unique ID for the attribute slot.
* **`name`** *(Required)*: Translation key or literal string for the attribute's display name.
* **`format`** *(Optional)*: Number format string for the UI display (e.g., `+#`, `+%.2f`). Use `+#` for whole-number stats like RO base stats.
* **`max_points`** *(Optional)*: Caps the stat scaling to a maximum number of invested points.
* **`point_cost`** *(Optional)*: Math expression defining how many class stat points this slot costs per allocation. Defaults to `1` per point if omitted. Supports the `points` variable.
* **`command`** *(Optional)*: A server command fired once when the player allocates a point into this slot. Tokens: `{value}` (the current computed value), `{player}` (player name). When present the attribute modifier is still applied — this runs in addition, not instead.
* **`icon`** *(Optional)*: Item ID used as the icon in the attribute UI.
* **`description`** *(Optional)*: Tooltip description text.

### Command Slot Behavior

When using `command` on a slot, keep these runtime rules in mind:

- **Shorthand numeric `value` is per-point scaling**: If `value` is a plain number and does not reference `points` (for example, `"1"`), the effective value is treated as `number * points`.
- **Reset-safe command slots**: On full stat reset, affected command slots are executed once with `{value}=0` so command-driven side effects are explicitly cleared.
- **Immediate reset UI consistency**: The Class Book applies an optimistic client-side clear on reset confirmation so command-only slot values update immediately in the open UI.

> [!NOTE]
> The `"global"` class key is a special reserved key in `attributes_by_class`. Pages listed under `"global"` are shown in the Class Book attribute screen for **every** class. Use this for shared stat pools (e.g. RO base stats STR/AGI/VIT/INT/DEX/LUK) that all classes in your server share.

### Mathematical Expressions (`value`)

The `value` field evaluates mathematical formulas to determine the attribute's exact bonus. The only variable available in this context is `points`, which represents how many class points the player has invested into this specific attribute.

You can use standard math to create exactly the scaling curve you want:

* **Basic Operators:** `+`, `-`, `*`, `/`
* **Exponents/Powers:** `^` (e.g., `points ^ 2` for quadratic scaling)
* **Parentheses:** `()` (e.g., `(points + 1) * 0.5`)
* **Functions (Clamping/Bounds):** `min(A, B)`, `max(A, B)`

**Examples:**
- **Linear:** `points * 0.2` (Directly scales by 0.2 per point)
- **Base + Scaling:** `1.0 + (points * 0.1)` (Starts at a baseline of 1.0, adds +0.1 per point)
- **Exponential:** `(points ^ 1.5) * 0.05` (Grows faster the more points are invested)
- **Expression Clamping:** `min(points * 0.5, 10.0)` (Caps the generated value at exactly 10.0, regardless of points)

> [!TIP]
> If you just want to clamp how many *points* apply to the stat (e.g., "stop giving bonuses after 20 points"), use the **`max_points`** field instead of building a complex `min()` math expression!

---

[< Back to Epic Classes Index](index.md) | [Next: Global Bridge Configuration >](GLOBAL_BRIDGE_CONFIG.md)
