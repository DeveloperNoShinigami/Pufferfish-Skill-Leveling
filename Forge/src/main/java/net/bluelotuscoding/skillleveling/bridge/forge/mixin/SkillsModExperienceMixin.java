package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "net.puffish.skillsmod.SkillsMod", remap = false)
public class SkillsModExperienceMixin {

    @Inject(method = "addExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceLocation;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onAddExperienceGuard(@Coerce Object player, @Coerce Object categoryId, int amount, CallbackInfo ci) {
        if (net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.isCategoryLocked(player, categoryId.toString())) {
            ci.cancel();
        }
    }

    @Inject(method = "setExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceLocation;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onSetExperienceGuard(@Coerce Object player, @Coerce Object categoryId, int experience, CallbackInfo ci) {
        if (net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.isCategoryLocked(player, categoryId.toString())) {
            ci.cancel();
        }
    }

    @Inject(method = "addExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceLocation;I)V", at = @At("RETURN"), remap = false)
    private void onAddExperience(@Coerce Object player, @Coerce Object categoryId, int amount, CallbackInfo ci) {
        triggerSync(player, categoryId, amount);
    }

    @Inject(method = "setExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceLocation;I)V", at = @At("RETURN"), remap = false)
    private void onSetExperience(@Coerce Object player, @Coerce Object categoryId, int experience, CallbackInfo ci) {
        triggerSync(player, categoryId, 0);
    }

    private void triggerSync(Object player, Object categoryId, int lastGain) {
        // Use mapping-agnostic check
        if (player == null || !player.getClass().getName().contains("ServerPlayer")) {
            return;
        }

        try {
            // Use active category check to ensure we only sync the class-related category
            net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.getActiveCategory(player).ifPresent(activeId -> {
                if (activeId.toString().equals(categoryId.toString())) {
                    net.bluelotuscoding.skillleveling.util.Platform platform = net.bluelotuscoding.skillleveling.SkillLevelingMod
                            .getInstance().getPlatform();
                    int level = platform.getPufferfishLevel(player, activeId);
                    int xp = platform.getPufferfishExperience(player, activeId);
                    platform.syncEpicClassLevel(player, level, xp, lastGain);
                }
            });
        } catch (Exception e) {
            // Silently fail to avoid affecting game logic
        }
    }
}
