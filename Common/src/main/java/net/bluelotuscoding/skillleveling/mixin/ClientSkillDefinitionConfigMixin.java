package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage;
import net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.puffish.skillsmod.client.config.skill.ClientSkillDefinitionConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept ClientSkillDefinitionConfig.description() and
 * extraDescription()
 * to APPEND level-specific descriptions.
 * 
 * Tooltip structure (main description):
 * 1. Original description (from skill mod - optional)
 * 2. Level X/Y indicator + dynamic descriptions (our mod)
 * 3. "Hold Shift to see next level" hint (ALWAYS at bottom)
 * 
 * Extra description (shown when Shift is held):
 * - Original extra description (if any)
 * - Next level preview OR final message at max level
 */
@Mixin(value = ClientSkillDefinitionConfig.class, remap = false)
public class ClientSkillDefinitionConfigMixin {

    /**
     * Intercept description() at RETURN to APPEND our level info after original.
     */
    @Inject(method = "description", at = @At("RETURN"), cancellable = true)
    private void onGetDescriptionReturn(CallbackInfoReturnable<Text> cir) {
        try {
            ClientSkillDefinitionConfig self = (ClientSkillDefinitionConfig) (Object) this;
            String defId = self.id();

            if (!ClientDescriptionStorage.hasDescriptions(defId)) {
                return; // No custom descriptions, keep original
            }

            Text originalDesc = cir.getReturnValue();
            int currentLevel = ClientSkillLevelStorage.getLevelByDefinitionId(defId);
            int maxLevel = ClientDescriptionStorage.getMaxLevel(defId);

            MutableText result = Text.empty();

            // 1. Keep original description first (from skill mod)
            if (originalDesc != null && !originalDesc.getString().isEmpty()) {
                result.append(originalDesc);
                result.append(Text.literal("\n"));
            }

            // 2. Add level indicator + dynamic description
            if (currentLevel <= 0) {
                // Not unlocked - show level 0/X only (no preview in main desc)
                result.append(Text.literal("§7Level 0/" + maxLevel + "\n"));
            } else {
                // Unlocked - show current level
                String levelIndicator = "§6Level " + currentLevel + "/" + maxLevel;
                if (currentLevel >= maxLevel) {
                    levelIndicator += " §c§lMAX";
                }
                result.append(Text.literal(levelIndicator + "\n"));

                String customDesc = ClientDescriptionStorage.getDescription(defId, currentLevel);
                if (customDesc != null && !customDesc.isEmpty()) {
                    result.append(Text.literal(customDesc + "\n"));
                }
            }

            // 3. ALWAYS add "Hold Shift" hint at the end (unless at max level)
            if (currentLevel < maxLevel) {
                result.append(Text.literal("§7§oHold Shift to see more info"));
            }

            cir.setReturnValue(result);
        } catch (Exception e) {
            // On error, keep original
        }
    }

    /**
     * Intercept extraDescription() at RETURN to APPEND next level preview.
     * This shows ONLY when Shift is held.
     */
    @Inject(method = "extraDescription", at = @At("RETURN"), cancellable = true)
    private void onGetExtraDescriptionReturn(CallbackInfoReturnable<Text> cir) {
        try {
            ClientSkillDefinitionConfig self = (ClientSkillDefinitionConfig) (Object) this;
            String defId = self.id();

            if (!ClientDescriptionStorage.hasDescriptions(defId)) {
                return;
            }

            Text originalExtra = cir.getReturnValue();
            int currentLevel = ClientSkillLevelStorage.getLevelByDefinitionId(defId);
            int maxLevel = ClientDescriptionStorage.getMaxLevel(defId);

            MutableText result = Text.empty();

            // Keep original extra description first
            if (originalExtra != null && !originalExtra.getString().isEmpty()) {
                result.append(originalExtra);
                result.append(Text.literal("\n"));
            }

            // Get next level preview (NEVER merged - always single level)
            String nextLevelDesc = ClientDescriptionStorage.getExtraDescriptionSingle(defId, currentLevel);

            if (nextLevelDesc != null && !nextLevelDesc.isEmpty()) {
                if (currentLevel >= maxLevel) {
                    // At max level - show final message in purple
                    result.append(Text.literal("§d" + nextLevelDesc));
                } else if (currentLevel <= 0) {
                    // Not unlocked - show what level 1 gives
                    result.append(Text.literal("§aLevel 1:\n§f" + nextLevelDesc));
                } else {
                    // Show next level preview
                    result.append(Text.literal("§aLevel " + (currentLevel + 1) + ":\n§f" + nextLevelDesc));
                }
            }

            cir.setReturnValue(result);
        } catch (Exception e) {
            // On error, keep original
        }
    }
}
