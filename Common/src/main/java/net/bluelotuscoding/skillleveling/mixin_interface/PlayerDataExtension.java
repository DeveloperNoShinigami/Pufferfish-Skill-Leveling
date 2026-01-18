package net.bluelotuscoding.skillleveling.mixin_interface;

import net.minecraft.server.network.ServerPlayerEntity;

public interface PlayerDataExtension {
    void addon$setOwner(ServerPlayerEntity player);
}
