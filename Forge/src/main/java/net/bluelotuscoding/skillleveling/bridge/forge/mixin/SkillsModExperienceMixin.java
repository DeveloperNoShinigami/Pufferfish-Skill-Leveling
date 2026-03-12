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
    private void onSetExperienceGuard(@Coerce Object player, @Coerce Object categoryId, int experience,
            CallbackInfo ci) {
        if (net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.isCategoryLocked(player, categoryId.toString())) {
            ci.cancel();
        }
    }

}
