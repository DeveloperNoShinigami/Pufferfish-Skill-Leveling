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

            Text originalDesc = cir.getReturnValue();
            int baseLevel = ClientSkillLevelStorage.getLevelByDefinitionId(defId);
            int totalLevel = ClientSkillLevelStorage.getTotalLevelByDefinitionId(defId);
            int bonusLevel = totalLevel - baseLevel;
            int currentLevel = totalLevel;
            int maxLevel = Math.max(ClientDescriptionStorage.getMaxLevel(defId),
                    ClientSkillLevelStorage.getMaxLevelByDefinitionId(defId));

            boolean isToggle = ClientSkillLevelStorage.isToggleByDefinitionId(defId);

            // If we have no level info and no descriptions, keep original
            // FIX: Allow toggle skills to show "READY" status even at level 0.
            if (currentLevel <= 0 && !ClientDescriptionStorage.hasDescriptions(defId) && maxLevel <= 1
                    && !isToggle) {
                return;
            }

            // DEBUG: Log if we are in a toggle skill
            if (isToggle) {
                // net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(Text.literal("§d[Tooltip]
                // defId: " + defId + " Found Config: " + (config != null)), false);
            }

            MutableText result = Text.empty();

            // 1. Keep original description first (from skill mod)
            if (originalDesc != null && !originalDesc.getString().isEmpty()) {
                result.append(originalDesc);
                result.append(Text.literal("\n"));
            }

            // 2. Add toggle status if applicable
            if (isToggle) {
                int cooldown = ClientSkillLevelStorage.getRemainingCooldownSecondsByDefinitionId(defId);
                boolean isToggledOn = ClientSkillLevelStorage.isToggledOnByDefinitionId(defId);

                if (isToggledOn) {
                    result.append(Text.literal("§a§lENABLED"));
                    result.append(Text.literal(" §7(Click to Disable)\n"));
                } else {
                    // Check if toggle is disabled due to missing requirements (level <= 0 for
                    // loot/learned)
                    String lootMode = ClientDescriptionStorage.getLootMode(defId);
                    if (lootMode.isEmpty()) {
                        lootMode = ClientSkillLevelStorage.getLootModeByDefinitionId(defId);
                    }
                    boolean isLootLearned = lootMode != null && !lootMode.isEmpty();

                    if (isLootLearned && currentLevel <= 0) {
                        result.append(Text.literal("§c§lDISABLED"));
                        if ("imbue_only".equals(lootMode)) {
                            result.append(Text.literal(" §7(Equip item to use)\n"));
                        } else if ("tome_only".equals(lootMode)) {
                            result.append(Text.literal(" §7(Read tome to learn)\n"));
                        } else if ("both".equals(lootMode)) {
                            result.append(Text.literal(" §7(Equip or Learn to use)\n"));
                        } else {
                            result.append(Text.literal(" §7(Locked)\n"));
                        }
                    } else if (cooldown > 0) {
                        result.append(Text.literal("§c§lON COOLDOWN"));
                        result.append(Text.literal(" §7(" + cooldown + "s remaining)\n"));
                    } else {
                        result.append(Text.literal("§7§lREADY"));
                        result.append(Text.literal(" §7(Click to Enable)\n"));
                    }
                }
            }

            // 3. Add level indicator (Base + Bonus)
            // ONLY show if maxLevel > 1 (leveled skill)
            if (maxLevel > 1) {
                if (currentLevel <= 0) {
                    // Not unlocked - show level 0/X only
                    result.append(Text.literal("§7Base Level: 0/" + maxLevel + "\n"));
                } else {
                    // Unlocked - show Base vs Bonus breakdown
                    // COLOR FIX: Use Gold only for MAX level, Yellow/White for partial
                    String color = (currentLevel >= maxLevel) ? "§6" : "§e";
                    MutableText levelText = Text.literal(color + "Level " + currentLevel + "/" + maxLevel);
                    if (currentLevel >= maxLevel && maxLevel > 0) {
                        levelText.append(Text.literal(" §c§lMAX"));
                    }
                    result.append(levelText.append(Text.literal("\n")));

                    // Show breakdown line ONLY for imbue-only skills
                    if (ClientDescriptionStorage.isImbueOnly(defId)) {
                        MutableText breakdown = Text.literal("§8(Base: " + baseLevel);
                        if (bonusLevel > 0) {
                            breakdown.append(Text.literal(" §b+ " + bonusLevel + " Equipment"));
                        }
                        breakdown.append(Text.literal(")"));
                        result.append(breakdown.append(Text.literal("\n")));
                    }

                    String customDesc = ClientDescriptionStorage.getDescription(defId, currentLevel);
                    if (customDesc != null && !customDesc.isEmpty()) {
                        result.append(Text.literal(customDesc + "\n"));
                    }
                }
            } else if (ClientDescriptionStorage.hasDescriptions(defId)) {
                // Fallback for single-level skills that might have custom descriptions
                String customDesc = ClientDescriptionStorage.getDescription(defId, currentLevel);
                if (customDesc != null && !customDesc.isEmpty()) {
                    result.append(Text.literal(customDesc + "\n"));
                }
            }

            // 3. Only add "Hold Shift" hint if additional extra descriptions exist
            // for this definition. Avoid showing the hint when no extra data is
            // available.
            String nextLevelPreview = ClientDescriptionStorage.getExtraDescriptionSingle(defId, currentLevel);
            if (nextLevelPreview != null && !nextLevelPreview.isEmpty() && currentLevel < maxLevel) {
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
            int totalLevel = ClientSkillLevelStorage.getTotalLevelByDefinitionId(defId);
            int currentLevel = totalLevel;

            int maxLevel = Math.max(ClientDescriptionStorage.getMaxLevel(defId),
                    ClientSkillLevelStorage.getMaxLevelByDefinitionId(defId));

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
