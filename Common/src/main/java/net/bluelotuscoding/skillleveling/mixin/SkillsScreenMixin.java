package net.bluelotuscoding.skillleveling.mixin;

import net.minecraft.client.gui.DrawContext;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.config.ClientCategoryConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConnectionConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(value = net.puffish.skillsmod.client.gui.SkillsScreen.class, remap = false)
public abstract class SkillsScreenMixin {

    @Shadow
    private Optional<ClientCategoryData> optActiveCategoryData;

    /**
     * Filter skills to hide those marked as hidden when prerequisites aren't met.
     */
    @Redirect(method = "drawContentWithCategory", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"), remap = false)
    private Collection<ClientSkillConfig> addon$filterSkillsInDraw(
            Map<String, ClientSkillConfig> instance,
            DrawContext context, double mouseX, double mouseY, ClientCategoryData activeCategoryData) {
        if (activeCategoryData == null) {
            return instance.values();
        }

        String categoryId = activeCategoryData.getConfig().id().toString();

        return instance.values().stream()
                .filter(skill -> {
                    try {
                        // Check if skill is hidden and prerequisites aren't met
                        if (net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.isHidden(categoryId,
                                skill.id())) {
                            // Check if prerequisites are met by checking state
                            var state = activeCategoryData.getSkillState(skill);
                            // If hidden and state is LOCKED (prerequisites not met), filter it out
                            if (state == net.puffish.skillsmod.api.Skill.State.LOCKED) {
                                return false;
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        // On error, show the skill to avoid breaking the GUI
                        return true;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Filter normal connections to hide those involving hidden skills.
     */
    @Redirect(method = "drawContentWithCategory", at = @At(value = "INVOKE", target = "Lnet/puffish/skillsmod/client/config/ClientCategoryConfig;normalConnections()Ljava/util/Collection;"), remap = false)
    private Collection<ClientSkillConnectionConfig> addon$filterNormalConnections(
            ClientCategoryConfig category,
            DrawContext context, double mouseX, double mouseY, ClientCategoryData activeCategoryData) {
        if (activeCategoryData == null) {
            return category.normalConnections();
        }
        return category.normalConnections().stream()
                .filter(conn -> addon$isConnectionVisible(conn, category, activeCategoryData))
                .collect(Collectors.toList());
    }

    @Unique
    private boolean addon$isConnectionVisible(ClientSkillConnectionConfig conn, ClientCategoryConfig category,
            ClientCategoryData categoryData) {
        var skillA = category.skills().get(conn.skillAId());
        var skillB = category.skills().get(conn.skillBId());
        if (skillA == null || skillB == null) {
            return false;
        }

        String categoryId = category.id().toString();

        // Check if either skill will be filtered out (hidden with unmet prerequisites)
        // This prevents connections from revealing hidden skills
        try {
            // Check skill A
            if (net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.isHidden(categoryId, skillA.id())) {
                var stateA = categoryData.getSkillState(skillA);
                if (stateA == net.puffish.skillsmod.api.Skill.State.LOCKED) {
                    return false; // Skill A will be filtered - hide connection
                }
            }

            // Check skill B
            if (net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage.isHidden(categoryId, skillB.id())) {
                var stateB = categoryData.getSkillState(skillB);
                if (stateB == net.puffish.skillsmod.api.Skill.State.LOCKED) {
                    return false; // Skill B will be filtered - hide connection
                }
            }
        } catch (Exception e) {
            // On error, hide the connection to be safe
            return false;
        }

        return true;
    }
}
