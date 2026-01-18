package net.bluelotuscoding.skillleveling.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.gui.SkillsScreen;
import net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SkillsScreen.class, remap = false)
public abstract class SkillsScreenMixin {

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

        String categoryId = activeCategoryData.getConfig().id().toString();
        var client = MinecraftClient.getInstance();

        // Draw level indicator for each skill that has leveling enabled
        for (var skill : activeCategoryData.getConfig().skills().values()) {
            String skillId = skill.id();

            if (ClientSkillLevelStorage.hasLevelInfo(categoryId, skillId)) {
                int level = ClientSkillLevelStorage.getLevel(categoryId, skillId);
                int maxLevel = ClientSkillLevelStorage.getMaxLevel(categoryId, skillId);

                String levelText = level + "/" + maxLevel;

                // Skill coordinates in the category plane
                int x = skill.x();
                int y = skill.y() + 14; // Position below the icon

                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 500); // Draw on top

                float scale = 0.7f;
                context.getMatrices().scale(scale, scale, 1.0f);

                // Center the text
                int textWidth = client.textRenderer.getWidth(levelText);
                float scaledX = (x / scale) - (textWidth / 2f);
                float scaledY = (y / scale);

                context.drawText(client.textRenderer, levelText, (int) scaledX, (int) scaledY, 0xFFFFFF, true);

                context.getMatrices().pop();
            }
        }
    }
}
