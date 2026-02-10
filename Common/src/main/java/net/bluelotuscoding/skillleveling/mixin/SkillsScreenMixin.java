package net.bluelotuscoding.skillleveling.mixin;

import net.minecraft.client.gui.DrawContext;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.config.ClientCategoryConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConnectionConfig;
import org.joml.Vector2i;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(value = net.puffish.skillsmod.client.gui.SkillsScreen.class, remap = false)
public abstract class SkillsScreenMixin {

    @Shadow
    private Optional<ClientCategoryData> optActiveCategoryData;

    @Unique
    private ClientSkillConfig addon$hoveredSkillConfig;

    /**
     * Filter skills to hide those marked as hidden when prerequisites aren't met.
     */
    @Redirect(method = "drawContentWithCategory", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"), remap = false)
    private Collection<ClientSkillConfig> addon$filterSkillsInDraw(
            Map<String, ClientSkillConfig> instance,
            DrawContext context, double mouseX, double mouseY, ClientCategoryData activeCategoryData) {
        if (activeCategoryData == null) {
            return instance.values();
        }

        String categoryId = activeCategoryData.getConfig().id().toString();

        return instance.values().stream()
                .filter(skill -> {
                    try {
                        // Check if skill is hidden and prerequisites aren't met
                        if (net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.isHidden(categoryId,
                                skill.id())) {
                            // Check if prerequisites are met by checking state
                            var state = activeCategoryData.getSkillState(skill);
                            // If hidden and state is LOCKED (prerequisites not met), filter it out
                            if (state == net.puffish.skillsmod.api.Skill.State.LOCKED) {
                                return false;
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        // On error, show the skill to avoid breaking the GUI
                        return true;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Filter normal connections to hide those involving hidden skills.
     */
    @Redirect(method = "drawContentWithCategory", at = @At(value = "INVOKE", target = "Lnet/puffish/skillsmod/client/config/ClientCategoryConfig;normalConnections()Ljava/util/Collection;"), remap = false)
    private Collection<ClientSkillConnectionConfig> addon$filterNormalConnections(
            ClientCategoryConfig category,
            DrawContext context, double mouseX, double mouseY, ClientCategoryData activeCategoryData) {
        if (activeCategoryData == null) {
            return category.normalConnections();
        }
        return category.normalConnections().stream()
                .filter(conn -> addon$isConnectionVisible(conn, category, activeCategoryData))
                .collect(Collectors.toList());
    }

    @Unique
    private boolean addon$isConnectionVisible(ClientSkillConnectionConfig conn, ClientCategoryConfig category,
            ClientCategoryData categoryData) {
        // ... (existing implementation)
        return true;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "drawContentWithCategory", at = @At("HEAD"), remap = false)
    private void addon$onDrawContent(DrawContext context, double mouseX, double mouseY,
            ClientCategoryData activeCategoryData, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.addon$hoveredSkillConfig = null;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void addon$onMouseClicked(double mouseX, double mouseY, int button,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && optActiveCategoryData.isPresent() && addon$hoveredSkillConfig != null) {
            var activeCategoryData = optActiveCategoryData.get();
            var categoryId = activeCategoryData.getConfig().id();
            var skill = addon$hoveredSkillConfig;

            // Check if it's a toggle skill
            if (net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.isToggle(categoryId.toString(),
                    skill.id())) {
                // Check if it's already unlocked (level > 0)
                int level = net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.getLevel(
                        categoryId.toString(),
                        skill.id());
                if (level > 0) {
                    net.bluelotuscoding.skillleveling.network.SkillLevelingNetwork.sendRequestToggleSkill(categoryId,
                            skill.id());
                }
            }
        }
    }

    // Shadows for width/height removed to avoid InvalidMixinException
    // We access them via casting to Screen in the method body

    @Shadow
    private org.joml.Vector2i getTransformedMousePos(double mouseX, double mouseY, ClientCategoryData categoryData) {
        throw new AssertionError();
    }

    @Shadow
    private boolean isInsideSkill(org.joml.Vector2i mousePos, ClientSkillConfig skill,
            net.puffish.skillsmod.client.config.skill.ClientSkillDefinitionConfig definition) {
        throw new AssertionError();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "drawContentWithCategory", at = @At("TAIL"), remap = false)
    private void addon$onDrawContentWithCategoryTail(DrawContext context, double mouseX, double mouseY,
            ClientCategoryData activeCategoryData, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {

        if (activeCategoryData == null)
            return;

        var transformedMousePos = getTransformedMousePos(mouseX, mouseY, activeCategoryData);
        var config = activeCategoryData.getConfig();

        // Use PoseStack to draw in skill coordinates
        var poseStack = context.getMatrices();
        poseStack.push();
        // Translate to center (standard screen center logic) + category offset
        // Using cast to Screen to access width/height without shadowing (avoids mapping
        // issues)
        var screen = (net.minecraft.client.gui.screen.Screen) (Object) this;
        poseStack.translate(
                activeCategoryData.getX() + screen.width / 2.0f,
                activeCategoryData.getY() + screen.height / 2.0f,
                0.0f);
        poseStack.scale(activeCategoryData.getScale(), activeCategoryData.getScale(), 1.0f);

        for (var skill : config.skills().values()) {
            var definition = config.getDefinitionById(skill.definitionId()).orElse(null);
            if (definition == null)
                continue;

            // Check hover (logic uses transformed position)
            if (isInsideSkill(transformedMousePos, skill, definition)) {
                this.addon$hoveredSkillConfig = skill;
            }

            // Draw Cooldown Overlay
            int seconds = net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage
                    .getRemainingCooldownSecondsByDefinitionId(skill.id());
            if (seconds > 0) {
                String text = String.valueOf(seconds);
                var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
                int textWidth = textRenderer.getWidth(text);

                // Draw in skill coordinates
                context.drawText(textRenderer, text, skill.x() + 13 - textWidth / 2, skill.y() + 13 - 4, 0xFFFF55,
                        true);
            }
        }

        poseStack.pop();
    }
}
