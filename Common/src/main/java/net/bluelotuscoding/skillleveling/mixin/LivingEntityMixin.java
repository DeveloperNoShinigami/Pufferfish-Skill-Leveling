package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * Injects into equipment changes to refresh skill rewards (like bonus
     * attributes).
     * Uses onEquipStack which is often more stable across environments for
     * monitoring changes.
     * require = 0 is used to prevent startup crashes if the method name doesn't
     * match
     * exactly in a specific loader/environment, though we aim for it to work.
     * Uses delayed sync to ensure equipment state is fully updated.
     */
    @Inject(method = "onEquipStack", at = @At("RETURN"), require = 0)
    private void onEquipStackChange(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity != null && !entity.getWorld().isClient() && entity instanceof ServerPlayerEntity player) {
            var mod = SkillLevelingMod.getInstance();
            if (mod != null && mod.getSkillLevelingManager() != null) {
                // Schedule sync for next tick to ensure equipment state is fully updated
                player.getServer().execute(() -> {
                    // Trigger a refresh of all skill rewards to account for potentially changed
                    // imbued levels
                    mod.getSkillLevelingManager().refreshAllRewards(player);
                    // REAL-TIME UI SYNC: Notify client to update its level cache and UI
                    mod.getSkillLevelingManager().syncAllSkillsToPlayer(player);
                });
            }
        }
    }

    /**
     * Fallback for environments where onEquipStack is not available.
     * Uses delayed sync for consistency.
     */
    @Inject(method = "equipStack", at = @At("RETURN"), require = 0)
    private void onEquipStack(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity != null && !entity.getWorld().isClient() && entity instanceof ServerPlayerEntity player) {
            var mod = SkillLevelingMod.getInstance();
            if (mod != null && mod.getSkillLevelingManager() != null) {
                // Schedule sync for next tick to ensure equipment state is fully updated
                player.getServer().execute(() -> {
                    mod.getSkillLevelingManager().refreshAllRewards(player);
                    mod.getSkillLevelingManager().syncAllSkillsToPlayer(player);
                });
            }
        }
    }
}
