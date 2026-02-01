package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.registry.ModVillagers;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerData;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(VillagerEntity.class)
public abstract class VillageProfessionMixin {

    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void onSetVillagerData(VillagerData villagerData, CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;

        // Log ALL profession changes to help diagnose reversions
        Identifier newProfId = Registries.VILLAGER_PROFESSION.getId(villagerData.getProfession());
        Identifier currentProfId = Registries.VILLAGER_PROFESSION.getId(self.getVillagerData().getProfession());

        if (ModVillagers.SKILL_MASTER_ID.equals(currentProfId) && !ModVillagers.SKILL_MASTER_ID.equals(newProfId)) {
            SkillLevelingMod.getInstance().getLogger().warn(
                    "Villager " + self.getUuid() + " is LOSING Skill Master profession! New profession: " + newProfId);
            // Print stack trace to identify the cause of profession reset
            Exception stackTrace = new Exception("Profession Reversion Stack Trace");
            for (StackTraceElement element : stackTrace.getStackTrace()) {
                SkillLevelingMod.getInstance().getLogger().warn("  at " + element.toString());
            }
        }

        // If trying to become a Skill Master
        if (ModVillagers.SKILL_MASTER_ID.equals(newProfId)
                || villagerData.getProfession() == ModVillagers.SKILL_MASTER) {
            // Check if they are ALREADY a Skill Master (allow level ups)
            if (ModVillagers.SKILL_MASTER_ID.equals(currentProfId)
                    || self.getVillagerData().getProfession() == ModVillagers.SKILL_MASTER) {
                return;
            }

            // Check if there's already a Skill Master nearby (reduce range to 32, more
            // reasonable)
            List<VillagerEntity> nearbyVillagers = self.getWorld().getEntitiesByClass(
                    VillagerEntity.class,
                    self.getBoundingBox().expand(32.0),
                    villager -> {
                        if (villager == self)
                            return false;
                        Identifier otherId = Registries.VILLAGER_PROFESSION
                                .getId(villager.getVillagerData().getProfession());
                        return ModVillagers.SKILL_MASTER_ID.equals(otherId);
                    });

            if (!nearbyVillagers.isEmpty()) {
                SkillLevelingMod.getInstance().getLogger().info("Villager " + self.getUuid()
                        + " blocked from becoming Skill Master: " + nearbyVillagers.size() + " already exist nearby.");
                ci.cancel();
            } else {
                SkillLevelingMod.getInstance().getLogger()
                        .info("Villager " + self.getUuid() + " allowed to become Skill Master.");
            }
        }
    }
}
