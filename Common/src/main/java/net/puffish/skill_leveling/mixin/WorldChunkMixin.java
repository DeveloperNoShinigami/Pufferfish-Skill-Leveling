package net.puffish.skill_leveling.mixin;

import net.minecraft.world.chunk.WorldChunk;
import net.puffish.skill_leveling.access.WorldChunkAccess;
import net.puffish.skill_leveling.experience.source.builtin.util.AntiFarmingPerChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements WorldChunkAccess {
	@Unique
	private final AntiFarmingPerChunk.Data antiFarmingData = new AntiFarmingPerChunk.Data();

	@Override
	public AntiFarmingPerChunk.Data getAntiFarmingData() {
		return antiFarmingData;
	}
}
