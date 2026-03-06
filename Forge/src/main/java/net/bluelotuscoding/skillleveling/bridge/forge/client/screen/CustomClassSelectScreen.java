package net.bluelotuscoding.skillleveling.bridge.forge.client.screen;

import com.example.epicclassmod.client.ClientClassState;
import com.example.epicclassmod.client.ClientPacketHandlers;
import com.example.epicclassmod.data.ModSettings;
import com.example.epicclassmod.data.PlayerClassData;
import com.example.epicclassmod.data.PlayerClassData.ClassType;
import com.example.epicclassmod.network.ChooseClassPacket;
import com.example.epicclassmod.network.ModNetwork;
import com.example.epicclassmod.network.OpenDialoguePacket;
import net.bluelotuscoding.skillleveling.bridge.network.CustomChooseClassPacket;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.bluelotuscoding.skillleveling.util.PuffishItemHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Language;
import net.minecraft.text.Text;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Identifier;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Hand;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.RegistryKeys;
import org.joml.Quaternionf;
import net.puffish.skillsmod.api.SkillsAPI;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CustomClassSelectScreen extends Screen {

    private final List<Object> allChoices = new ArrayList<>();
    private int currentIndex = 0;
    private final String parentClassId;

    private static final List<String> ARMOR_SUFFIXES = List.of("helmet", "chestplate", "leggings", "boots");

    private static final int PAGE_W0 = 520;
    private static final int PAGE_H0 = 360;
    private static final float CONTENT_SCALE_BIAS = 0.75f;
    private float globalScale = 1.0f;
    private int drawX;
    private int drawY;
    private int drawW;
    private int drawH;
    private int cachedW = -1;
    private int cachedH = -1;
    private float cachedGuiScale = -1.0f;
    private int popupX = 0;
    private int popupY = 0;

    private static final int CLR_POPUP_BG = -266460621;
    private static final int CLR_POPUP_EDGE = 1089003960;
    private static final int CLR_TITLE = -2734779;
    private static final int CLR_TEXT = -1515080;
    private static final int CLR_SUBTEXT = -3620955;
    private static final int CLR_DIVIDER = 1087784261;
    private static final int CLR_CARD_BG = -2144592364;
    private static final int CLR_CARD_EDGE = 820568504;

    private static final Identifier ICON_HEART = new Identifier("epicclassmod", "textures/gui/icons/heart.png");
    private static final Identifier ICON_SHIELD = new Identifier("epicclassmod", "textures/gui/icons/shield.png");
    private static final Identifier ICON_SWORD = new Identifier("epicclassmod", "textures/gui/icons/sword.png");
    private static final Identifier ICON_SPEED = new Identifier("epicclassmod", "textures/gui/icons/boots.png");

    private boolean dragging = false;
    private double lastMouseX;
    private double lastMouseY;
    // Per-class model rotation: [yaw, pitch] indexed by allChoices index
    private final Map<Integer, float[]> modelRotations = new HashMap<>();

    /** Epic Fight animated model previewer — no real-player involvement. */
    private EpicFightPreviewComponent epicPreview;

    private float getModelYaw() {
        return modelRotations.computeIfAbsent(currentIndex, k -> defaultRotation())[0];
    }

    private float getModelPitch() {
        return modelRotations.computeIfAbsent(currentIndex, k -> defaultRotation())[1];
    }

    private void addModelYaw(float delta) {
        float[] rot = modelRotations.computeIfAbsent(currentIndex, k -> defaultRotation());
        rot[0] += delta;
    }

    private void addModelPitch(float delta) {
        float[] rot = modelRotations.computeIfAbsent(currentIndex, k -> defaultRotation());
        rot[1] = Math.max(-80.0f, Math.min(80.0f, rot[1] + delta));
    }

    private static float[] defaultRotation() {
        float[] r = new float[2];
        r[0] = 180.0f;
        r[1] = 0.0f;
        return r;
    }

    public CustomClassSelectScreen() {
        this(null);
    }

    public CustomClassSelectScreen(String parentClassId) {
        super(parentClassId == null ? Text.translatable("screen.epicclassmod.class_select")
                : Text.literal("Class Advancement"));
        this.parentClassId = parentClassId;

        if (parentClassId == null) {
            // Setup choices: Base classes first
            allChoices.add(PlayerClassData.ClassType.WARRIOR);
            allChoices.add(PlayerClassData.ClassType.PALADIN);
            allChoices.add(PlayerClassData.ClassType.BERSERKER);
            allChoices.add(PlayerClassData.ClassType.REAPER);
            allChoices.add(PlayerClassData.ClassType.SORCERER);
            allChoices.add(PlayerClassData.ClassType.ARCHER);

            // Then custom classes that are base level (no parent)
            for (EpicClassDef def : EpicClassConfigManager.getClasses().values()) {
                if (def.class_parent == null || def.class_parent.trim().isEmpty()) {
                    allChoices.add(def);
                }
            }
        } else {
            // Advanced Classes
            allChoices.addAll(EpicClassConfigManager.getChildClasses(parentClassId));
        }
    }

    private void prevClass() {
        currentIndex = (currentIndex - 1 + allChoices.size()) % allChoices.size();
        initEpicPreview();
    }

    private void nextClass() {
        currentIndex = (currentIndex + 1) % allChoices.size();
        initEpicPreview();
    }

    private int pageToScreenX(int pagePx) {
        return drawX + Math.round((float) pagePx * globalScale);
    }

    private int pageToScreenY(int pagePy) {
        return drawY + Math.round((float) pagePy * globalScale);
    }

    private int pageSizeToScreenW(int pageW) {
        return Math.round((float) pageW * globalScale);
    }

    private int pageSizeToScreenH(int pageH) {
        return Math.round((float) pageH * globalScale);
    }

    private int screenToPageY(int screenY) {
        return Math.round((float) (screenY - this.drawY) / this.globalScale);
    }

    private float guiScaleF() {
        return (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
    }

    private void computeLayout() {
        float fitW = (float) this.width / 520.0f;
        float fitH = (float) this.height / 360.0f;
        float sWindow = Math.min(fitW, fitH);
        float sGui = guiScaleF() / 4.0f;
        globalScale = sWindow * sGui * 0.75f;
        drawW = Math.round(520.0f * globalScale);
        drawH = Math.round(360.0f * globalScale);
        drawX = (this.width - drawW) / 2;
        drawY = (this.height - drawH) / 2;
    }

    private void rebuildButtons() {
        this.clearChildren();
        int arrowW0 = 22;
        int arrowH0 = 20;
        int btnW0 = 90;
        int btnH0 = 20;
        int leftArrowX = pageToScreenX(popupX + 12);
        int leftArrowY = pageToScreenY(popupY + 10);
        int rightArrowX = pageToScreenX(popupX + 520 - arrowW0 - 12);
        int rightArrowY = pageToScreenY(popupY + 10);
        int arrowW = pageSizeToScreenW(arrowW0);
        int arrowH = pageSizeToScreenH(arrowH0);
        int selectX = pageToScreenX(popupX + 520 - btnW0 - 14);
        int selectY = pageToScreenY(popupY + 360 - btnH0 - 10);
        int btnW = pageSizeToScreenW(btnW0);
        int btnH = pageSizeToScreenH(btnH0);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> prevClass())
                .dimensions(leftArrowX, leftArrowY, arrowW, arrowH).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> nextClass())
                .dimensions(rightArrowX, rightArrowY, arrowW, arrowH).build());
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("gui.epicclassmod.button.select"), b -> choose(allChoices.get(currentIndex)))
                .dimensions(selectX, selectY, btnW, btnH).build());
    }

    @Override
    protected void init() {
        computeLayout();
        rebuildButtons();
        initEpicPreview();
    }

    private void initEpicPreview() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            epicPreview = new EpicFightPreviewComponent(null, false, Armatures.BIPED.get().deepCopy());
        } else {
            boolean slimArms = false;
            try {
                com.mojang.authlib.properties.Property skinProp = player.getGameProfile().getProperties()
                        .get("textures").iterator().next();
                String decoded = new String(
                        java.util.Base64.getDecoder().decode(skinProp.getValue()));
                slimArms = decoded.contains("slim");
            } catch (Exception ignored) {
            }

            Identifier skin = player.getSkinTexture();
            epicPreview = new EpicFightPreviewComponent(skin, slimArms, Armatures.BIPED.get().deepCopy());
        }

        epicPreview.setLocked(true);
        refreshPreviewAnimation();
        applyPreviewEquipment();
    }

    @Override
    public void resize(MinecraftClient mc, int w, int h) {
        super.resize(mc, w, h);
        cachedW = -1;
        cachedH = -1;
        init();
    }

    @Override
    public void tick() {
        super.tick();
        if (epicPreview != null) {
            epicPreview.tick();
        }
    }

    /**
     * Apply the preview_animation (or default idle) for the current class choice.
     */
    private void refreshPreviewAnimation() {
        if (epicPreview == null) {
            return;
        }
        Object choice = allChoices.isEmpty() ? null : allChoices.get(currentIndex);
        if (choice instanceof EpicClassDef customDef) {
            if (customDef.preview_animation != null && !customDef.preview_animation.isEmpty()) {
                try {
                    Identifier animId = new Identifier(customDef.preview_animation);
                    AssetAccessor<? extends StaticAnimation> anim = AnimationManager
                            .byKey(animId);
                    if (anim != null) {
                        epicPreview.setAnimation(anim);
                    } else {
                        epicPreview.setAnimation((AssetAccessor<? extends StaticAnimation>) null);
                    }
                } catch (Exception e) {
                    epicPreview.setAnimation((AssetAccessor<? extends StaticAnimation>) null);
                }
            } else {
                epicPreview.setAnimation((AssetAccessor<? extends StaticAnimation>) null);
            }
        } else if (choice instanceof PlayerClassData.ClassType t) {
            // Provide default idle animations for base classes
            Identifier idleAnimId = switch (t) {
                case WARRIOR, PALADIN, BERSERKER -> new Identifier("epicfight:biped/living/idle");
                case REAPER -> new Identifier("epicfight:biped/living/idle"); // Could use a more specific one if
                                                                              // available
                case SORCERER -> new Identifier("epicfight:biped/living/idle");
                case ARCHER -> new Identifier("epicfight:biped/living/idle");
                default -> new Identifier("epicfight:biped/living/idle");
            };

            try {
                AssetAccessor<? extends StaticAnimation> anim = AnimationManager
                        .byKey(idleAnimId);
                epicPreview.setAnimation(anim);
            } catch (Exception e) {
                epicPreview.setAnimation(null);
            }
        } else {
            epicPreview.setAnimation((AssetAccessor<? extends StaticAnimation>) null);
        }
    }

    /** Set equipment items on the preview from the current class definition. */
    private void applyPreviewEquipment() {
        if (epicPreview == null) {
            return;
        }
        Object choice = allChoices.isEmpty() ? null : allChoices.get(currentIndex);

        ItemStack head = ItemStack.EMPTY;
        ItemStack chest = ItemStack.EMPTY;
        ItemStack legs = ItemStack.EMPTY;
        ItemStack feet = ItemStack.EMPTY;
        ItemStack mainhand = ItemStack.EMPTY;
        ItemStack offhand = ItemStack.EMPTY;

        if (choice instanceof EpicClassDef def) {
            String base = def.preview_armor_base;
            if (base != null && !base.isEmpty()) {
                // Suffixes added: helmet, chestplate, leggings, boots
                head = modArmor(base, EquipmentSlot.HEAD, ItemStack.EMPTY);
                chest = modArmor(base, EquipmentSlot.CHEST, ItemStack.EMPTY);
                legs = modArmor(base, EquipmentSlot.LEGS, ItemStack.EMPTY);
                feet = modArmor(base, EquipmentSlot.FEET, ItemStack.EMPTY);
            }
            mainhand = parseItemStack(def.preview_mainhand_item, null);
            offhand = parseItemStack(def.preview_offhand_item, null);
        } else if (choice instanceof PlayerClassData.ClassType t) {
            String set = "";
            switch (t) {
                case WARRIOR:
                    set = "dungeons_and_combat:oni_slayer_";
                    mainhand = modItem("efn:yamato_dmc4_in_sheath", new ItemStack(Items.IRON_SWORD));
                    break;
                case PALADIN:
                    set = "dungeons_and_combat:silver_";
                    mainhand = modItem("dungeons_and_combat:cobalt_long_sword", new ItemStack(Items.IRON_SWORD));
                    break;
                case BERSERKER:
                    set = "dungeons_and_combat:crimson_";
                    mainhand = modItem("efn:ruinsgreatsword", new ItemStack(Items.IRON_SWORD));
                    break;
                case REAPER:
                    set = "dungeons_and_combat:rogue_";
                    mainhand = modItem("efn:nf_dual_sword", new ItemStack(Items.IRON_SWORD));
                    offhand = modItem("efn:nf_dual_sword", new ItemStack(Items.IRON_SWORD)); // dual wield
                    break;
                case SORCERER:
                    set = "irons_spellbooks:netherite_mage_";
                    mainhand = modItem("dungeons_and_combat:fairy_scepter", new ItemStack(Items.STICK));
                    break;
                case ARCHER:
                    set = "dungeons_and_combat:forgotten_knight_";
                    mainhand = modItem("cataclysm:wrath_of_the_desert", new ItemStack(Items.BOW));
                    break;
                default:
                    break;
            }
            if (!set.isEmpty()) {
                head = modArmor(set, EquipmentSlot.HEAD, ItemStack.EMPTY);
                chest = modArmor(set, EquipmentSlot.CHEST, ItemStack.EMPTY);
                legs = modArmor(set, EquipmentSlot.LEGS, ItemStack.EMPTY);
                feet = modArmor(set, EquipmentSlot.FEET, ItemStack.EMPTY);
            }
        }

        epicPreview.setEquipment(head, chest, legs, feet, mainhand, offhand);
    }

    /**
     * Parse an item ID string (e.g. "minecraft:iron_sword") into an ItemStack.
     * {@code slot} is unused for now but kept for future armor-piece-picking logic.
     */
    private ItemStack parseItemStack(@Nullable String id, @Nullable EquipmentSlot slot) {
        if (id == null || id.isBlank()) {
            return ItemStack.EMPTY;
        }
        return PuffishItemHelper.parseItemStack(id);
    }

    private void choose(Object choice) {
        if (choice instanceof PlayerClassData.ClassType t) {
            ClientClassState.selectedType = t;
            ModNetwork.CHANNEL.sendToServer(new ChooseClassPacket(t));
        } else if (choice instanceof EpicClassDef customDef) {
            // Use proxy type for local visual state instead of hardcoded WARRIOR
            PlayerClassData.ClassType proxyT = PlayerClassData.ClassType.WARRIOR;
            if (customDef.epic_class_proxy != null) {
                try {
                    proxyT = PlayerClassData.ClassType.valueOf(customDef.epic_class_proxy.toUpperCase());
                } catch (Exception ignored) {
                }
            }
            ClientClassState.selectedType = proxyT;

            net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL
                    .sendToServer(new CustomChooseClassPacket(customDef.class_name));
        }

        MinecraftClient.getInstance().setScreen(null);
        if (this.parentClassId == null) {
            OpenDialoguePacket fake = new OpenDialoguePacket(-1, "main__gui.epicclassmod.quest.main.1", false);
            ClientPacketHandlers.scheduleMainDialogue(fake, 2500L);
        }
    }

    private void drawStringAtPx(DrawContext g, TextRenderer font, String text, int xPx, int yPx, int color,
            float pixelHeight, boolean shadow) {
        Objects.requireNonNull(font);
        float basePx = Math.max(1.0f, 9.0f);
        float s = pixelHeight > 0.0f ? pixelHeight / basePx : 1.0f;
        g.getMatrices().push();
        if (s != 1.0f) {
            g.getMatrices().scale(s, s, 1.0f);
            xPx = Math.round((float) xPx / s);
            yPx = Math.round((float) yPx / s);
        }
        g.drawText(font, text, xPx, yPx, color, shadow);
        g.getMatrices().pop();
    }

    private int linePxH(float localScale) {
        return Math.round(9.0f * (globalScale * localScale));
    }

    private void drawStringPxBase(DrawContext g, TextRenderer font, String text, int xPx, int yPx, int color,
            float localScale, boolean shadow) {
        float pixelH = 9.0f * (globalScale * localScale);
        drawStringAtPx(g, font, text, xPx, yPx, color, pixelH, shadow);
    }

    private void drawSeqPxBase(DrawContext g, TextRenderer font, OrderedText seq, int xPx, int yPx, int color,
            float localScale, boolean shadow) {
        float pixelH = 9.0f * (globalScale * localScale);
        float basePx = Math.max(1.0f, 9.0f);
        float s = pixelH > 0.0f ? pixelH / basePx : 1.0f;
        g.getMatrices().push();
        if (s != 1.0f) {
            g.getMatrices().scale(s, s, 1.0f);
            xPx = Math.round((float) xPx / s);
            yPx = Math.round((float) yPx / s);
        }
        g.drawText(font, seq, xPx, yPx, color, shadow);
        g.getMatrices().pop();
    }

    private int drawWrappedScaled(DrawContext g, String text, int xPx, int yPx, int wPx, int color, float textScale) {
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(text),
                Math.max(1, Math.round((float) wPx / this.globalScale)));
        int lh = this.linePxH(textScale);
        int y = yPx;
        for (OrderedText seq : lines) {
            this.drawSeqPxBase(g, this.textRenderer, seq, xPx, y, color, textScale, false);
            y += lh;
        }
        return y;
    }

    private void drawRepeatedIcons(DrawContext g, Identifier icon, int x, int y, int count, int size, int gap) {
        for (int i = 0; i < count; ++i) {
            int sx = pageSizeToScreenW(size);
            int gx = pageSizeToScreenW(gap);
            g.drawTexture(icon, x + i * (sx + gx), y, 0.0f, 0.0f, sx, sx, sx, sx);
        }
    }

    private void renderItemScaled(DrawContext g, ItemStack stack, int x, int y, int targetPx) {
        float s = (float) targetPx / 16.0f;
        g.getMatrices().push();
        g.getMatrices().translate((float) x, (float) y, 0.0f);
        g.getMatrices().scale(s, s, 1.0f);
        g.drawItem(stack, 0, 0);
        g.getMatrices().pop();
    }

    @Override
    public void render(DrawContext g, int mx, int my, float pt) {
        float nowGui = guiScaleF();
        if (nowGui != cachedGuiScale || width != cachedW || height != cachedH) {
            cachedGuiScale = nowGui;
            cachedW = width;
            cachedH = height;
            computeLayout();
            rebuildButtons();
        }
        this.renderBackground(g);
        int px = pageToScreenX(popupX);
        int py = pageToScreenY(popupY);
        int pw = pageSizeToScreenW(520);
        int ph = pageSizeToScreenH(360);

        g.fill(px, py, px + pw, py + ph, CLR_POPUP_BG);
        g.fill(px, py, px + pw, py + 1, CLR_POPUP_EDGE);
        g.fill(px, py + ph - 1, px + pw, py + ph, CLR_POPUP_EDGE);
        g.fill(px, py, px + 1, py + ph, CLR_POPUP_EDGE);
        g.fill(px + pw - 1, py, px + pw, py + ph, CLR_POPUP_EDGE);

        super.render(g, mx, my, pt);

        drawStringPxBase(g, textRenderer, getDynamicTitle(),
                pageToScreenX(popupX + 260) - pageSizeToScreenW(60), pageToScreenY(popupY + 12), CLR_TITLE, 1.1f,
                false);

        int leftW0 = 210;
        int rightX0 = popupX + leftW0 + 12;
        int rightW0 = 520 - (rightX0 - popupX) - 12;

        g.fill(pageToScreenX(rightX0 - 6), pageToScreenY(popupY + 36), pageToScreenX(rightX0 - 5),
                pageToScreenY(popupY + 360 - 16), CLR_DIVIDER);

        int modelAreaX0 = popupX + 30;
        int modelAreaY0 = popupY + 70;
        int modelAreaW0 = leftW0 - 57;
        int modelAreaH0 = 240;

        int mX = pageToScreenX(modelAreaX0);
        int mY = pageToScreenY(modelAreaY0);
        int mW = pageSizeToScreenW(modelAreaW0);
        int mH = pageSizeToScreenH(modelAreaH0);

        g.fill(mX, mY, mX + mW, mY + mH, CLR_CARD_BG);
        g.fill(mX, mY, mX + mW, mY + 1, CLR_CARD_EDGE);
        g.fill(mX, mY + mH - 1, mX + mW, mY + mH, CLR_CARD_EDGE);
        g.fill(mX, mY, mX + 1, mY + mH, CLR_CARD_EDGE);
        g.fill(mX + mW - 1, mY, mX + mW, mY + mH, CLR_CARD_EDGE);

        Object choice = allChoices.get(currentIndex);

        // Use EpicFightPreviewComponent instead of rendering the real player.
        if (epicPreview != null) {
            try {
                // Updated scale factor to 0.58 as requested
                int modelSize = pageSizeToScreenH((int) (PAGE_H0 * 0.58));
                int modelX = mX + mW / 2;
                int modelY = mY + mH - pageSizeToScreenH(20);

                // EpicFightPreviewComponent.render() takes DrawContext — pass g directly.
                epicPreview.render(g, modelX, modelY, modelSize,
                        getModelYaw(), getModelPitch(), pt);
            } catch (Throwable t) {
                // Failsafe so the UI doesn't break if model rendering crashes
            }
        }

        String jobName = getJobName(choice);
        drawStringPxBase(g, textRenderer, jobName, pageToScreenX(rightX0), pageToScreenY(popupY + 44), -1, 1.0f, false);

        int statY0 = popupY + 44 + Math.round(9.0f);
        int statY = pageToScreenY(statY0 + 2);

        for (StatEntry s : statsFor(choice)) {
            drawStringPxBase(g, textRenderer, tr(s.labelKey), pageToScreenX(rightX0), statY, CLR_TEXT, 1.0f, false);
            int iconStartX = pageToScreenX(rightX0 + 36);
            if ("number".equals(s.statType)) {
                // Numeric display for mana, damage, etc.
                String numText = s.count + (s.unit != null && !s.unit.isEmpty() ? " " + s.unit : "");
                drawStringPxBase(g, textRenderer, numText, iconStartX, statY, CLR_TEXT, 1.0f, false);
            } else {
                // Hearts/icon mode — cap at 10 to prevent overflow
                int cappedCount = Math.min(s.count, 10);
                drawRepeatedIcons(g, s.icon, iconStartX, statY - pageSizeToScreenH(2), cappedCount, s.size, 2);
            }
            statY += linePxH(1.0f) + pageSizeToScreenH(4);
        }

        String desc = getJobDesc(choice);
        int descX = pageToScreenX(rightX0);
        int descY = statY + pageSizeToScreenH(4);
        int descW = pageSizeToScreenW(rightW0 - 8);
        int nextY = drawWrappedScaled(g, desc, descX, descY, descW, CLR_SUBTEXT, 0.95f);

        int siTitleY = nextY + pageSizeToScreenH(8);
        drawStringPxBase(g, textRenderer, trOr("gui.epicclassmod.starting_items", "Starting Items"),
                pageToScreenX(rightX0), siTitleY, CLR_TEXT, 1.0f, false);

        List<ItemStack> items = startingItems(choice);
        int iconX = pageToScreenX(rightX0);
        int iconY = siTitleY + pageSizeToScreenH(14);
        int slot = 0;
        int START_ITEM_PX0 = 18;
        int startItemPx = pageSizeToScreenW(START_ITEM_PX0);
        int iconGapPx = pageSizeToScreenW(6);

        if (items.isEmpty()) {
            drawStringPxBase(g, textRenderer, trOr("gui.epicclassmod.starting_items.none", "-"), iconX,
                    iconY + pageSizeToScreenH(4), -5592406, 1.0f, false);
        } else {
            for (ItemStack st : items) {
                int ix = iconX + slot * (startItemPx + iconGapPx);
                renderItemScaled(g, st, ix, iconY, startItemPx);
                ++slot;
            }
        }

        int gridX0 = rightX0;
        int iconBottomPage = screenToPageY(iconY) + 16 + 6;
        int gridY0 = Math.max(popupY + 170, iconBottomPage);
        int gridW0 = rightW0;
        int gridH0 = 150;

        drawStringPxBase(g, textRenderer, trOr("gui.epicclassmod.passives", "Passives"), pageToScreenX(gridX0),
                pageToScreenY(gridY0 - 12), CLR_TITLE, 1.0f, false);

        int cols = 2;
        int spacing0 = 8;
        int cardW0 = (gridW0 - spacing0) / cols;
        int cardH0 = (gridH0 - spacing0) / 2;
        List<PassiveEntry> pv = passivesFor(choice);

        for (int i = 0; i < pv.size(); ++i) {
            int col = i % cols;
            int row = i / cols;
            int cx0 = gridX0 + col * (cardW0 + spacing0);
            int cy0 = gridY0 + row * (cardH0 + spacing0);
            int cx = pageToScreenX(cx0);
            int cy = pageToScreenY(cy0);
            int cw = pageSizeToScreenW(cardW0);
            int ch = pageSizeToScreenH(cardH0);

            g.fill(cx, cy, cx + cw, cy + ch, CLR_CARD_BG);
            g.fill(cx, cy, cx + cw, cy + 1, CLR_CARD_EDGE);
            g.fill(cx, cy + ch - 1, cx + cw, cy + ch, CLR_CARD_EDGE);
            g.fill(cx, cy, cx + 1, cy + ch, CLR_CARD_EDGE);
            g.fill(cx + cw - 1, cy, cx + cw, cy + ch, CLR_CARD_EDGE);

            PassiveEntry p = pv.get(i);
            int iconSize0 = 12;
            int ix = cx + pageSizeToScreenW(8);
            int iy = cy + pageSizeToScreenH(6);
            int isz = pageSizeToScreenW(iconSize0);
            g.drawTexture(p.icon, ix, iy, 0.0f, 0.0f, isz, isz, isz, isz);

            drawStringPxBase(g, textRenderer, trOr(p.nameKey, p.nameKey), ix + isz + pageSizeToScreenW(6),
                    iy + pageSizeToScreenH(1), CLR_TEXT, 1.0f, false);
            int textX = cx + pageSizeToScreenW(8);
            int textY = iy + isz + pageSizeToScreenH(4);
            int textW = cw - pageSizeToScreenW(16);
            drawWrappedScaled(g, trOr(p.descKey, p.descKey), textX, textY, textW, CLR_SUBTEXT, 0.95f);
        }
    }

    private void applyBasePreview(PlayerEntity player, PlayerClassData.ClassType t) {
        switch (t) {
            case WARRIOR:
                equipArmorSet(player, "dungeons_and_combat:oni_slayer_");
                player.setStackInHand(Hand.MAIN_HAND,
                        modItem("efn:yamato_dmc4_in_sheath", new ItemStack(Items.IRON_SWORD)));
                player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                break;
            case PALADIN:
                equipArmorSet(player, "dungeons_and_combat:silver_");
                player.setStackInHand(Hand.MAIN_HAND,
                        modItem("dungeons_and_combat:cobalt_long_sword", new ItemStack(Items.IRON_SWORD)));
                player.setStackInHand(Hand.OFF_HAND,
                        modItem("dungeons_and_combat:silver_shield", new ItemStack(Items.SHIELD)));
                break;
            case BERSERKER:
                equipArmorSet(player, "dungeons_and_combat:crimson_helmet");
                player.setStackInHand(Hand.MAIN_HAND, modItem("efn:ruinsgreatsword", new ItemStack(Items.IRON_SWORD)));
                player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                break;
            case REAPER:
                equipArmorSet(player, "dungeons_and_combat:rogue_helmet");
                player.setStackInHand(Hand.MAIN_HAND, modItem("efn:nf_dual_sword", new ItemStack(Items.IRON_SWORD)));
                player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                break;
            case SORCERER:
                player.setStackInHand(Hand.MAIN_HAND,
                        modItem("dungeons_and_combat:fairy_scepter", new ItemStack(Items.STICK)));
                player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                player.equipStack(EquipmentSlot.HEAD,
                        modItem("irons_spellbooks:netherite_mage_helmet", new ItemStack(Items.NETHERITE_HELMET)));
                player.equipStack(EquipmentSlot.CHEST, modItem("irons_spellbooks:netherite_mage_chestplate",
                        new ItemStack(Items.NETHERITE_CHESTPLATE)));
                player.equipStack(EquipmentSlot.LEGS,
                        modItem("irons_spellbooks:netherite_mage_leggings", new ItemStack(Items.NETHERITE_LEGGINGS)));
                player.equipStack(EquipmentSlot.FEET,
                        modItem("irons_spellbooks:netherite_mage_boots", new ItemStack(Items.NETHERITE_BOOTS)));
                break;
            case ARCHER:
                equipArmorSet(player, "dungeons_and_combat:forgotten_knight_");
                player.setStackInHand(Hand.MAIN_HAND,
                        modItem("cataclysm:wrath_of_the_desert", new ItemStack(Items.BOW)));
                player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                break;
            default:
                break;
        }
    }

    private void applyCustomPreview(PlayerEntity player, EpicClassDef def) {
        if (def.preview_armor_base != null) {
            equipArmorSet(player, def.preview_armor_base);
        } else {
            player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            player.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
            player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
        }

        if (def.preview_mainhand_item != null) {
            player.setStackInHand(Hand.MAIN_HAND, modItem(def.preview_mainhand_item, ItemStack.EMPTY));
        } else {
            player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        }

        if (def.preview_offhand_item != null) {
            player.setStackInHand(Hand.OFF_HAND, modItem(def.preview_offhand_item, ItemStack.EMPTY));
        } else {
            player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private String getDynamicTitle() {
        if (this.parentClassId == null || this.parentClassId.isEmpty()
                || this.parentClassId.equals("epic_classes:none")) {
            return tr("gui.epicclassmod.class_select.title");
        }

        int depth = 1;
        EpicClassDef def = EpicClassConfigManager.getClassDef(this.parentClassId);
        while (def != null && def.class_parent != null && !def.class_parent.isEmpty()) {
            depth++;
            def = EpicClassConfigManager.getClassDef(def.class_parent);
        }

        int targetTier = depth + 1;
        String suffix = "th";
        if (targetTier % 10 == 1 && targetTier % 100 != 11) {
            suffix = "st";
        } else if (targetTier % 10 == 2 && targetTier % 100 != 12) {
            suffix = "nd";
        } else if (targetTier % 10 == 3 && targetTier % 100 != 13) {
            suffix = "rd";
        }

        return "Choose your " + targetTier + suffix + " class";
    }

    private String getJobName(Object choice) {
        if (choice instanceof PlayerClassData.ClassType t) {
            return trOr(titleKey(t), t.name());
        } else if (choice instanceof EpicClassDef customDef) {
            // Priority: raw display_name field > translation key > class_name as fallback
            if (customDef.display_name != null && !customDef.display_name.isBlank()) {
                return customDef.display_name;
            }
            if (customDef.display_name_key != null && !customDef.display_name_key.isBlank()) {
                return trOr(customDef.display_name_key, customDef.class_name);
            }
            // Strip namespace prefix for a cleaner display (e.g. "epic_classes:necromancer"
            // -> "necromancer")
            String name = customDef.class_name;
            if (name != null && name.contains(":")) {
                name = name.substring(name.indexOf(':') + 1);
            }
            return name != null ? name : "Unknown";
        }
        return "Unknown";
    }

    private String getJobDesc(Object choice) {
        if (choice instanceof PlayerClassData.ClassType t) {
            return tr(descKey(t));
        } else if (choice instanceof EpicClassDef customDef) {
            if (customDef.gui_description != null) {
                return trOr(customDef.gui_description, customDef.gui_description);
            }
            return trOr(customDef.lore_key, "");
        }
        return "";
    }

    private List<StatEntry> statsFor(Object choice) {
        if (choice instanceof PlayerClassData.ClassType t) {
            int hearts;
            if (ModSettings.isClassStartHpEnabled()) {
                hearts = Math.max(1, (int) Math.round(ModSettings.getStartingMaxHealth(t) / 2.0));
            } else {
                ClientPlayerEntity p = MinecraftClient.getInstance().player;
                float max = p != null ? p.getMaxHealth() : 20.0f;
                hearts = Math.max(1, (int) Math.ceil((double) max / 2.0));
            }
            return List.of(new StatEntry("class.epicclassmod.stat.health", ICON_HEART, hearts, 13));
        } else if (choice instanceof EpicClassDef customDef) {
            if (customDef.gui_stats != null) {
                List<StatEntry> res = new ArrayList<>();
                for (EpicClassDef.StatDef s : customDef.gui_stats) {
                    Identifier icon = new Identifier(
                            s.icon != null ? s.icon : "epicclassmod:textures/gui/icons/heart.png");
                    res.add(new StatEntry(s.label_key, icon, s.count, 13, s.stat_type, s.unit));
                }
                return res;
            }
        }
        return Collections.emptyList();
    }

    private List<PassiveEntry> passivesFor(Object choice) {
        if (choice instanceof PlayerClassData.ClassType t) {
            switch (t) {
                case WARRIOR:
                    return List.of(
                            new PassiveEntry("class.epicclassmod.warrior.effect1",
                                    "class.epicclassmod.warrior.effect1.desc", ICON_HEART),
                            new PassiveEntry("class.epicclassmod.warrior.effect2",
                                    "class.epicclassmod.warrior.effect2.desc", ICON_SWORD),
                            new PassiveEntry("class.epicclassmod.warrior.effect3",
                                    "class.epicclassmod.warrior.effect3.desc", ICON_SPEED),
                            new PassiveEntry("class.epicclassmod.warrior.effect4",
                                    "class.epicclassmod.warrior.effect4.desc", ICON_SHIELD));
                case PALADIN:
                    return List.of(
                            new PassiveEntry("class.epicclassmod.paladin.effect1",
                                    "class.epicclassmod.paladin.effect1.desc", ICON_SHIELD),
                            new PassiveEntry("class.epicclassmod.paladin.effect2",
                                    "class.epicclassmod.paladin.effect2.desc", ICON_HEART),
                            new PassiveEntry("class.epicclassmod.paladin.effect3",
                                    "class.epicclassmod.paladin.effect3.desc", ICON_SHIELD),
                            new PassiveEntry("class.epicclassmod.paladin.effect4",
                                    "class.epicclassmod.paladin.effect4.desc", ICON_SWORD));
                case BERSERKER:
                    return List.of(
                            new PassiveEntry("class.epicclassmod.berserker.effect1",
                                    "class.epicclassmod.berserker.effect1.desc", ICON_SWORD),
                            new PassiveEntry("class.epicclassmod.berserker.effect2",
                                    "class.epicclassmod.berserker.effect2.desc", ICON_SHIELD),
                            new PassiveEntry("class.epicclassmod.berserker.effect3",
                                    "class.epicclassmod.berserker.effect3.desc", ICON_SPEED),
                            new PassiveEntry("class.epicclassmod.berserker.effect4",
                                    "class.epicclassmod.berserker.effect4.desc", ICON_HEART));
                case REAPER:
                    return List.of(
                            new PassiveEntry("class.epicclassmod.reaper.effect1",
                                    "class.epicclassmod.reaper.effect1.desc", ICON_SWORD),
                            new PassiveEntry("class.epicclassmod.reaper.effect2",
                                    "class.epicclassmod.reaper.effect2.desc", ICON_SPEED),
                            new PassiveEntry("class.epicclassmod.reaper.effect3",
                                    "class.epicclassmod.reaper.effect3.desc", ICON_HEART),
                            new PassiveEntry("class.epicclassmod.reaper.effect4",
                                    "class.epicclassmod.reaper.effect4.desc", ICON_SHIELD));
                case SORCERER:
                    return List.of(
                            new PassiveEntry("class.epicclassmod.sorcerer.effect1",
                                    "class.epicclassmod.sorcerer.effect1.desc", ICON_SWORD),
                            new PassiveEntry("class.epicclassmod.sorcerer.effect2",
                                    "class.epicclassmod.sorcerer.effect2.desc", ICON_HEART),
                            new PassiveEntry("class.epicclassmod.sorcerer.effect3",
                                    "class.epicclassmod.sorcerer.effect3.desc", ICON_SPEED),
                            new PassiveEntry("class.epicclassmod.sorcerer.effect4",
                                    "class.epicclassmod.sorcerer.effect4.desc", ICON_SHIELD));
                case ARCHER:
                    return List.of(
                            new PassiveEntry("class.epicclassmod.archer.effect1",
                                    "class.epicclassmod.archer.effect1.desc", ICON_SPEED),
                            new PassiveEntry("class.epicclassmod.archer.effect2",
                                    "class.epicclassmod.archer.effect2.desc", ICON_SWORD),
                            new PassiveEntry("class.epicclassmod.archer.effect3",
                                    "class.epicclassmod.archer.effect3.desc", ICON_HEART),
                            new PassiveEntry("class.epicclassmod.archer.effect4",
                                    "class.epicclassmod.archer.effect4.desc", ICON_SHIELD));
                default:
                    break;
            }
        } else if (choice instanceof EpicClassDef customDef) {
            if (customDef.gui_passives != null) {
                List<PassiveEntry> res = new ArrayList<>();
                for (EpicClassDef.PassiveUIDef p : customDef.gui_passives) {
                    Identifier iconId = new Identifier(
                            p.icon != null ? p.icon : "epicclassmod:textures/gui/icons/heart.png");

                    // Use name_key/desc_key as fallbacks; pufferfish_skill_id is the preferred
                    // source
                    String nameStr = p.name_key != null ? p.name_key
                            : (p.pufferfish_skill_id != null
                                    ? p.pufferfish_skill_id.replace("_", " ")
                                    : "Unknown Skill");
                    String descStr = p.desc_key != null ? p.desc_key : "";

                    // Fetch title/description from server-side parsed definitions.json cache
                    if (p.pufferfish_skill_id != null && !p.pufferfish_skill_id.isBlank()
                            && customDef.skill_category_id != null) {
                        String[] display = net.bluelotuscoding.skillleveling.bridge.EpicClassBridge
                                .getSkillDisplay(customDef.skill_category_id, p.pufferfish_skill_id);
                        if (display != null) {
                            nameStr = display[0];
                            descStr = display[1];
                        }
                    }

                    res.add(new PassiveEntry(nameStr, descStr, iconId));
                }
                return res;
            }
        }
        return Collections.emptyList();
    }

    private List<ItemStack> startingItems(Object choice) {
        if (choice instanceof PlayerClassData.ClassType t) {
            ArrayList<ItemStack> list = new ArrayList<>();
            switch (t) {
                case WARRIOR:
                    list.add(modItem("epicfight:stone_tachi", new ItemStack(Items.STONE_SWORD)));
                    break;
                case PALADIN:
                    list.add(new ItemStack(Items.STONE_SWORD));
                    list.add(new ItemStack(Items.SHIELD));
                    break;
                case BERSERKER:
                    list.add(modItem("epicfight:stone_greatsword", new ItemStack(Items.STONE_SWORD)));
                    break;
                case REAPER:
                    list.add(modItem("epicfight:stone_dagger", new ItemStack(Items.STONE_SWORD)));
                    list.add(modItem("epicfight:stone_dagger", new ItemStack(Items.STONE_SWORD)));
                    break;
                case SORCERER:
                    list.add(modItem("irons_spellbooks:iron_spell_book", new ItemStack(Items.BOOK)));
                    list.add(new ItemStack(Items.STICK));
                    list.add(new ItemStack(Items.STICK));
                    list.add(modItem("irons_spellbooks:graybeard_staff", new ItemStack(Items.STICK)));
                    break;
                case ARCHER:
                    list.add(new ItemStack(Items.BOW));
                    list.add(new ItemStack(Items.ARROW, 32));
                    break;
                default:
                    break;
            }
            list.add(new ItemStack(Items.BREAD, 10));
            list.add(new ItemStack(Items.TORCH, 3));
            return list;
        } else if (choice instanceof EpicClassDef customDef) {
            ArrayList<ItemStack> list = new ArrayList<>();
            if (customDef.starting_items != null) {
                for (String itemId : customDef.starting_items) {
                    // Extract count if format is "mod:item@count"
                    int count = 1;
                    String id = itemId;
                    if (itemId.contains("@")) {
                        String[] parts = itemId.split("@");
                        id = parts[0];
                        try {
                            count = Integer.parseInt(parts[1]);
                        } catch (Exception ignored) {
                        }
                    }
                    ItemStack stack = modItem(id, ItemStack.EMPTY);
                    if (!stack.isEmpty()) {
                        stack.setCount(count);
                        list.add(stack);
                    }
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    private static String trOr(String key, String fallback, Object... args) {
        Language lang = Language.getInstance();
        return lang.hasTranslation(key) ? Text.translatable(key, args).getString() : fallback;
    }

    private String titleKey(PlayerClassData.ClassType t) {
        return switch (t) {
            case WARRIOR -> "class.epicclassmod.warrior.title";
            case PALADIN -> "class.epicclassmod.paladin.title";
            case BERSERKER -> "class.epicclassmod.berserker.title";
            case REAPER -> "class.epicclassmod.reaper.title";
            case SORCERER -> "class.epicclassmod.sorcerer.title";
            case ARCHER -> "class.epicclassmod.archer.title";
            default -> t.name();
        };
    }

    private String descKey(PlayerClassData.ClassType t) {
        return switch (t) {
            case WARRIOR -> "class.epicclassmod.warrior.select_desc";
            case PALADIN -> "class.epicclassmod.paladin.select_desc";
            case BERSERKER -> "class.epicclassmod.berserker.select_desc";
            case REAPER -> "class.epicclassmod.reaper.select_desc";
            case SORCERER -> "class.epicclassmod.sorcerer.select_desc";
            case ARCHER -> "class.epicclassmod.archer.select_desc";
            default -> t.name();
        };
    }

    private static ItemStack modItem(String id, ItemStack fallback) {
        ItemStack stack = PuffishItemHelper.parseItemStack(id);
        return stack.isEmpty() ? fallback : stack;
    }

    private static String normalizeArmorBase(String idOrBase) {
        if (idOrBase == null || idOrBase.isEmpty()) {
            return "";
        }
        String s = idOrBase;
        for (String suf : ARMOR_SUFFIXES) {
            if (s.endsWith("_" + suf)) {
                s = s.substring(0, s.length() - suf.length());
                break;
            }
        }
        if (!s.endsWith("_")) {
            s += "_";
        }
        return s;
    }

    private static ItemStack modArmor(String idOrBase, EquipmentSlot slot, ItemStack fallback) {
        String base = normalizeArmorBase(idOrBase);
        String standardSuf = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            default -> "helmet";
        };

        // 1. Try standard suffix first
        ItemStack stack = modItem(base + standardSuf, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            return stack;
        }

        // 2. Try tag-based Search (more robust for Hoods/Robes etc)
        // We look for any item starting with 'base' that matches the slot
        for (Item item : ForgeRegistries.ITEMS) {
            Identifier id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null && id.toString().startsWith(base)) {
                if (item instanceof ArmorItem armor && armor.getSlotType() == slot) {
                    return new ItemStack(item);
                }
            }
        }

        if (fallback.isEmpty()) {
            net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                    .warn("[Preview] FAILED to resolve armor " + slot + " for " + idOrBase);
        }

        return fallback;
    }

    private static void equipArmorSet(PlayerEntity player, String idOrBase) {
        player.equipStack(EquipmentSlot.HEAD, modArmor(idOrBase, EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)));
        player.equipStack(EquipmentSlot.CHEST,
                modArmor(idOrBase, EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE)));
        player.equipStack(EquipmentSlot.LEGS,
                modArmor(idOrBase, EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS)));
        player.equipStack(EquipmentSlot.FEET, modArmor(idOrBase, EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int px = pageToScreenX(popupX);
        int py = pageToScreenY(popupY);
        int pw = pageSizeToScreenW(520);
        int ph = pageSizeToScreenH(360);
        if (button == 0 && mouseX >= px && mouseX <= px + pw && mouseY >= py && mouseY <= py + ph) {
            dragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            if (epicPreview == null || !epicPreview.isLocked()) {
                addModelYaw((float) (mouseX - lastMouseX));
                addModelPitch(-(float) (mouseY - lastMouseY));
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    // For StatEntry & PassiveEntry helpers
    private static class StatEntry {
        final String labelKey;
        final Identifier icon;
        final int count;
        final int size;
        /** null or "hearts" = icon mode; "number" = numeric text display */
        final String statType;
        /** Unit suffix for number mode, e.g. "HP", "Mana", "DMG" */
        final String unit;

        StatEntry(String k, Identifier i, int c, int s) {
            this(k, i, c, s, null, null);
        }

        StatEntry(String k, Identifier i, int c, int s, String statType, String unit) {
            labelKey = k;
            icon = i;
            count = c;
            size = s;
            this.statType = statType;
            this.unit = unit;
        }
    }

    private static class PassiveEntry {
        final String nameKey;
        final String descKey;
        final Identifier icon;

        PassiveEntry(String n, String d, Identifier i) {
            nameKey = n;
            descKey = d;
            icon = i;
        }
    }
}
