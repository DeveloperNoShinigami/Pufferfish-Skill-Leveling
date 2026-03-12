package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementDef;
import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementsManager;
import net.bluelotuscoding.skillleveling.client.ItemRestrictionKeybind;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (client.player == null || client.options.hudHidden)
            return;

        HitResult hit = client.crosshairTarget;
        if (hit == null)
            return;

        String targetId = null;
        ItemRequirementsManager.TargetType type = null;

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockState state = client.world.getBlockState(((BlockHitResult) hit).getBlockPos());
            targetId = Registries.BLOCK.getId(state.getBlock()).toString();
            type = ItemRequirementsManager.TargetType.BLOCK;

            // SPECIAL CASE: Redirect portal blocks to dimension restrictions
            if (targetId.contains("portal")) {
                String dimRedirect = null;
                if (targetId.equals("minecraft:nether_portal"))
                    dimRedirect = "minecraft:the_nether";
                else if (targetId.equals("minecraft:end_portal") || targetId.equals("minecraft:end_gateway"))
                    dimRedirect = "minecraft:the_end";

                if (dimRedirect != null) {
                    ItemRequirementDef dimDef = ItemRequirementsManager.getRequirements(dimRedirect,
                            ItemRequirementsManager.TargetType.DIMENSION);
                    if (dimDef != null) {
                        targetId = dimRedirect;
                        type = ItemRequirementsManager.TargetType.DIMENSION;
                    }
                }
            }
        } else if (hit.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) hit).getEntity();
            targetId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            type = ItemRequirementsManager.TargetType.ENTITY;
        }

        if (targetId == null)
            return;

        ItemRequirementDef def = ItemRequirementsManager.getRequirements(targetId, type);
        if (def == null)
            return;

        // Skip if tooltips are explicitly disabled
        if (def.tooltip != null && !def.tooltip)
            return;

        List<String> failures = ItemRequirementsManager.checkRequirements(client.player, targetId, type);
        if (failures.isEmpty())
            return;

        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("Target: ").append(convertToTitleCase(targetId.split(":")[1])).append(" \u26A0")
                .formatted(net.minecraft.util.Formatting.GOLD));
        lines.add(Text.literal(" "));

        if (ItemRestrictionKeybind.isShowingRestrictions()) {
            lines.add(Text.literal("\u2716 Requirements Not Met:").formatted(net.minecraft.util.Formatting.RED,
                    net.minecraft.util.Formatting.BOLD));
            for (String failure : failures) {
                lines.add(Text.literal("  \u2022 " + failure).formatted(net.minecraft.util.Formatting.RED));
            }
        } else {
            String keyName = ItemRestrictionKeybind.getKeyName();
            lines.add(Text.literal("[Hold " + keyName + " to view restrictions]")
                    .formatted(net.minecraft.util.Formatting.YELLOW));
        }

        // Render at a fixed position on the screen (e.g., top-center or slightly above
        // hotbar)
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // Render slightly above the hotbar area
        int x = width / 2 + 10;
        int y = height / 2 + 10;

        context.drawTooltip(this.getTextRenderer(), lines, x, y);
    }

    private String convertToTitleCase(String text) {
        if (text == null || text.isEmpty())
            return text;
        StringBuilder result = new StringBuilder();
        String[] parts = text.split("_");
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (result.length() > 0)
                    result.append(" ");
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
}
