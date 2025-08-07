package net.puffish.skillsmod.server.data;

import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.common.BackgroundPosition;
import net.puffish.skillsmod.config.BackgroundConfig;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.config.FrameConfig;
import net.puffish.skillsmod.config.GeneralConfig;
import net.puffish.skillsmod.config.IconConfig;
import net.puffish.skillsmod.config.colors.ColorsConfig;
import net.puffish.skillsmod.config.skill.SkillConfig;
import net.puffish.skillsmod.config.skill.SkillDefinitionConfig;
import net.puffish.skillsmod.config.skill.SkillDefinitionsConfig;
import net.puffish.skillsmod.config.skill.SkillRewardConfig;
import net.puffish.skillsmod.config.skill.SkillConnectionsConfig;
import net.puffish.skillsmod.config.skill.SkillConnectionsGroupConfig;
import net.puffish.skillsmod.config.skill.SkillsConfig;
import net.puffish.skillsmod.reward.builtin.PerLevelRewardsReward;
import net.puffish.skillsmod.util.PointSources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CategoryDataTest {
    @Test
    public void testPerLevelRewardExtendsMaxLevel() throws Exception {
        Map<Integer, List<SkillRewardConfig>> levelRewards = new HashMap<>();
        Constructor<PerLevelRewardsReward> ctor = PerLevelRewardsReward.class.getDeclaredConstructor(Map.class, int.class, int.class);
        ctor.setAccessible(true);
        PerLevelRewardsReward plr = ctor.newInstance(levelRewards, 3, 0);
        SkillRewardConfig rewardConfig = new SkillRewardConfig(PerLevelRewardsReward.ID, plr);

        SkillDefinitionConfig definition = new SkillDefinitionConfig(
                "def",
                Identifier.of("puffish_skill_leveling", "default"),
                1,
                List.of(),
                List.of(),
                Text.of("Test"),
                new IconConfig.TextureIconConfig(Identifier.of("minecraft", "stone")),
                new FrameConfig.AdvancementFrameConfig(AdvancementFrame.TASK),
                1f,
                false,
                List.of(rewardConfig),
                0,
                0,
                0,
                0,
                1
        );

        Constructor<SkillDefinitionsConfig> defsCtor = SkillDefinitionsConfig.class.getDeclaredConstructor(Map.class);
        defsCtor.setAccessible(true);
        SkillDefinitionsConfig definitions = defsCtor.newInstance(Map.of("def", definition));
        SkillConfig skill = new SkillConfig("skill", 0, 0, "def", true);
        SkillsConfig skills = new SkillsConfig(Map.of("skill", skill));
        SkillConnectionsConfig connections = new SkillConnectionsConfig(SkillConnectionsGroupConfig.empty(), SkillConnectionsGroupConfig.empty());

        GeneralConfig general = new GeneralConfig(
                Text.of("Cat"),
                new IconConfig.TextureIconConfig(Identifier.of("minecraft", "stone")),
                new BackgroundConfig(Identifier.of("minecraft", "stone"), 16, 16, BackgroundPosition.NONE),
                ColorsConfig.createDefault(),
                true,
                0,
                false,
                Integer.MAX_VALUE
        );

        CategoryConfig category = new CategoryConfig(new Identifier("test", "cat"), general, definitions, skills, connections, Optional.empty());
        CategoryData data = CategoryData.create(general);

        Assertions.assertEquals(Skill.State.AFFORDABLE, data.getSkillState(category, skill, definition));
        data.unlockSkill("skill");
        Assertions.assertEquals(Skill.State.AFFORDABLE, data.getSkillState(category, skill, definition));
        data.unlockSkill("skill");
        Assertions.assertEquals(Skill.State.AFFORDABLE, data.getSkillState(category, skill, definition));
        data.unlockSkill("skill");
        Assertions.assertEquals(Skill.State.UNLOCKED, data.getSkillState(category, skill, definition));
    }

    @Test
    public void testPerLevelRewardCostsPointsPerLevel() throws Exception {
        Map<Integer, List<SkillRewardConfig>> levelRewards = new HashMap<>();
        Constructor<PerLevelRewardsReward> ctor = PerLevelRewardsReward.class.getDeclaredConstructor(Map.class, int.class, int.class);
        ctor.setAccessible(true);
        PerLevelRewardsReward plr = ctor.newInstance(levelRewards, 1, 2);
        SkillRewardConfig rewardConfig = new SkillRewardConfig(PerLevelRewardsReward.ID, plr);

        SkillDefinitionConfig definition = new SkillDefinitionConfig(
                "def",
                Identifier.of("puffish_skill_leveling", "default"),
                1,
                List.of(),
                List.of(),
                Text.of("Test"),
                new IconConfig.TextureIconConfig(Identifier.of("minecraft", "stone")),
                new FrameConfig.AdvancementFrameConfig(AdvancementFrame.TASK),
                1f,
                false,
                List.of(rewardConfig),
                0,
                0,
                0,
                0,
                1
        );

        Constructor<SkillDefinitionsConfig> defsCtor = SkillDefinitionsConfig.class.getDeclaredConstructor(Map.class);
        defsCtor.setAccessible(true);
        SkillDefinitionsConfig definitions = defsCtor.newInstance(Map.of("def", definition));
        SkillConfig skill = new SkillConfig("skill", 0, 0, "def", true);
        SkillsConfig skills = new SkillsConfig(Map.of("skill", skill));
        SkillConnectionsConfig connections = new SkillConnectionsConfig(SkillConnectionsGroupConfig.empty(), SkillConnectionsGroupConfig.empty());

        GeneralConfig general = new GeneralConfig(
                Text.of("Cat"),
                new IconConfig.TextureIconConfig(Identifier.of("minecraft", "stone")),
                new BackgroundConfig(Identifier.of("minecraft", "stone"), 16, 16, BackgroundPosition.NONE),
                ColorsConfig.createDefault(),
                true,
                0,
                false,
                Integer.MAX_VALUE
        );

        CategoryConfig category = new CategoryConfig(new Identifier("test", "cat"), general, definitions, skills, connections, Optional.empty());
        CategoryData data = CategoryData.create(general);

        Assertions.assertEquals(Skill.State.AVAILABLE, data.getSkillState(category, skill, definition));

        data.setPoints(PointSources.STARTING, 2);
        Assertions.assertEquals(Skill.State.AFFORDABLE, data.getSkillState(category, skill, definition));
    }
}
