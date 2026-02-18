package net.bluelotuscoding.skillleveling.bridge.forge;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraftforge.fml.ModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads ClassBookScreen mixins only when Epic Class Mod is present.
 * This prevents crashes when the target mod is not installed.
 */
public class BridgeMixinPlugin implements IMixinConfigPlugin {
    private static final String EPICCLASSMOD_ID = "epicclassmod";
    
    @Override
    public void onLoad(String mixinPackage) {
        // Called when mixin plugin is loaded
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only apply ClassBookScreen mixin if Epic Class Mod is loaded
        if (mixinClassName.contains("ClassBookScreenMixin")) {
            // Defer ModList check to avoid potential null issues during early init
            try {
                boolean isLoaded = ModList.get() != null && ModList.get().isLoaded(EPICCLASSMOD_ID);
                if (!isLoaded) {
                    SkillLevelingMod.getInstance().getLogger().debug(
                        "Skipping mixin " + mixinClassName + " because " + EPICCLASSMOD_ID + " is not loaded"
                    );
                }
                return isLoaded;
            } catch (Exception e) {
                // If ModList isn't ready, assume we should load (will fail safely later if mod missing)
                SkillLevelingMod.getInstance().getLogger().warn(
                    "Could not check mod list for mixin " + mixinClassName + ", allowing it to load"
                );
                return true;
            }
        }
        
        // Apply all other mixins
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No action needed
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No action needed
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No action needed
    }
}
