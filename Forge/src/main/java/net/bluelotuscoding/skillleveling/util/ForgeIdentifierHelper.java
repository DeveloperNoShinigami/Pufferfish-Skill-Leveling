package net.bluelotuscoding.skillleveling.util;

import net.minecraft.resources.ResourceLocation;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

/**
 * Helper class to create ResourceLocation/Identifier objects that works in both Fabric and Forge environments
 */
public class ForgeIdentifierHelper {
    
    /**
     * Create a ResourceLocation for Forge with the mod ID prefix
     */
    public static ResourceLocation createResourceLocation(String path) {
        return new ResourceLocation(SkillLevelingMod.MOD_ID, path);
    }
}
