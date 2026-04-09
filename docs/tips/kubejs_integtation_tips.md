# KubeJS Integration Tips (RO Stats)

This is a generalized setup guide for RO-style stat systems in KubeJS packs.

## 1) Attribute Setup

Pick a stable attribute namespace and keep it consistent everywhere.

Common example IDs:

- `roleveling:str`
- `roleveling:agi`
- `roleveling:vit`
- `roleveling:int`
- `roleveling:dex`
- `roleveling:luk`

Example startup registration (custom attributes):

```js
// kubejs/startup_scripts/ro_attributes.js
let RangedAttribute = Java.loadClass('net.minecraft.world.entity.ai.attributes.RangedAttribute')

StartupEvents.registry('attribute', event => {
  event.createCustom('roleveling:str', () => new RangedAttribute('attribute.roleveling.str', 0.0, -999.0, 999.0))
  event.createCustom('roleveling:agi', () => new RangedAttribute('attribute.roleveling.agi', 0.0, -999.0, 999.0))
  event.createCustom('roleveling:vit', () => new RangedAttribute('attribute.roleveling.vit', 0.0, -999.0, 999.0))
  event.createCustom('roleveling:int', () => new RangedAttribute('attribute.roleveling.int', 0.0, -999.0, 999.0))
  event.createCustom('roleveling:dex', () => new RangedAttribute('attribute.roleveling.dex', 0.0, -999.0, 999.0))
  event.createCustom('roleveling:luk', () => new RangedAttribute('attribute.roleveling.luk', 0.0, -999.0, 999.0))
})
```

If your modpack already registers these in Java, do not register the same IDs again in KubeJS.

## 2) Attach Attributes To Entities

Attributes must be attached to entity types to be usable at runtime.

Example (attach to all types):

```js
// kubejs/startup_scripts/entity_attribute_reg.js
EntityJSEvents.attributes(event => {
  event.allTypes.forEach(type => {
    event.modify(type, a => {
      a.add('roleveling:str')
      a.add('roleveling:agi')
      a.add('roleveling:vit')
      a.add('roleveling:int')
      a.add('roleveling:dex')
      a.add('roleveling:luk')
    })
  })
})
```

If a type is missing attribute attachment, reads and formula output can become inconsistent.

## 3) Epic Class Book Slot Patterns

Use one of these three patterns when building Epic Class Book stat slots.

### A) Attribute-only slot

Use this when the slot should only modify an attribute.

```json
{
  "id": "str",
  "attribute_id": "roleveling:str",
  "value": "1",
  "operation": "ADDITION",
  "max_points": 99
}
```

### B) Command-only slot

Use this when the slot should only run a command.

```json
{
  "id": "atk_dmg_test",
  "attribute_id": null,
  "value": "1",
  "operation": "ADDITION",
  "max_points": 10,
  "command": "attribute {player} minecraft:generic.attack_damage base set {value}"
}
```

### C) Hybrid slot (attribute + command)

Use this when you want both direct attribute scaling and command-triggered refresh/effects.

```json
{
  "id": "dex",
  "attribute_id": "roleveling:dex",
  "value": "1",
  "operation": "ADDITION",
  "max_points": 99,
  "command": "roleveling refresh"
}
```

Token support in commands:

- `{player}` -> player name
- `{value}` -> computed slot value

## 4) Server vs Singleplayer Script Placement

Singleplayer:

- Usually one local KubeJS setup is enough because integrated server and client run together.

Dedicated server:

- Server must have authoritative gameplay scripts (allocation, refresh, formula application).
- Client should also have matching display/UI-side scripts when your stat display depends on client script context.

If scripts exist only on one side in dedicated setups, gameplay can still run but visible values may appear stale or mismatched.

## Quick Validation Checklist

1. Allocate one point and verify immediate stat/effect change.
2. Run your debug/status command and verify expected derived values.
3. Reset points and verify effects clear to expected values.
4. Re-log and verify values stay consistent.
