package net.puffish.skillsmod.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.stream.Stream;

/**
 * Extension of the public {@link Category} API that exposes point management
 * and reset operations used internally by the addon.  These methods mirror the
 * capabilities of {@link net.puffish.skillsmod.impl.CategoryImpl} while keeping
 * the upstream API untouched.
 */
public interface ExtendedCategory extends Category {

    Stream<Identifier> streamPointsSources(ServerPlayerEntity player);

    int getPoints(ServerPlayerEntity player, Identifier source);

    void setPoints(ServerPlayerEntity player, Identifier source, int count);

    void addPoints(ServerPlayerEntity player, Identifier source, int count);

    void setPointsSilently(ServerPlayerEntity player, Identifier source, int count);

    void addPointsSilently(ServerPlayerEntity player, Identifier source, int count);

    void resetSkills(ServerPlayerEntity player);

    void unlock(ServerPlayerEntity player);

    void lock(ServerPlayerEntity player);

    boolean isUnlocked(ServerPlayerEntity player);

    void erase(ServerPlayerEntity player);

    int getSpentPoints(ServerPlayerEntity player);

    int getPointsTotal(ServerPlayerEntity player);

    int getPointsLeft(ServerPlayerEntity player);

    @Deprecated
    int getExtraPoints(ServerPlayerEntity player);

    @Deprecated
    void setExtraPoints(ServerPlayerEntity player, int count);

    @Deprecated
    void addExtraPoints(ServerPlayerEntity player, int count);
}

