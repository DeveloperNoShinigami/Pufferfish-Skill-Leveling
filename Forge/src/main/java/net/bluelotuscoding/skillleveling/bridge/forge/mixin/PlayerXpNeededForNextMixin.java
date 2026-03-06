package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * Mixin to make Epic Class read XP progression from Puffish instead of using
 * hardcoded formulas.
 * 
 * This intercepts Epic Class's xpNeededForNext(level) method and returns
 * Puffish's
 * XP requirement for that level from experience.json.
 * 
 * This is THE critical mixin for Option 3: Epic Class uses Puffish's
 * progression curve entirely.
 * 
 * Epic Class's native formula: level * 100
 * With this mixin: reads from Puffish's curves, arrays, or custom formulas
 */
@Mixin(targets = "com.example.epicclassmod.data.PlayerLevelData", remap = false, priority = 1000)
public abstract class PlayerXpNeededForNextMixin {

    private static boolean loggedOnce = false;

    /**
     * Injects into xpNeededForNext(int level) to return Puffish's XP requirement.
     * 
     * This replaces Epic Class's hardcoded XP formula with Puffish's configured
     * progression,
     * making Puffish the source of truth for leveling curves.
     * 
     * Since this is a static method with no player context, we use the first
     * configured category.
     */
    @Inject(method = "xpNeededForNext(I)I", at = @At("HEAD"), remap = false, cancellable = true)
    private static void onXpNeededForNext(int level, CallbackInfoReturnable<Integer> cir) {
        if (!EpicClassBridge.isEnabled()) {
            return; // Use Epic Class's default formula
        }
        if (!loggedOnce) {
            loggedOnce = true;
            SkillLevelingMod.getInstance().getLogger().info(
                    "[Bridge] PlayerXpNeededForNextMixin active (Epic Class xpNeededForNext intercepted)");
        }

        try {
            // Try to get XP requirement from any configured Puffish category
            var configuredCategories = EpicClassBridge.getConfiguredCategories();
            if (configuredCategories.isEmpty()) {
                return; // No categories configured, use Epic Class default
            }

            // Use the first configured category's progression formula
            Identifier firstCategory = configuredCategories.iterator().next();
            var category = SkillsAPI.getCategory(firstCategory);
            if (category.isEmpty()) {
                return;
            }

            var experience = category.get().getExperience();
            if (experience.isEmpty()) {
                return;
            }

            // Get XP needed for this level from Puffish.
            // Since we are now 0-based (Epic Level 0 = Pufferfish 0),
            // Level 0 requires experience.getRequired(0) to reach Level 1.
            int puffishXpNeeded = experience.get().getRequired(level);

            // If Puffish returns 0 or negative (max level reached), return
            // Integer.MAX_VALUE
            // This prevents Epic Class from leveling up further (redundant with maxLevel()
            // check)
            if (puffishXpNeeded <= 0) {
                cir.setReturnValue(Integer.MAX_VALUE);
                return;
            }

            cir.setReturnValue(puffishXpNeeded);

        } catch (Exception e) {
            // Let Epic Class use its default on error
        }
    }
}
