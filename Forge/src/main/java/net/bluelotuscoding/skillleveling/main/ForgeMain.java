package net.bluelotuscoding.skillleveling.main;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;
import net.bluelotuscoding.skillleveling.registry.ForgeBlockRegistry;
import net.bluelotuscoding.skillleveling.registry.ForgeItemRegistry;
import net.bluelotuscoding.skillleveling.registry.ForgeVillagerRegistry;
import net.bluelotuscoding.skillleveling.registry.ForgeVillagerTrades;
import net.bluelotuscoding.skillleveling.registry.ModBlocks;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.bluelotuscoding.skillleveling.registry.ModVillagers;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge main class for the Skill Leveling addon
 */
@Mod(SkillLevelingMod.MOD_ID)
public class ForgeMain {

        public ForgeMain() {
                IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

                // Initialize common addon logic
                SkillLevelingMod.init();

                // Register through dedicated registry classes
                ForgeBlockRegistry.register(modEventBus);
                ForgeItemRegistry.register(modEventBus);
                ForgeVillagerRegistry.register(modEventBus);

                modEventBus.addListener(this::setup);
                MinecraftForge.EVENT_BUS.register(this);
                MinecraftForge.EVENT_BUS.register(new ForgeVillagerTrades());
        }

        private void setup(FMLCommonSetupEvent event) {
                // Populate common registry instances from Forge RegistryObjects
                ModItems.TOME_OF_PROGRESSION = ForgeItemRegistry.TOME_OF_PROGRESSION.get();
                ModItems.TOME_OF_CLEAR_MIND = ForgeItemRegistry.TOME_OF_CLEAR_MIND.get();
                ModItems.TOME_OF_GREATER_CLEAR_MIND = ForgeItemRegistry.TOME_OF_GREATER_CLEAR_MIND.get();
                ModItems.SKILL_TOME = ForgeItemRegistry.SKILL_TOME.get();
                ModItems.SIGIL_OF_IMBUEMENT = ForgeItemRegistry.SIGIL_OF_IMBUEMENT.get();
                ModItems.TOME_OF_CLEANSING = ForgeItemRegistry.TOME_OF_CLEANSING.get();
                ModItems.TOME_OF_CLEANSING_2 = ForgeItemRegistry.TOME_OF_CLEANSING_2.get();
                ModItems.TOME_OF_CLEANSING_3 = ForgeItemRegistry.TOME_OF_CLEANSING_3.get();

                ModBlocks.SKILL_SCRIBE_TABLE = (net.bluelotuscoding.skillleveling.block.SkillScribeTableBlock) ForgeBlockRegistry.SKILL_SCRIBE_TABLE
                                .get();
                ModItems.SKILL_SCRIBE_TABLE_ITEM = ForgeItemRegistry.SKILL_SCRIBE_TABLE_ITEM.get();
                ModVillagers.SKILL_MASTER = ForgeVillagerRegistry.SKILL_MASTER.get();

                // Initialize and register the Forge network channel
                net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.init();

                // Set the network handler instance on the addon so manager sync calls use it
                try {
                        SkillLevelingMod.getInstance().setNetworkHandler(
                                        new net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler());
                } catch (Exception e) {
                        SkillLevelingMod.getInstance().getLogger()
                                        .error("Failed to set Forge network handler: " + e.getMessage());
                }
        }

        @Mod.EventBusSubscriber(modid = SkillLevelingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
        public static class ForgeEvents {
                @SubscribeEvent
                public static void onRegisterCommands(RegisterCommandsEvent event) {
                        SkillLevelingCommand.register(event.getDispatcher());
                }

                @SubscribeEvent
                public static void onAddReloadListener(AddReloadListenerEvent event) {
                        event.addListener(SkillLevelingMod.getInstance().getTradeLoader());
                }

                @SubscribeEvent
                public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
                        if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
                                SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerJoin(serverPlayer);
                        }
                }

                @SubscribeEvent
                public static void onServerStarting(ServerStartingEvent event) {
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStarting(event.getServer());
                }

                @SubscribeEvent
                public static void onServerStopping(ServerStoppingEvent event) {
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStopping(event.getServer());
                }

                @SubscribeEvent
                public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
                        if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
                                ItemStack from = event.getFrom();
                                ItemStack to = event.getTo();

                                boolean fromImbued = from.hasNbt()
                                                && from.getNbt().contains("SkillLevelingImbued", 10);
                                boolean toImbued = to.hasNbt()
                                                && to.getNbt().contains("SkillLevelingImbued", 10);

                                if (fromImbued || toImbued) {
                                        SkillLevelingMod.getInstance().getSkillLevelingManager()
                                                        .refreshAllRewards(serverPlayer);
                                        SkillLevelingMod.getInstance().getSkillLevelingManager()
                                                        .syncAllSkillsToPlayer(serverPlayer);
                                }
                        }
                }
        }
}
