package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class PuffishForgeMixinPlugin implements IMixinConfigPlugin {
    private static final Set<String> CNPC_MIXINS = Set.of(
            "net.bluelotuscoding.skillleveling.bridge.forge.mixin.PacketAchievementMixin",
            "net.bluelotuscoding.skillleveling.bridge.forge.mixin.PacketChatMixin",
            "net.bluelotuscoding.skillleveling.bridge.forge.mixin.PacketDialogMixin",
            "net.bluelotuscoding.skillleveling.bridge.forge.mixin.PacketQuestCompletionMixin");

    private static final String CNPC_API_CLASS = "noppes.npcs.api.NpcAPI";

    private boolean cnpcAvailable;

    @Override
    public void onLoad(String mixinPackage) {
        this.cnpcAvailable = isClassPresent(CNPC_API_CLASS);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (CNPC_MIXINS.contains(mixinClassName)) {
            return cnpcAvailable;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, PuffishForgeMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
