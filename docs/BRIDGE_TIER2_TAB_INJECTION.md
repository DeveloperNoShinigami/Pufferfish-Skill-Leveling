# Tier 2 — ClassBookScreen Tab Injection (Skill Tree Tab)

## Overview

Tier 2 injects a **"Skills" tab** into Epic Class Mod's `ClassBookScreen` — the book-style interface players open after choosing a class. This tab renders the Pufferfish Skills skill tree for the player's active category, embedded directly inside the class book UI.

This tier **requires Mixins** into `ClassBookScreen` to:
1. Add a new side tab icon alongside the existing 4 tabs
2. Intercept the render method to draw skill tree content when the new tab is active
3. Handle mouse input for skill tree interaction within the tab

---

## Epic Class Mod ClassBookScreen Internals

### Tab Enum
```java
// ClassBookScreen$Tab — inner enum, 4 values
enum Tab {
    PROFILE,   // ordinal 0 — stat allocation, portrait, class name
    QUESTS,    // ordinal 1 — quest log with scrollable entries
    MAP,       // ordinal 2 — world/dungeon map view
    SETTINGS   // ordinal 3 — keybind, display, gameplay settings
}
```

### Side Tab System

The `ClassBookScreen` uses an array-based side tab system with up to 5 visual tab icons positioned along the **left edge** of the book:

| Index | Tab | Icon Texture | Page Position (X, Y) |
|-------|-----|-------------|----------------------|
| 0 | Profile | `profile_sidetap.png` | (83, 328) |
| 1 | Quests | `quests_sidetap.png` | (81, 375) |
| 2 | Map | `map_sidetap.png` | (83, 280) |
| 3 | Settings | `setting_sidetap.png` (note: no 's') | (82, 519) |
| 4 | Pet (unused?) | `pet_sidetap.png` | (81, 424) |

Each tab icon is rendered as a 32×32 texture in page coordinates, then converted to screen coordinates via `pageToScreenX()/pageToScreenY()`.

#### Key Fields
```java
private Rect[] sideTabHit;   // Array of clickable rectangles (5 elements)
private int hoveredSideTab;  // -1 when no tab hovered
private int activeSideTab;   // Currently active tab index (default: 0 = Profile)
private Tab currentTab;      // The Tab enum value for current page
```

#### renderSideTabs() Method
Renders 5 side tab icons at hardcoded positions. Each tab:
1. Draws the icon texture via `GuiGraphics.m_280411_()` (blit with source rect)
2. Creates a `Rect` hitbox in `sideTabHit[index]`
3. Checks if mouse is inside the hitbox → sets `hoveredSideTab`

#### Mouse Click Handler (m_6375_ / mouseClicked)
When a side tab is clicked:
```java
// Pseudocode from bytecode analysis
if (isInside(mouseX, mouseY, sideTabHit[0])) { activeSideTab = 0; /* ... */ }
if (isInside(mouseX, mouseY, sideTabHit[1])) { activeSideTab = 1; /* ... */ }
// ... etc

// Then maps activeSideTab → Tab enum:
Tab[] tabOrder = { Tab.PROFILE, Tab.QUESTS, Tab.MAP, Tab.SETTINGS };
// activeSideTab index maps to this order
// There's also special handling: some checks use != instead of ==
// The exact mapping from sideTab index to Tab is done in the click handler

// Setting the tab:
this.currentTab = selectedTab;
```

Then in the render method:
```java
// Line ~6480 in bytecode
switch (this.currentTab.ordinal()) {
    case 0: // PROFILE — render stat allocation, portrait, etc.
    case 1: // QUESTS — delegate to questsPage.render()
    case 2: // MAP — delegate to mapPage.render()
    case 3: // SETTINGS — delegate to settingsPage.render()
}
```

### Page Coordinate System

ClassBookScreen uses the same page-coordinate system as ClassSelectScreen:

```java
float globalScale;    // Computed from GUI scale + user scale setting
int drawX, drawY;     // Top-left of the book in screen pixels
int drawW, drawH;     // Book dimensions in screen pixels

// Conversion methods:
int pageToScreenX(int pageX) → drawX + (int)(pageX * globalScale)
int pageToScreenY(int pageY) → drawY + (int)(pageY * globalScale)
int pageSizeToScreenW(int w) → (int)(w * globalScale)
int pageSizeToScreenH(int h) → (int)(h * globalScale)
int screenToPageX(int screenX) → (int)((screenX - drawX) / globalScale)
int screenToPageY(int screenY) → (int)((screenY - drawY) / globalScale)
```

### Book Layout Constants
```java
static final int PAGE_W0_CONST;  // Total book width in page units
static final int PAGE_H0_CONST;  // Total book height in page units

// Profile tab content areas:
static int PORTRAIT_PAGE_X0;     // Portrait position
static int PORTRAIT_PAGE_Y0;
static int PORTRAIT_W0, PORTRAIT_H0;
static int PORTRAIT_PAD0;
```

### Resource References
```
textures/gui/bg/profile_frame.png      — Portrait frame
textures/gui/bg/bookwithicons.png      — Main book background
textures/gui/icons/profile_sidetap.png — Profile tab icon
textures/gui/icons/quests_sidetap.png  — Quests tab icon
textures/gui/icons/map_sidetap.png     — Map tab icon
textures/gui/icons/setting.png         — Settings icon
textures/gui/icons/pet_sidetap.png     — Pet tab icon (5th slot)
textures/gui/icons/skillframe.png      — Skill frame decoration
textures/gui/icons/skillbook_passive.png — Passive skill icon
textures/gui/portraits/warrior_master_portrait.png  — Per-class portraits
textures/gui/portraits/archer_master_portrait.png
textures/gui/portraits/berserker_master_portrait.png
textures/gui/portraits/paladin_master_portrait.png
textures/gui/portraits/reaper_master_portrait.png
textures/gui/portraits/sorcerer_master_portrait.png
```

---

## Implementation Plan

### 1. Tab Enum Extension via Mixin

We cannot add values to a Java enum at runtime without unsafe hacks. Instead, we bypass the enum entirely:

**Strategy: Use activeSideTab index directly, don't create a new Tab enum value.**

The `activeSideTab` is an integer. The `currentTab` enum is only set inside the click handler. We can:
1. Add a 6th side tab icon via Mixin `@Inject` into `renderSideTabs`
2. Intercept the click handler to set a custom flag when tab index 5 is clicked
3. Intercept the render switch to draw our content when the custom flag is set

### 2. Mixin: Add Skill Tree Side Tab Icon

**File: `Forge/src/main/java/net/puffish/skillsmod/mixin/ClassBookScreenMixin.java`**

```java
@Mixin(targets = "com.example.epicclassmod.client.ClassBookScreen")
public abstract class ClassBookScreenMixin extends Screen {

    @Shadow private Rect[] sideTabHit;
    @Shadow private int hoveredSideTab;
    @Shadow private int activeSideTab;
    @Shadow private Tab currentTab;
    @Shadow private float globalScale;
    @Shadow private int drawX, drawY;

    @Shadow abstract int pageToScreenX(int x);
    @Shadow abstract int pageToScreenY(int y);
    @Shadow abstract int pageSizeToScreenW(int w);
    @Shadow abstract int pageSizeToScreenH(int h);

    // Custom state for our injected tab
    @Unique
    private boolean puffish_skillTreeTabActive = false;

    @Unique
    private static final ResourceLocation SKILL_TREE_TAB_ICON =
        new ResourceLocation("puffish_skills", "textures/gui/icons/skill_tree_sidetap.png");

    /**
     * Inject at TAIL of renderSideTabs to add our 6th tab icon.
     */
    @Inject(method = "renderSideTabs", at = @At("TAIL"))
    private void puffish_renderSkillTreeTab(GuiGraphics graphics, CallbackInfo ci) {
        if (!EpicClassBridge.isEnabled()) return;

        // Position below the settings tab (or use the pet_sidetap slot)
        // Pet tab is at index 4, position (81, 424) — we can replace it
        // OR add at position (82, 470) for a 6th slot
        int tabX = pageToScreenX(82);
        int tabY = pageToScreenY(470); // Below settings tab
        int tabW = pageSizeToScreenW(32);
        int tabH = pageSizeToScreenH(32);

        // Draw tab icon
        graphics.blit(SKILL_TREE_TAB_ICON,
            tabX, tabY, tabW, tabH,
            0, 0, 32, 32, 32, 32);

        // Set up hitbox
        // We need to extend the sideTabHit array or use a separate field
        // Since the array is fixed at 5, use a separate Rect field:
        puffish_skillTabHitRect = new Rect(tabX, tabY, tabW, tabH);

        // Check hover
        if (lastMouseX >= tabX && lastMouseX < tabX + tabW &&
            lastMouseY >= tabY && lastMouseY < tabY + tabH) {
            hoveredSideTab = 5; // Our custom index
        }

        // Visual feedback: highlight if active
        if (puffish_skillTreeTabActive) {
            graphics.fill(tabX, tabY, tabX + tabW, tabY + tabH, 0x40FFFFFF);
        }
    }
}
```

### 3. Mixin: Intercept Tab Click

```java
/**
 * Inject into mouseClicked (m_6375_) to handle our tab click.
 * We inject at HEAD to check our hitbox before the vanilla logic.
 */
@Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true)
private void puffish_onMouseClicked(double mouseX, double mouseY, int button,
                                     CallbackInfoReturnable<Boolean> cir) {
    if (!EpicClassBridge.isEnabled()) return;
    if (button != 0) return;

    if (puffish_skillTabHitRect != null &&
        isInside((int) mouseX, (int) mouseY, puffish_skillTabHitRect)) {
        puffish_skillTreeTabActive = true;
        activeSideTab = 5;
        // Don't set currentTab — leave it as-is so the vanilla
        // switch statement falls through to default (draws nothing)
        // and we draw our content instead
        cir.setReturnValue(true);
        return;
    }

    // If any OTHER tab is clicked, deactivate our tab
    for (int i = 0; i < sideTabHit.length; i++) {
        if (sideTabHit[i] != null &&
            isInside((int) mouseX, (int) mouseY, sideTabHit[i])) {
            puffish_skillTreeTabActive = false;
            break;
        }
    }
}
```

### 4. Mixin: Render Skill Tree Content

```java
/**
 * Inject into the render method (m_88315_) at the point where
 * tab content is rendered (after the tab ordinal switch).
 *
 * The switch happens around bytecode line 6480:
 *   this.currentTab.ordinal() → tableswitch
 *
 * We inject at TAIL of the render method to overlay our content
 * when our tab is active.
 */
@Inject(method = "m_88315_", at = @At("TAIL"))
private void puffish_renderSkillTreeContent(GuiGraphics graphics,
                                             int mouseX, int mouseY,
                                             float partialTick,
                                             CallbackInfo ci) {
    if (!puffish_skillTreeTabActive) return;

    // Calculate content area (same as book page area)
    int contentX = pageToScreenX(100); // Left margin
    int contentY = pageToScreenY(50);  // Top margin
    int contentW = pageSizeToScreenW(PAGE_W0_CONST - 200);
    int contentH = pageSizeToScreenH(PAGE_H0_CONST - 100);

    // Draw background overlay to hide the profile/default content
    graphics.fill(contentX, contentY,
                  contentX + contentW, contentY + contentH,
                  0xFF1A1A2E); // Dark background

    // Render Pufferfish Skill Tree
    // This delegates to our custom SkillTreeRenderer which
    // draws the category's skill tree within the given bounds
    SkillTreeTabRenderer.render(
        graphics, minecraft.player,
        contentX, contentY, contentW, contentH,
        mouseX, mouseY, partialTick
    );
}
```

### 5. Skill Tree Tab Renderer

**File: `Common/src/main/java/net/puffish/skillsmod/bridge/SkillTreeTabRenderer.java`**

```java
package net.puffish.skillsmod.bridge;

/**
 * Renders the Pufferfish skill tree within the ClassBookScreen tab.
 *
 * This renderer adapts the existing SkillsScreen rendering logic
 * to fit within a bounded rectangular area inside the book.
 */
public class SkillTreeTabRenderer {

    private static float scrollX = 0, scrollY = 0;
    private static float zoom = 1.0f;

    public static void render(GuiGraphics graphics, Player player,
                               int x, int y, int w, int h,
                               int mouseX, int mouseY, float partialTick) {
        // 1. Get the player's active category from the bridge
        ResourceLocation categoryId = EpicClassBridge.getActiveCategoryForPlayer(player);
        if (categoryId == null) {
            drawCenteredString(graphics, "No skill tree available",
                               x + w / 2, y + h / 2, 0xFFAAAAAA);
            return;
        }

        // 2. Get skill tree data
        // Access the Pufferfish Skills client data for this category
        var categoryData = SkillsClientData.getCategory(categoryId);
        if (categoryData == null) return;

        // 3. Set up scissor (clip to book area)
        graphics.enableScissor(x, y, x + w, y + h);

        // 4. Draw category title
        String title = categoryData.getTitle();
        graphics.drawCenteredString(
            Minecraft.getInstance().font, title,
            x + w / 2, y + 5, 0xFFFFFFFF
        );

        // 5. Draw skill nodes (simplified tree rendering)
        // This iterates over skills in the category and renders
        // them as icons with connection lines
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + w / 2f + scrollX, y + h / 2f + scrollY, 0);
        pose.scale(zoom, zoom, 1.0f);

        for (var skill : categoryData.getSkills()) {
            int nodeX = skill.getX();
            int nodeY = skill.getY();
            boolean unlocked = skill.isUnlocked();

            // Draw connections first
            for (var connection : skill.getConnections()) {
                drawConnection(graphics, nodeX, nodeY,
                              connection.getX(), connection.getY(),
                              unlocked ? 0xFF4CAF50 : 0xFF555555);
            }

            // Draw skill node
            ResourceLocation icon = skill.getIcon();
            int nodeSize = 24;
            graphics.blit(icon,
                nodeX - nodeSize / 2, nodeY - nodeSize / 2,
                nodeSize, nodeSize,
                0, 0, 16, 16, 16, 16);

            // Draw lock overlay if not unlocked
            if (!unlocked) {
                graphics.fill(
                    nodeX - nodeSize / 2, nodeY - nodeSize / 2,
                    nodeX + nodeSize / 2, nodeY + nodeSize / 2,
                    0x80000000);
            }
        }

        pose.popPose();
        graphics.disableScissor();

        // 6. Draw skill points available
        int points = categoryData.getAvailablePoints();
        graphics.drawString(
            Minecraft.getInstance().font,
            "Skill Points: " + points,
            x + 5, y + h - 15, 0xFFFFD700
        );
    }

    /**
     * Handle mouse scroll for zoom within the skill tree tab.
     */
    public static boolean handleScroll(double mouseX, double mouseY, double delta,
                                        int x, int y, int w, int h) {
        if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
            zoom = (float) Math.max(0.5, Math.min(2.0, zoom + delta * 0.1));
            return true;
        }
        return false;
    }

    /**
     * Handle mouse drag for panning within the skill tree tab.
     */
    public static boolean handleDrag(double deltaX, double deltaY) {
        scrollX += (float) deltaX;
        scrollY += (float) deltaY;
        return true;
    }
}
```

### 6. Mouse Scroll & Drag Forwarding

```java
/**
 * Inject into mouseScrolled (m_6050_) to handle zoom in skill tree tab.
 */
@Inject(method = "m_6050_", at = @At("HEAD"), cancellable = true)
private void puffish_onMouseScrolled(double mouseX, double mouseY, double delta,
                                      CallbackInfoReturnable<Boolean> cir) {
    if (puffish_skillTreeTabActive) {
        int cx = pageToScreenX(100);
        int cy = pageToScreenY(50);
        int cw = pageSizeToScreenW(PAGE_W0_CONST - 200);
        int ch = pageSizeToScreenH(PAGE_H0_CONST - 100);

        if (SkillTreeTabRenderer.handleScroll(mouseX, mouseY, delta,
                                               cx, cy, cw, ch)) {
            cir.setReturnValue(true);
        }
    }
}

/**
 * Inject into mouseDragged (m_7979_) for panning.
 */
@Inject(method = "m_7979_", at = @At("HEAD"), cancellable = true)
private void puffish_onMouseDragged(double mouseX, double mouseY, int button,
                                     double deltaX, double deltaY,
                                     CallbackInfoReturnable<Boolean> cir) {
    if (puffish_skillTreeTabActive && button == 0) {
        SkillTreeTabRenderer.handleDrag(deltaX, deltaY);
        cir.setReturnValue(true);
    }
}
```

---

## Mixin Configuration

**File: `Forge/src/main/resources/puffish_skills_bridge.mixins.json`**

```json
{
    "required": false,
    "minVersion": "0.8",
    "package": "net.puffish.skillsmod.mixin.bridge",
    "refmap": "puffish_skills_bridge-refmap.json",
    "compatibilityLevel": "JAVA_17",
    "plugin": "net.puffish.skillsmod.mixin.bridge.BridgeMixinPlugin",
    "client": [
        "ClassBookScreenMixin"
    ],
    "mixins": [],
    "injectors": {
        "defaultRequire": 0
    }
}
```

### Conditional Mixin Plugin

Since the Mixin targets a class from another mod that may not be present:

```java
package net.puffish.skillsmod.mixin.bridge;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

public class BridgeMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only apply if epicclassmod is loaded
        try {
            Class.forName("com.example.epicclassmod.client.ClassBookScreen");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    // ... other methods return defaults
}
```

---

## Pet Tab Slot Strategy

The `renderSideTabs` method already renders **5 tab icons** but the Tab enum only has **4 values**. The 5th position (index 4) uses `pet_sidetap.png` and is positioned at page coordinates (81, 424). This suggests the pet tab is either:
- A placeholder for future content
- A subclass/mod extension tab

**We can either:**
1. **Replace the pet tab** with our Skills tab (reuse index 4, position (81, 424))
2. **Add a 6th tab** below settings at a new position

**Recommendation:** Replace the pet tab (index 4) since it appears unused. This avoids extending the `sideTabHit` array and simplifies hit detection.

If we replace index 4:
- Override the icon rendering for index 4 in `renderSideTabs`
- Intercept the click handler for `sideTabHit[4]`
- No need for a new Rect field

```java
@Redirect(
    method = "renderSideTabs",
    at = @At(
        value = "FIELD",
        target = "Lcom/example/epicclassmod/client/ClassBookScreen;pet_sidetap:Lnet/minecraft/resources/ResourceLocation;",
        opcode = Opcodes.GETSTATIC
    )
)
private ResourceLocation puffish_replacePetTabIcon() {
    if (EpicClassBridge.isEnabled()) {
        return SKILL_TREE_TAB_ICON;
    }
    return pet_sidetap; // original
}
```

---

## Texture Asset

Create a 32×32 pixel tab icon:

**File: `Forge/src/main/resources/assets/puffish_skills/textures/gui/icons/skill_tree_sidetap.png`**

Design: A small skill tree icon (branching nodes) or use the existing Pufferfish Skills icon scaled to 32×32.

---

## Content Area Layout

When the Skills tab is active, the book page area is available for rendering:

```
┌─────────────────────────────────────────────────────────┐
│  ClassBookScreen (full book)                             │
│  ┌──┐  ┌─────────────────────────────────────┐          │
│  │Tab│  │                                     │          │
│  │ 0 │  │  SKILL TREE CONTENT AREA            │          │
│  ├──┤  │                                     │          │
│  │Tab│  │  ┌────┐    ┌────┐                   │          │
│  │ 1 │  │  │Sk 1│────│Sk 2│                   │          │
│  ├──┤  │  └────┘    └──┬─┘                   │          │
│  │Tab│  │               │                     │          │
│  │ 2 │  │            ┌──┴─┐                   │          │
│  ├──┤  │            │Sk 3│                   │          │
│  │Tab│  │            └────┘                   │          │
│  │ 3 │  │                                     │          │
│  ├──┤  │  [Skill Points: 3]                  │          │
│  │📖│  │                                     │          │
│  │ 4 │  └─────────────────────────────────────┘          │
│  └──┘                                                   │
└─────────────────────────────────────────────────────────┘
```

Page coordinates for content area:
- Left margin: ~120 (past the tab icons)
- Top margin: ~50
- Available width: PAGE_W0 - 240 (margins both sides)
- Available height: PAGE_H0 - 100

---

## Integration with Pufferfish Skills Client Data

The renderer needs access to the client-side skill tree data. Key classes:

```java
// From Pufferfish Skills (existing):
// - SkillsScreen: The main skills screen
// - CategoryData: Contains skill nodes, connections, points
// - SkillData: Individual skill with position, icon, state

// We need to either:
// 1. Reuse the existing rendering from SkillsScreen
// 2. Create a simplified renderer for embedded use
```

**Option 1: Extract rendering from SkillsScreen**
- SkillsScreen does its own rendering in a custom widget system
- Extracting this is complex but most faithful

**Option 2: Simplified renderer (recommended for Tier 2)**
- Read category data from client sync
- Draw simple circles/icons for skills
- Draw lines for connections
- Handle click-to-unlock interaction
- Tooltip on hover

---

## Testing Checklist

- [ ] Skills tab icon appears in ClassBookScreen side tabs
- [ ] Clicking Skills tab switches to skill tree view
- [ ] Clicking any other tab switches back to normal content
- [ ] Skill tree displays correct skills for player's class/category
- [ ] Mouse scroll zooms the skill tree
- [ ] Mouse drag pans the skill tree
- [ ] Skill click sends unlock request to server
- [ ] Hover tooltip shows skill name and description
- [ ] Epic Class Mod not installed → no crash, tab not rendered
- [ ] Multiple category tabs work (if player has multiple categories)
- [ ] Tab persists across screen resize

---

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Mixin into third-party screen is fragile | High | Use `defaultRequire = 0`, MixinPlugin safety check |
| ClassBookScreen internal changes break offsets | High | Version-check in MixinPlugin, test each ECM update |
| Rendering conflicts with existing book content | Medium | Draw background overlay to fully obscure default content |
| Mouse event priority conflicts | Medium | Use `@At("HEAD")` with cancel for our tab |
| Pet tab actually used by ECM update | Low | Check ECM changelog, fall back to 6th slot |
| Skill tree data not synced to client | Medium | Ensure SkillsAPI syncs on category activation |

---

## Next Steps → Tier 3

Tier 3 extends this further by allowing **dynamic class registration** — the bridge creates new classes in Epic Class Mod from Pufferfish category data, appearing in the `ClassSelectScreen` with custom visuals. See [BRIDGE_TIER3_DYNAMIC_CLASSES.md](BRIDGE_TIER3_DYNAMIC_CLASSES.md).
