package net.puffish.skill_leveling.access;

import net.puffish.skill_leveling.experience.source.builtin.util.AntiFarmingPerChunk;

public interface WorldChunkAccess {
	AntiFarmingPerChunk.Data getAntiFarmingData();
}
