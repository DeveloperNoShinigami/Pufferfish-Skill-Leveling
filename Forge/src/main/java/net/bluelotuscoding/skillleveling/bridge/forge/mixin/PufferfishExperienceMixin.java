package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Optional;

/**
 * Mixin to sync Pufferfish experience changes to Epic Class.
 * 
 * We hook into SkillsMod directly because it's the central hub for all XP
 * changes,
 * including those from mob kills which bypass the public Experience API.
 */
@Mixin(targets = "net.puffish.skillsmod.SkillsMod", remap = false)
public abstract class PufferfishExperienceMixin {

    @Unique
    private static final AddonLogger LOGGER = new AddonLogger();

    private static final ThreadLocal<Integer> OLD_TOTAL = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> SYNC_DEPTH = ThreadLocal.withInitial(() -> 0);

    // --- addExperience Hooks ---

    @Inject(method = "addExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/puffish/skillsmod/config/CategoryConfig;Lnet/puffish/skillsmod/config/experience/ExperienceConfig;Lnet/puffish/skillsmod/server/data/CategoryData;I)V", at = @At("HEAD"), remap = false)
    private void onAddExperienceInternalHead(@Coerce Object player, @Coerce Object categoryConfig,
            @Coerce Object experienceConfig, @Coerce Object categoryData, int amount, CallbackInfo ci) {
        handlePreXpChange(player, categoryConfig);
    }

    @Inject(method = "addExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/puffish/skillsmod/config/CategoryConfig;Lnet/puffish/skillsmod/config/experience/ExperienceConfig;Lnet/puffish/skillsmod/server/data/CategoryData;I)V", at = @At("RETURN"), remap = false)
    private void onAddExperienceInternalReturn(@Coerce Object player, @Coerce Object categoryConfig,
            @Coerce Object experienceConfig, @Coerce Object categoryData, int amount, CallbackInfo ci) {
        handlePostXpChange(player, categoryConfig);
    }

    // --- setExperience Hooks ---
    @Inject(method = "setExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/puffish/skillsmod/config/CategoryConfig;Lnet/puffish/skillsmod/config/experience/ExperienceConfig;Lnet/puffish/skillsmod/server/data/CategoryData;I)V", at = @At("HEAD"), remap = false)
    private void onSetExperienceInternalHead(@Coerce Object player, @Coerce Object categoryConfig,
            @Coerce Object experienceConfig, @Coerce Object categoryData, int experience, CallbackInfo ci) {
        handlePreXpChange(player, categoryConfig);
    }

    @Inject(method = "setExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/puffish/skillsmod/config/CategoryConfig;Lnet/puffish/skillsmod/config/experience/ExperienceConfig;Lnet/puffish/skillsmod/server/data/CategoryData;I)V", at = @At("RETURN"), remap = false)
    private void onSetExperienceInternalReturn(@Coerce Object player, @Coerce Object categoryConfig,
            @Coerce Object experienceConfig, @Coerce Object categoryData, int experience, CallbackInfo ci) {
        handlePostXpChange(player, categoryConfig);
    }

    @Unique
    private void handlePreXpChange(Object player, Object categoryConfig) {
        if (player instanceof ServerPlayerEntity spe) {
            Identifier categoryId = invokeIdentifier(categoryConfig, "id");
            if (categoryId != null) {
                if (SYNC_DEPTH.get() == 0) {
                    captureOldTotal(spe, categoryId);
                }
                SYNC_DEPTH.set(SYNC_DEPTH.get() + 1);
            }
        }
    }

    @Unique
    private void handlePostXpChange(Object player, Object categoryConfig) {
        if (player instanceof ServerPlayerEntity spe) {
            Identifier categoryId = invokeIdentifier(categoryConfig, "id");
            if (categoryId != null) {
                int newDepth = Math.max(0, SYNC_DEPTH.get() - 1);
                SYNC_DEPTH.set(newDepth);
                if (newDepth == 0) {
                    int oldTotal = OLD_TOTAL.get();
                    int newTotal = invokeGetExperience(this, spe, categoryId).orElse(0);
                    int delta = newTotal - oldTotal;
                    if (delta > 0) {
                        syncFromSkillsMod(spe, categoryId, delta);
                    } else if (delta == 0) {
                        // Sometimes XP doesn't change visually, but internal state might (level ups).
                        // Let's force a silent sync just in case internal levels changed, but no toast.
                        syncFromSkillsMod(spe, categoryId, 0);
                    }
                }
            }
        }
    }

    @Unique
    private void captureOldTotal(ServerPlayerEntity player, Identifier categoryId) {
        try {
            // Find the getExperience method with correct Mojang names for parameters via
            // reflection
            // Actually, we can just use this.getExperience(player, categoryId) if we cast
            // 'this' to SkillsMod or similar
            // But we don't have the class in classpath.
            Optional<Integer> oldXpOpt = invokeGetExperience(this, player, categoryId);
            OLD_TOTAL.set(oldXpOpt.orElse(0));
        } catch (Exception e) {
            OLD_TOTAL.set(0);
        }
    }

    @Unique
    private void syncFromSkillsMod(ServerPlayerEntity player, Identifier categoryId, int lastGain) {
        if (!EpicClassBridge.isEnabled()) {
            return;
        }

        try {
            // Get current Level and XP from Pufferfish via SkillsMod methods
            Optional<Integer> levelOpt = invokeGetCurrentLevel(this, player, categoryId);
            Optional<Integer> currentXpOpt = invokeGetCurrentExperience(this, player, categoryId);

            if (levelOpt.isPresent() && currentXpOpt.isPresent()) {
                int level = levelOpt.get();
                int currentXp = currentXpOpt.get();

                LOGGER.info("[Bridge] Pufferfish XP sync: Category=" + categoryId + ", Level=" + level + ", Gain="
                        + lastGain);

                // Sync to Epic Class
                EpicClassSyncHelper.syncFromPufferfish(player, level, currentXp, lastGain);
            }

        } catch (Exception e) {
            LOGGER.warn("[Bridge] Error in PufferfishExperienceMixin: " + e.getMessage());
        }
    }

    @Unique
    private Identifier invokeIdentifier(Object target, String methodName) {
        try {
            Object result = target.getClass().getMethod(methodName).invoke(target);
            if (result instanceof Identifier id) {
                return id;
            }
            // Fallback for Mojang ResourceLocation which will be a different class at
            // runtime but have same toString
            if (result != null) {
                return new Identifier(result.toString());
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    @Unique
    private Optional<Integer> invokeGetExperience(Object skillsMod, ServerPlayerEntity player, Identifier categoryId) {
        return invokeOptionalInt(skillsMod, "getExperience", player, categoryId);
    }

    @Unique
    private Optional<Integer> invokeGetCurrentLevel(Object skillsMod, ServerPlayerEntity player,
            Identifier categoryId) {
        return invokeOptionalInt(skillsMod, "getCurrentLevel", player, categoryId);
    }

    @Unique
    private Optional<Integer> invokeGetCurrentExperience(Object skillsMod, ServerPlayerEntity player,
            Identifier categoryId) {
        return invokeOptionalInt(skillsMod, "getCurrentExperience", player, categoryId);
    }

    @Unique
    private Optional<Integer> invokeOptionalInt(Object target, String methodName, ServerPlayerEntity player,
            Identifier categoryId) {
        try {
            for (java.lang.reflect.Method m : target.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == 2) {
                    // Method parameters are Mojang types at runtime, but we pass Yarn types.
                    // This works if they are the same underlying class.
                    Object result = m.invoke(target, player, categoryId);
                    if (result instanceof Optional<?> opt) {
                        return (Optional<Integer>) opt;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return Optional.empty();
    }
}
