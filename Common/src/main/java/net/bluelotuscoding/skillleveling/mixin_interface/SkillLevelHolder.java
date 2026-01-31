package net.bluelotuscoding.skillleveling.mixin_interface;

import net.minecraft.nbt.NbtCompound;

/**
 * Interface to allow access to persistent skill leveling data stored on the
 * player.
 */
public interface SkillLevelHolder {
    NbtCompound addon$getSkillLevelingData();

    void addon$setSkillLevelingData(NbtCompound nbt);
}
