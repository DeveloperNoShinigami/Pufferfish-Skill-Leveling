package net.bluelotuscoding.puffishskillleveling.mixin;

import net.minecraft.world.chunk.WorldChunk;
import net.bluelotuscoding.puffishskillleveling.access.WorldChunkAccess;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.util.AntiFarmingPerChunk;
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
