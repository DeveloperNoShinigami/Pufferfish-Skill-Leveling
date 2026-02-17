package net.bluelotuscoding.skillleveling.main;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;
import net.bluelotuscoding.skillleveling.registry.ForgeBlockRegistry;
import net.bluelotuscoding.skillleveling.registry.ForgeCreativeTabs;
import net.bluelotuscoding.skillleveling.registry.ForgeItemRegistry;
import net.bluelotuscoding.skillleveling.registry.ForgeVillagerRegistry;
import net.bluelotuscoding.skillleveling.registry.ForgeVillagerTrades;
import net.bluelotuscoding.skillleveling.registry.ModBlocks;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.bluelotuscoding.skillleveling.registry.ModVillagers;
import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.bluelotuscoding.skillleveling.forge.loot.LootInjectionHandler;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

@Mod(SkillLevelingMod.MOD_ID)
public class ForgeMain {
        public ForgeMain() {
                SkillLevelingMod.init(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().toFile());
                EpicClassBridgeForgeLoader.init();

                IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

                ForgeBlockRegistry.register(bus);
                ForgeItemRegistry.register(bus);
                ForgeCreativeTabs.register(bus);
                ForgeVillagerRegistry.register(bus);
                net.bluelotuscoding.skillleveling.registry.ForgeLootModifierRegistry.register(bus);

                net.bluelotuscoding.skillleveling.registry.ForgeLootFunctionRegistry.register(bus);

                bus.addListener(this::commonSetup);

                net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.init();
                SkillLevelingMod.getInstance()
                                .setNetworkHandler(new net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler());
                SkillLevelingMod.getInstance()
                                .setPlatform(new net.bluelotuscoding.skillleveling.forge.util.ForgePlatform());

                MinecraftForge.EVENT_BUS.register(this);
                MinecraftForge.EVENT_BUS.register(new ForgeVillagerTrades());
                MinecraftForge.EVENT_BUS.register(new LootInjectionHandler());
        }

        private void commonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
                initInternal();
        }

        @SubscribeEvent
        public void onRegisterCommands(RegisterCommandsEvent event) {
                SkillLevelingCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public void onAddReloadListener(AddReloadListenerEvent event) {
                event.addListener(SkillLevelingMod.getInstance().getTradeLoader());
                event.addListener(SkillLevelingMod.getInstance().getReputationLoader());
                event.addListener(SkillLevelingMod.getInstance().getLootImbueManager());
                event.addListener(SkillLevelingMod.getInstance().getUniversalLootHandler());
        }

        @SubscribeEvent
        public void onServerStarting(ServerStartingEvent event) {
                SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStarting(event.getServer());
        }

        @SubscribeEvent
        public void onServerStopped(ServerStoppedEvent event) {
                SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStopping(event.getServer());
        }

        @SubscribeEvent
        public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
                if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                        SkillLevelingMod.getInstance().getSkillLevelingManager().tick(event.getServer());
                }
        }

        // Loot injection is handled by UniversalLootModifier (Chests) and
        // LootInjectionHandler (Mobs).

        @Mod.EventBusSubscriber(modid = SkillLevelingMod.MOD_ID)
        public static class ForgeEvents {

                @SubscribeEvent
                public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
                        if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
                                if (!serverPlayer.getWorld().isClient) {
                                        SkillLevelingMod.getInstance().getSkillLevelingManager()
                                                        .syncAllSkillsToPlayer(serverPlayer);

                                        // Defer category lock initialization to next tick so it runs
                                        // AFTER Pufferfish's own updateAllCategories sync completes.
                                        var server = serverPlayer.getServer();
                                        if (server != null) {
                                                server.execute(() -> {
                                                        net.bluelotuscoding.skillleveling.manager.CategoryLockManager
                                                                        .initializeLocks(serverPlayer);
                                                });
                                        }
                                }
                        }
                }

                @SubscribeEvent
                public static void onEquipmentChange(
                                net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
                        if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
                                SkillLevelingMod.getInstance().getSkillLevelingManager()
                                                .refreshAllRewards(serverPlayer);
                        }
                }

                @SubscribeEvent
                public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
                        if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
                                SkillLevelingMod.getInstance().getSkillLevelingManager()
                                                .refreshAllRewards(serverPlayer);
                        }
                }

                // Entity drops are now handled by LootInjectionHandler (direct event).
        }

        public static void initInternal() {
                ModItems.SKILL_TOME = ForgeItemRegistry.SKILL_TOME.get();
                ModItems.TOME_OF_PROGRESSION = ForgeItemRegistry.TOME_OF_PROGRESSION.get();
                ModItems.TOME_OF_CLEAR_MIND = ForgeItemRegistry.TOME_OF_CLEAR_MIND.get();
                ModItems.TOME_OF_GREATER_CLEAR_MIND = ForgeItemRegistry.TOME_OF_GREATER_CLEAR_MIND.get();
                ModItems.TOME_OF_CLEANSING = ForgeItemRegistry.TOME_OF_CLEANSING.get();
                ModItems.TOME_OF_CLEANSING_2 = ForgeItemRegistry.TOME_OF_CLEANSING_2.get();
                ModItems.TOME_OF_CLEANSING_3 = ForgeItemRegistry.TOME_OF_CLEANSING_3.get();
                ModItems.SIGIL_OF_IMBUEMENT = ForgeItemRegistry.SIGIL_OF_IMBUEMENT.get();
                ModItems.BLANK_TOME = ForgeItemRegistry.BLANK_TOME.get();
                ModItems.SKILL_SCRIBE_TABLE_ITEM = ForgeItemRegistry.SKILL_SCRIBE_TABLE_ITEM.get();
                ModItems.SKILL_CHARM = ForgeItemRegistry.SKILL_CHARM.get();

                ModBlocks.SKILL_SCRIBE_TABLE = ForgeBlockRegistry.SKILL_SCRIBE_TABLE.get();
                ModVillagers.SKILL_MASTER = ForgeVillagerRegistry.SKILL_MASTER.get();

                if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
                        SkillLevelingMod.getInstance().setEquipmentScanner(
                                        new net.bluelotuscoding.skillleveling.forge.integration.CuriosScanner());
                        MinecraftForge.EVENT_BUS.register(
                                        new net.bluelotuscoding.skillleveling.forge.integration.CuriosIntegration());
                        top.theillusivec4.curios.api.CuriosApi.registerCurio(ModItems.SKILL_CHARM,
                                        new net.bluelotuscoding.skillleveling.forge.integration.CurioItemImpl());
                }
        }
}
