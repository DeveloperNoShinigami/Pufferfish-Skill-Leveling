package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into Epic Class Mod's ClassBookScreen to add a Skills tab on the right
 * side.
 * Uses @Pseudo since the target class may not exist if Epic Class Mod is not
 * installed.
 */
@Pseudo
@Mixin(targets = "com.example.epicclassmod.client.ClassBookScreen", remap = false)
public abstract class ClassBookScreenMixin extends Screen {

    // ===== Texture and Layout Constants =====

    @Unique
    private static final Identifier QUEST_BOOK_TABS_TEXTURE = new Identifier("puffish_skill_leveling",
            "textures/gui/icons/quest_book_additional_tabs.png");

    @Unique
    private static final Identifier SKILL_ICON_TEXTURE = new Identifier("puffish_skill_leveling",
            "textures/item/skill_tome.png");

    // Full book texture dimensions (551×453 pixels)
    @Unique
    private static final int BOOK_TEXTURE_WIDTH = 551;
    @Unique
    private static final int BOOK_TEXTURE_HEIGHT = 453;

    // Left-side tab source position in the texture (Profile tab)
    @Unique
    private static final int TAB_SOURCE_U = 83;
    @Unique
    private static final int TAB_SOURCE_V = 328;
    @Unique
    private static final int TAB_SIZE = 32;

    // Right-side destination position (flush with right edge: 551 - 32 = 519)
    @Unique
    private static final int TAB_DEST_PAGE_X = 519;
    @Unique
    private static final int TAB_DEST_PAGE_Y = 328; // Same vertical position as left tabs

    // Icon positioning (16×16 pixel icon, centered on 32×32 tab)
    @Unique
    private static final int ICON_SIZE = 16;
    @Unique
    private static final int ICON_OFFSET = (TAB_SIZE - ICON_SIZE) / 2; // = 8 pixels

    // ===== Shadowed Fields from ClassBookScreen =====

    @Shadow(remap = false)
    private float globalScale; // Scale factor for page-to-screen conversion

    @Shadow(remap = false)
    private int drawX; // Book top-left X in screen pixels

    @Shadow(remap = false)
    private int drawY; // Book top-left Y in screen pixels

    // ===== Shadowed Methods from ClassBookScreen =====

    @Shadow(remap = false)
    protected abstract int pageToScreenX(int pageX);

    @Shadow(remap = false)
    protected abstract int pageToScreenY(int pageY);

    @Shadow(remap = false)
    protected abstract int pageSizeToScreenW(int pageWidth);

    @Shadow(remap = false)
    protected abstract int pageSizeToScreenH(int pageHeight);

    // ===== Constructor (required for Screen extension) =====

    protected ClassBookScreenMixin(Text title) {
        super(title);
    }

    // ===== Mixin Injection: Render Skills Tab =====

    /**
     * Injects at the TAIL of renderSideTabs() to add our Skills tab after all
     * vanilla tabs are drawn.
     * This ensures proper layering and doesn't interfere with existing tab
     * rendering.
     */
    @Inject(method = "renderSideTabs", at = @At("TAIL"), remap = false)
    private void puffish_renderSkillTreeTab(DrawContext context, CallbackInfo ci) {
        // Only render if bridge is enabled
        if (!EpicClassBridge.isEnabled()) {
            return;
        }

        // Convert page coordinates to screen coordinates
        int tabScreenX = pageToScreenX(TAB_DEST_PAGE_X);
        int tabScreenY = pageToScreenY(TAB_DEST_PAGE_Y);
        int tabScreenW = pageSizeToScreenW(TAB_SIZE);
        int tabScreenH = pageSizeToScreenH(TAB_SIZE);

        // === STEP 1: Extract and flip the tab ===
        // We need to flip the left-side tab to create a right-side tab
        var matrices = context.getMatrices();
        matrices.push();

        // Translate to tab center, flip horizontally, translate back
        float centerX = tabScreenX + tabScreenW / 2f;
        float centerY = tabScreenY + tabScreenH / 2f;
        matrices.translate(centerX, centerY, 0);
        matrices.scale(-1f, 1f, 1f); // Flip horizontal (mirror on Y-axis)
        matrices.translate(-centerX, -centerY, 0);

        // Draw the flipped tab using UV coordinates
        context.drawTexture(
                QUEST_BOOK_TABS_TEXTURE,
                tabScreenX, tabScreenY, // Destination position (screen coords)
                TAB_SOURCE_U, TAB_SOURCE_V, // Source position (UV coords: 83, 328)
                tabScreenW, tabScreenH, // Destination size (scaled)
                BOOK_TEXTURE_WIDTH, // Full texture width (551)
                BOOK_TEXTURE_HEIGHT // Full texture height (453)
        );

        matrices.pop(); // Restore non-flipped transformation

        // === STEP 2: Render skill icon on top ===
        // Icon is drawn without flipping, centered on the tab
        int iconScreenSize = pageSizeToScreenW(ICON_SIZE);
        int iconScreenX = tabScreenX + (tabScreenW - iconScreenSize) / 2;
        int iconScreenY = tabScreenY + (tabScreenH - iconScreenSize) / 2;

        context.drawTexture(
                SKILL_ICON_TEXTURE,
                iconScreenX, iconScreenY, // Centered on tab
                0, 0, // Icon starts at (0,0) in its file
                iconScreenSize, iconScreenSize, // Icon size (scaled)
                ICON_SIZE, ICON_SIZE // Texture size (16×16)
        );

        // TODO (Tier 2 future work):
        // - Add hover detection (check mouse position)
        // - Add click handler to switch to skills view
        // - Render hover overlay/highlight
        // - Integrate with skill tree rendering system
    }
}
