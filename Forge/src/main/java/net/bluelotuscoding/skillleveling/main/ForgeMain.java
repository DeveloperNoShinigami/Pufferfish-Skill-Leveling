package net.bluelotuscoding.skillleveling.main;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;
import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Forge main class for the Skill Leveling addon
 */
@Mod(SkillLevelingMod.MOD_ID)
public class ForgeMain {

        // Deferred Register for items
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
                        ForgeRegistries.ITEMS, SkillLevelingMod.MOD_ID);

        // Register Tome items (must be before creative tab)
        public static final RegistryObject<TomeItem> TOME_OF_PROGRESSION = ITEMS.register(
                        "tome_of_progression", ModItems::createTomeOfProgression);
        public static final RegistryObject<TomeItem> TOME_OF_CLEAR_MIND = ITEMS.register(
                        "tome_of_clear_mind", ModItems::createTomeOfClearMind);
        public static final RegistryObject<TomeItem> TOME_OF_GREATER_CLEAR_MIND = ITEMS.register(
                        "tome_of_greater_clear_mind", ModItems::createTomeOfGreaterClearMind);

        // Deferred Register for creative tabs
        public static final DeferredRegister<ItemGroup> CREATIVE_TABS = DeferredRegister.create(
                        Registries.ITEM_GROUP.getKey(), SkillLevelingMod.MOD_ID);

        // Our custom creative tab
        public static final RegistryObject<ItemGroup> SKILL_LEVELING_TAB = CREATIVE_TABS.register(
                        "skill_leveling_tab",
                        () -> ItemGroup.builder()
                                        .icon(() -> new ItemStack(TOME_OF_PROGRESSION.get()))
                                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling"))
                                        .entries((displayContext, entries) -> {
                                                entries.add(TOME_OF_PROGRESSION.get());
                                                entries.add(TOME_OF_CLEAR_MIND.get());
                                                entries.add(TOME_OF_GREATER_CLEAR_MIND.get());
                                        })
                                        .build());

        public ForgeMain() {
                IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

                // Register items and creative tabs
                ITEMS.register(modEventBus);
                CREATIVE_TABS.register(modEventBus);

                modEventBus.addListener(this::setup);
                MinecraftForge.EVENT_BUS.register(this);
        }

        private void setup(FMLCommonSetupEvent event) {
                net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.init();
                SkillLevelingMod.init();
                SkillLevelingMod.getInstance()
                                .setNetworkHandler(new net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler());

                // Populate ModItems references
                event.enqueueWork(() -> {
                        ModItems.TOME_OF_PROGRESSION = TOME_OF_PROGRESSION.get();
                        ModItems.TOME_OF_CLEAR_MIND = TOME_OF_CLEAR_MIND.get();
                        ModItems.TOME_OF_GREATER_CLEAR_MIND = TOME_OF_GREATER_CLEAR_MIND.get();
                });
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
        public void onAddReloadListener(AddReloadListenerEvent event) {
                // Hook into datapack reload to re-load addon configurations after Pufferfish
                // loads
                event.addListener(
                                (net.minecraft.resource.SynchronousResourceReloader) resourceManager -> {
                                        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
                                        var server = manager.getServer().orElse(null);
                                        if (server != null) {
                                                manager.onServerReload(server);
                                        }
                                });
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
