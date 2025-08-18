package net.bluelotuscoding.skillleveling.main;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Forge main class for the Skill Leveling addon
 */
@Mod(SkillLevelingMod.MOD_ID)
public class ForgeMain {

	public ForgeMain() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		MinecraftForge.EVENT_BUS.register(this);
	}

        private void setup(FMLCommonSetupEvent event) {
                SkillLevelingMod.init();
        }

        @SubscribeEvent
        public void onCommandsRegister(RegisterCommandsEvent event) {
                SkillLevelingCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public void onServerStarting(ServerStartingEvent event) {
                SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStarting(event.getServer());
        }

        @SubscribeEvent
        public void onServerStopping(ServerStoppingEvent event) {
                SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStopping(event.getServer());
        }

        @SubscribeEvent
        public void onPlayerJoin(PlayerLoggedInEvent event) {
                if (event.getEntity() instanceof ServerPlayerEntity player) {
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerJoin(player);
                }
        }

        @SubscribeEvent
        public void onPlayerLeave(PlayerLoggedOutEvent event) {
                if (event.getEntity() instanceof ServerPlayerEntity player) {
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerLeave(player);
                }
        }
}