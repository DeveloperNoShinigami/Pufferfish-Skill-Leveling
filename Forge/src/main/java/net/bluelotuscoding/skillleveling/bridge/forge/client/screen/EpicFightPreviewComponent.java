package net.bluelotuscoding.skillleveling.bridge.forge.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.DyeableArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.joml.Matrix4f;
import net.minecraft.util.Hand;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraft.client.model.Model;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.transformer.HumanoidModelBaker;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.MathUtils;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.List;

/**
 * Renders an Epic Fight skinned model (FakeEntityPatch + SimpleAnimator)
 * in a GUI panel without touching the real player entity.
 *
 * Supports:
 * - Player skin texture (slim / wide)
 * - Animated poses via {@link #setAnimation}
 * - Equipment: armor mesh overlays and mainhand item via Tool_R joint
 */
public class EpicFightPreviewComponent {

    // ------------------------------------------------------------------ fields
    private final FakeEntityPatch entityPatch;
    private final ClientAnimator animator;
    private final Armature armature;
    private final boolean isSlim;
    private final BipedEntityModel<?> innerArmorModel;
    private final BipedEntityModel<?> outerArmorModel;

    @Nullable
    private Identifier skinTexture;

    // Equipment
    private ItemStack headStack = ItemStack.EMPTY;
    private ItemStack chestStack = ItemStack.EMPTY;
    private ItemStack legsStack = ItemStack.EMPTY;
    private ItemStack feetStack = ItemStack.EMPTY;
    private ItemStack mainhandStack = ItemStack.EMPTY;
    private ItemStack offhandStack = ItemStack.EMPTY;
    private boolean locked = false;

    // ------------------------------------------------------------------ ctor
    /**
     * @param skinTexture player skin {@link Identifier} (Yarn), or {@code null} for
     *                    plain grey
     * @param isSlim      true = Alex (slim) model
     */
    public EpicFightPreviewComponent(Identifier skinTexture, boolean isSlim,
            Armature armature) {
        this.skinTexture = skinTexture;
        this.isSlim = isSlim;
        this.armature = armature;
        this.entityPatch = new FakeEntityPatch(armature);
        EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
        this.innerArmorModel = new BipedEntityModel<>(loader.getModelPart(EntityModelLayers.PLAYER_INNER_ARMOR));
        this.outerArmorModel = new BipedEntityModel<>(loader.getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR));
        this.animator = new SimpleAnimator(this.entityPatch);
        this.entityPatch.initAnimator(this.animator);
        this.animator.postInit();
    }

    public void setAnimation(yesman.epicfight.api.asset.AssetAccessor<? extends StaticAnimation> animation) {
        if (animation != null) {
            this.animator.playAnimation(animation, 0.0f);
        }
    }

    public void setAnimationFromInstance(StaticAnimation animation) {
        if (animation != null) {
            this.animator.playAnimation(new yesman.epicfight.api.animation.AnimationManager.AnimationAccessorImpl<>(
                    animation.getRegistryName(), -1, false, a -> animation), 0.0f);
        }
    }

    // ------------------------------------------------------------------ public API

    public void setEquipment(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet, ItemStack mainhand,
            ItemStack offhand) {
        this.headStack = head.copy();
        this.chestStack = chest.copy();
        this.legsStack = legs.copy();
        this.feetStack = feet.copy();
        this.mainhandStack = mainhand.copy();
        this.offhandStack = offhand.copy();
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void tick() {
        this.animator.tick();
    }

    /**
     * The main rendering entry point.
     * Sets up a custom Orthographic projection matrix for the duration of the call.
     */
    public void render(DrawContext g, int centerX, int bottomY, float sizePx,
            float yawDeg, float pitchDeg, float partialTick) {

        yesman.epicfight.api.animation.Pose pose = this.animator.getPose(partialTick);
        yesman.epicfight.api.utils.math.OpenMatrix4f[] poseMatrices = this.entityPatch.getArmature()
                .getPoseAsTransformMatrix(pose, false);

        // ---- Projection setup (Orthographic to fix proportions) ----
        Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
        ShaderProgram prevShader = RenderSystem.getShader();

        // Clear depth so model displays over GUI background
        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);

        float guiW = (float) MinecraftClient.getInstance().getWindow().getScaledWidth();
        float guiH = (float) MinecraftClient.getInstance().getWindow().getScaledHeight();
        float aspect = guiW / guiH;

        // Orthographic removes perspective distortion (Industry Standard for UI Models)
        Matrix4f ortho = new Matrix4f().setOrtho(-aspect, aspect, -1.0f, 1.0f, -20.0f, 20.0f);

        RenderSystem.setProjectionMatrix(ortho, VertexSorter.BY_DISTANCE);
        RenderSystem.getModelViewStack().push();
        RenderSystem.getModelViewStack().loadIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();

        MatrixStack ms = g.getMatrices();
        ms.push();

        try {
            // Calculate translation from screen pixels to Ortho space
            float tx = ((centerX / guiW) - 0.5f) * 2.0f * aspect;
            float ty = ((bottomY / guiH) - 0.5f) * 2.0f;

            ms.translate(tx, -ty, 0.0);

            float scale = (sizePx / guiH);
            ms.scale(scale, scale, scale);

            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitchDeg));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));

            VertexConsumerProvider bufferSource = g.getVertexConsumers();

            // 1. Base biped mesh (player skin or plain grey)
            yesman.epicfight.api.client.model.SkinnedMesh mesh = (this.isSlim
                    ? yesman.epicfight.api.client.model.Meshes.ALEX
                    : yesman.epicfight.api.client.model.Meshes.BIPED).get();
            if (mesh instanceof yesman.epicfight.client.mesh.HumanoidMesh humanoid) {
                // Force outer skin layers to be visible
                if (humanoid.hat != null) {
                    humanoid.hat.setHidden(false);
                }
                if (humanoid.jacket != null) {
                    humanoid.jacket.setHidden(false);
                }
                if (humanoid.leftSleeve != null) {
                    humanoid.leftSleeve.setHidden(false);
                }
                if (humanoid.rightSleeve != null) {
                    humanoid.rightSleeve.setHidden(false);
                }
                if (humanoid.leftPants != null) {
                    humanoid.leftPants.setHidden(false);
                }
                if (humanoid.rightPants != null) {
                    humanoid.rightPants.setHidden(false);
                }
            }

            renderMesh(ms, bufferSource, RenderLayer.getEntityTranslucentCull(getSkinOrDefault()), mesh, poseMatrices,
                    1f, 1f, 1f);

            // 3. Armor Rendering (Manual Loop with Cached Mesh lookup)
            net.minecraft.entity.player.PlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                // Equipment trick on the real player to satisfy GeckoLib/Animated armor state
                // requirements
                ItemStack oldHead = player.getEquippedStack(EquipmentSlot.HEAD);
                ItemStack oldChest = player.getEquippedStack(EquipmentSlot.CHEST);
                ItemStack oldLegs = player.getEquippedStack(EquipmentSlot.LEGS);
                ItemStack oldFeet = player.getEquippedStack(EquipmentSlot.FEET);
                ItemStack oldMainhand = player.getEquippedStack(EquipmentSlot.MAINHAND);
                ItemStack oldOffhand = player.getEquippedStack(EquipmentSlot.OFFHAND);

                player.equipStack(EquipmentSlot.HEAD, this.headStack);
                player.equipStack(EquipmentSlot.CHEST, this.chestStack);
                player.equipStack(EquipmentSlot.LEGS, this.legsStack);
                player.equipStack(EquipmentSlot.FEET, this.feetStack);
                player.equipStack(EquipmentSlot.MAINHAND, this.mainhandStack);
                player.equipStack(EquipmentSlot.OFFHAND, this.offhandStack);

                try {
                    renderArmorLayerSafe(ms, bufferSource, EquipmentSlot.HEAD, this.headStack, poseMatrices);
                    renderArmorLayerSafe(ms, bufferSource, EquipmentSlot.CHEST, this.chestStack, poseMatrices);
                    renderArmorLayerSafe(ms, bufferSource, EquipmentSlot.LEGS, this.legsStack, poseMatrices);
                    renderArmorLayerSafe(ms, bufferSource, EquipmentSlot.FEET, this.feetStack, poseMatrices);
                } finally {
                    player.equipStack(EquipmentSlot.HEAD, oldHead);
                    player.equipStack(EquipmentSlot.CHEST, oldChest);
                    player.equipStack(EquipmentSlot.LEGS, oldLegs);
                    player.equipStack(EquipmentSlot.FEET, oldFeet);
                    player.equipStack(EquipmentSlot.MAINHAND, oldMainhand);
                    player.equipStack(EquipmentSlot.OFFHAND, oldOffhand);
                }
            }

            // 4. Mainhand item
            if (!this.mainhandStack.isEmpty()) {
                renderMainhandItem(ms, bufferSource, pose, this.mainhandStack);
            }

            // 5. Offhand item
            if (!this.offhandStack.isEmpty()) {
                renderOffhandItem(ms, bufferSource, pose, this.offhandStack);
            }

            if (bufferSource instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw();
            }
        } finally {
            ms.pop();
            RenderSystem.disableDepthTest();
            RenderSystem.setProjectionMatrix(oldProjection, VertexSorter.BY_DISTANCE);
            RenderSystem.getModelViewStack().pop();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShader(() -> prevShader);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void renderMesh(MatrixStack ms, VertexConsumerProvider bufferSource,
            RenderLayer renderLayer,
            yesman.epicfight.api.client.model.SkinnedMesh mesh,
            yesman.epicfight.api.utils.math.OpenMatrix4f[] poseMatrices,
            float r, float g, float b) {
        if (mesh == null) {
            return;
        }
        mesh.initialize();
        VertexConsumer consumer = bufferSource
                .getBuffer(yesman.epicfight.client.renderer.EpicFightRenderTypes.getTriangulated(renderLayer));
        mesh.drawPosed(ms, consumer, yesman.epicfight.api.client.model.SkinnedMesh.DrawingFunction.NEW_ENTITY,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, r, g, b, 1.0f, OverlayTexture.DEFAULT_UV,
                this.entityPatch.getArmature(), poseMatrices);
    }

    private void renderArmorLayerSafe(MatrixStack ms, VertexConsumerProvider bufferSource, EquipmentSlot slot,
            ItemStack stack, yesman.epicfight.api.utils.math.OpenMatrix4f[] poseMatrices) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
            return;
        }
        try {
            renderArmorLayer(ms, bufferSource, slot, stack, poseMatrices);
        } catch (Exception ignored) {
        }
    }

    private void renderArmorLayer(MatrixStack ms, VertexConsumerProvider bufferSource, EquipmentSlot slot,
            ItemStack stack, yesman.epicfight.api.utils.math.OpenMatrix4f[] poseMatrices) {
        net.minecraft.entity.player.PlayerEntity player = MinecraftClient.getInstance().player;
        PlayerEntityRenderer renderer = (PlayerEntityRenderer) MinecraftClient.getInstance().getEntityRenderDispatcher()
                .getSkinMap().get(this.isSlim ? "slim" : "default");
        BipedEntityModel<?> rendererModel = renderer.getModel();

        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) {
            return;
        }

        ArmorItem item = armorItem;
        BipedEntityModel<?> defaultModel = slot == EquipmentSlot.LEGS ? innerArmorModel : outerArmorModel;
        Model armorModel = ForgeHooksClient.getArmorModel(player, stack, slot, (BipedEntityModel) defaultModel);

        yesman.epicfight.api.client.model.SkinnedMesh armorMesh = yesman.epicfight.client.renderer.patched.layer.WearableItemLayer
                .getCachedModel(armorItem);

        if (armorMesh == null) {
            armorMesh = HumanoidModelBaker.bakeArmor(
                    player, stack, armorItem, slot,
                    (BipedEntityModel) defaultModel,
                    armorModel,
                    (BipedEntityModel) rendererModel,
                    this.isSlim ? yesman.epicfight.api.client.model.Meshes.ALEX.get()
                            : yesman.epicfight.api.client.model.Meshes.BIPED.get());
        }

        final yesman.epicfight.api.client.model.SkinnedMesh finalArmorMesh = armorMesh;

        if (armorMesh == null) {
            return;
        }

        Identifier texture = getArmorTexture(stack, slot, null);
        renderMesh(ms, bufferSource, RenderLayer.getArmorCutoutNoCull(texture), armorMesh, poseMatrices, 1.0f, 1.0f,
                1.0f);

        // Trim rendering
        ArmorTrim.getTrim(MinecraftClient.getInstance().world.getRegistryManager(), stack).ifPresent(trim -> {
            renderTrim(ms, bufferSource, finalArmorMesh, item.getMaterial(), trim, slot, poseMatrices);
        });

        if (stack.hasGlint()) {
            renderMesh(ms, bufferSource, RenderLayer.getArmorEntityGlint(), armorMesh, poseMatrices, 1.0f, 1.0f, 1.0f);
        }
    }

    private Identifier getArmorTexture(ItemStack stack, EquipmentSlot slot, String type) {
        ArmorItem item = (ArmorItem) stack.getItem();
        String materialName = item.getMaterial().getName();
        String domain = "minecraft";
        String texture = materialName;

        int idx = materialName.indexOf(':');
        if (idx != -1) {
            domain = materialName.substring(0, idx);
            texture = materialName.substring(idx + 1);
        } else {
            Identifier itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            if (itemId != null && !itemId.getNamespace().equals("minecraft")) {
                domain = itemId.getNamespace();
            }
        }

        String suffix = (slot == EquipmentSlot.LEGS ? "2" : "1");
        String overlay = (type == null ? "" : "_" + type);
        String defaultPath = String.format("textures/models/armor/%s_layer_%s%s.png", texture, suffix, overlay);

        // 1. Try Forge's getArmorTexture hook (includes item.getArmorTexture and mod
        // overrides)
        String finalPath = defaultPath;
        try {
            // This hook is the standard for both GeckoLib and traditional Forge armor
            String forgeTex = item.getArmorTexture(stack, MinecraftClient.getInstance().player, slot, type);
            if (forgeTex != null && !forgeTex.isEmpty()) {
                finalPath = forgeTex;
                if (!finalPath.contains(":")) {
                    finalPath = domain + ":" + finalPath;
                }
            } else {
                finalPath = domain + ":" + defaultPath;
            }
        } catch (Throwable ignored) {
            finalPath = domain + ":" + defaultPath;
        }

        // 2. Epic Fight natively looks for a version in an 'epicfight' subfolder first
        // Example: textures/models/armor/iron_layer_1.png ->
        // textures/models/armor/epicfight/iron_layer_1.png
        int lastSlash = finalPath.lastIndexOf('/');
        if (lastSlash != -1) {
            String efPath = finalPath.substring(0, lastSlash) + "/epicfight" + finalPath.substring(lastSlash);
            Identifier efId = new Identifier(efPath);
            if (MinecraftClient.getInstance().getResourceManager().getResource(efId).isPresent()) {
                return efId;
            }
        }

        // 3. Fallback check for the finalPath itself
        Identifier finalId = new Identifier(finalPath);
        if (MinecraftClient.getInstance().getResourceManager().getResource(finalId).isPresent()) {
            return finalId;
        }

        // 4. Manual deep fallbacks (for when standard naming/paths fail entirely)
        String[] fallbackPaths = {
                String.format("textures/entities/%s_layer_%s%s.png", texture, suffix, overlay),
                String.format("textures/entities/%s.png", texture.replace("_", "")),
                String.format("textures/models/armor/%s.png", texture.replace("_", ""))
        };

        for (String p : fallbackPaths) {
            Identifier id = new Identifier(domain, p);
            if (MinecraftClient.getInstance().getResourceManager().getResource(id).isPresent()) {
                return id;
            }
        }

        return finalId;
    }

    private void renderTrim(MatrixStack ms, VertexConsumerProvider bufferSource,
            yesman.epicfight.api.client.model.SkinnedMesh mesh,
            net.minecraft.item.ArmorMaterial material, ArmorTrim trim, EquipmentSlot slot,
            yesman.epicfight.api.utils.math.OpenMatrix4f[] poseMatrices) {
        try {
            // 100% reflection to avoid compile-time symbol errors on different mappings
            String methodName = (slot == EquipmentSlot.LEGS) ? "innerTexture" : "outerTexture";
            Identifier texture = null;
            try {
                texture = (Identifier) trim.getClass().getMethod(methodName, ArmorMaterial.class).invoke(trim,
                        material);
            } catch (NoSuchMethodException e) {
                methodName = (slot == EquipmentSlot.LEGS) ? "getLeggingsTexture" : "getGenericTexture";
                texture = (Identifier) trim.getClass().getMethod(methodName, ArmorMaterial.class).invoke(trim,
                        material);
            }

            if (texture != null) {
                renderMesh(ms, bufferSource, RenderLayer.getArmorCutoutNoCull(texture), mesh, poseMatrices, 1.0f, 1.0f,
                        1.0f);
            }
        } catch (Throwable ignored) {
        }
    }

    private Identifier getSkinOrDefault() {
        return (this.skinTexture != null) ? this.skinTexture : new Identifier("minecraft", "textures/entity/steve.png");
    }

    private void renderMainhandItem(MatrixStack ms, VertexConsumerProvider bufferSource,
            yesman.epicfight.api.animation.Pose pose, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        boolean renderedViaEpicFight = false;
        try {
            yesman.epicfight.client.renderer.patched.item.RenderItemBase itemRenderer = yesman.epicfight.client.ClientEngine
                    .getInstance().renderEngine.getItemRenderer(stack);
            if (itemRenderer != null) {
                yesman.epicfight.api.utils.math.OpenMatrix4f[] unskinnedPoses = this.entityPatch.getArmature()
                        .getPoseAsTransformMatrix(pose, false);
                itemRenderer.renderItemInHand(stack, this.entityPatch, Hand.MAIN_HAND, unskinnedPoses, bufferSource, ms,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE, 0.0f);
                renderedViaEpicFight = true;
            }
        } catch (Exception ignored) {
        }
        if (!renderedViaEpicFight) {
            renderVanillaItemInHand(ms, bufferSource, pose, stack);
        }
    }

    private void renderVanillaItemInHand(MatrixStack ms, VertexConsumerProvider bufferSource,
            yesman.epicfight.api.animation.Pose pose, ItemStack stack) {
        ms.push();
        yesman.epicfight.api.utils.math.OpenMatrix4f[] poses = this.entityPatch.getArmature().getPoseAsTransformMatrix(
                pose,
                false);
        yesman.epicfight.api.animation.Joint parentJoint = this.entityPatch.getParentJointOfHand(Hand.MAIN_HAND);
        yesman.epicfight.api.utils.math.OpenMatrix4f toolTransform = poses[parentJoint.getId()];
        yesman.epicfight.api.utils.math.MathUtils.mulStack(ms, toolTransform);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
        ms.translate(0.0, -0.1, 0.0);
        MinecraftClient.getInstance().getItemRenderer().renderItem(stack,
                net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_RIGHT_HAND,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, ms, bufferSource, null, 0);
        ms.pop();
    }

    private void renderOffhandItem(MatrixStack ms, VertexConsumerProvider bufferSource,
            yesman.epicfight.api.animation.Pose pose, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        boolean renderedViaEpicFight = false;
        try {
            yesman.epicfight.client.renderer.patched.item.RenderItemBase itemRenderer = yesman.epicfight.client.ClientEngine
                    .getInstance().renderEngine.getItemRenderer(stack);
            if (itemRenderer != null) {
                yesman.epicfight.api.utils.math.OpenMatrix4f[] unskinnedPoses = this.entityPatch.getArmature()
                        .getPoseAsTransformMatrix(pose, false);
                itemRenderer.renderItemInHand(stack, this.entityPatch, Hand.OFF_HAND, unskinnedPoses, bufferSource, ms,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE, 0.0f);
                renderedViaEpicFight = true;
            }
        } catch (Exception ignored) {
        }
        if (!renderedViaEpicFight) {
            renderVanillaItemInOffHand(ms, bufferSource, pose, stack);
        }
    }

    private void renderVanillaItemInOffHand(MatrixStack ms, VertexConsumerProvider bufferSource,
            yesman.epicfight.api.animation.Pose pose, ItemStack stack) {
        ms.push();
        yesman.epicfight.api.utils.math.OpenMatrix4f[] poses = this.entityPatch.getArmature().getPoseAsTransformMatrix(
                pose,
                false);
        yesman.epicfight.api.animation.Joint parentJoint = this.entityPatch.getParentJointOfHand(Hand.OFF_HAND);
        yesman.epicfight.api.utils.math.OpenMatrix4f toolTransform = poses[parentJoint.getId()];
        yesman.epicfight.api.utils.math.MathUtils.mulStack(ms, toolTransform);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
        ms.translate(0.0, -0.1, 0.0);
        MinecraftClient.getInstance().getItemRenderer().renderItem(stack,
                net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_LEFT_HAND,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, ms, bufferSource, null, 0);
        ms.pop();
    }

    // ================================================================= INNER
    // CLASSES

    private static class SimpleAnimator extends yesman.epicfight.api.client.animation.ClientAnimator {
        SimpleAnimator(yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch<?> ep) {
            super(ep, yesman.epicfight.api.client.animation.Layer.BaseLayer::new);
        }

        @Override
        public void tick() {
            this.baseLayer.update(this.entitypatch);
        }

        @Override
        public void postInit() {
        }
    }

    static class FakeEntityPatch extends yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch<LivingEntity>
            implements yesman.epicfight.api.physics.SimulatableObject {
        public FakeEntityPatch(Armature armature) {
            this.armature = armature;
            this.original = (LivingEntity) (Object) MinecraftClient.getInstance().player;
            this.initialized = true;
        }

        @Override
        public void initAnimator(Animator a) {
            super.initAnimator(a);
            this.animator = a;
        }

        @Override
        public void poseTick(yesman.epicfight.api.animation.types.DynamicAnimation animation,
                yesman.epicfight.api.animation.Pose pose, float elapsed, float partialTick) {
        }

        @Override
        public void updateMotion(boolean considerInaction) {
        }

        @Override
        public yesman.epicfight.api.asset.AssetAccessor<? extends yesman.epicfight.api.animation.types.StaticAnimation> getHitAnimation(
                yesman.epicfight.world.damagesource.StunType stunType) {
            return null;
        }

        @Override
        public boolean isLogicalClient() {
            return true;
        }

        @Override
        public yesman.epicfight.world.capabilities.entitypatch.Faction getFaction() {
            return yesman.epicfight.world.capabilities.entitypatch.Factions.NEUTRAL;
        }

        @Override
        public boolean overrideRender() {
            return true;
        }

        public yesman.epicfight.api.utils.math.OpenMatrix4f getModelMatrix(float partialTick) {
            return yesman.epicfight.api.utils.math.MathUtils.getModelMatrixIntegral(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    partialTick, 1, 1, 1);
        }

        @Override
        public yesman.epicfight.api.animation.Joint getParentJointOfHand(Hand hand) {
            String jointName = (hand == Hand.MAIN_HAND) ? "Tool_R" : "Tool_L";
            return this.armature.searchJointByName(jointName);
        }

        @Override
        public <SIM extends yesman.epicfight.api.physics.PhysicsSimulator<?, ?, ?, ?, ?>> Optional<SIM> getSimulator(
                yesman.epicfight.api.physics.SimulationTypes<?, ?, ?, ?, ?, SIM> type) {
            return Optional.empty();
        }
    }
}
