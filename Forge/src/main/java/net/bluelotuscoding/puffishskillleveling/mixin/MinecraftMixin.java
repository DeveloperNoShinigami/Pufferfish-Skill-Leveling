package net.bluelotuscoding.puffishskillleveling.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.bluelotuscoding.puffishskillleveling.client.ForgeClientEventReceiver;
import net.bluelotuscoding.puffishskillleveling.client.ForgeKeyBindingReceiver;
import net.bluelotuscoding.puffishskillleveling.client.network.ForgeClientPacketSender;
import net.bluelotuscoding.puffishskillleveling.client.network.ForgeClientRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects the Skills client bootstrap into the Minecraft client so
 * that the addon can interact with the external Skills API.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void puffish_skill_leveling$init(GameConfig config, CallbackInfo ci) {
        SkillsClientMod.setup(
                new ForgeClientRegistrar(),
                new ForgeClientEventReceiver(),
                new ForgeKeyBindingReceiver(),
                new ForgeClientPacketSender()
        );
    }
}
