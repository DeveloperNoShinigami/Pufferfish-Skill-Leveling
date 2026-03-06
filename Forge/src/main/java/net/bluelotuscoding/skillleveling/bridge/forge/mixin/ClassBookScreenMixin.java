package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.bridge.config.AttributeDef;
import net.bluelotuscoding.skillleveling.bridge.config.ClassPageDef;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper;
import net.bluelotuscoding.skillleveling.client.ClientCustomClassState;
import net.bluelotuscoding.skillleveling.util.PuffishItemHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Mixin to ClassBookScreen to handle passive skill visibility and unlock
 * status.
 * Uses Pufferfish Skill Leveling as the source of truth.
 * Using Yarn mappings as per project environment.
 */
@Pseudo
@Mixin(targets = "com.example.epicclassmod.client.ClassBookScreen", remap = false)
public abstract class ClassBookScreenMixin {

    @Unique
    private static final Identifier addon$ICON_STAT_RESET = new Identifier("epicclassmod",
            "textures/gui/icons/reset_button.png");
    @Unique
    private static final Identifier addon$BUTTONS_TEX = new Identifier("epicclassmod",
            "textures/gui/icons/buttons.png"); // The mapping is counter-intuitive in original
    @Unique
    private static final Identifier addon$TEXTPLATE2_TEX = new Identifier("epicclassmod",
            "textures/gui/icons/textplate2.png");

    @Unique
    private static final Set<String> BLOCKED_WEAPON_ICONS = new HashSet<>(Arrays.asList(
            "epicclassmod:textures/gui/icons/uchigatana_gui.png",
            "epicclassmod:textures/gui/icons/blood_staff.png",
            "epicclassmod:textures/gui/icons/golden_dagger.png",
            "epicclassmod:textures/gui/icons/iron_greatsword_gui.png",
            "epicclassmod:textures/gui/icons/shield.png",
            "epicclassmod:textures/gui/icons/soserer.png",
            "epicclassmod:textures/gui/icons/reapper.png",
            "epicclassmod:textures/gui/icons/berserker.png",
            "epicclassmod:textures/gui/icons/sword_master.png",
            "epicclassmod:textures/gui/icons/archer.png",
            "epicclassmod:textures/gui/icons/paladin.png"));

    @Unique
    private static java.lang.reflect.Field addon$activeTooltipBtnField = null;
    @Unique
    private static java.lang.reflect.Field addon$statPointsField = null;

    @Unique
    private static java.lang.reflect.Field addon$drawXField = null;
    @Unique
    private static java.lang.reflect.Field addon$drawYField = null;
    @Unique
    private static java.lang.reflect.Field addon$globalScaleField = null;

    @Unique
    private static java.lang.reflect.Method addon$drawStringMethod = null;
    @Unique
    private static java.lang.reflect.Method addon$selectedTypeMethod = null;
    @Unique
    private static java.lang.reflect.Method addon$isPassiveUnlockedMethod = null;
    @Unique
    private static java.lang.reflect.Method addon$pageToScreenXMethod = null;
    @Unique
    private static java.lang.reflect.Method addon$pageToScreenYMethod = null;
    @Unique
    private static java.lang.reflect.Method addon$pageSizeToScreenWMethod = null;
    @Unique
    private static java.lang.reflect.Method addon$pageSizeToScreenHMethod = null;

    @Unique
    private static java.lang.reflect.Field addon$rectXField = null;
    @Unique
    private static java.lang.reflect.Field addon$rectYField = null;
    @Unique
    private static java.lang.reflect.Field addon$rectWField = null;
    @Unique
    private static java.lang.reflect.Field addon$rectHField = null;

    @Unique
    private float addon$getGlobalScale() {
        try {
            addon$initReflection();
            if (addon$globalScaleField != null) {
                return addon$globalScaleField.getFloat(this);
            }
        } catch (Exception ignored) {
        }
        return 1.0f;
    }

    @Unique
    private int addon$getDrawX() {
        try {
            addon$initReflection();
            if (addon$drawXField != null) {
                return addon$drawXField.getInt(this);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    @Unique
    private int addon$getDrawY() {
        try {
            addon$initReflection();
            if (addon$drawYField != null) {
                return addon$drawYField.getInt(this);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    @Unique
    private int addon$pageSizeToScreenW(int w0) {
        try {
            addon$initReflection();
            if (addon$pageSizeToScreenWMethod != null) {
                return (int) addon$pageSizeToScreenWMethod.invoke(this, w0);
            }
        } catch (Exception ignored) {
        }
        return Math.round((float) w0 * addon$getGlobalScale());
    }

    @Unique
    private int addon$pageSizeToScreenH(int h0) {
        try {
            addon$initReflection();
            if (addon$pageSizeToScreenHMethod != null) {
                return (int) addon$pageSizeToScreenHMethod.invoke(this, h0);
            }
        } catch (Exception ignored) {
        }
        return Math.round((float) h0 * addon$getGlobalScale());
    }

    @Unique
    private int addon$pageToScreenX(int px) {
        try {
            addon$initReflection();
            if (addon$pageToScreenXMethod != null) {
                return (int) addon$pageToScreenXMethod.invoke(this, px);
            }
        } catch (Exception ignored) {
        }
        return addon$getDrawX() + Math.round((float) px * addon$getGlobalScale());
    }

    @Unique
    private int addon$pageToScreenY(int py) {
        try {
            addon$initReflection();
            if (addon$pageToScreenYMethod != null) {
                return (int) addon$pageToScreenYMethod.invoke(this, py);
            }
        } catch (Exception ignored) {
        }
        return addon$getDrawY() + Math.round((float) py * addon$getGlobalScale());
    }

    @Unique
    private final Map<String, Double> addon$expressionVars = new HashMap<>();

    @Unique
    private static boolean addon$reflectionInit = false;

    @Unique
    private static void addon$initReflection() {
        if (addon$reflectionInit) {
            return;
        }
        try {
            Class<?> levelStateClazz = Class.forName("com.example.epicclassmod.client.ClientLevelState");
            addon$statPointsField = levelStateClazz.getDeclaredField("statPoints");
            addon$statPointsField.setAccessible(true);

            Class<?> screenClazz = Class.forName("com.example.epicclassmod.client.ClassBookScreen");
            addon$activeTooltipBtnField = screenClazz.getDeclaredField("activeTooltipBtn");
            addon$activeTooltipBtnField.setAccessible(true);

            addon$drawXField = screenClazz.getDeclaredField("drawX");
            addon$drawYField = screenClazz.getDeclaredField("drawY");
            addon$globalScaleField = screenClazz.getDeclaredField("globalScale");
            addon$drawXField.setAccessible(true);
            addon$drawYField.setAccessible(true);
            addon$globalScaleField.setAccessible(true);

            addon$pageToScreenXMethod = screenClazz.getDeclaredMethod("pageToScreenX", int.class);
            addon$pageToScreenXMethod.setAccessible(true);
            addon$pageToScreenYMethod = screenClazz.getDeclaredMethod("pageToScreenY", int.class);
            addon$pageToScreenYMethod.setAccessible(true);
            addon$pageSizeToScreenWMethod = screenClazz.getDeclaredMethod("pageSizeToScreenW", int.class);
            addon$pageSizeToScreenWMethod.setAccessible(true);
            addon$pageSizeToScreenHMethod = screenClazz.getDeclaredMethod("pageSizeToScreenH", int.class);
            addon$pageSizeToScreenHMethod.setAccessible(true);
            addon$selectedTypeMethod = screenClazz.getDeclaredMethod("selectedTypeOrDefault");
            addon$selectedTypeMethod.setAccessible(true);
            try {
                addon$drawStringMethod = screenClazz.getDeclaredMethod("drawString", DrawContext.class,
                        TextRenderer.class,
                        String.class, int.class, int.class, int.class, float.class, boolean.class);
                addon$drawStringMethod.setAccessible(true);
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Exception e) {
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                    .error("Failed to cache reflection fields: " + e.getMessage());
        }
        addon$reflectionInit = true;
    }

    @Inject(method = "isSorcererSelected()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void onIsSorcererSelected(CallbackInfoReturnable<Boolean> cir) {
        var def = addon$getSelectedClassDef();
        if (def != null && "SORCERER".equals(def.epic_class_proxy)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private EpicClassDef addon$getSelectedClassDef() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return null;
        }
        String customId = ClientCustomClassState.getCustomClass(mc.player.getUuid());
        if (customId != null && !"epic_classes:none".equals(customId)) {
            EpicClassDef res = EpicClassConfigManager.getClassDef(customId);
            return res;
        }

        // 2. Fallback to enum-based class if no custom class is active
        try {
            Class<?> stateClazz = Class.forName("com.example.epicclassmod.client.ClientClassState");
            java.lang.reflect.Field selectedTypeField = stateClazz.getDeclaredField("selectedType");
            selectedTypeField.setAccessible(true);
            Object selectedTypeObj = selectedTypeField.get(null);
            if (selectedTypeObj instanceof Enum) {
                String enumName = ((Enum<?>) selectedTypeObj).name();
                if (!"NONE".equals(enumName)) {
                    // Try direct match first (e.g. enum NECROMANCER -> epic_classes:necromancer)
                    String direct = "epic_classes:" + enumName.toLowerCase(java.util.Locale.ROOT);
                    EpicClassDef directDef = EpicClassConfigManager.getClassDef(direct);
                    if (directDef != null) {
                        return directDef;
                    }

                    // Reverse-lookup by epic_class_proxy (e.g. enum SORCERER -> necromancer with
                    // proxy SORCERER)
                    for (EpicClassDef candidate : EpicClassConfigManager.getClasses().values()) {
                        if (enumName.equals(candidate.epic_class_proxy)) {
                            return candidate;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    @Unique
    private int addon$capturedSlot;

    @Unique
    private Object addon$getSelectedType() {
        try {
            if (addon$selectedTypeMethod == null) {
                // Look for the method on the class
                Class<?> clazz = ((Object) this).getClass();
                // Traverse up to find declaring class if needed, or just getDeclaredMethod from
                // the target class logic
                // The target is com.example.epicclassmod.client.ClassBookScreen
                // We can try to finding it by name.
                // Note: getMethod finds public, getDeclared finds all declared in that class.
                // "selectedTypeOrDefault" is protected in ClassBookScreen.
                while (clazz != null) {
                    try {
                        java.lang.reflect.Method m = clazz.getDeclaredMethod("selectedTypeOrDefault");
                        m.setAccessible(true);
                        addon$selectedTypeMethod = m;
                        break;
                    } catch (NoSuchMethodException e) {
                        clazz = clazz.getSuperclass();
                    }
                }
            }
            if (addon$selectedTypeMethod != null) {
                return addon$selectedTypeMethod.invoke(this);
            }
        } catch (Exception e) {
            // Log error just once or fail silently
        }
        return null;
    }

    /**
     * Redirect passive unlock check to Pufferfish skill levels.
     */
    @Inject(method = "isPassiveUnlockedClient(I)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void onIsPassiveUnlockedClient(int slot, CallbackInfoReturnable<Boolean> cir) {
        // Disabled for now as per user request (Tier 3)
        // if (!EpicClassBridge.isEnabled()) {
        // return;
        // }
        // ... (rest of logic commented out or removed)
    }

    /**
     * Internal visibility check logic.
     */
    @Unique
    private boolean addon$isPassiveVisible(int slot) {
        // Disabled for now as per user request (Tier 3)
        return true;
    }

    @Unique
    private boolean addon$invokeIsPassiveUnlocked(int slot) {
        try {
            if (addon$isPassiveUnlockedMethod == null) {
                Class<?> clazz = ((Object) this).getClass();
                while (clazz != null) {
                    try {
                        java.lang.reflect.Method m = clazz.getDeclaredMethod("isPassiveUnlockedClient", int.class);
                        m.setAccessible(true);
                        addon$isPassiveUnlockedMethod = m;
                        break;
                    } catch (NoSuchMethodException e) {
                        clazz = clazz.getSuperclass();
                    }
                }
            }
            if (addon$isPassiveUnlockedMethod != null) {
                return (boolean) addon$isPassiveUnlockedMethod.invoke(this, slot);
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Unique
    private void addon$invokeDrawString(DrawContext context, TextRenderer font, String text, int x, int y, int color,
            float scale, boolean shadow) {
        try {
            // Target method: drawStringPxBase(DrawContext, TextRenderer, String, int, int,
            // int, float, boolean)
            if (addon$drawStringMethod == null) {
                Class<?> clazz = ((Object) this).getClass();
                while (clazz != null) {
                    try {
                        // Note: We need to match the runtime parameter types.
                        // DrawContext, TextRenderer are Minecraft classes (remapped in dev environment
                        // usually? Mixin remap=false?)
                        // Since we are in Yarn dev, these are mapped. But target is "remap=false"?
                        // com.example.epicclassmod is likely compiled with Mojang mappings or similar.
                        // Usage of DrawContext means it's 1.20+.
                        // We will try to find it by name and parameter count/types loosely or exact.
                        // Let's assume standard reflection works if we use the classes present in our
                        // classpath (Yarn).
                        // If EpicClassMod uses different names for DrawContext, this might fail unless
                        // we look up by name primarily.
                        // For now, standard lookup:
                        java.lang.reflect.Method m = clazz.getDeclaredMethod("drawStringPxBase",
                                DrawContext.class, TextRenderer.class, String.class, int.class, int.class, int.class,
                                float.class, boolean.class);
                        m.setAccessible(true);
                        addon$drawStringMethod = m;
                        break;
                    } catch (NoSuchMethodException e) {
                        clazz = clazz.getSuperclass();
                    }
                }
            }
            if (addon$drawStringMethod != null) {
                addon$drawStringMethod.invoke(this, context, font, text, x, y, color, scale, shadow);
            }
        } catch (Exception e) {
        }
    }

    @Unique
    private String addon$getCategoryId(String className) {
        var opt = EpicClassBridge.getCategoryForClass(className);
        return opt.map(Identifier::toString).orElse(null);
    }

    // --- Hooks for renderPassiveUl ---

    @Inject(method = "renderPassiveUl(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), remap = false)
    private void addon$resetPassiveIdx(DrawContext g, int anchorPageX, int anchorPageY, CallbackInfo ci) {
        this.addon$capturedSlot = -1;
    }

    @Inject(method = "renderPassiveUl(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280411_(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V", ordinal = 0), remap = false)
    private void addon$incPassiveIdx(DrawContext g, int anchorPageX, int anchorPageY, CallbackInfo ci) {
        this.addon$capturedSlot++;
    }

    @Redirect(method = "renderPassiveUl(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280411_(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"), remap = false)
    private void redirUlBlit(DrawContext instance, @Coerce Object atlas, int x, int y, int width, int height, float u,
            float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        if (addon$isPassiveVisible(this.addon$capturedSlot)) {
            if (atlas instanceof Identifier) {
                instance.drawTexture((Identifier) atlas, x, y, width, height, u, v, uWidth, vHeight, textureWidth,
                        textureHeight);
            } else {
                instance.drawTexture(new Identifier(atlas.toString()), x, y, width, height, u, v, uWidth, vHeight,
                        textureWidth, textureHeight);
            }
        }
    }

    @Redirect(method = "renderPassiveUl(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;drawStringPxBase(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIFZ)V"), remap = false)
    private void redirUlString(@Coerce Object screen, DrawContext g, TextRenderer font, String s, int x, int y,
            int color,
            float scale, boolean shadow) {
        if (addon$isPassiveVisible(this.addon$capturedSlot)) {
            this.addon$invokeDrawString(g, font, s, x, y, color, scale, shadow);
        }
    }

    // @Shadow removed
    // protected abstract void drawStringPxBase(...)

    @Unique
    private int addon$currentStatIdx = -1;

    @Unique
    private int addon$getClientAllocPoints(String statId) {
        // 1. Try mapping custom stat IDs to ClientLevelState fields (optimistic local
        // state)
        String fieldName = switch (statId) {
            case "atk" -> "allocAtk";
            case "def" -> "allocDef";
            case "aspd" -> "allocAS";
            case "mspd" -> "allocMS";
            case "cooldown" -> "allocCooldown";
            case "regen" -> "allocRegen";
            default -> null;
        };

        if (fieldName != null) {
            try {
                Class<?> cls = Class.forName("com.example.epicclassmod.client.ClientLevelState");
                java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.getInt(null);
            } catch (Exception ignored) {
            }
        }

        // 2. Fall back to synced persistent NBT data on the client (synced via
        // SyncCustomNbtPacket)
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            var nbt = player.getPersistentData().getCompound("ecm_leveling");
            if (nbt != null && nbt.contains("alloc_" + statId)) {
                return nbt.getInt("alloc_" + statId);
            }
        }

        return 0;
    }

    @Unique
    private void addon$invokeOptimisticAllocate(int idx, int delta) {
        try {
            Class<?> stateClazz = Class.forName("com.example.epicclassmod.client.ClientLevelState");
            java.lang.reflect.Method m = stateClazz.getDeclaredMethod("optimisticAllocate", int.class, int.class);
            m.setAccessible(true);
            m.invoke(null, idx, delta);
        } catch (Exception e) {
            // Fallback: directly modify the field if method call fails
            try {
                java.lang.reflect.Field f = Class.forName("com.example.epicclassmod.client.ClientLevelState")
                        .getDeclaredField("statPoints");
                f.setAccessible(true);
                int current = f.getInt(null);
                f.setInt(null, Math.max(0, current - delta));
            } catch (Exception ignored) {
            }
        }
    }

    @Inject(method = "renderStatsGrid(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), remap = false)
    private void addon$resetStatIdx(DrawContext g, CallbackInfo ci) {
        this.addon$currentStatIdx = -1;
    }

    @Inject(method = "renderStatsGrid(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;pageToScreenX(I)I"), remap = false)
    private void addon$incStatIdx(DrawContext g, CallbackInfo ci) {
        this.addon$currentStatIdx++;
    }

    @Redirect(method = "renderStatsGrid(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280411_(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"), remap = false)
    private void redirStatsIcon(DrawContext instance, Identifier atlas, int x, int y, int width, int height, float u,
            float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        if (!EpicClassBridge.isEnabled()) {
            instance.drawTexture(atlas, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
            return;
        }

        var def = addon$getSelectedClassDef();
        if (def == null) {
            instance.drawTexture(atlas, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
            return;
        }

        // 1. Reset Button Handle
        if (atlas.equals(addon$ICON_STAT_RESET)) {
            instance.drawTexture(atlas, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
            return;
        }

        // 2. Grid Handling
        int idx = this.addon$currentStatIdx;
        List<ClassPageDef> pages = EpicClassConfigManager.getPagesForClass(def.class_name);
        if (idx >= 0 && !pages.isEmpty()
                && net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage < pages
                        .size()) {
            var page = pages
                    .get(net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage);

            // Hide everything if slot is unused
            if (idx >= page.slots.size()) {
                return;
            }

            // If it's the plate, let it draw
            if (atlas.getPath().contains("textplate2")) {
                instance.drawTexture(atlas, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
                return;
            }

            // If it's the plus button, let it draw
            if (atlas.getPath().contains("buttons")) {
                instance.drawTexture(atlas, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
                return;
            }

            // Grid Icon Swap
            var slot = page.slots.get(idx);
            if (slot.icon != null && !slot.icon.isEmpty()) {
                ItemStack stack = addon$getIconStack(slot.icon);
                if (stack.isEmpty()) {
                    instance.drawTexture(new Identifier(slot.icon), x, y, width, height, 0.0f, 0.0f, 16, 16, 16, 16);
                    return;
                }
                // Item icon
                float s = (float) width / 16.0f;
                instance.getMatrices().push();
                instance.getMatrices().translate((float) x, (float) y, 201.0f);
                instance.getMatrices().scale(s, s, 1.0f);
                instance.drawItem(stack, 0, 0);
                instance.getMatrices().pop();
                return;
            }
        }

        instance.drawTexture(atlas, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
    }

    @Redirect(method = "renderStatsGrid(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280480_(Lnet/minecraft/world/item/ItemStack;II)V"), remap = false)
    private void redirStatsItem(DrawContext instance, ItemStack stack, int x, int y) {
        if (!EpicClassBridge.isEnabled()) {
            instance.drawItem(stack, x, y);
            return;
        }
        var def = addon$getSelectedClassDef();
        int idx = this.addon$currentStatIdx;
        if (def != null && idx >= 0) {
            List<ClassPageDef> pages = EpicClassConfigManager.getPagesForClass(def.class_name);
            if (!pages.isEmpty()
                    && net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage < pages
                            .size()) {
                var page = pages
                        .get(net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage);
                if (idx < page.slots.size()) {
                    var slot = page.slots.get(idx);
                    ItemStack custom = addon$getIconStack(slot.icon);
                    if (!custom.isEmpty()) {
                        instance.drawItem(custom, x, y);
                        return;
                    }
                } else {
                    return; // Hide vanilla items for unused slots
                }
            }
        }
        instance.drawItem(stack, x, y);
    }

    @Redirect(method = "renderStatsGrid(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;drawStringPxBase(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIFZ)V"), remap = false)
    private void redirStatsString(@Coerce Object screen, DrawContext g, TextRenderer font, String text, int x, int y,
            int color, float scale, boolean shadow) {
        if (!EpicClassBridge.isEnabled()) {
            this.addon$invokeDrawString(g, font, text, x, y, color, scale, shadow);
            return;
        }

        var def = addon$getSelectedClassDef();
        if (def == null) {
            this.addon$invokeDrawString(g, font, text, x, y, color, scale, shadow);
            return;
        }

        // 1. Stat Points Label
        if (text.contains("Stat Points") || text.contains(": ")) {
            this.addon$invokeDrawString(g, font, text, x, y, color, scale, shadow);
            return;
        }

        // 2. Grid Values
        int idx = this.addon$currentStatIdx;
        List<ClassPageDef> pages = EpicClassConfigManager.getPagesForClass(def.class_name);
        if (idx >= 0 && !pages.isEmpty()
                && net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage < pages
                        .size()) {
            var page = pages
                    .get(net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage);
            if (idx >= page.slots.size()) {
                return; // Hide string for unused slots
            }

            // Prefix with stat name
            var slot = page.slots.get(idx);
            String name = slot.name != null ? net.minecraft.client.resource.language.I18n.translate(slot.name) : "";
            if (!name.isEmpty()) {
                text = name + ": " + text;
            }
        }

        this.addon$invokeDrawString(g, font, text, x, y, color, scale, shadow);
    }

    @Redirect(method = "renderStatsGrid(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;trOr(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"), remap = false)
    private String redirStatsTrOr(String key, String fallback, Object[] args) {
        if (EpicClassBridge.isEnabled() && "gui.epicclassmod.statpoints".equals(key)) {
            return net.minecraft.client.resource.language.I18n.translate("gui.epicclassmod.statpoints");
        }
        return addon$invokeTrOr(key, fallback, args);
    }

    @Inject(method = "m_7856_()V", at = @At("HEAD"), remap = false)
    private void onInit(CallbackInfo ci) {
        net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage = 0;
    }

    @Inject(method = "renderStatsGrid(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("TAIL"), remap = false)
    private void onRenderStatsGridTail(DrawContext g, CallbackInfo ci) {
        if (!EpicClassBridge.isEnabled()) {
            return;
        }
        var def = addon$getSelectedClassDef();
        if (def == null) {
            return;
        }

        List<ClassPageDef> pages = EpicClassConfigManager.getPagesForClass(def.class_name);
        if (pages.size() <= 1) {
            return;
        }

        // Paging arrows are now drawn in ClassBookScreenRenderer (post-render)
        // We just pass the state to it
        net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPageCount = pages.size();
        net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.lastStatsRenderTime = System
                .currentTimeMillis();
    }

    @Unique
    private ItemStack addon$getIconStack(String iconPath) {
        if (iconPath == null || iconPath.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // If it contains a colon and doesn't look like a standard texture path
        if (iconPath.contains(":") && !iconPath.contains("/textures/")) {
            return PuffishItemHelper.parseItemStack(iconPath);
        }
        return ItemStack.EMPTY;
    }

    @Unique

    @Inject(method = "calcStatValues()[Ljava/lang/String;", at = @At("HEAD"), cancellable = true, remap = false)
    private void onCalcStatValues(CallbackInfoReturnable<String[]> cir) {
        if (EpicClassBridge.isEnabled()) {
            EpicClassDef def = addon$getSelectedClassDef();
            if (def != null) {
                List<ClassPageDef> pages = EpicClassConfigManager.getPagesForClass(def.class_name);
                if (pages != null && !pages.isEmpty()) {
                    String[] results = new String[6];
                    Arrays.fill(results, "");
                    if (net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage < pages
                            .size()) {
                        ClassPageDef page = pages.get(
                                net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage);
                        MinecraftClient mc = MinecraftClient.getInstance();

                        for (int i = 0; i < page.slots.size() && i < 6; i++) {
                            AttributeDef customAttr = page.slots.get(i);
                            String fmt = customAttr.format != null ? customAttr.format : "+#.#";
                            int p = addon$getClientAllocPoints(customAttr.id);
                            double val = 0;

                            // 1. Compute expression value (fallback / bonus display)
                            double expressionVal = 0;
                            if (customAttr.compiledExpression != null) {
                                addon$expressionVars.clear();
                                addon$expressionVars.put("points", (double) p);
                                try {
                                    expressionVal = customAttr.compiledExpression.eval(addon$expressionVars);
                                } catch (Exception ignored) {
                                }
                            }

                            // 2. Display strategy: Total value if attribute exists, expression bonus
                            // otherwise
                            try {
                                var attrRegistry = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES;
                                var attr = attrRegistry
                                        .getValue(new net.minecraft.util.Identifier(customAttr.attribute_id));

                                if (attr == null && mc.player != null) {
                                    for (var inst2 : mc.player.getAttributes().getTracked()) {
                                        var regEntry = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES
                                                .getKey(inst2.getAttribute());
                                        if (regEntry != null && regEntry.toString().equals(customAttr.attribute_id)) {
                                            attr = inst2.getAttribute();
                                            break;
                                        }
                                    }
                                }

                                if (attr != null && mc.player != null) {
                                    var inst = mc.player.getAttributeInstance(attr);
                                    if (inst != null) {
                                        val = inst.getValue();
                                        fmt = "#.##";
                                    } else {
                                        val = expressionVal;
                                    }
                                } else {
                                    val = expressionVal;
                                }
                            } catch (Exception e) {
                                val = expressionVal;
                            }

                            results[i] = EpicClassBridge.formatValue(fmt, val);
                        }
                    }
                    cir.setReturnValue(results);
                }
            }
        }
    }

    @Redirect(method = "m_6375_(DDI)Z", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClientLevelState;optimisticAllocate(II)Z"), remap = false)
    private boolean redirOptimisticAllocate(int idx, int delta) {
        if (EpicClassBridge.isEnabled()) {
            EpicClassDef def = addon$getSelectedClassDef();
            if (def != null) {
                List<ClassPageDef> pages = EpicClassConfigManager.getPagesForClass(def.class_name);
                if (net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage < pages
                        .size()) {
                    ClassPageDef page = pages.get(
                            net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage);
                    if (page.slots != null && idx >= 0 && idx < page.slots.size()) {
                        var slot = page.slots.get(idx);
                        // Intercept and send our custom packet
                        new net.bluelotuscoding.skillleveling.bridge.network.CustomAllocateStatPacket(slot.id, delta)
                                .sendToServer();
                        // We return true but DON'T call vanilla optimisticAllocate to avoid standard
                        // stat logic
                        // We also need to subtract locally to give immediate feedback
                        try {
                            Class<?> levelStateClazz = Class
                                    .forName("com.example.epicclassmod.client.ClientLevelState");
                            java.lang.reflect.Field f = levelStateClazz.getDeclaredField("statPoints");
                            f.setAccessible(true);
                            int current = f.getInt(null);
                            if (current >= delta) {
                                f.setInt(null, current - delta);
                            }
                        } catch (Exception ignored) {
                        }
                        return false; // prevents vanilla from sending AllocateStatPacket
                    }
                    // idx is beyond defined slots (empty slot) — BLOCK allocation
                    return false;
                }
            }
        }
        // Fallback to vanilla
        try {
            Class<?> stateClazz = Class.forName("com.example.epicclassmod.client.ClientLevelState");
            java.lang.reflect.Method m = stateClazz.getDeclaredMethod("optimisticAllocate", int.class, int.class);
            m.setAccessible(true);
            return (boolean) m.invoke(null, idx, delta);
        } catch (Exception e) {
            return false;
        }
    }

    @Inject(method = "statTooltipLines(I)[Ljava/lang/String;", at = @At("HEAD"), cancellable = true, remap = false)
    private void onStatTooltipLines(int idx, CallbackInfoReturnable<String[]> cir) {
        if (EpicClassBridge.isEnabled()) {
            EpicClassDef def = addon$getSelectedClassDef();
            if (def != null) {
                List<ClassPageDef> pages = EpicClassConfigManager.getPagesForClass(def.class_name);
                if (pages != null && !pages.isEmpty()
                        && net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage < pages
                                .size()) {

                    ClassPageDef page = pages.get(
                            net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage);

                    if (page.slots != null && idx < page.slots.size()) {
                        AttributeDef customAttr = page.slots.get(idx);

                        String desc = customAttr.description != null ? customAttr.description : "";
                        String translated = I18n.hasTranslation(desc)
                                ? I18n.translate(desc)
                                : desc;
                        cir.setReturnValue(translated.split("\\n"));
                    } else if (idx < 6) {
                        cir.setReturnValue(new String[0]);
                    }
                }
            }
        }
    }

    @Redirect(method = "renderProfileBody(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280411_(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"), remap = false)
    private void redirWeaponIcon(DrawContext instance, @Coerce Object atlas, int x, int y, int width, int height,
            float u,
            float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        var def = addon$getSelectedClassDef();
        String path = atlas.toString();

        if (def != null && def.class_weapon_icon != null && !def.class_weapon_icon.isEmpty()) {
            if (BLOCKED_WEAPON_ICONS.contains(path)) {
                if (addon$renderCustomWeapon(instance, def.class_weapon_icon, x, y, width)) {
                    return;
                }
            }
        }

        // Restore original y without the "360" override
        if (atlas instanceof Identifier) {
            instance.drawTexture((Identifier) atlas, x, y, width, height, u, v, uWidth, vHeight, textureWidth,
                    textureHeight);
        } else {
            instance.drawTexture(new Identifier(path), x, y, width, height, u, v, uWidth, vHeight,
                    textureWidth, textureHeight);
        }
    }

    @Redirect(method = "renderProfileBody(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;drawItemIcon(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;III)V"), remap = false)
    private void redirDrawItemIcon(@Coerce Object screen, DrawContext g, ItemStack stack, int x, int y, int size) {
        var def = addon$getSelectedClassDef();
        if (def != null && def.class_weapon_icon != null && !def.class_weapon_icon.isEmpty()) {
            if (addon$renderCustomWeapon(g, def.class_weapon_icon, x, y, size)) {
                return;
            }
        }
        // Fallback to original via reflection (using passed-in y)
        try {
            java.lang.reflect.Method m = screen.getClass().getDeclaredMethod("drawItemIcon", DrawContext.class,
                    ItemStack.class, int.class, int.class, int.class);
            m.setAccessible(true);
            m.invoke(screen, g, stack, x, y, size);
        } catch (Exception e) {
            g.drawItem(stack, x, y);
        }
    }

    @Unique
    private boolean addon$renderCustomWeapon(DrawContext g, String icon, int x, int y, int size) {
        if (icon == null || icon.isEmpty()) {
            return false;
        }

        // If it's an item (contains : and likely no .png)
        if (icon.contains(":") && !icon.endsWith(".png")) {
            ItemStack stack = PuffishItemHelper.parseItemStack(icon);
            if (stack == ItemStack.EMPTY) {
                try {
                    Class<?> forgeRegs = Class.forName("net.minecraftforge.registries.ForgeRegistries");
                    Object reg = forgeRegs.getField("ITEMS").get(null);
                    java.lang.reflect.Method getValue = reg.getClass().getMethod("getValue",
                            Identifier.class);
                    Item item = (Item) getValue.invoke(reg, new Identifier(icon));
                    if (item != null && item != Items.AIR) {
                        stack = new ItemStack(item);
                    }
                } catch (Exception ignored) {
                }
            }

            if (!stack.isEmpty()) {
                float scale = (float) size / 16.0f;
                g.getMatrices().push();
                // Increase Z to ensuring it renders clearly over the backdrop
                // Translating by 300 to be even safer
                g.getMatrices().translate((float) x, (float) y, 350.0f);
                g.getMatrices().scale(scale, scale, 1.0f);
                g.drawItem(stack, 0, 0);
                g.getMatrices().pop();
                return true;
            }
        }

        // Try drawing as texture if item failed
        try {
            g.drawTexture(new Identifier(icon), x, y, size, size, 0.0f, 0.0f, 16, 16, 16, 16);
            return true;
        } catch (Exception e) {
        }

        return false;
    }

    @Redirect(method = "renderProfileBody(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;drawStringPxBase(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIFZ)V"), remap = false)
    private void redirProfileString(@Coerce Object screen, DrawContext g, TextRenderer font, String text, int x, int y,
            int color, float scale, boolean shadow) {
        // Restore original y without the invasive targetShift overrides
        this.addon$invokeDrawString(g, font, text, x, y, color, scale, shadow);
    }

    @Redirect(method = "renderProfileBody(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"), remap = false)
    private String redirProfileText(String key, Object[] args, @Coerce Object g, float partialTicks) {
        var def = addon$getSelectedClassDef();
        if (def != null) {
            // Handle class name (job title)
            if (key != null && (key.contains("job.") || key.contains("bridge."))) {
                return (def.display_name != null && !def.display_name.isEmpty()) ? def.display_name
                        : (def.gui_title != null ? net.minecraft.client.resource.language.I18n.translate(def.gui_title)
                                : key);
            }

            // Handle notes (lore)
            if (key != null && key.startsWith("notes.") || key.startsWith("gui.epicclassmod.lore")) {
                return (def.book_lore != null && !def.book_lore.isEmpty()) ? def.book_lore
                        : ((def.description != null && !def.description.isEmpty()) ? def.description : "");
            }
        }
        // Fallback to original via reflection
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("tr", String.class, Object[].class);
            m.setAccessible(true);
            return (String) m.invoke(null, key, args);
        } catch (Exception e) {
            return key;
        }
    }

    @Redirect(method = "renderProfileBody(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;trOr(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"), remap = false)
    private String redirProfileTrOr(String key, String fallback, Object[] args, @Coerce Object g, float partialTicks) {
        var def = addon$getSelectedClassDef();
        if (def != null) {
            // Check for Job Title primarily by fallback pattern or stale value
            // If the fallback is the known stale value, or either key/fallback look like
            // job titles or class names
            if ("Water Hashira".equals(fallback)
                    || (key != null && (key.contains(".title") || key.contains("job.") || key.contains("bridge.")
                            || key.contains("Shadowmancer")))
                    || (fallback != null && (fallback.contains(".title") || fallback.contains("job.")
                            || fallback.contains("bridge.") || fallback.contains("Hashira")))) {
                return (def.display_name != null && !def.display_name.isEmpty()) ? def.display_name
                        : (def.gui_title != null ? net.minecraft.client.resource.language.I18n.translate(def.gui_title)
                                : (fallback != null ? fallback : "Shadowmancer"));
            }
        }

        // Fallback to original via reflection
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("trOr", String.class, String.class,
                    Object[].class);
            m.setAccessible(true);
            String result = (String) m.invoke(null, key, fallback, args);

            // Double check if we still got "Water Hashira" or our magic key in the result
            if (def != null && ("Water Hashira".equals(result) || "bridge.job_title".equals(result))) {
                return (def.display_name != null && !def.display_name.isEmpty()) ? def.display_name
                        : (def.gui_title != null ? net.minecraft.client.resource.language.I18n.translate(def.gui_title)
                                : result);
            }
            return result;
        } catch (Exception e) {
            return fallback;
        }
    }

    @Redirect(method = "renderPassiveUl(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;trOr(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"), remap = false)
    private String redirPassiveUlTrOr(String key, String fallback, Object[] args, @Coerce Object g, int x, int y) {
        var def = addon$getSelectedClassDef();
        if (def != null && def.gui_passives != null && this.addon$capturedSlot >= 0
                && this.addon$capturedSlot < def.gui_passives.size()) {
            var passive = def.gui_passives.get(this.addon$capturedSlot);
            if (passive.pufferfish_skill_id != null && !passive.pufferfish_skill_id.isEmpty()) {
                String[] display = EpicClassBridge.getSkillDisplay(def.skill_category_id, passive.pufferfish_skill_id);
                if (display != null) {
                    return display[0]; // Name
                }
            }
            if (passive.name_key != null && !passive.name_key.isEmpty()) {
                return net.minecraft.client.resource.language.I18n.translate(passive.name_key);
            }
        }
        // Fallback to original
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("trOr", String.class, String.class,
                    Object[].class);
            m.setAccessible(true);
            return (String) m.invoke(null, key, fallback, args);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Redirect(method = "renderActiveTooltip(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookScreen;trOr(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"), remap = false)
    private String redirTooltipTrOr(String key, String fallback, Object[] args, @Coerce Object g) {
        var def = addon$getSelectedClassDef();
        int slot = this.addon$invokeGetActiveTooltipBtn();

        if (def != null && def.gui_passives != null && slot >= 0 && slot < def.gui_passives.size()) {
            var passive = def.gui_passives.get(slot);
            if (passive.pufferfish_skill_id != null && !passive.pufferfish_skill_id.isEmpty()) {
                String[] display = EpicClassBridge.getSkillDisplay(def.skill_category_id, passive.pufferfish_skill_id);
                if (display != null) {
                    return display[1]; // Description
                }
            }
            if (passive.desc_key != null && !passive.desc_key.isEmpty()) {
                return net.minecraft.client.resource.language.I18n.translate(passive.desc_key);
            }
        }

        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("trOr", String.class, String.class,
                    Object[].class);
            m.setAccessible(true);
            return (String) m.invoke(null, key, fallback, args);
        } catch (Exception e) {
            return fallback;
        }
    }

    // Passive Buttons visibility
    @Inject(method = "renderButtonUl(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280411_(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"), locals = LocalCapture.CAPTURE_FAILHARD, remap = false)
    private void captureButtonSlot(DrawContext g, int anchorPageX, int anchorPageY, CallbackInfo ci, int baseX,
            int baseY, int dW, int dH, int gapY, int SRC_W, int SRC_H, int i, int x, int y) {
        this.addon$capturedSlot = i;
    }

    @Redirect(method = "renderButtonUl(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280411_(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"), remap = false)
    private void redirButtonBlit(DrawContext instance, @Coerce Object atlas, int x, int y, int width, int height,
            float u,
            float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        if (addon$isPassiveVisible(this.addon$capturedSlot)) {
            if (atlas instanceof Identifier) {
                instance.drawTexture((Identifier) atlas, x, y, width, height, u, v, uWidth, vHeight, textureWidth,
                        textureHeight);
            } else {
                instance.drawTexture(new Identifier(atlas.toString()), x, y, width, height, u, v, uWidth, vHeight,
                        textureWidth, textureHeight);
            }
        }
    }

    @Redirect(method = "renderButtonUl(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At(value = "FIELD", target = "Lcom/example/epicclassmod/client/ClassBookScreen;buttonHit:[Lcom/example/epicclassmod/util/Rect;", opcode = 181), // PUTFIELD
            remap = false)
    private void redirButtonHit(@Coerce Object screen, Object[] array, int index, @Coerce Object value) {
        if (addon$isPassiveVisible(index)) {
            array[index] = value;
        } else {
            array[index] = null; // Ensure it's not clickable
        }
    }

    @Inject(method = "m_6375_(DDI)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void onMouseClicked(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0) {
            var def = addon$getSelectedClassDef();
            if (def != null) {
                var pages = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                        .getPagesForClass(def.class_name);
                if (!pages.isEmpty()
                        && net.bluelotuscoding.skillleveling.bridge.forge.ClassBookScreenRenderer.currentStatPage < pages
                                .size()) {

                    float scale = addon$getGlobalScale();
                    Object resetBtn = null;
                    try {
                        java.lang.reflect.Field f = this.getClass().getDeclaredField("statResetBtnHit");
                        f.setAccessible(true);
                        resetBtn = f.get(this);
                    } catch (Exception ignored) {
                    }

                    if (resetBtn != null) {
                        if (addon$isRectHit(resetBtn, mx, my)) {
                            addon$invokeOpenResetPopup();
                            cir.setReturnValue(true);
                            return;
                        }
                    } else {
                        // Fallback manual calc
                        int sx = addon$pageToScreenX(480);
                        int sy = addon$pageToScreenY(240);
                        int sp = EpicClassSyncHelper.getAvailablePoints(MinecraftClient.getInstance().player);
                        String spText = net.minecraft.client.resource.language.I18n
                                .translate("gui.epicclassmod.statpoints") + ": " + sp;
                        int spTextW = MinecraftClient.getInstance().textRenderer.getWidth(spText);
                        float sc = 0.8f;
                        int textW = Math.round(spTextW * sc * scale);
                        int gap = addon$pageSizeToScreenW(6);
                        int btnSize = Math.max(12, Math.round(14 * scale));
                        int bx = sx + textW + gap;
                        int by = sy - Math.round((float) btnSize * 0.12f);

                        if (mx >= bx && mx <= bx + btnSize && my >= by && my <= by + btnSize) {
                            addon$invokeOpenResetPopup();
                            cir.setReturnValue(true);
                            return;
                        }
                    }

                    // DO NOT cancel here! This allows other UI elements like tabs to work.
                    // cir.setReturnValue(true);
                    // return;
                }
            }
        }
    }

    @Unique
    private boolean addon$isRectHit(Object rect, double mx, double my) {
        if (rect == null) {
            return false;
        }
        addon$initReflection();
        if (addon$rectXField == null) {
            try {
                Class<?> rectClass = rect.getClass();
                addon$rectXField = rectClass.getDeclaredField("x");
                addon$rectYField = rectClass.getDeclaredField("y");
                addon$rectWField = rectClass.getDeclaredField("w");
                addon$rectHField = rectClass.getDeclaredField("h");
                addon$rectXField.setAccessible(true);
                addon$rectYField.setAccessible(true);
                addon$rectWField.setAccessible(true);
                addon$rectHField.setAccessible(true);
            } catch (Exception e) {
                return false;
            }
        }

        try {
            int rx = addon$rectXField.getInt(rect);
            int ry = addon$rectYField.getInt(rect);
            int rw = addon$rectWField.getInt(rect);
            int rh = addon$rectHField.getInt(rect);
            return mx >= (double) rx && mx <= (double) (rx + rw) && my >= (double) ry && my <= (double) (ry + rh);
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private int addon$invokeFixedWidthPx(String text, float scale, TextRenderer font) {
        try {
            if (addon$drawStringMethod == null) {
                // Initialize if needed
                addon$invokeDrawString(null, font, "", 0, 0, 0, 0, false);
            }
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("fixedWidthPx", String.class, float.class,
                    TextRenderer.class);
            m.setAccessible(true);
            return (int) m.invoke(this, text, scale, font);
        } catch (Exception e) {
            return 0;
        }
    }

    @Unique
    private int addon$invokeLinePxH(float scale) {
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("linePxH", float.class);
            m.setAccessible(true);
            return (int) m.invoke(this, scale);
        } catch (Exception e) {
            return 8;
        }
    }

    @Unique
    private int addon$invokeGetMouseX() {
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("getMouseXpx");
            m.setAccessible(true);
            return (int) m.invoke(this);
        } catch (Exception e) {
            return 0;
        }
    }

    @Unique
    private int addon$invokeGetMouseY() {
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("getMouseYpx");
            m.setAccessible(true);
            return (int) m.invoke(this);
        } catch (Exception e) {
            return 0;
        }
    }

    @Unique
    private void addon$invokeOpenResetPopup() {
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("openResetPopup");
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception e) {
        }
    }

    @Unique
    private String addon$invokeTrOr(String key, String fallback, Object[] args) {
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("trOr", String.class, String.class,
                    Object[].class);
            m.setAccessible(true);
            return (String) m.invoke(null, key, fallback, args);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Unique
    private String addon$invokeSelectedType() {
        try {
            addon$initReflection();
            Object type = addon$selectedTypeMethod.invoke(this);
            return type != null ? type.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Unique
    private String[] addon$invokeCalcStatValues() {
        try {
            Class<?> clazz = this.getClass();
            while (clazz != null) {
                try {
                    java.lang.reflect.Method m = clazz.getDeclaredMethod("calcStatValues");
                    m.setAccessible(true);
                    return (String[]) m.invoke(this);
                } catch (NoSuchMethodException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
        }
        return new String[6];
    }

    @Unique
    private void addon$invokeSetActiveTooltipBtn(int val) {
        try {
            addon$initReflection();
            addon$activeTooltipBtnField.setInt(this, val);
        } catch (Exception e) {
        }
    }

    @Unique
    private int addon$invokeGetActiveTooltipBtn() {
        try {
            addon$initReflection();
            return addon$activeTooltipBtnField.getInt(this);
        } catch (Exception e) {
            return -1;
        }
    }
}
