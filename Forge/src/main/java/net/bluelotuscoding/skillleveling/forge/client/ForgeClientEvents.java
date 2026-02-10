package net.bluelotuscoding.skillleveling.forge.client;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.client.MasteryKeybinds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SkillLevelingMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeClientEvents {

    @SubscribeEvent
    public static void registerKeybinds(RegisterKeyMappingsEvent event) {
        MasteryKeybinds.init();
        for (var key : MasteryKeybinds.KEYBINDINGS) {
            event.register(key);
        }
    }

    @Mod.EventBusSubscriber(modid = SkillLevelingMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeClientTick {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                MasteryKeybinds.onClientTick();
            }
        }
    }
}
