package net.bluelotuscoding.skillleveling.bridge.forge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Renders a Skills tab on Epic Class Mod's ClassBookScreen using Forge
 * ScreenEvent and reflection. This avoids mixin mapping issues between
 * Yarn (our project) and Mojang (Epic Class Mod) mappings.
 *
 * <p>
 * The tab is drawn as a 32×32 icon (matching the native side tabs)
 * using the same coordinate system as ClassBookScreen's own tabs.
 */
public final class ClassBookScreenRenderer {

    private static final String TARGET_CLASS = "com.example.epicclassmod.client.ClassBookScreen";

    // Tab background (parchment-colored blank tab, 27×37)
    private static final Identifier TAB_BG_TEXTURE = new Identifier(
            "puffish_skill_leveling", "textures/gui/icons/quest_book_additional_tab_right.png");
    private static final int TAB_TEX_W = 27;
    private static final int TAB_TEX_H = 37;

    // Skill icon overlay (16×16 item texture drawn centered on the tab)
    private static final Identifier SKILL_ICON_TEXTURE = new Identifier(
            "puffish_skill_leveling", "textures/item/skill_tome.png");
    private static final int ICON_TEX_SIZE = 16;

    // Tab position in 896×736 page-coordinate space
    // Position on RIGHT side mirrored from left tabs (which are at X≈83)
    // Right side mirror: decrease X to place tab naturally on right edge
    private static final int TAB_PAGE_X = 753;
    private static final int TAB_PAGE_Y = 280;
    private static final int TAB_PAGE_W = 27;
    private static final int TAB_PAGE_H = 37;

    // Icon is drawn smaller than the tab, centered with padding
    private static final int ICON_PAGE_SIZE = 28;
    private static final int ICON_PADDING_X = (TAB_PAGE_W - ICON_PAGE_SIZE) / 2;
    private static final int ICON_PADDING_Y = 2;

    // State passed from ClassBookScreenMixin
    public static int currentStatPage = 0;
    public static int currentStatPageCount = 1;
    public static long lastStatsRenderTime = 0;

    // Arrow textures
    private static final Identifier ARROW_TEX = new Identifier("epicclassmod",
            "textures/gui/icons/right_arrow.png");
    private static final Identifier LEFT_ARROW_TEX = new Identifier("puffish_skill_leveling",
            "textures/gui/icons/left_arrow.png");

    // Reflection cache — resolved once, reused every frame
    private boolean reflectionResolved;
    private boolean reflectionOk;
    private Field fieldGlobalScale;
    private Field fieldDrawX;
    private Field fieldDrawY;
    private Method methodPageToScreenX;
    private Method methodPageToScreenY;
    private Method methodPageSizeToScreenW;
    private Method methodPageSizeToScreenH;
    private Method methodSelectedType;

    @SubscribeEvent
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!EpicClassBridge.isEnabled()) {
            return;
        }

        Screen screen = event.getScreen();

        // Check by class name to avoid hard class reference across mappings
        if (!TARGET_CLASS.equals(screen.getClass().getName())) {
            return;
        }

        if (!resolveReflection(screen.getClass())) {
            return;
        }

        try {
            renderSkillTab(screen, event.getGuiGraphics());

            // Render arrows if the stats tab was drawn recently (within 150ms)
            if (System.currentTimeMillis() - lastStatsRenderTime < 150) {
                renderPagingArrows(screen, event.getGuiGraphics());
            }
        } catch (Exception e) {
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.error(
                    "[Bridge] Failed to render skill tab or arrows: " + e.getMessage());
        }
    }

    private void renderPagingArrows(Screen screen, DrawContext g) throws Exception {
        if (currentStatPageCount <= 1) {
            return;
        }

        int arrowW = (int) methodPageSizeToScreenW.invoke(screen, 28);
        int arrowH = (int) methodPageSizeToScreenH.invoke(screen, 28);
        int arrowY = (int) methodPageToScreenY.invoke(screen, 240);

        if (currentStatPage > 0) {
            int lx = (int) methodPageToScreenX.invoke(screen, 636);
            g.drawTexture(LEFT_ARROW_TEX, lx, arrowY, arrowW, arrowH, 0, 0, 14, 14, 14, 14);
        }

        if (currentStatPage < currentStatPageCount - 1) {
            int rx = (int) methodPageToScreenX.invoke(screen, 664);
            g.drawTexture(ARROW_TEX, rx, arrowY, arrowW, arrowH, 0, 0, 14, 14, 14, 14);
        }
    }

    private void renderSkillTab(Screen screen, DrawContext guiGraphics) throws Exception {
        int screenX = (int) methodPageToScreenX.invoke(screen, TAB_PAGE_X);
        int screenY = (int) methodPageToScreenY.invoke(screen, TAB_PAGE_Y);
        int w = (int) methodPageSizeToScreenW.invoke(screen, TAB_PAGE_W);
        int h = (int) methodPageSizeToScreenH.invoke(screen, TAB_PAGE_H);

        // Step 1: Draw the tab background on the right side
        guiGraphics.drawTexture(
                TAB_BG_TEXTURE,
                screenX, screenY, w, h,
                0.0f, 0.0f,
                TAB_TEX_W, TAB_TEX_H,
                TAB_TEX_W, TAB_TEX_H);

        // Step 2: Draw the skill tome icon centered on the tab
        int iconW = (int) methodPageSizeToScreenW.invoke(screen, ICON_PAGE_SIZE);
        int iconH = (int) methodPageSizeToScreenH.invoke(screen, ICON_PAGE_SIZE);
        int iconX = screenX + (int) methodPageSizeToScreenW.invoke(screen, ICON_PADDING_X);
        int iconY = screenY + (int) methodPageSizeToScreenH.invoke(screen, ICON_PADDING_Y);

        guiGraphics.drawTexture(
                SKILL_ICON_TEXTURE,
                iconX, iconY, iconW, iconH,
                0.0f, 0.0f,
                ICON_TEX_SIZE, ICON_TEX_SIZE,
                ICON_TEX_SIZE, ICON_TEX_SIZE);
    }

    @SubscribeEvent
    public void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!EpicClassBridge.isEnabled() || event.getButton() != 0) {
            return;
        }

        Screen screen = event.getScreen();
        if (!TARGET_CLASS.equals(screen.getClass().getName())) {
            return;
        }

        if (!resolveReflection(screen.getClass())) {
            return;
        }

        try {
            int screenX = (int) methodPageToScreenX.invoke(screen, TAB_PAGE_X);
            int screenY = (int) methodPageToScreenY.invoke(screen, TAB_PAGE_Y);
            int w = (int) methodPageSizeToScreenW.invoke(screen, TAB_PAGE_W);
            int h = (int) methodPageSizeToScreenH.invoke(screen, TAB_PAGE_H);

            double mouseX = event.getMouseX();
            double mouseY = event.getMouseY();

            // --- Existing Pufferfish Tab click handling ---
            if (mouseX >= screenX && mouseX < screenX + w && mouseY >= screenY && mouseY < screenY + h) {
                net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.info("[Bridge] Skill tab clicked!");

                // Try to get determining category for the current class
                java.util.Optional<Identifier> categoryId = java.util.Optional.empty();
                try {
                    if (methodSelectedType != null) {
                        Object classType = methodSelectedType.invoke(screen);
                        if (classType instanceof Enum) {
                            String className = ((Enum<?>) classType).name();
                            categoryId = EpicClassBridge.getCategoryForClass(className);
                        }
                    }
                } catch (Exception e) {
                    net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                            .warn("[Bridge] Failed to determine category for class: " + e.getMessage());
                }

                // Open Pufferfish GUI
                net.puffish.skillsmod.client.SkillsClientMod.getInstance().openScreen(categoryId);
                event.setCanceled(true);
                return;
            }

            // --- Arrow Paging logic ---
            // If the stats grid was rendered in the last 150ms, we must be on the PROFILE
            // tab!
            if (System.currentTimeMillis() - lastStatsRenderTime < 150 && currentStatPageCount > 1) {
                int arrowY = (int) methodPageToScreenY.invoke(screen, 240);
                int arrowW = (int) methodPageSizeToScreenW.invoke(screen, 28);
                int arrowH = (int) methodPageSizeToScreenH.invoke(screen, 28);

                // Left Arrow
                if (currentStatPage > 0) {
                    int lx = (int) methodPageToScreenX.invoke(screen, 636);
                    if (mouseX >= lx && mouseX < lx + arrowW && mouseY >= arrowY && mouseY < arrowY + arrowH) {
                        currentStatPage--;
                        net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                                .info("[Bridge] Left arrow clicked! Page now: " + currentStatPage);
                        event.setCanceled(true);
                        return;
                    }
                }

                // Right Arrow
                if (currentStatPage < currentStatPageCount - 1) {
                    int rx = (int) methodPageToScreenX.invoke(screen, 664);
                    if (mouseX >= rx && mouseX < rx + arrowW && mouseY >= arrowY && mouseY < arrowY + arrowH) {
                        currentStatPage++;
                        net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                                .info("[Bridge] Right arrow clicked! Page now: " + currentStatPage);
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.error(
                    "[Bridge] Failed to handle skill tab click: " + e.getMessage());
        }
    }

    /**
     * Resolve reflection handles once. Epic Class Mod uses Mojang mappings,
     * so field/method names in bytecode are the same as in the decompiled
     * source: globalScale, drawX, drawY, pageToScreenX, etc.
     */
    private boolean resolveReflection(Class<?> clazz) {
        if (reflectionResolved) {
            return reflectionOk;
        }
        reflectionResolved = true;
        try {
            fieldGlobalScale = clazz.getDeclaredField("globalScale");
            fieldGlobalScale.setAccessible(true);

            fieldDrawX = clazz.getDeclaredField("drawX");
            fieldDrawX.setAccessible(true);

            fieldDrawY = clazz.getDeclaredField("drawY");
            fieldDrawY.setAccessible(true);

            methodPageToScreenX = clazz.getDeclaredMethod("pageToScreenX", int.class);
            methodPageToScreenX.setAccessible(true);

            methodPageToScreenY = clazz.getDeclaredMethod("pageToScreenY", int.class);
            methodPageToScreenY.setAccessible(true);

            methodPageSizeToScreenW = clazz.getDeclaredMethod("pageSizeToScreenW", int.class);
            methodPageSizeToScreenW.setAccessible(true);

            methodPageSizeToScreenH = clazz.getDeclaredMethod("pageSizeToScreenH", int.class);
            methodPageSizeToScreenH.setAccessible(true);

            try {
                methodSelectedType = clazz.getDeclaredMethod("selectedTypeOrDefault");
                methodSelectedType.setAccessible(true);
            } catch (NoSuchMethodException e) {
                // Ignore if not found, we just won't open specific category
                net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                        .warn("[Bridge] selectedTypeOrDefault not found on ClassBookScreen");
            }

            reflectionOk = true;
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.info(
                    "[Bridge] ClassBookScreen reflection resolved successfully");
        } catch (Exception e) {
            reflectionOk = false;
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.error(
                    "[Bridge] Failed to resolve ClassBookScreen reflection: "
                            + e.getMessage());
        }
        return reflectionOk;
    }

}
