package net.bluelotuscoding.skillleveling.bridge.forge;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads ClassBookScreen mixins only when Epic
 * Class Mod is present.
 * This prevents crashes when the target mod is not installed.
 */
public class BridgeMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // Called when mixin plugin is loaded
        net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.info("Mixin plugin loaded");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("net.bluelotuscoding.skillleveling.bridge.forge.mixin")) {
            // Check for critical class instead of ModList to avoid null errors during early loading
            try {
                Class.forName("com.example.epicclassmod.EpicClassMod", false, this.getClass().getClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            } catch (Exception e) {
                return false;
            }
        }
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
