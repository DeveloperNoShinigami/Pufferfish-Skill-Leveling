package net.bluelotuscoding.skillleveling.main;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.item.SigilOfImbuementItem;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.bluelotuscoding.skillleveling.item.TomeOfCleansingItem;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
        public static final RegistryObject<SkillTomeItem> SKILL_TOME = ITEMS.register(
                        "skill_tome", ModItems::createSkillTome);
        public static final RegistryObject<SigilOfImbuementItem> SIGIL_OF_IMBUEMENT = ITEMS.register(
                        "sigil_of_imbuement", ModItems::createSigilOfImbuement);
        public static final RegistryObject<TomeOfCleansingItem> TOME_OF_CLEANSING = ITEMS.register(
                        "tome_of_cleansing", ModItems::createTomeOfCleansing);
        public static final RegistryObject<TomeOfCleansingItem> TOME_OF_CLEANSING_2 = ITEMS.register(
                        "tome_of_cleansing_2", ModItems::createTomeOfCleansing2);
        public static final RegistryObject<TomeOfCleansingItem> TOME_OF_CLEANSING_3 = ITEMS.register(
                        "tome_of_cleansing_3", ModItems::createTomeOfCleansing3);

        // Deferred Register for creative tabs
        public static final DeferredRegister<ItemGroup> CREATIVE_TABS = DeferredRegister.create(
                        Registries.ITEM_GROUP.getKey(), SkillLevelingMod.MOD_ID);

        // Base Tomes Tab
        public static final RegistryObject<ItemGroup> BASE_TOMES_TAB = CREATIVE_TABS.register(
                        "base_tomes_tab",
                        () -> ItemGroup.builder()
                                        .icon(() -> new ItemStack(TOME_OF_PROGRESSION.get()))
                                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling_base"))
                                        .entries((displayContext, entries) -> {
                                                ModItems.fillBaseTomesTab(entries::add);
                                        })
                                        .build());

        // Skill Tomes Tab
        public static final RegistryObject<ItemGroup> SKILL_TOMES_TAB = CREATIVE_TABS.register(
                        "skill_tomes_tab",
                        () -> ItemGroup.builder()
                                        .withTabsBefore(BASE_TOMES_TAB.getId())
                                        .icon(() -> new ItemStack(SKILL_TOME.get()))
                                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling_tomes"))
                                        .entries((displayContext, entries) -> {
                                                ModItems.fillSkillTomesTab(entries::add);
                                        })
                                        .build());

        public ForgeMain() {
                IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

                // Initialize common addon logic
                SkillLevelingMod.init();

                // Register items and creative tabs
                ITEMS.register(modEventBus);
                CREATIVE_TABS.register(modEventBus);

                modEventBus.addListener(this::setup);
                MinecraftForge.EVENT_BUS.register(this);
        }

        private void setup(FMLCommonSetupEvent event) {
                ModItems.TOME_OF_PROGRESSION = TOME_OF_PROGRESSION.get();
                ModItems.TOME_OF_CLEAR_MIND = TOME_OF_CLEAR_MIND.get();
                ModItems.TOME_OF_GREATER_CLEAR_MIND = TOME_OF_GREATER_CLEAR_MIND.get();
                ModItems.SKILL_TOME = SKILL_TOME.get();
                ModItems.SIGIL_OF_IMBUEMENT = SIGIL_OF_IMBUEMENT.get();
                ModItems.TOME_OF_CLEANSING = TOME_OF_CLEANSING.get();
                ModItems.TOME_OF_CLEANSING_2 = TOME_OF_CLEANSING_2.get();
                ModItems.TOME_OF_CLEANSING_3 = TOME_OF_CLEANSING_3.get();

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

                // No delayed tick handling required - rely on immediate syncs
        }

        @Mod.EventBusSubscriber(modid = SkillLevelingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
        public static class ForgeEvents {
                @SubscribeEvent
                public static void onRegisterCommands(RegisterCommandsEvent event) {
                        SkillLevelingCommand.register(event.getDispatcher());
                }

                @SubscribeEvent
                public static void onPlayerJoin(
                                net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
                        if (event.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                                SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerJoin(serverPlayer);
                        }
                }

                @SubscribeEvent
                public static void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStarting(event.getServer());
                }

                @SubscribeEvent
                public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStopping(event.getServer());
                }

                @SubscribeEvent
                public static void onEquipmentChange(
                                net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
                        if (event.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                                ItemStack from = event.getFrom();
                                ItemStack to = event.getTo();

                                boolean fromImbued = from.hasNbt()
                                                && from.getNbt().contains("SkillLevelingImbued",
                                                                net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
                                boolean toImbued = to.hasNbt()
                                                && to.getNbt().contains("SkillLevelingImbued",
                                                                net.minecraft.nbt.NbtElement.COMPOUND_TYPE);

                                if (fromImbued || toImbued) {
                                        // CRITICAL FIX: Refresh attributes on equipment change
                                        SkillLevelingMod.getInstance().getSkillLevelingManager()
                                                        .refreshAllRewards(serverPlayer);
                                        // Then sync UI levels
                                        SkillLevelingMod.getInstance().getSkillLevelingManager()
                                                        .syncAllSkillsToPlayer(serverPlayer);
                                }
                        }
                }
        }
}
