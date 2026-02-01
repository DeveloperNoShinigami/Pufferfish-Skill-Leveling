package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.block.SkillScribeTableBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;

/**
 * Common block definitions.
 * Platform-specific registration is handled in FabricMain/ForgeMain.
 */
public class ModBlocks {

    public static final Identifier SKILL_SCRIBE_TABLE_ID = SkillLevelingMod.createIdentifier("skill_scribe_table");

    // Block instance - will be populated during platform registration
    public static SkillScribeTableBlock SKILL_SCRIBE_TABLE;

    /**
     * Create the Skill Scribe Table block.
     */
    public static SkillScribeTableBlock createSkillScribeTable() {
        return new SkillScribeTableBlock(
                AbstractBlock.Settings.copy(Blocks.CARTOGRAPHY_TABLE)
                        .strength(2.5F)
                        .nonOpaque());
    }
}
