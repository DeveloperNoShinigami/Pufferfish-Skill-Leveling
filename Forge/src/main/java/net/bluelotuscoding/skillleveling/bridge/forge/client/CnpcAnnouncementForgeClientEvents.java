package net.bluelotuscoding.skillleveling.bridge.forge.client;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc.CnpcClientBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SkillLevelingMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CnpcAnnouncementForgeClientEvents {
    private CnpcAnnouncementForgeClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        CnpcClientBridge.renderGlobalAnnouncementOverlay(event.getGuiGraphics());
    }
}
