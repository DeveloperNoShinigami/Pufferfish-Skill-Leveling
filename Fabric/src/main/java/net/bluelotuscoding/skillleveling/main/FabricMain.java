package net.bluelotuscoding.skillleveling.main;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.bluelotuscoding.skillleveling.registry.ModBlocks;
import net.bluelotuscoding.skillleveling.registry.ModVillagers;
import net.bluelotuscoding.skillleveling.registry.FabricCreativeTabs;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;
import net.bluelotuscoding.skillleveling.data.SkillMasterTradeProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.fabricmc.fabric.api.object.builder.v1.villager.VillagerProfessionBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.resource.ResourceManager;
import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementsManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FabricMain implements ModInitializer {

        @Override
        public void onInitialize() {
                // Initialize common addon logic
                SkillLevelingMod.init(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile());

                // Initialize networking
                net.bluelotuscoding.skillleveling.network.FabricNetworkHandler.init();
                SkillLevelingMod.getInstance().setNetworkHandler(
                                new net.bluelotuscoding.skillleveling.network.FabricNetworkHandler());
                SkillLevelingMod.getInstance()
                                .setPlatform(new net.bluelotuscoding.skillleveling.fabric.util.FabricPlatform());

                // Register Loot Functions
                net.bluelotuscoding.skillleveling.registry.FabricLootFunctionRegistry.register();

                // Register items
                ModItems.TOME_OF_PROGRESSION = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_PROGRESSION_ID,
                                ModItems.createTomeOfProgression());

                ModItems.TOME_OF_CLEAR_MIND = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_CLEAR_MIND_ID,
                                ModItems.createTomeOfClearMind());

                ModItems.TOME_OF_GREATER_CLEAR_MIND = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_GREATER_CLEAR_MIND_ID,
                                ModItems.createTomeOfGreaterClearMind());

                ModItems.SKILL_TOME = Registry.register(
                                Registries.ITEM,
                                ModItems.SKILL_TOME_ID,
                                ModItems.createSkillTome());

                ModItems.SIGIL_OF_IMBUEMENT = Registry.register(
                                Registries.ITEM,
                                ModItems.SIGIL_OF_IMBUEMENT_ID,
                                ModItems.createSigilOfImbuement());

                ModItems.TOME_OF_CLEANSING = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_OF_CLEANSING_ID,
                                ModItems.createTomeOfCleansing());

                ModItems.TOME_OF_CLEANSING_2 = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_OF_CLEANSING_2_ID,
                                ModItems.createTomeOfCleansing2());

                ModItems.TOME_OF_CLEANSING_3 = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_OF_CLEANSING_3_ID,
                                ModItems.createTomeOfCleansing3());

                ModItems.BLANK_TOME = Registry.register(
                                Registries.ITEM,
                                ModItems.BLANK_TOME_ID,
                                ModItems.createBlankTome());

                // Register Block
                ModBlocks.SKILL_SCRIBE_TABLE = Registry.register(
                                Registries.BLOCK,
                                ModBlocks.SKILL_SCRIBE_TABLE_ID,
                                ModBlocks.createSkillScribeTable());

                // Register Block Item
                ModItems.SKILL_SCRIBE_TABLE_ITEM = Registry.register(
                                Registries.ITEM,
                                ModBlocks.SKILL_SCRIBE_TABLE_ID,
                                new net.minecraft.item.BlockItem(ModBlocks.SKILL_SCRIBE_TABLE,
                                                new Item.Settings().rarity(Rarity.RARE)));

                // Register POI
                @SuppressWarnings("unused")
                PointOfInterestType skillMasterPoi = PointOfInterestHelper.register(
                                ModVillagers.SKILL_MASTER_ID, 1, 1, ModBlocks.SKILL_SCRIBE_TABLE);

                // Register Profession
                ModVillagers.SKILL_MASTER = Registry.register(
                                Registries.VILLAGER_PROFESSION,
                                ModVillagers.SKILL_MASTER_ID,
                                VillagerProfessionBuilder.create()
                                                .id(ModVillagers.SKILL_MASTER_ID)
                                                .workstation(ModVillagers.SKILL_SCRIBE_TABLE_POI_KEY)
                                                .workSound(net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_WORK_CARTOGRAPHER)
                                                .build());

                // Register Mob Drops
                ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
                        // Legacy loot drops removed
                });

                // Register Loot Injection
                LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
                        if (source.isBuiltin()) {
                                // Apply universal loot injection (Charms, Tomes, etc)
                                tableBuilder.modifyPools(poolBuilder -> {
                                        // This is a bit tricky with tableBuilder. Fabric usually wants modifications
                                        // to pools.
                                });

                                // We'll use a hack if needed, or better, use the applyRandomImbue logic.
                                // Actually, for Fabric, we can use tableBuilder.modifyPools but it's hard to
                                // add NEW pools derived from our JSON.

                                // PROPER WAY: Listen to the list of items being generated.
                                // But Fabric modify event happens BEFORE generation.

                                // For now, let's just make sure the Imbue function is applied.
                                SkillLevelingMod.getInstance().getUniversalLootHandler().injectLoot(id, tableBuilder);
                                tableBuilder.apply(net.bluelotuscoding.skillleveling.loot.SkillImbueLootFunction
                                                .builder());
                        }
                });

                // Register Proxy Trade (Required by Minecraft for career adoption)
                net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper.registerVillagerOffers(
                                ModVillagers.SKILL_MASTER, 1, factories -> {
                                        factories.add((entity, random) -> SkillMasterTradeProvider
                                                        .createLevel1ProxyTrade(null, null));
                                });

                // Register groups
                FabricCreativeTabs.register();

                // Register commands
                CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                        SkillLevelingCommand.register(dispatcher);
                });

                // Register Resource Reload Listener
                ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                                .registerReloadListener(new IdentifiableResourceReloadListener() {
                                        @Override
                                        public Identifier getFabricId() {
                                                return SkillLevelingMod.createIdentifier("skill_master_trades");
                                        }

                                        @Override
                                        public CompletableFuture<Void> reload(
                                                        ResourceReloader.Synchronizer synchronizer,
                                                        ResourceManager manager,
                                                        Profiler prepareProfiler,
                                                        Profiler applyProfiler, Executor prepareExecutor,
                                                        Executor applyExecutor) {
                                                return SkillLevelingMod.getInstance().getTradeLoader().reload(
                                                                synchronizer, manager,
                                                                prepareProfiler, applyProfiler, prepareExecutor,
                                                                applyExecutor);
                                        }
                                });

                ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                                .registerReloadListener(new IdentifiableResourceReloadListener() {
                                        @Override
                                        public Identifier getFabricId() {
                                                return SkillLevelingMod.createIdentifier("skill_master_reputation");
                                        }

                                        @Override
                                        public CompletableFuture<Void> reload(
                                                        ResourceReloader.Synchronizer synchronizer,
                                                        ResourceManager manager,
                                                        Profiler prepareProfiler,
                                                        Profiler applyProfiler, Executor prepareExecutor,
                                                        Executor applyExecutor) {
                                                return SkillLevelingMod.getInstance().getReputationLoader().reload(
                                                                synchronizer, manager,
                                                                prepareProfiler, applyProfiler, prepareExecutor,
                                                                applyExecutor);
                                        }
                                });

                ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                                .registerReloadListener(new IdentifiableResourceReloadListener() {
                                        @Override
                                        public Identifier getFabricId() {
                                                return SkillLevelingMod.createIdentifier("global_loot_configuration");
                                        }

                                        @Override
                                        public CompletableFuture<Void> reload(
                                                        ResourceReloader.Synchronizer synchronizer,
                                                        ResourceManager manager,
                                                        Profiler prepareProfiler,
                                                        Profiler applyProfiler, Executor prepareExecutor,
                                                        Executor applyExecutor) {
                                                // GlobalLootConfigLoader removed
                                                return CompletableFuture.completedFuture(null);
                                        }
                                });

                ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                                .registerReloadListener(new IdentifiableResourceReloadListener() {
                                        @Override
                                        public Identifier getFabricId() {
                                                return SkillLevelingMod.createIdentifier("skill_imbue_loot");
                                        }

                                        @Override
                                        public CompletableFuture<Void> reload(
                                                        ResourceReloader.Synchronizer synchronizer,
                                                        ResourceManager manager,
                                                        Profiler prepareProfiler,
                                                        Profiler applyProfiler, Executor prepareExecutor,
                                                        Executor applyExecutor) {
                                                return SkillLevelingMod.getInstance().getLootImbueManager()
                                                                .reload(
                                                                                synchronizer, manager,
                                                                                prepareProfiler, applyProfiler,
                                                                                prepareExecutor,
                                                                                applyExecutor);
                                        }
                                });

                ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                                .registerReloadListener(new IdentifiableResourceReloadListener() {
                                        @Override
                                        public Identifier getFabricId() {
                                                return SkillLevelingMod.createIdentifier("item_restrictions");
                                        }

                                        @Override
                                        public CompletableFuture<Void> reload(
                                                        ResourceReloader.Synchronizer synchronizer,
                                                        ResourceManager manager,
                                                        Profiler prepareProfiler,
                                                        Profiler applyProfiler, Executor prepareExecutor,
                                                        Executor applyExecutor) {
                                                return SkillLevelingMod.getInstance().getItemRequirementsManager()
                                                                .reload(synchronizer, manager,
                                                                                prepareProfiler, applyProfiler,
                                                                                prepareExecutor,
                                                                                applyExecutor);
                                        }
                                });

                net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
                        SkillLevelingMod.getInstance().getSkillLevelingManager().tick(server);
                });

                ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
                        SkillLevelingMod.getInstance().getSkillLevelingManager()
                                        .refreshAllRewards(newPlayer);
                });

                // Player join: initialize category locks after Pufferfish syncs
                net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN
                                .register((handler, sender, server) -> {
                                        var player = handler.getPlayer();
                                        // Defer category lock initialization...
                                        // AFTER Pufferfish's own updateAllCategories sync completes.
                                        server.execute(() -> {
                                                // Sync item restrictions to client
                                                var networkHandler = SkillLevelingMod.getInstance().getNetworkHandler();
                                                if (networkHandler != null) {
                                                        networkHandler.sendToPlayer(
                                                                        new net.bluelotuscoding.skillleveling.network.SyncItemRestrictionsPacket(
                                                                                        ItemRequirementsManager
                                                                                                        .getItemRequirements(),
                                                                                        ItemRequirementsManager
                                                                                                        .getBlockRequirements(),
                                                                                        ItemRequirementsManager
                                                                                                        .getEntityRequirements(),
                                                                                        ItemRequirementsManager
                                                                                                        .getDimensionRequirements(),
                                                                                        ItemRequirementsManager
                                                                                                        .getStructureRequirements()),
                                                                        player);
                                                }

                                                net.bluelotuscoding.skillleveling.manager.CategoryLockManager
                                                                .initializeLocks(player);
                                        });
                                });
        }
}
