package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;

/**
 * Common villager and POI definitions.
 */
public class ModVillagers {

    public static final Identifier SKILL_MASTER_ID = SkillLevelingMod.createIdentifier("skill_master");

    // Profession Key
    public static final RegistryKey<VillagerProfession> SKILL_MASTER_KEY = RegistryKey
            .of(RegistryKeys.VILLAGER_PROFESSION, SKILL_MASTER_ID);

    // POI Key
    public static final RegistryKey<PointOfInterestType> SKILL_SCRIBE_TABLE_POI_KEY = RegistryKey
            .of(RegistryKeys.POINT_OF_INTEREST_TYPE, SKILL_MASTER_ID);

    // Profession Instance - will be populated during platform registration
    public static VillagerProfession SKILL_MASTER;
}
