package net.bluelotuscoding.skillleveling.bridge.forge.cnpc;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcNpcRoleInfo;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcNpcRoleResolver;
import net.bluelotuscoding.skillleveling.bridge.cnpc.runtime.CnpcRuntimeBridge;
import net.bluelotuscoding.skillleveling.bridge.network.SyncCnpcNpcRolePacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SkillLevelingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CnpcQuestForgeEvents {
    private CnpcQuestForgeEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) {
            return;
        }
        CnpcNpcRoleInfo role = CnpcNpcRoleResolver.resolve(event.getTarget());
        if (role.hasAnyRole() && SkillLevelingMod.getInstance().getNetworkHandler() != null) {
            SkillLevelingMod.getInstance().getNetworkHandler().sendToPlayer(
                    new SyncCnpcNpcRolePacket(event.getTarget().getId(), role.getJobMasterClassId(),
                            role.getQuestNpcRoleId()),
                    player);
        }
        CnpcRuntimeBridge.refreshIfRelevantInteraction(player, event.getTarget());
    }
}
