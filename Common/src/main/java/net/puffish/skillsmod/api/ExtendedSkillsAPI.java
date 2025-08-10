package net.puffish.skillsmod.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.config.skill.SkillRewardConfig;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Additional helper methods built on top of {@link SkillsAPI}.  These simplify
 * updating rewards and experience sources without exposing internal classes to
 * consumers of the base API.
 */
public final class ExtendedSkillsAPI {

    private ExtendedSkillsAPI() {
    }

    public static void updateRewards(ServerPlayerEntity player, Predicate<SkillRewardConfig> predicate) {
        SkillsMod.getInstance().updateRewards(player, predicate);
    }

    public static void updateExperienceSources(
            ServerPlayerEntity player,
            Predicate<ExperienceSource> predicate,
            Function<ExperienceSource, Integer> function
    ) {
        SkillsMod.getInstance().visitExperienceSources(player, source -> {
            if (predicate.test(source)) {
                return function.apply(source);
            }
            return 0;
        });
    }
}

