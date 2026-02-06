package net.bluelotuscoding.skillleveling.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.config.ClientCategoryConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConnectionConfig;

import net.bluelotuscoding.skillleveling.client.TomeClientHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(value = net.puffish.skillsmod.client.gui.SkillsScreen.class, remap = false)
public abstract class SkillsScreenMixin {

    @Shadow
    private Optional<ClientCategoryData> optActiveCategoryData;

    @Shadow
    private double dragTotal;

    /**
     * Intercept mouseReleased to handle Tome selection mode.
     * When in selection mode, sends a TomeActionPacket instead of normal skill
     * click.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (optActiveCategoryData.isEmpty()) {
            return;
        }

        var activeCategoryData = optActiveCategoryData.get();
        var activeCategory = activeCategoryData.getConfig();

        // 1. HIDDEN SKILL BLOCKER: Prevent interaction with skills that are currently
        // hidden
        String clickedSkillId = addon$findClickedSkillId(mouseX, mouseY, activeCategoryData);
        if (clickedSkillId != null) {
            String categoryId = activeCategory.id().toString();
            // ABSOLUTE BLOCKER for truly hidden skills
            if (net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.isHidden(categoryId, clickedSkillId)) {
                var skill = activeCategory.skills().get(clickedSkillId);
                var state = skill != null ? activeCategoryData.getSkillState(skill) : null;
                if (state == net.puffish.skillsmod.api.Skill.State.LOCKED) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        // 2. TOME SELECTION MODE
        if (!TomeClientHandler.isInSelectionMode()) {
            return; // Not in selection mode, let normal processing happen
        }

        if (button != 0) { // Only handle left click
            return;
        }

        if (dragTotal > 2) {
            return; // Was dragging, not clicking
        }

        var categoryId = activeCategory.id();

        if (clickedSkillId != null) {
            var tomeType = TomeClientHandler.getPendingTomeType();

            // Send tome action packet to server
            TomeClientHandler.sendTomeAction(categoryId, clickedSkillId, tomeType);

            // Clear selection mode
            TomeClientHandler.clearSelectionMode();

            // Close the screen
            net.bluelotuscoding.skillleveling.client.SideSafeClient.closeScreen();

            cir.setReturnValue(true);
        }
    }

    /**
     * Find the skill ID under the mouse cursor.
     */
    @Unique
    private String addon$findClickedSkillId(double mouseX, double mouseY, ClientCategoryData activeCategoryData) {
        var activeCategory = activeCategoryData.getConfig();

        // Calculate transformed mouse position
        double transformedX = mouseX - activeCategoryData.getX();
        double transformedY = mouseY - activeCategoryData.getY();

        // Check each skill
        for (var skill : activeCategory.skills().values()) {
            var state = activeCategoryData.getSkillState(skill);
            if (state == null) {
                continue;
            }

            // Respect visibility filter: if LOCKED + hidden, it's not "on screen" to be
            // clicked
            String categoryId = activeCategory.id().toString();
            if (state == net.puffish.skillsmod.api.Skill.State.LOCKED &&
                    net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.isHidden(categoryId, skill.id())) {
                continue;
            }

            var optDefinition = activeCategory.getDefinitionById(skill.definitionId());
            if (optDefinition.isEmpty()) {
                continue;
            }

            var definition = optDefinition.get();
            int skillX = skill.x();
            int skillY = skill.y();
            int size = (int) definition.size();
            int halfSize = size / 2;

            if (transformedX >= skillX - halfSize
                    && transformedX <= skillX + halfSize
                    && transformedY >= skillY - halfSize
                    && transformedY <= skillY + halfSize) {
                return skill.id();
            }
        }
        return null;
    }

    /**
     * Injects at the end of the skill drawing loop to render level indicators.
     * We use a stable injection point at the return of drawContentWithCategory.
     */
    @Inject(method = "drawContentWithCategory", at = @At("RETURN"))
    private void onDrawContentReturn(DrawContext context, double mouseX, double mouseY,
            ClientCategoryData activeCategoryData, CallbackInfo ci) {
        if (activeCategoryData == null) {
            return;
        }

        var client = MinecraftClient.getInstance();

        // Draw "Selection Mode" overlay when in selection mode
        if (TomeClientHandler.isInSelectionMode()) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 600);
            String modeText = "Select a skill...";
            int textWidth = client.textRenderer.getWidth(modeText);
            int screenWidth = client.getWindow().getScaledWidth();
            context.drawText(client.textRenderer, modeText, (screenWidth - textWidth) / 2, 10, 0xFFFF00, true);
            context.getMatrices().pop();
        }
    }

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
        var skillA = category.skills().get(conn.skillAId());
        var skillB = category.skills().get(conn.skillBId());
        if (skillA == null || skillB == null) {
            return false;
        }

        String categoryId = category.id().toString();

        // Check if either skill will be filtered out (hidden with unmet prerequisites)
        // This prevents connections from revealing hidden skills
        try {
            // Check skill A
            if (net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.isHidden(categoryId, skillA.id())) {
                var stateA = categoryData.getSkillState(skillA);
                if (stateA == net.puffish.skillsmod.api.Skill.State.LOCKED) {
                    return false; // Skill A will be filtered - hide connection
                }
            }

            // Check skill B
            if (net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.isHidden(categoryId, skillB.id())) {
                var stateB = categoryData.getSkillState(skillB);
                if (stateB == net.puffish.skillsmod.api.Skill.State.LOCKED) {
                    return false; // Skill B will be filtered - hide connection
                }
            }
        } catch (Exception e) {
            // On error, hide the connection to be safe
            return false;
        }

        return true;
    }
}
