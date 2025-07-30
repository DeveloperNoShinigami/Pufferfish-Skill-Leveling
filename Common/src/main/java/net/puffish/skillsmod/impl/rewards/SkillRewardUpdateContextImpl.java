package net.puffish.skillsmod.impl.rewards;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public record SkillRewardUpdateContextImpl(
        ServerPlayerEntity player,
        int count,
        boolean isAction,
        Identifier categoryId,
        String skillId
) implements SkillRewardUpdateContext {
    @Override
    public ServerPlayerEntity getPlayer() {
        return player;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public boolean isAction() {
        return isAction;
    }

    @Override
    public Identifier getCategoryId() {
        return categoryId;
    }

    @Override
    public String getSkillId() {
        return skillId;
    }
}
