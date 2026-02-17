# Tier 3 — Dynamic Class Registration & ClassSelectScreen Extension

## Overview

Tier 3 is the most ambitious: it allows Pufferfish Skills categories to **define entirely new classes** that appear in Epic Class Mod's `ClassSelectScreen` with full visual presentation — custom armor, weapons, stats, passives, descriptions, and a draggable 3D player model.

This tier requires:
1. **Extending the ClassType enum** at runtime (or bypassing it entirely)
2. **Injecting new class pages** into ClassSelectScreen's navigation cycle
3. **Providing visual data** (armor sets, weapons, stat icons, passive icons) via optional fields in `category.json`
4. **Extending the network protocol** to handle string-based class identifiers instead of enum ordinals

**Key design decision:** All bridge data is embedded as optional fields inside the existing Pufferfish Skills `category.json` (under an `"epic_class_bridge"` key). No separate definition files are needed. Categories without this key work exactly as before — the integration is fully opt-in per category.

---

## Critical Challenge: Enum-Based Architecture

Epic Class Mod's entire architecture is built around the `ClassType` enum:

```java
public enum ClassType {
    NONE, WARRIOR, PALADIN, BERSERKER, REAPER, SORCERER, ARCHER
}
```

This enum is used **everywhere**:
- `PlayerClassData.classType` field (serialized as ordinal to NBT)
- `ChooseClassPacket` (sends ordinal over network)
- `SyncClassPacket` (sends ordinal over network)
- `ClassSelectScreen.current` (current displayed class)
- `ClassSelectScreen.statsFor()` switch (ordinal-based)
- `ClassSelectScreen.passivesFor()` switch (ordinal-based)
- `ClassSelectScreen.startingItems()` switch (ordinal-based)
- `ClassBookScreen` portrait resolution
- `JobWeaponRegistry` (EnumMap<ClassType, Set>)
- `ModSettings` per-class HP config
- All passive hooks (WarriorPassives, PaladinPassives, etc.)

**Adding new enum values at runtime is technically possible** via Unsafe/MethodHandles, but extremely fragile. The recommended approach is to **bypass** the enum system for custom classes.

---

## Architecture: Dual-Track System

```
┌─────────────────────────────────────┐
│  ClassSelectScreen (modified)       │
│                                     │
│  ◀── [WARRIOR] [PALADIN] ... ──▶   │  ← Original 6 classes (enum-based)
│  ◀── [CUSTOM1] [CUSTOM2] ... ──▶   │  ← Dynamic classes (bridge-based)
│                                     │
│  Navigation: prevClass/nextClass    │
│  modified to include custom entries │
│                                     │
│  Render: if custom → use bridge     │
│          data instead of switch     │
└─────────────────────────────────────┘
```

Custom classes exist **alongside** the original enum-based classes. The player cycles through all classes (original + custom) using the prev/next buttons. When a custom class is displayed, all visual data comes from the bridge's datapack-driven definition instead of the hardcoded switch statements.

---

## ClassSelectScreen Render Pipeline (Decompiled)

Understanding the full render pipeline is essential for injection. Here's the complete flow:

### render() method (m_88315_) — Step by Step

#### 1. Layout Computation
```java
if (guiScaleChanged || windowSizeChanged) {
    computeLayout();    // Recalculate drawX, drawY, drawW, drawH, globalScale
    rebuildButtons();   // Recreate prev/next/select Button widgets
}
```

#### 2. Popup Box
```java
// 520×360 page-unit popup centered on screen
fill(popupX, popupY, popupX+520, popupY+360, CLR_POPUP_BG);     // Background
fill(popupX, popupY, popupX+520, popupY+1, CLR_POPUP_EDGE);     // Top border
fill(popupX, popupY+359, popupX+520, popupY+360, CLR_POPUP_EDGE); // Bottom
fill(popupX, popupY, popupX+1, popupY+360, CLR_POPUP_EDGE);     // Left
fill(popupX+519, popupY, popupX+520, popupY+360, CLR_POPUP_EDGE); // Right
```

#### 3. Title
```java
drawStringPxBase(guiGraphics, font,
    tr("gui.epicclassmod.class_select.title"),
    pageToScreenX(popupX + 10),
    pageToScreenY(popupY + 10),
    -1, 1.0f, false);
```

#### 4. Left Panel Divider
```java
// Vertical divider at popupX + 210 (left panel = 210 units wide)
fill(dividerX, popupY+30, dividerX+1, popupY+350, CLR_DIVIDER);
```

#### 5. Player Model (Left Panel, ~210 page units wide)

```java
// Get the player entity
Player previewPlayer = minecraft.player;

// Save current equipment
ItemStack oldMainHand = previewPlayer.getMainHandItem().copy();
ItemStack oldOffHand = previewPlayer.getOffhandItem().copy();
ItemStack oldHead = previewPlayer.getItemBySlot(HEAD).copy();
ItemStack oldChest = previewPlayer.getItemBySlot(CHEST).copy();
ItemStack oldLegs = previewPlayer.getItemBySlot(LEGS).copy();
ItemStack oldFeet = previewPlayer.getItemBySlot(FEET).copy();

// Equip class-specific gear (switch on current.ordinal())
switch (current.ordinal()) {
    case 1: // WARRIOR
        equipArmorSet(player, "dungeons_and_combat:oni_slayer_");
        mainHand = modItem("efn:yamato_dmc4_in_sheath", new ItemStack(Items.IRON_SWORD));
        offHand = ItemStack.EMPTY;
        break;
    case 2: // PALADIN
        equipArmorSet(player, "dungeons_and_combat:silver_");
        mainHand = modItem("dungeons_and_combat:cobalt_long_sword", new ItemStack(Items.IRON_SWORD));
        offHand = modItem("dungeons_and_combat:silver_shield", new ItemStack(Items.SHIELD));
        break;
    case 3: // BERSERKER
        equipArmorSet(player, "dungeons_and_combat:crimson_helmet");
        mainHand = modItem("efn:ruinsgreatsword", new ItemStack(Items.IRON_SWORD));
        offHand = ItemStack.EMPTY;
        break;
    case 4: // REAPER
        equipArmorSet(player, "dungeons_and_combat:rogue_helmet");
        mainHand = modItem("efn:nf_dual_sword", new ItemStack(Items.IRON_SWORD));
        offHand = ItemStack.EMPTY;
        break;
    case 5: // SORCERER
        // Sorcerer has custom equipment assignment (no equipArmorSet call)
        mainHand = modItem("dungeons_and_combat:fairy_scepter", new ItemStack(Items.STICK));
        offHand = ItemStack.EMPTY;
        setSlot(HEAD, modItem("irons_spellbooks:netherite_mage_helmet", ...));
        setSlot(CHEST, modItem("irons_spellbooks:netherite_mage_chestplate", ...));
        setSlot(LEGS, modItem("irons_spellbooks:netherite_mage_leggings", ...));
        setSlot(FEET, modItem("irons_spellbooks:netherite_mage_boots", ...));
        break;
    case 6: // ARCHER
        equipArmorSet(player, "dungeons_and_combat:forgotten_knight_");
        mainHand = modItem("cataclysm:wrath_of_the_desert", new ItemStack(Items.BOW));
        offHand = ItemStack.EMPTY;
        break;
}

// Render the 3D player model
Quaternionf rotation = new Quaternionf()
    .rotationYXZ(
        (float)Math.toRadians(modelYaw),
        (float)Math.toRadians(modelPitch),
        (float)Math.toRadians(180)
    );
Quaternionf cameraRot = new Quaternionf().rotationXYZ(0, 0, 0);

InventoryScreen.renderEntityInInventoryFollowsMouse(
    guiGraphics,
    pageToScreenX(popupX + 105),     // X center of left panel
    pageToScreenY(popupY + 210),     // Y (above center)
    Math.max(30, pageSizeToScreenH(80)), // Scale
    rotation, cameraRot,
    previewPlayer
);

// Restore original equipment
previewPlayer.setItemInHand(MAIN_HAND, oldMainHand);
previewPlayer.setItemInHand(OFF_HAND, oldOffHand);
previewPlayer.setItemBySlot(HEAD, oldHead);
previewPlayer.setItemBySlot(CHEST, oldChest);
previewPlayer.setItemBySlot(LEGS, oldLegs);
previewPlayer.setItemBySlot(FEET, oldFeet);
```

#### 6. Class Title (Right Panel)
```java
String classTitle = trOr(titleKey(current), fallbackName(current));
drawStringPxBase(guiGraphics, font, classTitle,
    pageToScreenX(rightPanelStart),
    pageToScreenY(popupY + 44),
    -1, 1.0f, false);
```

#### 7. Stats (Right Panel)
```java
List<StatEntry> stats = statsFor(current);
for (StatEntry stat : stats) {
    // Draw stat label (e.g. "Health", "Attack")
    drawStringPxBase(graphics, font, tr(stat.labelKey),
        pageToScreenX(rightPanelStart), y, -1515080, 1.0f, false);

    // Draw repeated icons (e.g. 5 hearts out of 10 max)
    drawRepeatedIcons(graphics, stat.icon,
        pageToScreenX(rightPanelStart + 36), y - 2px,
        stat.count, stat.size, iconSpacing);

    y += lineHeight + 4px;
}
```

**StatEntry structure:**
```java
class StatEntry {
    String labelKey;            // e.g. "class.epicclassmod.stat.health"
    ResourceLocation icon;      // e.g. ICON_HEART
    int count;                  // Filled icon count (value)
    int size;                   // Total icon count (max)
}
```

**statsFor() per class:**
| Class | HP (of 10) | DEF (of 10) | ATK (of 10) | ASPD (of 10) | MSPD (of 10) |
|-------|-----------|-------------|-------------|--------------|--------------|
| Warrior | 6 | 5 | 7 | 5 | 6 |
| Paladin | 7 | 8 | 5 | 4 | 5 |
| Berserker | 5 | 3 | 9 | 6 | 5 |
| Reaper | 4 | 3 | 8 | 8 | 7 |
| Sorcerer | 4 | 2 | 6 | 3 | 5 |
| Archer | 5 | 4 | 7 | 7 | 7 |

**Icons:**
| Field | Texture Path |
|-------|-------------|
| ICON_HEART | `epicclassmod:textures/gui/icons/heart.png` |
| ICON_SHIELD | `epicclassmod:textures/gui/icons/shield.png` |
| ICON_SWORD | `epicclassmod:textures/gui/icons/sword.png` |
| ICON_SPEED | `epicclassmod:textures/gui/icons/boots.png` |
| ICON_BOOTS | `epicclassmod:textures/gui/icons/boots.png` |

#### 8. Class Description (Right Panel)
```java
String desc = tr(descKey(current));  // e.g. "class.epicclassmod.warrior.select_desc"
drawWrappedScaled(graphics, desc,
    pageToScreenX(rightPanelStart), y + 4px,
    pageSizeToScreenW(rightPanelWidth - 8), -3620955, 0.95f);
```

#### 9. Starting Items (Right Panel)
```java
drawStringPxBase(graphics, font,
    trOr("gui.epicclassmod.starting_items", "Starting Items"),
    ...);

List<ItemStack> items = startingItems(current);
// Render items in a row, 18px spacing per item, 6px gap
for (int i = 0; i < items.size(); i++) {
    int itemX = startX + i * (itemSize + gapSize);

    if (current == SORCERER && i <= 2) {
        // Sorcerer uses custom textures for first 3 items
        ResourceLocation specialIcon = (i == 0) ? iron_spell_book :
                                       (i == 1) ? fireball : fortify;
        graphics.blit(specialIcon, itemX, itemY, ...);
    } else {
        renderItemScaled(graphics, items.get(i), itemX, itemY, itemSize);
    }
}
```

#### 10. Passives Section (Bottom Area)
```java
drawStringPxBase(graphics, font,
    trOr("gui.epicclassmod.passives", "Passives"),
    pageToScreenX(leftX), pageToScreenY(passivesY - 12), -2734779, ...);

List<PassiveEntry> passives = passivesFor(current);
int cols = 2, gap = 8;
int cellW = (totalWidth - gap) / cols;
int cellH = (150 - gap) / 2;

for (int i = 0; i < passives.size(); i++) {
    int col = i % cols;
    int row = i / cols;
    int cx = leftX + col * (cellW + gap);
    int cy = passivesY + row * (cellH + gap);

    // Card background
    fill(cx, cy, cx + cellW, cy + cellH, 0x80201828); // semi-transparent dark

    // Borders (top, bottom, left, right)
    fill(cx, cy, cx+cellW, cy+1, 0x30EEEEEE);
    fill(cx, cy+cellH-1, cx+cellW, cy+cellH, 0x30EEEEEE);
    fill(cx, cy, cx+1, cy+cellH, 0x30EEEEEE);
    fill(cx+cellW-1, cy, cx+cellW, cy+cellH, 0x30EEEEEE);

    PassiveEntry passive = passives.get(i);

    // Passive icon (12px, positioned inside card)
    blit(passive.icon, iconX, iconY, 0, 0, 12, 12, 12, 12);

    // Passive name
    drawStringPxBase(graphics, font, trOr(passive.nameKey, passive.nameKey),
        iconX + 12 + 6, iconY + 1, -1515080, 1.0f, false);

    // Passive description (wrapped, smaller text)
    drawWrappedScaled(graphics, trOr(passive.descKey, passive.descKey),
        iconX + 8, descY, descWidth, -3620955, 0.95f);
}
```

**PassiveEntry structure:**
```java
class PassiveEntry {
    String nameKey;             // Lang key for passive name
    String descKey;             // Lang key for passive description
    ResourceLocation icon;      // Icon texture
}
```

#### 11. Navigation Buttons

Created in `rebuildButtons()`:
```java
// Previous class button
addRenderableWidget(new Button(
    pageToScreenX(popupX + 4),
    pageToScreenY(popupY + 10),
    pageSizeToScreenW(20),
    pageSizeToScreenH(20),
    Component.literal("<"),
    btn -> prevClass()
));

// Next class button
addRenderableWidget(new Button(
    pageToScreenX(popupX + 496),
    pageToScreenY(popupY + 10),
    pageSizeToScreenW(20),
    pageSizeToScreenH(20),
    Component.literal(">"),
    btn -> nextClass()
));

// Select button
addRenderableWidget(new Button(
    pageToScreenX(popupX + 200),
    pageToScreenY(popupY + 330),
    pageSizeToScreenW(120),
    pageSizeToScreenH(20),
    Component.translatable("gui.epicclassmod.class_select.choose"),
    btn -> choose(current)
));
```

The `prevClass()` / `nextClass()` methods cycle through `ClassType.values()`, skipping `NONE`:
```java
private void prevClass() {
    int ord = current.ordinal();
    ord--;
    if (ord <= 0) ord = ClassType.values().length - 1;
    current = ClassType.values()[ord];
}

private void nextClass() {
    int ord = current.ordinal();
    ord++;
    if (ord >= ClassType.values().length) ord = 1;
    current = ClassType.values()[ord];
}
```

#### 12. Draggable Player Model

Mouse interaction for rotating the 3D model:
```java
// mouseClicked: if click is inside popup bounds → start dragging
if (click inside (popupX, popupY, 520, 360)) {
    dragging = true;
    lastMouseX = mouseX;
    lastMouseY = mouseY;
}

// mouseDragged: update yaw/pitch
if (dragging) {
    modelYaw += (mouseX - lastMouseX);
    modelPitch = clamp(modelPitch - (mouseY - lastMouseY), -80, 80);
    lastMouseX = mouseX;
    lastMouseY = mouseY;
}

// mouseReleased: stop dragging
if (dragging) { dragging = false; }
```

---

## Dynamic Class Data Structure

### Datapack Definition — Embedded in `category.json`

Instead of defining class data in separate files, the bridge embeds optional fields directly into the existing Pufferfish Skills `category.json`. This is the same file where categories already declare their `title`, `icon`, and `background`. All bridge fields live under an optional `"epic_class_bridge"` key — if this key is absent, the category operates normally with no bridge integration.

**File: `data/<namespace>/puffish_skills/categories/<category_id>/category.json`**

This approach leverages the existing `CategoryConfigMixin` pattern: inject at `@At("HEAD")` of `CategoryConfig.parse()`, strip the `"epic_class_bridge"` object from raw JSON via `rawObj.remove()` before Pufferfish sees it, and parse it into the bridge registry. The same pattern is already proven for `"prerequisite_skills"` and `"keep_unlocked"`.

**Key advantage:** The category's `title` and `icon` fields are **already present** in `category.json`, so the bridge reuses them directly as the class display name and icon — no duplication needed.

```json
{
    "title": "Necromancer",
    "icon": { "type": "item", "data": { "item": "minecraft:wither_skeleton_skull" } },
    "background": "minecraft:textures/block/soul_sand.png",

    "epic_class_bridge": {
        "description": "A dark magic wielder who commands the undead and drains life from enemies.",
        "visuals": {
            "armorSet": {
                "type": "individual",
                "head": "irons_spellbooks:netherite_mage_helmet",
                "chest": "irons_spellbooks:netherite_mage_chestplate",
                "legs": "irons_spellbooks:netherite_mage_leggings",
                "feet": "irons_spellbooks:netherite_mage_boots",
                "headFallback": "minecraft:iron_helmet",
                "chestFallback": "minecraft:iron_chestplate",
                "legsFallback": "minecraft:iron_leggings",
                "feetFallback": "minecraft:iron_boots"
            },
            "mainHand": {
                "item": "irons_spellbooks:blood_staff",
                "fallback": "minecraft:stick"
            },
            "offHand": {
                "item": "",
                "fallback": ""
            }
        },
        "stats": [
            { "label": "Health",  "icon": "epicclassmod:textures/gui/icons/heart.png",  "value": 3, "max": 10 },
            { "label": "Defense", "icon": "epicclassmod:textures/gui/icons/shield.png", "value": 2, "max": 10 },
            { "label": "Attack",  "icon": "epicclassmod:textures/gui/icons/sword.png",  "value": 7, "max": 10 },
            { "label": "Speed",   "icon": "epicclassmod:textures/gui/icons/boots.png",  "value": 4, "max": 10 }
        ],
        "passives": {
            "source": "category",
            "max_display": 4,
            "filter": ["vitality", "berserker_rage", "arcane_striker"],
            "show_level": true
        },
        "startingItems": [
            "irons_spellbooks:blood_staff",
            "minecraft:bone"
        ],
        "startingItemIcons": {
            "0": "my_server:textures/gui/icons/blood_staff_icon.png"
        }
    }
}
```

**Field Reference — `epic_class_bridge` (all optional at the top level):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `description` | string | Yes (if bridge present) | Class description shown on ClassSelectScreen |
| `visuals` | object | No | Armor set + weapon definitions for 3D model |
| `stats` | array | No | Stat bar definitions (label, icon, value, max) |
| `passives` | object | No | Passive display config (source, filter, etc.) |
| `startingItems` | array | No | Starting item IDs listed below the description |
| `startingItemIcons` | object | No | Custom icon overrides keyed by item index |

**Categories WITHOUT bridge integration remain unchanged:**

```json
{
    "title": "Combat",
    "icon": { "type": "item", "data": { "item": "minecraft:iron_sword" } },
    "background": "minecraft:textures/block/stone.png"
}
```

No `"epic_class_bridge"` key → the category works exactly as before. The field is fully optional, so existing datapacks require zero changes.

**Fields reused from the category itself (NOT duplicated inside `epic_class_bridge`):**

| category.json field | Bridge usage |
|---------------------|-------------|
| `"title"` | Class display name on ClassSelectScreen (e.g., "Necromancer") |
| `"icon"` | Class icon rendered in navigation / selection UI |

### Alternative: Armor Set Shorthand
```json
{
    "armorSet": {
        "type": "prefix",
        "prefix": "dungeons_and_combat:dark_mage_",
        "fallbackPrefix": "minecraft:iron_"
    }
}
```

This maps to:
- `dungeons_and_combat:dark_mage_helmet`
- `dungeons_and_combat:dark_mage_chestplate`
- `dungeons_and_combat:dark_mage_leggings`
- `dungeons_and_combat:dark_mage_boots`

Using `equipArmorSet()` style resolution with `normalizeArmorBase()` and `armorSuffix()`:
```java
private static String normalizeArmorBase(String base) {
    // Strips trailing underscore, etc.
    return base.endsWith("_") ? base : base + "_";
}

private static String armorSuffix(EquipmentSlot slot) {
    return switch (slot) {
        case HEAD -> "helmet";
        case CHEST -> "chestplate";
        case LEGS -> "leggings";
        case FEET -> "boots";
        default -> "";
    };
}

// Full ID: normalizeArmorBase(prefix) + armorSuffix(slot)
// e.g. "dungeons_and_combat:dark_mage_" + "helmet" → "dungeons_and_combat:dark_mage_helmet"
```

---

## CategoryConfigMixin Extension Pattern

The bridge data is parsed by extending the existing `CategoryConfigMixin` — the same mixin that already handles `"keep_unlocked"` and `"prerequisite_skills"`. This is a proven pattern in the addon.

### How It Works

1. Pufferfish Skills calls `CategoryConfig.parse()` for each `category.json`
2. Our `@Inject(method = "parse", at = @At("HEAD"))` fires first
3. We access the raw GSON `JsonObject` backing the Pufferfish wrapper
4. We call `rawObj.remove("epic_class_bridge")` — this strips the field **before** Pufferfish's internal `JsonObjectTrackingImpl` scans it. Without this, Pufferfish would log a warning about an "unused" field
5. We parse the removed JSON object into a `DynamicClassDefinition`
6. We register it in `DynamicClassRegistry` using the category's own `Identifier` as the class ID

### Existing Pattern Reference

```java
// Already in CategoryConfigMixin (proven, in production):
if (rawObj.has("keep_unlocked")) {
    keepUnlocked = rawObj.remove("keep_unlocked").getAsBoolean();
}
if (rawObj.has("prerequisite_skills")) {
    var prereqArray = rawObj.remove("prerequisite_skills");
    // ... parse into LeveledConfigStorage ...
}

// NEW — same pattern for bridge data:
if (rawObj.has("epic_class_bridge")) {
    var bridgeJson = rawObj.remove("epic_class_bridge").getAsJsonObject();
    parseBridgeDefinition(id, rawObj, bridgeJson);
}
```

### Parse Timing & DynamicClassRegistry Lifecycle

```
Server Startup / /reload:
  1. Pufferfish calls CategoryConfig.parse() for each category
  2. CategoryConfigMixin.onParseHead() fires:
     a. Strips keep_unlocked, prerequisite_skills (existing)
     b. Strips epic_class_bridge (new)
     c. parseBridgeDefinition() → DynamicClassRegistry.register()
  3. Pufferfish continues parsing the cleaned JSON normally
  4. After all categories parsed:
     → DynamicClassRegistry.freeze() (optional — marks registry as complete)
     → SyncDynamicClassesPacket sent to all connected clients
  5. On /reload: DynamicClassRegistry.clear() before re-parsing
```

### Why the Category ID IS the Class ID

Since the bridge data lives inside `category.json`, the category's `Identifier` (e.g., `my_server:necromancer`) naturally becomes the class ID. This eliminates an entire class of bugs where manually-typed `categoryId` fields could mismatch:

```
OLD (separate file):
  class_definitions/necro.json  →  "categoryId": "my_server:necromancer"  ← manual, typo-prone
  
NEW (embedded):
  categories/necromancer/category.json  →  class ID = "my_server:necromancer"  ← automatic
```

---

## Passive Resolution: Datapack-Driven vs Manual

### The Problem

Epic Class Mod's `passivesFor()` method is entirely hardcoded — each of the 6 classes has 4 `PassiveEntry` objects with lang keys (e.g., `class.epicclassmod.warrior.effect1`) and statically assigned icons (`ICON_HEART`, `ICON_SWORD`, etc.). These are **display-only** — they show text cards at the bottom of the ClassSelectScreen. The actual passive functionality lives in separate event hook classes (`WarriorPassives.java`, etc.).

For our dynamic classes, the "passives" displayed on the ClassSelectScreen should represent the **skills available in the linked Pufferfish Skills category**. Since those skills are already fully defined via datapacks, we have a choice:

### Two Modes for Passive Display

#### Mode 1: `"source": "category"` — Auto-Parse from Pufferfish Datapacks (Recommended)

The bridge reads skill definitions directly from the linked category's datapack. No manual duplication needed.

**Why this works:** Pufferfish Skills' `definitions.json` already contains everything `PassiveEntry` needs:

| `PassiveEntry` field | Source from Pufferfish Skills `definitions.json` |
|---------------------|--------------------------------------------------|
| Name (display text) | `"title"` field (e.g., `"Vitality"`, `"Berserker Rage"`) |
| Description (display text) | `"description"` field (e.g., `"Increases your maximum health."`) |
| Icon (texture) | `"icon"` → `{type: "item", data: {item: "minecraft:apple"}}` → rendered as item icon |

**Datapack JSON format (inside `category.json`):**
```json
{
    "title": "Necromancer",
    "icon": { "type": "item", "data": { "item": "minecraft:wither_skeleton_skull" } },
    "background": "minecraft:textures/block/soul_sand.png",

    "epic_class_bridge": {
        "description": "A dark magic wielder who commands the undead.",
        "passives": {
            "source": "category",
            "max_display": 4,
            "filter": ["life_drain", "undead_army", "soul_harvest", "dark_pact"],
            "show_level": true
        }
    }
}
```

**Fields:**
- `"source": "category"` — tells the bridge to pull passives from this category's own skill definitions
- `"max_display"` (optional, default 4) — maximum passive cards to show (ECM shows 4 per class). The ClassSelectScreen layout fits 4 cards (2 cols x 2 rows)
- `"filter"` (optional) — if provided, only show these specific skill definition IDs. If omitted, shows the first `max_display` non-hidden skills in definition order
- `"show_level"` (optional, default true) — if true and the skill has `max_skill_level > 1`, appends level info like "(Max Lv. 5)" to the description

**How it resolves at runtime (server side):**
```java
// CategoryConfigMixin.onParseHead() — when parsing category.json with epic_class_bridge
if (passivesSource.equals("category")) {
    // The category ID is already known — it's THIS category being parsed
    Identifier categoryId = id; // the category's own ID
    
    // Access the loaded Pufferfish category config (server-side)
    // The addon's LeveledConfigStorage already has all parsed skill data
    Map<String, LeveledConfigStorage.LeveledConfig> allConfigs = 
        LeveledConfigStorage.getAllEntries();
    
    // Filter by categoryId to get skills belonging to this category
    List<PassiveDisplay> passives = new ArrayList<>();
    for (var entry : allConfigs.entrySet()) {
        LeveledConfigStorage.LeveledConfig config = entry.getValue();
        if (config.categoryId != null && config.categoryId.equals(categoryId.toString())) {
            if (config.hidden) continue; // Skip hidden skills
            if (filter != null && !filter.contains(entry.getKey())) continue;
            
            // We'll sync the display data to the client
            // Title and description are resolved from the SkillDefinitionConfig
            passives.add(new PassiveDisplay(
                entry.getKey(),    // skill ID (used for lookup)
                null,              // filled client-side from ClientSkillDefinitionConfig
                null,              // filled client-side
                null,              // filled client-side
                config.maxLevels,
                config.toggle
            ));
            
            if (passives.size() >= maxDisplay) break;
        }
    }
}
```

**How it resolves at runtime (client side):**
```java
// DynamicClassDefinition.java — when rendering passives
public List<ResolvedPassive> resolvePassives() {
    if (passiveSource == PassiveSource.MANUAL) {
        return manualPassives; // Use the manually defined passives
    }
    
    // Auto-resolve from Pufferfish Skills client data
    // Since class ID = category ID, we can look up directly
    ClientSkillScreenData screenData = /* access singleton */;
    ResourceLocation catId = new ResourceLocation(this.id); // class ID IS the category ID
    Optional<ClientCategoryData> catData = screenData.getCategory(catId);
    
    if (catData.isEmpty()) {
        return List.of(); // Category not loaded yet
    }
    
    ClientCategoryConfig config = catData.get().getConfig();
    // config.definitions() → Map<String, ClientSkillDefinitionConfig>
    // Each ClientSkillDefinitionConfig has:
    //   .id()              → String
    //   .title()           → Component (display name)
    //   .description()     → Component (skill description)
    //   .icon()            → ClientIconConfig (item-based or texture-based)
    
    List<ResolvedPassive> result = new ArrayList<>();
    for (var entry : config.definitions().entrySet()) {
        String skillId = entry.getKey();
        ClientSkillDefinitionConfig def = entry.getValue();
        
        // Apply filter if specified
        if (passiveFilter != null && !passiveFilter.contains(skillId)) continue;
        
        result.add(new ResolvedPassive(
            def.title().getString(),       // "Vitality", "Berserker Rage", etc.
            buildDescription(def),         // Description with optional level info
            def.icon(),                    // ClientIconConfig for rendering
            skillId
        ));
        
        if (result.size() >= maxDisplay) break;
    }
    return result;
}

private String buildDescription(ClientSkillDefinitionConfig def) {
    String desc = def.description().getString();
    if (showLevel) {
        // Get leveled config to append max level info
        LeveledConfigStorage.LeveledConfig leveled = LeveledConfigStorage.get(def.id());
        if (leveled != null && leveled.maxLevels > 1) {
            desc += " §7(Max Lv. " + leveled.maxLevels + ")";
        }
        if (leveled != null && leveled.toggle) {
            desc += " §e[Toggle]";
        }
    }
    return desc;
}
```

**Icon rendering adaptation:**

Pufferfish Skills uses `ClientIconConfig` which is item-based (`{type: "item", data: {item: "minecraft:apple"}}`), while Epic Class Mod's `PassiveEntry` uses `ResourceLocation` textures (12×12 PNG). The bridge needs to handle both:

```java
// When rendering a passive card icon
ResolvedPassive passive = passives.get(i);

if (passive.iconOverride() != null) {
    // Manual texture override (for manual passives or custom icons)
    graphics.blit(passive.iconOverride(), 
        iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
} else if (passive.clientIcon() != null) {
    // Pufferfish item-based icon — render as item
    ClientIconConfig icon = passive.clientIcon();
    // ClientIconConfig resolves to an ItemStack internally
    // Use renderItemScaled to match ECM's visual style
    ItemStack iconStack = icon.getItemStack(); // or resolve from the icon config
    renderItemScaled(graphics, iconStack, iconX, iconY, iconSize);
}
```

#### Mode 2: `"source": "manual"` — Explicit Passive Definitions

For cases where you want passives that DON'T match the Pufferfish category skills, or for classes where the linked category doesn't exist yet:

```json
{
    "title": "Necromancer",
    "icon": { "type": "item", "data": { "item": "minecraft:wither_skeleton_skull" } },
    "background": "minecraft:textures/block/soul_sand.png",

    "epic_class_bridge": {
        "description": "A dark magic wielder who commands the undead.",
        "passives": {
            "source": "manual",
            "entries": [
                {
                    "name": "Life Drain",
                    "description": "Attacks heal 2% of damage dealt",
                    "icon": "my_server:textures/gui/icons/life_drain.png"
                },
                {
                    "name": "Undead Army",
                    "description": "Summon skeletal minions to fight for you",
                    "icon": "my_server:textures/gui/icons/undead_army.png"
                }
            ]
        }
    }
}
```

This matches the original `PassiveEntry` pattern 1:1 — each entry is a name/description/icon tuple rendered as a card.

#### Mode 3: `"source": "mixed"` — Category Skills + Manual Extras

Combines both: start with category skills, then append manual entries to fill remaining slots:

```json
{
    "title": "Necromancer",
    "icon": { "type": "item", "data": { "item": "minecraft:wither_skeleton_skull" } },
    "background": "minecraft:textures/block/soul_sand.png",

    "epic_class_bridge": {
        "description": "A dark magic wielder who commands the undead.",
        "passives": {
            "source": "mixed",
            "max_display": 4,
            "category_skills": ["life_drain", "soul_harvest"],
            "manual_extras": [
                {
                    "name": "Class Mastery",
                    "description": "Unlocked at Level 30",
                    "icon": "my_server:textures/gui/icons/mastery.png"
                }
            ]
        }
    }
}
```

### Comparison of Passive Sources

| Aspect | `"category"` (Auto) | `"manual"` | `"mixed"` |
|--------|---------------------|------------|-----------|
| Data source | Pufferfish Skills `definitions.json` | `epic_class_bridge` in category.json | Both |
| Updates automatically | Yes — add a skill to category, it appears | No — must edit JSON | Partially |
| Requires Pufferfish category | Yes | No | Yes (for category portion) |
| Custom text/icons | No (uses skill's title/desc/icon) | Yes | Mixed |
| Icon type | Item-based (rendered as ItemStack) | Texture-based (12×12 PNG) | Both |
| Best for | Categories where skills = passives | Lore/flavor passives | Hybrid |

### New Skill Definition Fields: `auto_grant` and `exclusive` (in `prerequisite_skills`)

#### `auto_grant` — Class Passive Skill

A new per-skill field in `definitions.json`, alongside the existing `toggle`, `hidden`, `loot_mode`, etc. Parsed by `SkillDefinitionConfigMixin` and stored in `LeveledConfigStorage.LeveledConfig`.

```json
"undead_affinity": {
    "type": "puffish_skills:default",
    "category_id": "necromancer",
    "title": "Undead Affinity",
    "description": "Your dark magic resonates with undead creatures.",
    "icon": { "type": "item", "data": { "item": "minecraft:wither_skeleton_skull" } },
    "max_skill_level": 1,
    "auto_grant": true,
    "rewards": [
        {
            "type": "puffish_skills:attribute",
            "data": {
                "attribute": "minecraft:generic.attack_damage",
                "operation": "addition",
                "value": 1.0
            }
        }
    ]
}
```

**Behavior:**
- When `"auto_grant": true` is set on a skill, that skill is **automatically unlocked at level 1** when the player selects the class linked to its category
- The skill appears as a **passive card** on the ClassSelectScreen (marked with ★)
- **Limit: up to 4** `auto_grant` skills per category (matches the ClassSelectScreen's 4-card layout). If more than 4 exist, only the first 4 in definition order are granted; the rest are treated as display-only passives
- On class change/reset, auto-granted skills are **revoked** (set to level 0) unless the player manually leveled them
- Rewards fire immediately on grant (attribute changes, effects, commands)

**Validation rules (enforced at grant time):**
- Must have `max_skill_level: 1` — multi-level skills with `auto_grant: true` log a WARN and are treated as display-only
- Must NOT have `toggle: true` — toggle skills require keybind management, auto-grant doesn't make sense
- Must NOT have `loot_mode: "tome_only"` / `"imbue_only"` / `"both"` — loot-gated skills are intentionally gated
- Must NOT have `hidden: true` — hidden skills shouldn't be visible as passives

#### `exclusive` — Class-Exclusive Skill (via `prerequisite_skills`)

`exclusive` is **NOT** a standalone top-level field on the skill. Instead, `"exclusive": true` is a flag inside a `prerequisite_skills` entry. This reuses the existing prerequisite system — the datapack author only needs to add `exclusive`, `min_level`, and `category` to a prereq entry to make the skill class-exclusive and level-gated.

```json
"necro_mastery": {
    "type": "puffish_skills:default",
    "category_id": "necromancer",
    "title": "Necromancer's Mastery",
    "description": "A powerful ability only available to Necromancers at high level.",
    "icon": { "type": "item", "data": { "item": "minecraft:nether_star" } },
    "max_skill_level": 3,
    "prerequisite_skills": [
        { "exclusive": true, "min_level": 20, "category": "necromancer" },
        { "skill": "soul_harvest", "min_level": 3 }
    ],
    "rewards": [ ... ]
}
```

**How the `exclusive` prereq entry works:**
- `"exclusive": true` — marks this prerequisite as a class-exclusive gate
- `"min_level": 20` — the category/class level the player must reach before this skill appears
- `"category": "necromancer"` — the category whose level is checked (typically the skill's own category)
- No `"skill"` field needed — `exclusive` implies a category-level gate, not a skill-level gate

**Behavior:**
- When a `prerequisite_skills` entry has `"exclusive": true`, the skill is:
  1. **Hidden from the skill tree** until the category reaches the required `min_level`
  2. **Displayed in the ClassBookScreen** (Tier 2) under an "Exclusive Skills" section as a locked entry (🔒)
  3. Once the level is reached, the skill **appears in the tree** and can be purchased normally
- The `exclusive` flag only controls **visibility and display location** — it does not change how the skill is unlocked (points, tree clicks, etc.)
- A skill can have **both** an `exclusive` prereq AND normal skill prereqs — all must be met:
  ```json
  "prerequisite_skills": [
      { "exclusive": true, "min_level": 20, "category": "necromancer" },
      { "skill": "soul_harvest", "min_level": 3 }
  ]
  ```
  This means: "hidden until Necromancer level 20, AND requires Soul Harvest level 3"
- Exclusive skills can be any type: multi-level, toggle, loot-gated — the `exclusive` flag only controls where/when they appear
- Exclusive skills are **not** auto-granted — they must be earned through the tree

#### Combined Example: Full Necromancer `definitions.json`

```json
{
    "undead_affinity": {
        "type": "puffish_skills:default",
        "category_id": "necromancer",
        "title": "Undead Affinity",
        "description": "Your dark magic resonates with undead creatures.",
        "icon": { "type": "item", "data": { "item": "minecraft:wither_skeleton_skull" } },
        "max_skill_level": 1,
        "auto_grant": true,
        "rewards": [ { "type": "puffish_skills:attribute", "data": { "attribute": "minecraft:generic.attack_damage", "operation": "addition", "value": 1.0 } } ]
    },
    "life_drain": {
        "type": "puffish_skills:default",
        "category_id": "necromancer",
        "title": "Life Drain",
        "description": "Attacks heal 2% of damage dealt per level.",
        "icon": { "type": "item", "data": { "item": "minecraft:ghast_tear" } },
        "max_skill_level": 5,
        "auto_grant": false,
        "rewards": [ ... ]
    },
    "soul_harvest": {
        "type": "puffish_skills:default",
        "category_id": "necromancer",
        "title": "Soul Harvest",
        "description": "Reap souls from fallen enemies.",
        "icon": { "type": "item", "data": { "item": "minecraft:soul_lantern" } },
        "max_skill_level": 3,
        "rewards": [ ... ]
    },
    "dark_pact": {
        "type": "puffish_skills:default",
        "category_id": "necromancer",
        "title": "Dark Pact",
        "description": "Sacrifice HP for power.",
        "icon": { "type": "item", "data": { "item": "minecraft:wither_rose" } },
        "max_skill_level": 1,
        "auto_grant": true,
        "rewards": [ ... ]
    },
    "necro_mastery": {
        "type": "puffish_skills:default",
        "category_id": "necromancer",
        "title": "Necromancer's Mastery",
        "description": "Ultimate power — only for dedicated Necromancers.",
        "icon": { "type": "item", "data": { "item": "minecraft:nether_star" } },
        "max_skill_level": 3,
        "prerequisite_skills": [
            { "exclusive": true, "min_level": 20, "category": "necromancer" },
            { "skill": "soul_harvest", "min_level": 3 },
            { "skill": "life_drain", "min_level": 3 }
        ],
        "rewards": [ ... ]
    },
    "raise_dead": {
        "type": "puffish_skills:default",
        "category_id": "necromancer",
        "title": "Raise Dead",
        "description": "Summon undead minions to fight. Necromancer exclusive.",
        "icon": { "type": "item", "data": { "item": "minecraft:skeleton_skull" } },
        "max_skill_level": 1,
        "toggle": true,
        "keybind_slot": 3,
        "cooldown": 1200,
        "prerequisite_skills": [
            { "exclusive": true, "min_level": 10, "category": "necromancer" },
            { "skill": "undead_affinity", "min_level": 1 }
        ],
        "rewards": [ ... ]
    }
}
```

In this example:
- **`undead_affinity`** and **`dark_pact`** have `auto_grant: true` → auto-unlocked when player picks Necromancer, shown as passive cards on ClassSelectScreen
- **`life_drain`** and **`soul_harvest`** are normal skills (no special flags) → player earns them through the tree
- **`necro_mastery`** and **`raise_dead`** have `{ "exclusive": true, "min_level": ..., "category": "necromancer" }` in their `prerequisite_skills` → hidden from tree until Necromancer reaches the required level, shown in ClassBookScreen as locked entries

#### LeveledConfig Extension

One new field added to `LeveledConfigStorage.LeveledConfig`:

```java
public class LeveledConfig {
    // ... existing fields: maxLevels, pointsPerLevel, toggle, keybindSlot,
    //     cooldown, categoryId, hidden, lootMode, requiredSkills, etc.
    
    public final boolean autoGrant;     // NEW — auto-unlock on class selection
    
    // Updated constructor adds this boolean
}
```

**Note:** `exclusive` is NOT a LeveledConfig field — it's a flag on individual `prerequisite_skills` entries. It's stored inside `RequiredSkillEntry`:

```java
public static class RequiredSkillEntry {
    public final String skillId;     // null when exclusive=true (category-level gate)
    public final int minLevel;
    public final String categoryId;  // null means same category
    public final boolean exclusive;   // NEW — marks this prereq as a class-exclusive gate
    
    // Existing constructors unchanged (exclusive defaults to false)
    
    public RequiredSkillEntry(String skillId, int minLevel, String categoryId, boolean exclusive) {
        this.skillId = skillId;
        this.minLevel = minLevel;
        this.categoryId = categoryId;
        this.exclusive = exclusive;
    }
}
```

#### SkillDefinitionConfigMixin Parsing

The `auto_grant` field is consumed in `onParseHead()` and stored in `onParseReturn()`. The `exclusive` flag is parsed from individual `prerequisite_skills` entries:

```java
// In onParseHead() — consume to avoid "unused field" warnings:
rootObject.get("auto_grant").getSuccess();

// In onParseReturn() — parse auto_grant:
boolean autoGrant = rootObject.get("auto_grant").getSuccess()
        .flatMap(e -> e.getAsBoolean().getSuccess())
        .orElse(false);

// In the prerequisite_skills parsing loop (already exists):
var prereqObj = prereqElem.getAsJsonObject();
boolean exclusive = prereqObj.has("exclusive")
        ? prereqObj.get("exclusive").getAsBoolean()
        : false;

if (exclusive) {
    // Exclusive prereq: no "skill" field needed, uses "min_level" + "category"
    String category = prereqObj.has("category")
            ? prereqObj.get("category").getAsString()
            : null;
    int minLevel = prereqObj.has("min_level")
            ? prereqObj.get("min_level").getAsInt()
            : 1;
    requiredSkillsList.add(new LeveledConfigStorage.RequiredSkillEntry(
            null, minLevel, category, true)); // skillId=null for exclusive
} else {
    // Normal prereq: existing parsing (skill + min_level + optional category)
    // ... existing code unchanged ...
}

// Pass auto_grant to LeveledConfig constructor:
LeveledConfigStorage.put(id.toString(),
    new LeveledConfigStorage.LeveledConfig(
        finalMaxLevels, points, merge,
        requiredSkillsList, levelPrereqs, lootMode, categoryId,
        enchantmentCost, imbuementCost,
        slotOpeningCost, cleansingCost, isLootable, hidden,
        toggle, keybindSlot, cooldown,
        autoGrant   // ← new (exclusive is in requiredSkillsList entries)
    ));
```

#### Discovery: Finding Auto-Grant and Exclusive Skills for a Category

Instead of listing skills in `epic_class_bridge`, the bridge **discovers** them from `LeveledConfigStorage`:

```java
/**
 * Find all auto_grant skills belonging to a category.
 * Called when a player selects a dynamic class.
 */
public static List<String> getAutoGrantSkills(String categoryId) {
    List<String> result = new ArrayList<>();
    for (var entry : LeveledConfigStorage.getAllEntries().entrySet()) {
        LeveledConfig config = entry.getValue();
        if (config.autoGrant 
                && config.categoryId != null 
                && config.categoryId.equals(categoryId)) {
            result.add(entry.getKey());
        }
    }
    return result; // max 4 enforced at grant time
}

/**
 * Find all exclusive skills belonging to a category.
 * Scans each skill's prerequisite_skills for an entry with exclusive=true
 * whose category matches the target.
 * Used by ClassBookScreen to populate the "Exclusive Skills" section.
 */
public static List<ExclusiveSkillInfo> getExclusiveSkills(String categoryId) {
    List<ExclusiveSkillInfo> result = new ArrayList<>();
    for (var entry : LeveledConfigStorage.getAllEntries().entrySet()) {
        LeveledConfig config = entry.getValue();
        if (config.requiredSkills == null) continue;
        
        for (var prereq : config.requiredSkills) {
            if (prereq.exclusive) {
                // Check if this exclusive prereq targets our category
                String targetCat = prereq.categoryId != null 
                        ? prereq.categoryId 
                        : config.categoryId; // default: same category as the skill
                if (categoryId.equals(targetCat)) {
                    result.add(new ExclusiveSkillInfo(
                        entry.getKey(),     // skill ID
                        prereq.minLevel,    // category level required
                        config              // full config for display
                    ));
                    break; // one exclusive prereq per skill is enough
                }
            }
        }
    }
    return result;
}

public record ExclusiveSkillInfo(String skillId, int requiredLevel, LeveledConfig config) {}
```

### Passive Functional Behavior

#### How Passives Work on ClassSelectScreen

The passive cards on ClassSelectScreen are populated from skills with `auto_grant: true` in the category's `definitions.json`. Normal (non-auto_grant) skills shown via the passive filter are **display-only previews**.

```
ClassSelectScreen Passive Section:
  ┌────────────────────────────────────────────────────────────────┐
  │  Skills with auto_grant: true are GIVEN on class selection.   │
  │  Other filtered skills are shown as previews only.            │
  │                                                               │
  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
  │  │ ★ Undead│  │ ★ Dark  │  │ Life    │  │ Soul    │        │
  │  │ Affinity│  │ Pact    │  │ Drain   │  │ Harvest │        │
  │  │ (GIVEN) │  │ (GIVEN) │  │ preview │  │ preview │        │
  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘        │
  │                                                               │
  │  ★ = auto_grant: true in definitions.json                    │
  │  preview = must be earned through skill tree                  │
  └────────────────────────────────────────────────────────────────┘
```

#### How Exclusive Skills Work in ClassBookScreen

Exclusive skills appear in a dedicated section on the ClassBookScreen (Tier 2). They are **hidden from the skill tree** until the player's category/class reaches the required level. The `exclusive` flag in `prerequisite_skills` controls both the visibility gate and the ClassBookScreen display:

```
ClassBookScreen — Necromancer Tab:
  ┌────────────────────────────────────────────────────────────────┐
  │  § Exclusive Skills                                           │
  │                                                               │
  │  ┌──────────────────────┐  ┌──────────────────────┐          │
  │  │ 🔒 Necro Mastery     │  │ 🔒 Raise Dead        │          │
  │  │ Unlocks at:          │  │ Unlocks at:          │          │
  │  │  Necromancer Lv.20   │  │  Necromancer Lv.10   │          │
  │  │ Also requires:       │  │ Also requires:       │          │
  │  │  Soul Harvest Lv.3   │  │  Undead Affinity     │          │
  │  │  Life Drain Lv.3     │  │                      │          │
  │  │ (Max Lv. 3)          │  │ [Toggle] Cooldown:20s│          │
  │  └──────────────────────┘  └──────────────────────┘          │
  └────────────────────────────────────────────────────────────────┘

  When the player reaches Necromancer Level 20:
  → Necro Mastery APPEARS in the skill tree
  → Player can still only unlock it after also meeting
    Soul Harvest Lv.3 + Life Drain Lv.3 prereqs
```

#### Server-Side Grant/Revoke Flow

```java
// When player selects a dynamic class:
public void onClassChosen(ServerPlayer player, String classId) {
    // 1. Store the class choice
    BridgePlayerData.setDynamicClass(player, classId);
    
    // 2. Discover and grant auto_grant skills for this category
    List<String> autoSkills = getAutoGrantSkills(classId);
    Identifier categoryId = new Identifier(classId);
    var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
    
    int granted = 0;
    for (String skillId : autoSkills) {
        if (granted >= 4) break; // Hard cap at 4 passives
        
        var config = LeveledConfigStorage.get(skillId);
        if (config == null) continue;
        
        // Validation: only safe skills
        if (config.maxLevels > 1) {
            LOGGER.warn("[Bridge] Skipping auto-grant '{}': multi-level (max={})", 
                skillId, config.maxLevels);
            continue;
        }
        if (config.toggle) {
            LOGGER.warn("[Bridge] Skipping auto-grant '{}': toggle skill", skillId);
            continue;
        }
        if (config.lootMode != null && !config.lootMode.equals("tree")) {
            LOGGER.warn("[Bridge] Skipping auto-grant '{}': loot-gated ({})", 
                skillId, config.lootMode);
            continue;
        }
        
        // Grant at level 1, bypass points
        manager.setSkillLevel(player, categoryId, skillId, 1, true);
        granted++;
    }
}

// When player changes class or class is revoked:
public void onClassRevoked(ServerPlayer player, String oldClassId) {
    List<String> autoSkills = getAutoGrantSkills(oldClassId);
    Identifier categoryId = new Identifier(oldClassId);
    var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
    
    for (String skillId : autoSkills) {
        // Only revoke if the skill was auto-granted (not manually leveled)
        var ext = manager.getCategoryDataExtension(player, categoryId);
        if (ext != null && ext.addon$getSkillLevel(skillId) == 1) {
            long paidBits = ext.addon$getPaidLevels(skillId);
            if ((paidBits & (1L << 1)) == 0) {
                // Level 1 was NOT paid for → was auto-granted → safe to revoke
                manager.setSkillLevel(player, categoryId, skillId, 0, true);
            }
        }
    }
}
```

#### Behavior Summary

| Behavior | `auto_grant: true` | `exclusive` prereq | Normal skill |
|----------|--------------------|--------------------|---------------|
| Defined where | Top-level field on skill | Inside `prerequisite_skills` entry | N/A |
| Where displayed | ClassSelectScreen passive card (★) | ClassBookScreen exclusive section (🔒) | Skill tree only |
| Granted on class select | **Yes** — level 1 | No | No |
| Hidden from tree | No | **Yes** — until category reaches `min_level` | No |
| Requires spending points | No | Yes (after appearing in tree) | Yes |
| Gating mechanism | None (auto) | `{ exclusive: true, min_level, category }` | Tree prerequisites |
| Revoked on class change | Yes (if not manually paid) | No (earned through tree) | No |
| Max per category | 4 | Unlimited | Unlimited |
| Rewards fire on grant | Yes | On manual unlock | On manual unlock |
| Can be multi-level | No (validated) | Yes | Yes |
| Can be toggle | No (validated) | Yes | Yes |
| Can be loot-gated | No (validated) | Yes | Yes |

### Data Flow: Category-Sourced Passives

```
Datapack Loaded:
  data/my_server/puffish_skills/categories/necromancer/category.json
    ├── "title": "Necromancer"           ← reused as class display name
    ├── "icon": { item: "minecraft:wither_skeleton_skull" }  ← reused as class icon
    └── "epic_class_bridge": {
          "passives": { source: "category", max_display: 4, filter: [...] }
        }

  data/my_server/puffish_skills/categories/necromancer/definitions.json
    ├── "life_drain":    { title: "Life Drain",    description: "...", icon: {item: "minecraft:ghast_tear"} }
    ├── "undead_army":   { title: "Undead Army",   description: "...", icon: {item: "minecraft:bone"}       }
    ├── "soul_harvest":  { title: "Soul Harvest",  description: "...", icon: {item: "minecraft:soul_lantern"}}
    └── "dark_pact":     { title: "Dark Pact",     description: "...", icon: {item: "minecraft:wither_rose"} }

Server:
  CategoryConfigMixin.onParseHead() reads epic_class_bridge from category.json
  → rawObj.remove("epic_class_bridge") strips it before Pufferfish sees it
  → parses bridge fields → DynamicClassRegistry.register()
  → resolves "source": "category" → stores skill IDs to sync
  → SyncDynamicClassesPacket → Client

Client:
  DynamicClassDefinition.resolvePassives()
  → reads ClientCategoryConfig("my_server:necromancer").definitions()
  → maps each ClientSkillDefinitionConfig to a passive card
  → renders on ClassSelectScreen:
  
  ┌──────────────────┐  ┌──────────────────┐
  │ 💀 Life Drain    │  │ 🦴 Undead Army   │
  │ Attacks heal 2%  │  │ Summon skeletal  │
  │ of damage dealt  │  │ minions          │
  │ (Max Lv. 5)      │  │ [Toggle]         │
  └──────────────────┘  └──────────────────┘
  ┌──────────────────┐  ┌──────────────────┐
  │ 🏮 Soul Harvest  │  │ 🥀 Dark Pact     │
  │ Reap souls from  │  │ Sacrifice HP for │
  │ fallen enemies   │  │ power            │
  │                   │  │ (Max Lv. 3)      │
  └──────────────────┘  └──────────────────┘
```

### The `LeveledConfigStorage` Connection

The addon's `LeveledConfigStorage` is the **server-side bridge** between Pufferfish datapack data and the bridge:

```java
// What LeveledConfigStorage stores per skill (from SkillDefinitionConfigMixin parsing):
LeveledConfig {
    maxLevels:       5                 // from "max_skill_level": 5
    pointsPerLevel:  1                 // from "points_per_level": 1
    toggle:          true              // from "toggle": true
    keybindSlot:     2                 // from "keybind_slot": 2
    cooldown:        600               // from "cooldown": 600
    categoryId:      "combat"          // from "category_id": "combat"
    hidden:          false             // from "hidden": false
    lootMode:        "both"            // from "loot_mode": "both"
    autoGrant:       true              // from "auto_grant": true — NEW
    requiredSkills:  [                 // from "prerequisite_skills": [...]
        RequiredSkillEntry {
            skillId:    null           //   (null for exclusive gates)
            minLevel:   20             //   from "min_level": 20
            categoryId: "necromancer"  //   from "category": "necromancer"
            exclusive:  true           //   from "exclusive": true — NEW
        },
        RequiredSkillEntry {
            skillId:    "soul_harvest" //   from "skill": "soul_harvest"
            minLevel:   3              //   from "min_level": 3
            categoryId: null           //   (same category)
            exclusive:  false          //   (default)
        }
    ]
    // ... plus enchantment costs, etc.
}
```

This means on the server, we can:
1. **Filter** — exclude hidden skills, toggle-only skills, etc.
2. **Annotate** — append max level, toggle status, cooldown to descriptions
3. **Sort** — order by skill tree position, level requirement, etc.
4. **Validate** — ensure referenced skills actually exist in the category

### Client-Side Access: `ClientSkillScreenData`

On the client, the full Pufferfish Skills data is accessible via:

```java
// Singleton access pattern (from upstream Pufferfish Skills):
ClientSkillScreenData screenData = /* injected or accessed via static field */;

// Get a specific category
Optional<ClientCategoryData> catData = screenData.getCategory(
    new ResourceLocation("my_server", "necromancer")
);

catData.ifPresent(cat -> {
    ClientCategoryConfig config = cat.getConfig();
    
    // All skill definitions in this category:
    Map<String, ClientSkillDefinitionConfig> defs = config.definitions();
    
    for (var entry : defs.entrySet()) {
        String skillId = entry.getKey();
        ClientSkillDefinitionConfig skillDef = entry.getValue();
        
        Component title = skillDef.title();              // "Vitality"
        Component description = skillDef.description();  // "Increases your max health."
        ClientIconConfig icon = skillDef.icon();          // Item-based icon
        
        // Can also access:
        // skillDef.extraDescription()  → addon-injected level descriptions
        // skillDef.cost()              → point cost
        // skillDef.size()              → visual size on tree
    }
    
    // Skill positions on the tree:
    Map<String, ClientSkillConfig> skills = config.skills();
    // skills.get("vitality").x(), .y() → position coordinates
    
    // Connections between skills:
    Collection<ClientSkillConnectionConfig> connections = config.normalConnections();
});
```

### Icon Rendering Detail

Pufferfish Skills' `ClientIconConfig` uses item-based icons (the vast majority of skills use `{type: "item"}`). The ClassSelectScreen's passive cards originally use 12×12 PNG textures via `blit()`. For maximum visual compatibility:

```java
/**
 * Render a Pufferfish skill icon in place of an ECM passive icon.
 * Handles the difference between ECM's blit-based icons and 
 * Pufferfish's item-based icons.
 */
private void renderPassiveIcon(GuiGraphics graphics, ResolvedPassive passive,
                                int x, int y, int size) {
    if (passive.iconTexture() != null) {
        // Texture-based (manual mode) — same as vanilla ECM
        graphics.blit(passive.iconTexture(), x, y, 0, 0, size, size, size, size);
    } else {
        // Item-based (category mode) — render the item icon scaled down to fit
        // This actually looks BETTER than ECM's flat icons because it shows
        // the actual Minecraft item with its native texture + enchant shimmer
        ItemStack iconStack = passive.resolveItemIcon();
        if (!iconStack.isEmpty()) {
            // Scale down from 16px default to our target size
            float scale = size / 16.0f;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.renderItem(iconStack, 0, 0);
            graphics.pose().popPose();
        }
    }
}
```

---

## Implementation Plan

### 1. Dynamic Class Registry

**File: `Common/src/main/java/net/puffish/skillsmod/bridge/DynamicClassRegistry.java`**

```java
package net.puffish.skillsmod.bridge;

import java.util.*;

/**
 * Stores dynamically defined classes loaded from datapacks.
 * These classes are displayed in ClassSelectScreen alongside
 * the built-in enum classes.
 */
public class DynamicClassRegistry {

    private static final LinkedHashMap<String, DynamicClassDefinition> CLASSES = new LinkedHashMap<>();

    public static void register(String id, DynamicClassDefinition def) {
        CLASSES.put(id, def);
    }

    public static void clear() {
        CLASSES.clear();
    }

    public static Collection<DynamicClassDefinition> getAll() {
        return Collections.unmodifiableCollection(CLASSES.values());
    }

    public static DynamicClassDefinition get(String id) {
        return CLASSES.get(id);
    }

    public static int count() {
        return CLASSES.size();
    }

    /**
     * Total number of selectable classes: built-in (6) + dynamic.
     */
    public static int totalClassCount() {
        return 6 + CLASSES.size(); // 6 = WARRIOR through ARCHER
    }

    /**
     * Get the class at a given index in the combined list.
     * 0-5: built-in classes (WARRIOR=0, ... ARCHER=5)
     * 6+: dynamic classes in registration order
     *
     * @return null if index is for a built-in class (caller handles those)
     */
    public static DynamicClassDefinition getAtIndex(int index) {
        if (index < 6) return null; // Built-in
        int dynIndex = index - 6;
        int i = 0;
        for (DynamicClassDefinition def : CLASSES.values()) {
            if (i == dynIndex) return def;
            i++;
        }
        return null;
    }
}
```

### 2. Dynamic Class Definition

**File: `Common/src/main/java/net/puffish/skillsmod/bridge/DynamicClassDefinition.java`**

```java
package net.puffish.skillsmod.bridge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * Represents a dynamic class definition parsed from the "epic_class_bridge"
 * field in a Pufferfish Skills category.json file.
 *
 * The class ID is the category's own Identifier (e.g., "my_server:necromancer").
 * Title and icon are reused from the category's existing fields.
 */
public class DynamicClassDefinition {
    private final String id;           // = category ID (e.g., "my_server:necromancer")
    private final String title;        // reused from category.json "title"
    private final String description;  // from epic_class_bridge.description

    // Visual equipment
    private final EquipmentVisuals visuals;

    // Stats display
    private final List<StatDisplay> stats;

    // Passives display — supports multiple resolution modes
    private final PassiveSource passiveSource;
    private final List<PassiveDisplay> manualPassives;   // for "manual" mode
    private final List<String> passiveFilter;            // for "category" mode — skill IDs to show
    private final int maxPassiveDisplay;                 // max cards to render
    private final boolean showPassiveLevel;              // append level info to descriptions
    private final List<PassiveDisplay> manualExtras;     // for "mixed" mode — extras after category skills

    // Starting items
    private final List<StartingItemEntry> startingItems;

    // ... constructor, getters

    /**
     * Discover auto_grant skills from LeveledConfigStorage for this class's category.
     * auto_grant is a per-skill field in definitions.json, NOT a list in the bridge config.
     */
    public List<String> getAutoGrantSkills() {
        List<String> result = new ArrayList<>();
        for (var entry : LeveledConfigStorage.getAllEntries().entrySet()) {
            LeveledConfig config = entry.getValue();
            if (config.autoGrant
                    && config.categoryId != null
                    && config.categoryId.equals(this.id)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Discover exclusive skills for this class's category.
     * Exclusive skills have { "exclusive": true, "min_level": X, "category": Y }
     * inside their prerequisite_skills. They are hidden from the tree until
     * the category reaches the required level.
     */
    public List<ExclusiveSkillInfo> getExclusiveSkills() {
        return BridgeSkillDiscovery.getExclusiveSkills(this.id);
    }

    public boolean hasAutoGrant() {
        return !getAutoGrantSkills().isEmpty();
    }

    /**
     * Passive resolution source.
     */
    public enum PassiveSource {
        /** Read passives from the linked Pufferfish Skills category's definitions.json */
        CATEGORY,
        /** Use explicitly defined passive entries in the epic_class_bridge JSON */
        MANUAL,
        /** Pull some from category, append manual extras */
        MIXED
    }

    public record EquipmentVisuals(
        String headItem, String headFallback,
        String chestItem, String chestFallback,
        String legsItem, String legsFallback,
        String feetItem, String feetFallback,
        String mainHandItem, String mainHandFallback,
        String offHandItem, String offHandFallback
    ) {
        /**
         * Resolve an item string to an ItemStack.
         * If the modded item doesn't exist, use the fallback.
         */
        public ItemStack resolveItem(String modItem, String fallbackItem) {
            if (modItem == null || modItem.isEmpty()) return ItemStack.EMPTY;
            // Try to resolve modded item
            ResourceLocation rl = new ResourceLocation(modItem);
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
            // Fallback
            if (fallbackItem != null && !fallbackItem.isEmpty()) {
                ResourceLocation frl = new ResourceLocation(fallbackItem);
                Item fItem = ForgeRegistries.ITEMS.getValue(frl);
                if (fItem != null) return new ItemStack(fItem);
            }
            return new ItemStack(Items.IRON_SWORD); // Ultimate fallback
        }
    }

    public record StatDisplay(
        String label,
        ResourceLocation icon,
        int value,
        int max
    ) {}

    public record PassiveDisplay(
        String name,
        String description,
        ResourceLocation icon     // texture path for manual mode, null for category mode
    ) {}

    /**
     * Resolved passive ready for rendering. Created at render time
     * from either manual PassiveDisplay or category ClientSkillDefinitionConfig.
     */
    public record ResolvedPassive(
        String displayName,
        String displayDescription,
        ClientIconConfig clientIcon,      // non-null for category-sourced (item icon)
        ResourceLocation iconTexture,     // non-null for manual (texture icon)
        String skillId                    // non-null for category-sourced (for tooltip lookup)
    ) {
        public ItemStack resolveItemIcon() {
            if (clientIcon == null) return ItemStack.EMPTY;
            // ClientIconConfig wraps an item ID — resolve to ItemStack
            return clientIcon.getItemStack();
        }
    }

    public record StartingItemEntry(
        String itemId,
        ResourceLocation customIcon // null = render as item, non-null = render as texture
    ) {}
}
```

### 3. Mixin: Extend Navigation

**File: `Forge/src/main/java/net/puffish/skillsmod/mixin/bridge/ClassSelectScreenMixin.java`**

```java
@Mixin(targets = "com.example.epicclassmod.client.ClassSelectScreen")
public abstract class ClassSelectScreenMixin extends Screen {

    @Shadow private PlayerClassData.ClassType current;
    @Shadow private float modelYaw;
    @Shadow private float modelPitch;

    @Shadow abstract int pageToScreenX(int x);
    @Shadow abstract int pageToScreenY(int y);
    @Shadow abstract int pageSizeToScreenW(int w);
    @Shadow abstract int pageSizeToScreenH(int h);

    // Track whether we're showing a dynamic class
    @Unique private DynamicClassDefinition puffish_currentDynamic = null;
    @Unique private int puffish_dynamicIndex = -1; // -1 = showing built-in

    /**
     * Override prevClass() to include dynamic classes in the cycle.
     */
    @Inject(method = "prevClass", at = @At("HEAD"), cancellable = true)
    private void puffish_prevClass(CallbackInfo ci) {
        if (DynamicClassRegistry.count() == 0) return; // No custom classes

        if (puffish_dynamicIndex >= 0) {
            // Currently on a dynamic class
            puffish_dynamicIndex--;
            if (puffish_dynamicIndex < 0) {
                // Wrap to last built-in class (ARCHER = ordinal 6)
                puffish_currentDynamic = null;
                current = ClassType.values()[ClassType.values().length - 1];
            } else {
                puffish_currentDynamic = DynamicClassRegistry.getAtIndex(
                    puffish_dynamicIndex + 6);
            }
            ci.cancel();
        } else if (current.ordinal() == 1) {
            // At first built-in class (WARRIOR), wrap to last dynamic
            puffish_dynamicIndex = DynamicClassRegistry.count() - 1;
            puffish_currentDynamic = DynamicClassRegistry.getAtIndex(
                puffish_dynamicIndex + 6);
            ci.cancel();
        }
        // Otherwise: let vanilla handle (moving between built-in classes)
    }

    /**
     * Override nextClass() to include dynamic classes.
     */
    @Inject(method = "nextClass", at = @At("HEAD"), cancellable = true)
    private void puffish_nextClass(CallbackInfo ci) {
        if (DynamicClassRegistry.count() == 0) return;

        if (puffish_dynamicIndex >= 0) {
            // On a dynamic class
            puffish_dynamicIndex++;
            if (puffish_dynamicIndex >= DynamicClassRegistry.count()) {
                // Wrap to first built-in (WARRIOR)
                puffish_dynamicIndex = -1;
                puffish_currentDynamic = null;
                current = ClassType.WARRIOR;
            } else {
                puffish_currentDynamic = DynamicClassRegistry.getAtIndex(
                    puffish_dynamicIndex + 6);
            }
            ci.cancel();
        } else if (current.ordinal() == ClassType.values().length - 1) {
            // At last built-in (ARCHER), move to first dynamic
            puffish_dynamicIndex = 0;
            puffish_currentDynamic = DynamicClassRegistry.getAtIndex(6);
            ci.cancel();
        }
    }
}
```

### 4. Mixin: Override Rendering for Dynamic Classes

```java
/**
 * Inject at HEAD of the render method to check if we should
 * render dynamic class content instead of built-in.
 *
 * Strategy: Let the vanilla render draw the popup box, title bar,
 * divider, and buttons. Then override just the content sections
 * (equipment, stats, description, items, passives) via @Redirect
 * or @Inject at specific points.
 *
 * Alternative (simpler): Override the entire content area by
 * drawing OVER the vanilla content when a dynamic class is shown.
 */
@Inject(method = "m_88315_", at = @At("TAIL"))
private void puffish_renderDynamicClass(GuiGraphics graphics,
                                         int mouseX, int mouseY,
                                         float partialTick, CallbackInfo ci) {
    if (puffish_currentDynamic == null) return;

    DynamicClassDefinition def = puffish_currentDynamic;

    // We need to redraw the content area over the default render.
    // The popup is at (popupX, popupY) in page coords, 520x360.
    // Left panel: 210 wide (player model)
    // Right panel: 310 wide (stats, desc, items, passives)
    // Bottom: passives area below both panels

    // ─── LEFT PANEL: Player Model with Custom Equipment ───

    Player previewPlayer = minecraft.player;

    // Save current equipment (same pattern as vanilla)
    ItemStack[] saved = savePlayerEquipment(previewPlayer);

    // Apply dynamic class equipment
    EquipmentVisuals vis = def.visuals();
    previewPlayer.setItemInHand(InteractionHand.MAIN_HAND,
        vis.resolveItem(vis.mainHandItem(), vis.mainHandFallback()));
    previewPlayer.setItemInHand(InteractionHand.OFF_HAND,
        vis.resolveItem(vis.offHandItem(), vis.offHandFallback()));
    previewPlayer.setItemBySlot(EquipmentSlot.HEAD,
        vis.resolveItem(vis.headItem(), vis.headFallback()));
    previewPlayer.setItemBySlot(EquipmentSlot.CHEST,
        vis.resolveItem(vis.chestItem(), vis.chestFallback()));
    previewPlayer.setItemBySlot(EquipmentSlot.LEGS,
        vis.resolveItem(vis.legsItem(), vis.legsFallback()));
    previewPlayer.setItemBySlot(EquipmentSlot.FEET,
        vis.resolveItem(vis.feetItem(), vis.feetFallback()));

    // Render 3D player model (same as vanilla)
    Quaternionf rotation = new Quaternionf()
        .rotationYXZ(
            (float) Math.toRadians(modelYaw),
            (float) Math.toRadians(modelPitch),
            (float) Math.toRadians(180));
    Quaternionf cameraRot = new Quaternionf().rotationXYZ(0, 0, 0);

    // Clear the left panel area first
    int leftPanelX = pageToScreenX(popupX + 1);
    int leftPanelY = pageToScreenY(popupY + 30);
    int leftPanelW = pageSizeToScreenW(209);
    int leftPanelH = pageSizeToScreenH(320);
    graphics.fill(leftPanelX, leftPanelY,
        leftPanelX + leftPanelW, leftPanelY + leftPanelH,
        CLR_POPUP_BG); // Same as popup background

    InventoryScreen.renderEntityInInventoryFollowsMouse(
        graphics,
        pageToScreenX(popupX + 105),
        pageToScreenY(popupY + 210),
        Math.max(30, pageSizeToScreenH(80)),
        rotation, cameraRot,
        previewPlayer);

    // Restore equipment
    restorePlayerEquipment(previewPlayer, saved);

    // ─── RIGHT PANEL: Custom Content ───

    int rightX = popupX + 215; // After divider
    int rightW = 300;          // Remaining width

    // Clear right panel
    int rpScreenX = pageToScreenX(rightX);
    int rpScreenY = pageToScreenY(popupY + 30);
    int rpScreenW = pageSizeToScreenW(rightW);
    int rpScreenH = pageSizeToScreenH(320);
    graphics.fill(rpScreenX, rpScreenY,
        rpScreenX + rpScreenW, rpScreenY + rpScreenH,
        CLR_POPUP_BG);

    // Class title
    drawStringPxBase(graphics, font, def.title(),
        pageToScreenX(rightX), pageToScreenY(popupY + 44),
        -1, 1.0f, false);

    // Stats
    int statY = pageToScreenY(popupY + 60);
    for (var stat : def.stats()) {
        drawStringPxBase(graphics, font, stat.label(),
            pageToScreenX(rightX), statY, -1515080, 1.0f, false);
        drawRepeatedIcons(graphics, stat.icon(),
            pageToScreenX(rightX + 36), statY - pageSizeToScreenH(2),
            stat.value(), stat.max(), 2);
        statY += linePxH(1.0f) + pageSizeToScreenH(4);
    }

    // Description
    int descY = statY + pageSizeToScreenH(4);
    drawWrappedScaled(graphics, def.description(),
        pageToScreenX(rightX), descY,
        pageSizeToScreenW(rightW - 8), -3620955, 0.95f);

    // Starting Items
    int itemsY = descY + pageSizeToScreenH(22);
    drawStringPxBase(graphics, font, "Starting Items",
        pageToScreenX(rightX), itemsY, -1515080, 1.0f, false);

    int itemY = itemsY + pageSizeToScreenH(14);
    int itemSize = pageSizeToScreenW(18);
    int itemGap = pageSizeToScreenW(6);
    for (int i = 0; i < def.startingItems().size(); i++) {
        var entry = def.startingItems().get(i);
        int itemX = pageToScreenX(rightX) + i * (itemSize + itemGap);
        if (entry.customIcon() != null) {
            graphics.blit(entry.customIcon(),
                itemX, itemY, 0, 0, itemSize, itemSize, itemSize, itemSize);
        } else {
            renderItemScaled(graphics,
                new ItemStack(ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation(entry.itemId()))),
                itemX, itemY, itemSize);
        }
    }

    // ─── BOTTOM: Passives (Auto-resolved from Pufferfish Category or Manual) ───

    int passivesStartY = popupY + 230; // Below player model
    // Clear passives area
    graphics.fill(
        pageToScreenX(popupX + 1),
        pageToScreenY(passivesStartY),
        pageToScreenX(popupX + 519),
        pageToScreenY(popupY + 355),
        CLR_POPUP_BG);

    drawStringPxBase(graphics, font, "Passives",
        pageToScreenX(popupX + 5),
        pageToScreenY(passivesStartY - 12),
        -2734779, 1.0f, false);

    // Resolve passives — source depends on def.passiveSource():
    //   CATEGORY → reads from ClientCategoryConfig.definitions()
    //   MANUAL   → uses def.manualPassives() directly
    //   MIXED    → category skills first, then manual extras
    List<ResolvedPassive> passives = def.resolvePassives();

    int cols = 2, gap = 8;
    int cellW = (520 - gap * 3) / cols;
    int cellH = (120 - gap) / 2;

    for (int i = 0; i < passives.size(); i++) {
        var passive = passives.get(i);
        int col = i % cols;
        int row = i / cols;
        int cx = pageToScreenX(popupX + 5 + col * (cellW + gap));
        int cy = pageToScreenY(passivesStartY + row * (cellH + gap));
        int cw = pageSizeToScreenW(cellW);
        int ch = pageSizeToScreenH(cellH);

        // Card background and borders
        graphics.fill(cx, cy, cx + cw, cy + ch, 0x80201828);
        graphics.fill(cx, cy, cx + cw, cy + 1, 0x30EEEEEE);
        graphics.fill(cx, cy + ch - 1, cx + cw, cy + ch, 0x30EEEEEE);
        graphics.fill(cx, cy, cx + 1, cy + ch, 0x30EEEEEE);
        graphics.fill(cx + cw - 1, cy, cx + cw, cy + ch, 0x30EEEEEE);

        // Icon — handles both item-based (from Pufferfish) and texture-based (manual)
        int iconSize = pageSizeToScreenW(12);
        int iconX = cx + pageSizeToScreenW(8);
        int iconY = cy + pageSizeToScreenH(6);
        renderPassiveIcon(graphics, passive, iconX, iconY, iconSize);

        // Name
        drawStringPxBase(graphics, font, passive.displayName(),
            iconX + iconSize + pageSizeToScreenW(6),
            iconY + pageSizeToScreenH(1),
            -1515080, 1.0f, false);

        // Description (may include level/toggle annotations for category-sourced)
        drawWrappedScaled(graphics, passive.displayDescription(),
            iconX + pageSizeToScreenW(8),
            iconY + iconSize + pageSizeToScreenH(4),
            cw - pageSizeToScreenW(16), -3620955, 0.95f);
    }
}
```

### 5. Mixin: Override choose() for Dynamic Classes

```java
/**
 * Intercept the choose button to handle dynamic class selection.
 */
@Inject(method = "choose", at = @At("HEAD"), cancellable = true)
private void puffish_choose(ClassType type, CallbackInfo ci) {
    if (puffish_currentDynamic == null) return;

    // Don't use the normal choose flow (which sends ClassType enum)
    // Instead send a custom packet with the dynamic class ID
    DynamicClassDefinition def = puffish_currentDynamic;

    // Send custom packet
    BridgeNetwork.CHANNEL.sendToServer(
        new ChooseDynamicClassPacket(def.id())
    );

    // Close screen
    minecraft.setScreen(null);
    ci.cancel();
}
```

### 6. Custom Network Packet

**File: `Forge/src/main/java/net/puffish/skillsmod/bridge/network/ChooseDynamicClassPacket.java`**

```java
package net.puffish.skillsmod.bridge.network;

public class ChooseDynamicClassPacket {
    private final String classId;

    public ChooseDynamicClassPacket(String classId) {
        this.classId = classId;
    }

    public static void encode(ChooseDynamicClassPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.classId);
    }

    public static ChooseDynamicClassPacket decode(FriendlyByteBuf buf) {
        return new ChooseDynamicClassPacket(buf.readUtf());
    }

    public static void handle(ChooseDynamicClassPacket msg,
                               Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            DynamicClassDefinition def = DynamicClassRegistry.get(msg.classId);
            if (def == null) {
                LOGGER.warn("Unknown dynamic class: {}", msg.classId);
                return;
            }

            // Activate the corresponding Pufferfish category
            EpicClassBridge.onClassChanged(player, msg.classId);

            // Optionally also set the Epic Class to NONE or a marker class
            // to indicate "this player chose a custom class"
            player.getCapability(CLASS_DATA_CAP).ifPresent(data -> {
                // Set to NONE so vanilla ECM doesn't interfere
                data.setClassType(ClassType.NONE);
            });

            // Store the dynamic class selection in our own data
            BridgePlayerData.setDynamicClass(player, msg.classId);
        });
        ctx.get().setPacketHandled(true);
    }
}
```

### 7. Bridge Player Data (Persistent)

**File: `Common/src/main/java/net/puffish/skillsmod/bridge/BridgePlayerData.java`**

```java
package net.puffish.skillsmod.bridge;

/**
 * Stores which dynamic class a player chose.
 * Uses Pufferfish Skills' existing persistent data system
 * or a custom Forge Capability.
 */
public class BridgePlayerData {

    private static final String NBT_KEY = "PuffishBridge_DynamicClass";

    public static void setDynamicClass(ServerPlayer player, String classId) {
        CompoundTag data = player.getPersistentData();
        data.putString(NBT_KEY, classId);
    }

    public static String getDynamicClass(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.getString(NBT_KEY);
    }

    public static boolean hasDynamicClass(ServerPlayer player) {
        return player.getPersistentData().contains(NBT_KEY);
    }

    public static void clearDynamicClass(ServerPlayer player) {
        player.getPersistentData().remove(NBT_KEY);
    }
}
```

### 8. Bridge Parsing in CategoryConfigMixin

**File: `Common/src/main/java/net/bluelotuscoding/skillleveling/mixin/CategoryConfigMixin.java`** (extended)

Instead of a standalone loader, the bridge data is parsed directly from `category.json` inside the existing `CategoryConfigMixin`. This leverages the proven `rawObj.remove()` pattern already used for `"prerequisite_skills"` and `"keep_unlocked"`.

```java
// Added to the existing onParseHead() method in CategoryConfigMixin:

@Inject(method = "parse", at = @At("HEAD"))
private static void onParseHead(Identifier id,
        net.puffish.skillsmod.api.json.JsonElement categoryJson,
        /* ... existing parameters ... */
        CallbackInfoReturnable<Result<CategoryConfig, Problem>> cir) {

    var rawJson = categoryJson.getJson();
    if (!rawJson.isJsonObject()) return;
    var rawObj = rawJson.getAsJsonObject();

    // ... existing keep_unlocked and prerequisite_skills parsing ...

    // ── Epic Class Mod Bridge (optional) ──
    if (rawObj.has("epic_class_bridge")) {
        var bridgeJson = rawObj.remove("epic_class_bridge").getAsJsonObject();
        parseBridgeDefinition(id, rawObj, bridgeJson);
    }
}

/**
 * Parses the optional "epic_class_bridge" object from category.json.
 * Reuses the category's own "title" and "icon" for class display.
 *
 * @param categoryId  the category's Identifier (e.g., "my_server:necromancer")
 * @param categoryObj the raw category JSON (still has "title", "icon", etc.)
 * @param bridgeJson  the removed "epic_class_bridge" object
 */
private static void parseBridgeDefinition(Identifier categoryId,
                                           JsonObject categoryObj,
                                           JsonObject bridgeJson) {
    // Reuse the category's title and icon — no duplication
    String title = categoryObj.has("title")
        ? categoryObj.get("title").getAsString()
        : categoryId.getPath();

    // Parse bridge-specific fields
    String description = bridgeJson.has("description")
        ? bridgeJson.get("description").getAsString()
        : "";

    // Parse visuals (armor, weapons) — optional
    EquipmentVisuals visuals = bridgeJson.has("visuals")
        ? parseVisuals(bridgeJson.getAsJsonObject("visuals"))
        : EquipmentVisuals.EMPTY;

    // Parse stats — optional
    List<StatDefinition> stats = bridgeJson.has("stats")
        ? parseStats(bridgeJson.getAsJsonArray("stats"))
        : List.of();

    // Parse passives config — optional
    PassiveConfig passives = bridgeJson.has("passives")
        ? parsePassives(bridgeJson.getAsJsonObject("passives"))
        : PassiveConfig.NONE;

    // Parse starting items — optional
    List<String> startingItems = bridgeJson.has("startingItems")
        ? parseStringArray(bridgeJson.getAsJsonArray("startingItems"))
        : List.of();

    Map<Integer, String> startingItemIcons = bridgeJson.has("startingItemIcons")
        ? parseStartingItemIcons(bridgeJson.getAsJsonObject("startingItemIcons"))
        : Map.of();

    // Build and register the dynamic class definition
    DynamicClassDefinition def = new DynamicClassDefinition(
        categoryId.toString(),  // class ID = category ID
        title,
        description,
        visuals,
        stats,
        passives,
        startingItems,
        startingItemIcons
    );

    DynamicClassRegistry.register(def.id(), def);
    LOGGER.info("Registered bridge class '{}' from category.json", categoryId);
}
```

**Why this is better than a separate loader:**

| Aspect | Separate `ClassDefinitionLoader` | Embedded in `CategoryConfigMixin` |
|--------|----------------------------------|-----------------------------------|
| File count | category.json + class_definitions/*.json | category.json only |
| Sync timing | Separate reload listener, must run after categories | Same parse pass — always in sync |
| `categoryId` field | Required (must match manually) | Implicit — IS the category |
| Title/icon duplication | Must duplicate from category | Reused directly |
| Existing pattern | New `PreparableReloadListener` | Extends proven `rawObj.remove()` pattern |
| Datapack author effort | Two files per class | One file per class |
```

---

## Sync Flow

Dynamic class definitions are parsed from `category.json` during datapack loading and must be synced to clients so the `ClassSelectScreen` can render them:

```
Server:                                Client:
  Datapack reload                        
  → CategoryConfig.parse() called        
  → CategoryConfigMixin.onParseHead()    
  → rawObj.remove("epic_class_bridge")   
  → parseBridgeDefinition()              
  → DynamicClassRegistry populated       
  → SyncDynamicClassesPacket ──────────→ DynamicClassRegistry populated
                                         → ClassSelectScreen can render
```

### Sync Packet

```java
public class SyncDynamicClassesPacket {
    private final Map<String, JsonObject> definitions;

    public static void encode(SyncDynamicClassesPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.definitions.size());
        for (var entry : msg.definitions.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue().toString());
        }
    }

    // ... decode, handle (populate client DynamicClassRegistry)
}
```

---

## Complete File Listing

| File | Side | Purpose |
|------|------|---------|
| `Common/.../bridge/DynamicClassRegistry.java` | Both | Stores dynamic class definitions |
| `Common/.../bridge/DynamicClassDefinition.java` | Both | Data class for class definition |
| `Common/.../mixin/CategoryConfigMixin.java` | Server | Extended to parse `epic_class_bridge` from category.json |
| `Common/.../bridge/BridgePlayerData.java` | Server | Persists dynamic class choice per player |
| `Forge/.../mixin/bridge/ClassSelectScreenMixin.java` | Client | Extends navigation + rendering |
| `Forge/.../bridge/network/ChooseDynamicClassPacket.java` | Both | Client → Server class selection |
| `Forge/.../bridge/network/SyncDynamicClassesPacket.java` | Both | Server → Client definition sync |
| `Forge/.../bridge/network/BridgeNetwork.java` | Both | Network channel registration |
| `data/<ns>/puffish_skills/categories/<cat>/category.json` | Datapack | Optional `epic_class_bridge` field for bridge integration |
| `data/<ns>/puffish_skills/categories/<cat>/definitions.json` | Datapack | **Source** for passives (`auto_grant`), exclusives (`exclusive`), and category-mode display |
| `assets/puffish_skills/textures/gui/icons/*.png` | Client | Custom class icons/textures |

---

## Rendering Method Reference

All rendering calls used in `ClassSelectScreen` that our Mixin must replicate for dynamic classes:

| Method | Signature | Purpose |
|--------|-----------|---------|
| `drawStringPxBase` | `(GuiGraphics, Font, String, int x, int y, int color, float scale, boolean shadow)` | Draw text at pixel position with optional scaling |
| `drawWrappedScaled` | `(GuiGraphics, String, int x, int y, int maxWidth, int color, float scale)` | Draw word-wrapped text with scaling |
| `drawRepeatedIcons` | `(GuiGraphics, ResourceLocation, int x, int y, int count, int max, int spacing)` | Draw a row of icons (filled vs empty, like hearts) |
| `renderItemScaled` | `(GuiGraphics, ItemStack, int x, int y, int size)` | Render an item icon scaled to given size |
| `InventoryScreen.m_280432_` | `(GuiGraphics, int x, int y, int scale, Quaternionf rot, Quaternionf cam, LivingEntity entity)` | Render 3D entity model with rotation |
| `equipArmorSet` | `(Player, String prefix)` | Set all 4 armor slots from a prefix + suffix pattern |
| `modItem` | `(String modItemId, ItemStack fallback)` | Resolve mod item by ID with vanilla fallback |
| `modArmor` | `(String prefix, EquipmentSlot, ItemStack fallback)` | Resolve armor piece by prefix + slot |

---

## Testing Checklist

- [ ] Dynamic class definitions load from `epic_class_bridge` in category.json on server start
- [ ] Categories WITHOUT `epic_class_bridge` work exactly as before (no regressions)
- [ ] Definitions sync to client on login
- [ ] Definitions sync on `/reload`
- [ ] New classes appear in ClassSelectScreen navigation cycle
- [ ] Custom armor renders on the 3D player model
- [ ] Custom weapons render in player's hands
- [ ] Stats display with correct icons and values
- [ ] Description text wraps correctly
- [ ] Starting items render (both as items and custom textures)
- [ ] Passives display with icons, names, and descriptions
- [ ] Category-sourced passives auto-resolve from Pufferfish definitions.json
- [ ] Category-sourced passives show level annotations when show_level is true
- [ ] Category-sourced passives respect filter list (only show specified skills)
- [ ] Category-sourced passives skip hidden skills automatically
- [ ] Toggle skills show [Toggle] badge in passive description
- [ ] Manual passives render with texture-based icons via blit()
- [ ] Mixed mode displays category skills first, then manual extras
- [ ] Item-based icons from Pufferfish render correctly at passive card size (12px)
- [ ] Passive cards still render if linked category is not loaded (graceful empty)
- [ ] Passive display is preview-only for non-auto_grant skills
- [ ] `auto_grant: true` field parsed from definitions.json into LeveledConfig
- [ ] `auto_grant: true` field parsed from definitions.json into LeveledConfig
- [ ] `exclusive: true` flag parsed from `prerequisite_skills` entries into RequiredSkillEntry
- [ ] Skills with `auto_grant: true` are discovered from LeveledConfigStorage by category
- [ ] Skills with `auto_grant: true` are auto-unlocked at level 1 on class selection
- [ ] Auto-grant validation rejects multi-level skills (`max_skill_level > 1`) with WARN
- [ ] Auto-grant validation rejects toggle skills with WARN
- [ ] Auto-grant validation rejects loot-gated skills with WARN
- [ ] Auto-grant enforces max 4 passives per category
- [ ] Auto-granted skills are revoked (set level 0) when player changes class
- [ ] Auto-granted skills NOT revoked if player manually leveled them (paid bits check)
- [ ] Auto-granted skill rewards fire correctly (attributes, effects, commands)
- [ ] Skills with `exclusive` prereq are hidden from tree until category reaches `min_level`
- [ ] Skills with `exclusive` prereq appear in ClassBookScreen exclusive section (🔒)
- [ ] Exclusive cards show "Unlocks at: [Category] Lv.X" and additional prereqs
- [ ] Exclusive skills appear in tree once category level is reached
- [ ] Exclusive skills can be any type (multi-level, toggle, loot-gated)
- [ ] Choosing a dynamic class activates the Pufferfish category
- [ ] Choosing a dynamic class does NOT send a vanilla ChooseClassPacket
- [ ] Player model drag rotation works for dynamic classes
- [ ] Dynamic class choice persists across logout/login
- [ ] Vanilla classes still work as before
- [ ] No crash when navigating between vanilla and dynamic classes
- [ ] Multiple dynamic classes render correctly
- [ ] Missing mod items fall back to vanilla items gracefully
- [ ] Empty equipment slots render correctly (no ghost items)

---

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Mixin into render method doesn't cover all drawing | High | Draw OVER vanilla content with opaque fill first |
| prevClass/nextClass injection misaligns indices | High | Thorough unit testing, edge case for 0 and max |
| Equipment resolution fails for missing mods | Medium | Fallback items configured per slot |
| Packet injection alongside vanilla leads to double-choose | Medium | Cancel vanilla choose() via Mixin when dynamic |
| ClassSelectScreen internal refactor in ECM update | High | Version check, MixinPlugin, refmap |
| Player model renders with wrong skin | Low | Use actual player entity, only swap equipment |
| Category not loaded when passives resolve | Medium | Graceful empty list fallback, re-resolve on category sync |
| ClientIconConfig item icon doesn't scale well | Low | Use pose().scale() to reduce 16px item to 12px |
| Skill definitions change on /reload but passives stale | Medium | Re-resolve passives on SyncDynamicClassesPacket |
| Large number of dynamic classes causes navigation fatigue | Low | Add page numbers or direct selection UI later |
| NBT data for dynamic class lost on death | Medium | Use player.getPersistentData() (survives death on Forge) |
| `epic_class_bridge` parsed before definitions.json | Low | Passive filter validation deferred until all categories loaded |
| Future Pufferfish update adds `epic_class_bridge` field | Very Low | Unique namespace; coordinate with upstream if needed |
| `rawObj.remove()` order matters with other mixins | Low | Single mixin, all removals in one `onParseHead()` — deterministic |
| Auto-granted skill rewards stack or conflict with tree-earned | Medium | Track via `paidBits` — auto-grant sets level but NOT paid bit; revoke only if unpaid |
| Auto-grant fires rewards player shouldn't have yet | Medium | Strict validation: only non-leveled, non-toggle, non-loot-gated skills allowed |
| Class switch leaves orphaned auto-grant rewards active | High | Revoke flow calls `setSkillLevel(0)` which triggers `deactivateLevelRewards()` |
| Multi-level skill marked `auto_grant: true` | Low | Validation rejects with WARN log; skill is treated as display-only |
| More than 4 `auto_grant` skills in one category | Low | Hard cap at 4; extras logged as WARN, treated as display-only |
| Skill has `exclusive` prereq but category never reaches level | Low | Skill stays hidden — valid design allowing permanent exclusivity |
| `auto_grant` + `exclusive` prereq on same skill | Very Low | `auto_grant` takes priority — skill is granted on class select, exclusive gate ignored |
| `exclusive` prereq without `category` field | Low | Falls back to skill's own `category_id` — validated at parse time |

---

## Comparison: All Three Tiers

| Aspect | Tier 1 | Tier 2 | Tier 3 |
|--------|--------|--------|--------|
| Complexity | Low | Medium | High |
| Mixins Required | 0-1 | 3-4 | 5-8 |
| Modifies ECM GUI | No | ClassBookScreen | ClassSelectScreen |
| New Packets | 0 | 0 | 2 |
| Datapack Content | Config only | Config only | Optional fields in category.json |
| Visual Impact | None | New tab icon | New class pages |
| Risk Level | Low | Medium | High |
| Dependency on ECM internals | Capability only | Tab system | Entire render pipeline |
| Recommended Build Order | First | Second | Third |

---

## Appendix A: equipArmorSet() Decompiled

```java
private static void equipArmorSet(Player player, String base) {
    String normalized = normalizeArmorBase(base);
    player.setItemBySlot(HEAD,
        modArmor(normalized, EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)));
    player.setItemBySlot(CHEST,
        modArmor(normalized, EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE)));
    player.setItemBySlot(LEGS,
        modArmor(normalized, EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS)));
    player.setItemBySlot(FEET,
        modArmor(normalized, EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS)));
}

private static ItemStack modArmor(String base, EquipmentSlot slot, ItemStack fallback) {
    String normalized = normalizeArmorBase(base);
    String suffix = armorSuffix(slot);
    return modItem(normalized + suffix, fallback);
}

private static ItemStack modItem(String modItemId, ItemStack fallback) {
    ResourceLocation rl = new ResourceLocation(modItemId);
    Item item = ForgeRegistries.ITEMS.getValue(rl);
    return (item != null && item != Items.AIR) ? new ItemStack(item) : fallback;
}
```

## Appendix B: Per-Class Equipment Map

| Class | Armor Set Prefix | Main Hand | Off Hand |
|-------|-----------------|-----------|----------|
| Warrior | `dungeons_and_combat:oni_slayer_` | `efn:yamato_dmc4_in_sheath` (→ iron_sword) | Empty |
| Paladin | `dungeons_and_combat:silver_` | `dungeons_and_combat:cobalt_long_sword` (→ iron_sword) | `dungeons_and_combat:silver_shield` (→ shield) |
| Berserker | `dungeons_and_combat:crimson_helmet` ¹ | `efn:ruinsgreatsword` (→ iron_sword) | Empty |
| Reaper | `dungeons_and_combat:rogue_helmet` ¹ | `efn:nf_dual_sword` (→ iron_sword) | Empty |
| Sorcerer | Individual ISS items ² | `dungeons_and_combat:fairy_scepter` (→ stick) | Empty |
| Archer | `dungeons_and_combat:forgotten_knight_` | `cataclysm:wrath_of_the_desert` (→ bow) | Empty |

¹ Berserker and Reaper pass the full item ID to `equipArmorSet`, which normalizes it.
² Sorcerer uses `irons_spellbooks:netherite_mage_{helmet,chestplate,leggings,boots}` set individually.

## Appendix C: Texture Asset Structure

```
assets/epicclassmod/textures/
├── entity/
│   ├── npc/                    # NPC textures per class
│   │   ├── archer_npc.png
│   │   ├── berserker_npc.png
│   │   ├── paladin_npc.png
│   │   ├── reaper_npc.png
│   │   ├── samurai_npc.png     # Warrior variant
│   │   └── sorcerer_npc.png
│   └── npc_sub/                # Sub-class NPC textures
│       ├── archer_sub.png
│       ├── berserker_sub.png
│       ├── paladin_sub.png
│       ├── reaper_sub.png
│       ├── sorcerer_sub.png
│       └── warrior_sub.png
├── gui/
│   ├── bg/                     # Background textures
│   │   ├── bookwithicons.png   # ClassBookScreen background
│   │   ├── profile_frame.png   # Portrait frame
│   │   └── ...
│   ├── icons/                  # UI icons
│   │   ├── heart.png           # ICON_HEART (stat)
│   │   ├── shield.png          # ICON_SHIELD (stat)
│   │   ├── sword.png           # ICON_SWORD (stat)
│   │   ├── boots.png           # ICON_SPEED / ICON_BOOTS (stat)
│   │   ├── iron_spell_book.png # Sorcerer starting item icon
│   │   ├── fireball.png        # Sorcerer starting item icon
│   │   ├── fortify.png         # Sorcerer starting item icon
│   │   ├── berserker.png       # Class portrait icon
│   │   ├── paladin.png
│   │   ├── reapper.png         # Note: typo in original
│   │   ├── soserer.png         # Note: typo in original
│   │   ├── sword_master.png    # Warrior class icon
│   │   ├── profile_sidetap.png # Side tab icons
│   │   ├── quests_sidetap.png
│   │   ├── map_sidetap.png
│   │   ├── setting.png
│   │   ├── skillframe.png      # Skill tree frame
│   │   ├── skillbook_passive.png
│   │   ├── buttons.png / buttons2.png
│   │   └── ...
│   └── portraits/              # Class portraits (ClassBookScreen)
│       ├── warrior_master_portrait.png
│       ├── archer_master_portrait.png
│       ├── berserker_master_portrait.png
│       ├── paladin_master_portrait.png
│       ├── reaper_master_portrait.png
│       ├── sorcerer_master_portrait.png
│       └── sub/                # Sub-class portraits
│           ├── sub1.png through sub14.png
```

---

*This document is part of the Pufferfish Skills × Epic Class Mod bridge design. See also:*
- *[BRIDGE_TIER1_CATEGORY_MAPPING.md](BRIDGE_TIER1_CATEGORY_MAPPING.md) — Category-to-class mapping*
- *[BRIDGE_TIER2_TAB_INJECTION.md](BRIDGE_TIER2_TAB_INJECTION.md) — Skill tree tab in ClassBookScreen*
