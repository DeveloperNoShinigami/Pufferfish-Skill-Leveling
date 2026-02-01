package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ForgeBlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            SkillLevelingMod.MOD_ID);

    public static final RegistryObject<Block> SKILL_SCRIBE_TABLE = BLOCKS.register("skill_scribe_table",
            ModBlocks::createSkillScribeTable);

    public static void register(IEventBus bus) {
        SkillLevelingMod.getInstance().getLogger().info("Initializing Forge Block Registry...");
        BLOCKS.register(bus);
    }
}
