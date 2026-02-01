package net.bluelotuscoding.skillleveling.registry;

import com.google.common.collect.ImmutableSet;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.registry.ModVillagers;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;
import java.util.Set;

public class ForgeVillagerRegistry {
        public static final DeferredRegister<PointOfInterestType> POI_TYPES = DeferredRegister
                        .create(ForgeRegistries.POI_TYPES, SkillLevelingMod.MOD_ID);
        public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister
                        .create(ForgeRegistries.VILLAGER_PROFESSIONS, SkillLevelingMod.MOD_ID);

        public static final RegistryObject<PointOfInterestType> SKILL_SCRIBE_TABLE_POI = POI_TYPES
                        .register("skill_master",
                                        () -> {
                                                var block = ForgeBlockRegistry.SKILL_SCRIBE_TABLE.get();
                                                var states = Set.copyOf(block.getStateManager().getStates());
                                                SkillLevelingMod.getInstance().getLogger()
                                                                .info("[POI DEBUG] Registering POI 'skill_master' with "
                                                                                + states.size()
                                                                                + " block states from block: "
                                                                                + block.getTranslationKey());
                                                for (var state : states) {
                                                        SkillLevelingMod.getInstance().getLogger()
                                                                        .info("[POI DEBUG] - State: " + state);
                                                }
                                                return new PointOfInterestType(states, 1, 1);
                                        });

        public static final RegistryObject<VillagerProfession> SKILL_MASTER = PROFESSIONS.register("skill_master",
                        () -> {
                                SkillLevelingMod.getInstance().getLogger()
                                                .info("Creating Skill Master profession instance...");
                                SkillLevelingMod.getInstance().getLogger()
                                                .info("[PROFESSION DEBUG] Linking to POI: "
                                                                + SKILL_SCRIBE_TABLE_POI.getId());

                                // Matching tutorial pattern: use short ID and direct instance comparison
                                return new VillagerProfession(
                                                "skill_master",
                                                holder -> holder.value() == SKILL_SCRIBE_TABLE_POI.get(),
                                                holder -> holder.value() == SKILL_SCRIBE_TABLE_POI.get(),
                                                ImmutableSet.of(),
                                                ImmutableSet.of(),
                                                SoundEvents.ENTITY_VILLAGER_WORK_CARTOGRAPHER);
                        });

        public static void register(IEventBus bus) {
                SkillLevelingMod.getInstance().getLogger().info("Initializing Forge Villager Registry...");
                POI_TYPES.register(bus);
                PROFESSIONS.register(bus);
                // Register the common setup event to verify POI block state mapping
                bus.addListener(ForgeVillagerRegistry::onCommonSetup);
        }

        /**
         * Called during FMLCommonSetupEvent to verify POI block state registration.
         * Uses Forge's public API to check block state mapping.
         */
        public static void onCommonSetup(FMLCommonSetupEvent event) {
                event.enqueueWork(() -> {
                        SkillLevelingMod.getInstance().getLogger()
                                        .info("[POI SETUP] Verifying block state mapping for Skill Scribe Table POI...");

                        var block = ForgeBlockRegistry.SKILL_SCRIBE_TABLE.get();
                        var states = block.getStateManager().getStates();

                        // Use Forge's public API to check the block state -> POI type mapping
                        boolean found = false;
                        for (var state : states) {
                                Optional<RegistryEntry<PointOfInterestType>> entry = PointOfInterestTypes
                                                .getTypeForState(state);
                                if (entry.isPresent()) {
                                        found = true;
                                        RegistryEntry<PointOfInterestType> poiEntry = entry.get();
                                        SkillLevelingMod.getInstance().getLogger()
                                                        .info("[POI SETUP] SUCCESS! Block state " + state
                                                                        + " is mapped to POI: " + poiEntry.getKey());

                                        // Test if the profession's predicate matches this POI entry
                                        var profession = SKILL_MASTER.get();
                                        boolean predicateMatch = profession.acquirableWorkstation().test(poiEntry);
                                        SkillLevelingMod.getInstance().getLogger()
                                                        .info("[POI SETUP] Profession acquirableWorkstation predicate match: "
                                                                        + predicateMatch);

                                        // Also check if keys match directly
                                        boolean keyMatch = poiEntry.matchesKey(ModVillagers.SKILL_SCRIBE_TABLE_POI_KEY);
                                        SkillLevelingMod.getInstance().getLogger()
                                                        .info("[POI SETUP] Direct key match (matchesKey): " + keyMatch);
                                        SkillLevelingMod.getInstance().getLogger()
                                                        .info("[POI SETUP] Expected key: "
                                                                        + ModVillagers.SKILL_SCRIBE_TABLE_POI_KEY);
                                        SkillLevelingMod.getInstance().getLogger()
                                                        .info("[POI SETUP] Actual key: " + poiEntry.getKey());
                                } else {
                                        SkillLevelingMod.getInstance().getLogger()
                                                        .warn("[POI SETUP] MISSING! Block state " + state
                                                                        + " is NOT mapped to any POI type!");
                                }
                        }

                        if (!found) {
                                SkillLevelingMod.getInstance().getLogger()
                                                .error("[POI SETUP] CRITICAL: No block states are mapped! "
                                                                + "Villagers will not be able to detect the workstation.");
                                SkillLevelingMod.getInstance().getLogger()
                                                .error("[POI SETUP] This is likely a Forge DeferredRegister issue - "
                                                                + "the POI type is registered but block states are not linked.");
                        }
                });
        }

}
