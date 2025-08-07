package net.bluelotuscoding.puffishskillleveling.access;

import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.util.AntiFarmingPerChunk;

public interface WorldChunkAccess {
	AntiFarmingPerChunk.Data getAntiFarmingData();
}
