package net.bluelotuscoding.skillleveling.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.puffish.skillsmod.client.data.ClientCategoryData;

import net.bluelotuscoding.skillleveling.client.TomeClientHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

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
        if (!TomeClientHandler.isInSelectionMode()) {
            return; // Not in selection mode, let normal processing happen
        }

        if (button != 0) { // Only handle left click
            return;
        }

        if (dragTotal > 2) {
            return; // Was dragging, not clicking
        }

        if (optActiveCategoryData.isEmpty()) {
            return;
        }

        var activeCategoryData = optActiveCategoryData.get();
        var activeCategory = activeCategoryData.getConfig();
        var categoryId = activeCategory.id();

        // Find clicked skill
        String clickedSkillId = addon$findClickedSkillId(mouseX, mouseY, activeCategoryData);

        if (clickedSkillId != null) {
            var tomeType = TomeClientHandler.getPendingTomeType();

            // Send tome action packet to server
            TomeClientHandler.sendTomeAction(categoryId, clickedSkillId, tomeType);

            // Clear selection mode
            TomeClientHandler.clearSelectionMode();

            // Close the screen
            MinecraftClient.getInstance().setScreen(null);

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
}
