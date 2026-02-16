package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.impl.CategoryImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = CategoryImpl.class, remap = false)
public class CategoryImplMixin {

    @Shadow
    private Identifier categoryId;

    @Inject(method = "openScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(ServerPlayerEntity player, CallbackInfo ci) {
        // Check if category is locked - if so, block opening and show message
        var prereqs = LeveledConfigStorage.getCategoryPrerequisites(categoryId);
        if (prereqs == null || prereqs.isEmpty()) {
            return; // No prerequisites, allow opening
        }

        // Check if all prerequisites are met
        boolean allMet = true;
        for (var req : prereqs) {
            int currentLevel = SkillLevelingMod.getInstance().getSkillLevelingManager()
                    .getTotalSkillLevel(player, categoryId, req.skillId);
            if (currentLevel < req.minLevel) {
                allMet = false;
                break;
            }
        }

        if (!allMet) {
            // Build prerequisite message
            List<String> prereqLines = new ArrayList<>();
            for (var req : prereqs) {
                String categorySuffix = (req.categoryId != null && !req.categoryId.isEmpty())
                        ? " (" + req.categoryId + ")"
                        : "";
                prereqLines.add(req.skillId + categorySuffix + " Lv" + req.minLevel);
            }

            player.sendMessage(Text.literal("§8[§6Skill Leveling§8] §cCategory locked! Required skills: §e"
                    + String.join(", ", prereqLines)), false);

            ci.cancel(); // Prevent screen from opening
        }
    }
}
