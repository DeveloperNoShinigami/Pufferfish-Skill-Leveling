# Bridge Tier 2: UV Texture Rendering Research & Implementation Plan

## Research Summary

### Problem Statement
The previous Bridge Tier 2 implementation had several critical issues:
1. **UV Texture Extraction Not Working** - Tab region wasn't properly extracted using UV coordinates
2. **Tab Position Incorrect** - Tab appeared "too short" and not on the far right side
3. **No Tab Flipping** - The image shows left-side tabs but we need right-side rendering
4. **Icon Not Centered** - Skill icon wasn't properly positioned on the tab

---

## Image Analysis: quest_book_additional_tabs.png

**File**: `Common/src/main/resources/assets/puffish_skill_leveling/textures/gui/icons/quest_book_additional_tabs.png`

### Dimensions
- **Full Image**: 551 × 453 pixels
- **Format**: RGBA PNG

### Tab Analysis at Declared Position (479, 328)

**Finding**: **The declared tab position (479, 328) is INCORRECT!**

```
Tab at (X=479, Y=328):
  - Tab region: X=479 to 511, Y=328 to 360 (32×32px)
  - Distance from right edge: 72 pixels
  - Distance from bottom: 125 pixels
  - Normalized UV: (0.8693, 0.7241) to (0.9274, 0.7947)
  - Sample pixel at center (495, 344): RGBA(0, 0, 0, 0) ← FULLY TRANSPARENT!
```

**This means there's NO TAB at X=479, Y=328 - that region is empty.**

### Right Edge Content Analysis
Pixel samples at Y=344 (center of declared tab height):
- X=451: RGBA(220, 196, 173, 255) ← Paper texture (book page)
- X=461: RGBA(221, 197, 173, 255) ← Paper texture  
- X=471: RGBA(93, 109, 125, 198) ← Border/shadow (semi-transparent)

**Conclusion**: The right edge of the image (X=451-471) contains **the book page edge**, not a tab. The tab must be located elsewhere in the image.

---

## Minecraft DrawContext.drawTexture() API Research

Based on analysis of `SkillsScreen.java` and `TextureBatchedRenderer.java`:

### Method Signature
```java
// From SkillsScreen.java (line 853-900)
context.drawTexture(
    Identifier texture,     // Texture resource location
    int x,                  // Screen X position (destination)
    int y,                  // Screen Y position (destination)
    int u,                  // Texture X offset (UV source)
    int v,                  // Texture Y offset (UV source)
    int width,              // Width to draw (destination)
    int height,             // Height to draw (destination)
    int textureWidth,       // Full texture width
    int textureHeight       // Full texture height
);
```

### UV Coordinate System
- **U/V coordinates are in PIXELS**, not normalized  0-1 floats
- U = Horizontal offset in source texture (X-axis)
- V = Vertical offset in source texture (Y-axis)
- The method extracts a rectangle from (u, v) to (u + width, v + height)
- Then stretches/scales that rectangle to fit the destination (x, y, width, height)

### Example from SkillsScreen
```java
// Drawing bottom left corner of window frame
context.drawTexture(
    WINDOW_TEXTURE,
    FRAME_PADDING,                              // Screen X
    this.height - FRAME_PADDING - HALF_FRAME_HEIGHT,  // Screen Y
    0,                                          // Source U (left edge of texture)
    HALF_FRAME_HEIGHT,                          // Source V (halfway down texture)
    HALF_FRAME_WIDTH,                           // Width to draw
    HALF_FRAME_HEIGHT,                          // Height to draw
    TEXTURE_WIDTH,                              // Full texture width
    TEXTURE_HEIGHT                              // Full texture height
);
```

---

## ClassBookScreen Coordinate System

From [BRIDGE_TIER2_TAB_INJECTION.md](BRIDGE_TIER2_TAB_INJECTION.md):

### Page Coordinate System
ClassBookScreen uses a **page-based coordinate system** that scales with GUI scale:

```java
float globalScale;    // Computed from GUI scale + user scale setting
int drawX, drawY;     // Top-left of book in screen pixels
int drawW, drawH;     // Book dimensions in screen pixels

// Conversion methods:
int pageToScreenX(int pageX) → drawX + (int)(pageX * globalScale)
int pageToScreenY(int pageY) → drawY + (int)(pageY * globalScale)
int pageSizeToScreenW(int w) → (int)(w * globalScale)
int pageSizeToScreenH(int h) → (int)(h * globalScale)
```

### Existing Tab Positions (LEFT SIDE)
| Index | Tab | Icon Texture | Page Position (X, Y) |
|-------|-----|-------------|----------------------|
| 0 | Profile | `profile_sidetap.png` | **(83, 328)** |
| 1 | Quests | `quests_sidetap.png` | **(81, 375)** |
| 2 | Map | `map_sidetap.png` | **(83, 280)** |
| 3 | Settings | `setting_sidetap.png` | **(82, 519)** |
| 4 | Pet (unused?) | `pet _sidetap.png` | **(81, 424)** |

**Key Observations**:
- All tabs on LEFT side at X ≈ 80-83
- Y positions vary (280-519) to stack vertically
- Each tab is 32×32 in page coordinates

### Book Page Dimensions
```java
static final int PAGE_W0_CONST;  // Total book width (approx 551 page units)
static final int PAGE_H0_CONST;  // Total book height (approx 453 page units)
```

---

## Problem Diagnosis

### Issue 1: Wrong Tab Location
The declared position **(479, 328)** doesn't contain a tab - that's just empty space.

**Likely Explanation**: The user provided an image of the full book with left-side tabs, but they want us to:
1. **Extract** one of the left-side tab graphics
2. **Mirror/flip** it horizontally  
3. **Position** it on the RIGHT side of the book

### Issue 2: Right-Side Positioning Calculation
For a right-side tab, assuming book width is 551 page units and tab is 32 units wide:

```
Right tab X position = PAGE_W0_CONST - TAB_SIZE - MARGIN
                     = 551 - 32 - 20  (assuming 20px margin)
                     = 499 page units

OR to align with book edge:
Right tab X position = 551 - 32 = 519 page units
```

**This is MUCH further right than the declared 479!**

### Issue 3: Tab Source UVs
If we're extracting the Profile tab (which is at page position 83, 328):
- But the image is the FULL BOOK, then the tab sprite within the image is at:
  - U = 83 (matching its page X position)
  - V = 328 (matching its page Y position)
  - Width =  32
  - Height = 32

---

## Recommended Implementation Approach

### Option A: Extract & Flip Existing Tab (Recommended)
1. **Extract** the Profile tab sprite from the book image
   - Source UV: (83, 328, 32, 32)
2. **Flip** it horizontally (mirror)
3. **Position** on far right: X = 519 (book width 551 - tab width 32)
4. **Render** skill icon centered on top

### Option B: Create Separate Tab Asset
1. Ask user to provide a proper **right-side tab sprite** (32×32px standalone)
2. Position at X = 519
3. Simpler UV calculation

### Option C: Use Pet Tab Slot
The Pet tab at index 4 is "unused" according to docs.
- If it's empty, we could render there instead
- Position: (81, 424) but mirrored to right side
- Right-side position: (519 - 81) = **(470, 424)** ← This is close to the user's 479!

---

## DrawContext.drawTexture() Implementation

### For Extracting Tab from Full Book Image

```java
// Assuming quest_book_additional_tabs.png contains the full book with left tabs
private static final Identifier QUEST_BOOK_TABS_TEXTURE = 
    new Identifier("puffish_skill_leveling", "textures/gui/icons/quest_book_additional_tabs.png");

private static final int BOOK_TEXTURE_WIDTH = 551;
private static final int BOOK_TEXTURE_HEIGHT = 453;

// Source position of tab in the texture (LEFT side)
private static final int TAB_SOURCE_U = 83;   // Profile tab X position
private static final int TAB_SOURCE_V = 328;  // Profile tab Y position
private static final int TAB_SIZE = 32;

// Destination position (RIGHT side of book in page coordinates)
private static final int TAB_DEST_PAGE_X = 519;   // 551 - 32
private static final int TAB_DEST_PAGE_Y = 328;   // Match vertical position

@Inject(method = "renderSideTabs", at = @At("TAIL"))
private void puffish_renderSkillTab(DrawContext context, CallbackInfo ci) {
    if (!EpicClassBridge.isEnabled()) return;

    // Convert page coordinates to screen coordinates
    int tabScreenX = pageToScreenX(TAB_DEST_PAGE_X);
    int tabScreenY = pageToScreenY(TAB_DEST_PAGE_Y);
    int tabScreenW = pageSizeToScreenW(TAB_SIZE);
    int tabScreenH = pageSizeToScreenH(TAB_SIZE);

    // Extract tab sprite using UV coordinates
    context.drawTexture(
        QUEST_BOOK_TABS_TEXTURE,
        tabScreenX, tabScreenY,         // Destination on screen
        TAB_SOURCE_U, TAB_SOURCE_V,     // Source position in texture
        tabScreenW, tabScreenH,         // Destination size (scaled)
        BOOK_TEXTURE_WIDTH,             // Full texture width
        BOOK_TEXTURE_HEIGHT             // Full texture height
    );

    // TODO: Add horizontal flip
    // TODO: Render skill icon on top
    // TODO: Handle hover & click
}
```

### For Horizontal Flipping

Minecraft's `DrawContext.drawTexture()` doesn't support flipping directly. Options:

**Option 1: Use Matrix Transformation**
```java
var matrices = context.getMatrices();
matrices.push();
matrices.translate(tabScreenX + tabScreenW / 2f, tabScreenY + tabScreenH / 2f, 0);
matrices.scale(-1f, 1f, 1f);  // Flip horizontal
matrices.translate(-(tabScreenX + tabScreenW / 2f), -(tabScreenY + tabScreenH / 2f), 0);

context.drawTexture(...);

matrices.pop();
```

**Option 2: Swap UV Coordinates**
Some implementations allow negative width to flip:
```java
context.drawTexture(
    TEXTURE,
    tabScreenX + tabScreenW, tabScreenY,  // Start from right
    TAB_SOURCE_U + TAB_SIZE, TAB_SOURCE_V,  // Source right edge
    -tabScreenW, tabScreenH,               // Negative width = flip
    TEXTURE_WIDTH, TEXTURE_HEIGHT
);
```

**Option 3: Pre-flip the Texture Asset**
- Use image editing software to create a right-facing tab
- Save as separate file
- No runtime flipping needed

---

## Icon Rendering on Top

```java
private static final Identifier SKILL_ICON = 
    new Identifier("puffish_skill_leveling", "textures/gui/icons/skill_tome.png");
private static final int ICON_SIZE = 16;

// After drawing tab:
int iconX = tabScreenX + (tabScreenW - iconScreenSize) / 2;
int iconY = tabScreenY + (tabScreenH - iconScreenSize) / 2;
int iconScreenSize = pageSizeToScreenW(ICON_SIZE);

context.drawTexture(
    SKILL_ICON,
    iconX, iconY,
    0, 0,                // Icon starts at (0, 0) in its own file
    iconScreenSize, iconScreenSize,
    ICON_SIZE, ICON_SIZE      // 16×16 icon texture
);
```

---

## Correct Tab Position Calculation

### User's Issue: "Tab too short/not on far right"

Given:
- Book width: 551 page units
- Tab width: 32 pixels
- User's attempted position: 479

**Analysis**:
- 479 + 32 = 511 (right edge of tab)
- 551 - 511 = **40 pixels of empty space** to the right

**Solution**: Move tab further right
```
Ideal X position = 551 - 32 - margin
                 = 551 - 32 - 8  (8px breathing room)
                 = 511 page units

Or flush with edge:
X = 551 - 32 = 519 page units
```

### Recommended Position
```java
private static final int TAB_DEST_PAGE_X = 511;  // 8px margin from right edge
private static final int TAB_DEST_PAGE_Y = 328;   // Matches left Profile tab height
```

This places the tab **40 pixels further right** than the incorrect 479 position!

---

## Testing Checklist

- [ ] Tab extracts correctly from source texture (no distortion)
- [ ] Tab appears on FAR RIGHT side of book
- [ ] Tab is horizontally flipped (if using left-side tab sprite)
- [ ] Skill icon renders centered on tab
- [ ] Tab scales correctly with GUI scale changes
- [ ] Tab hitbox detects mouse clicks accurately
- [ ] No rendering artifacts or z-fighting
- [ ] Works with different screen resolutions

---

## Next Steps

1. **Clarify with user**:
   - Where is the actual tab sprite in the image?
   - Or should we extract from left side and flip?
   - Or create a new right-facing tab asset?

2. **Implement mixin**:
   - Create `ClassBookScreenMixin.java`
   - Add `@Inject` into `renderSideTabs`
   - Use proper UV coordinates
   - Position at X=511 or X=519

3. **Add icon rendering**:
   - Center skill_tome.png on the tab
   - Scale appropriately

4. **Test positioning**:
   - Verify tab appears on far right
   - Measure distance from edge
   - Adjust X coordinate if needed

---

## Files to Create/Modify

### New Files
```
Forge/src/main/java/net/bluelotuscoding/skillleveling/bridge/forge/mixin/
  ├── ClassBookScreenMixin.java       (NEW - tab rendering)
  ├── BridgeMixinPlugin.java           (NEW - conditional loading)

Forge/src/main/resources/
  └── puffish_skill_leveling_bridge.mixins.json  (NEW - mixin config)
```

### Assets Required
```
Common/src/main/resources/assets/puffish_skill_leveling/textures/gui/icons/
  ├── quest_book_additional_tabs.png  (EXISTS - full book image)
  ├── skill_tome.png                  (EXISTS? - 16×16 icon)
  └── skill_tab_right.png             (OPTIONAL - if creating separate asset)
```

---

## Conclusion

**The core issue is twofold**:

1. **Wrong source UV**: The declared position (479, 328) is empty space. The actual tab sprite is at (83, 328) on the LEFT side.

2. **Wrong destination position**: 479 is too far left. The tab should be at **511-519** to appear on the far right edge.

**Recommended fix**:
- Extract tab from LEFT side at UV (83, 328)
- Flip it horizontally
- Render at RIGHT side position (511, 328) in page coordinates
- Add skill icon centered on top

This will place the tab **40+ pixels further right**, solving the "too short" issue.
