package net.bluelotuscoding.skillleveling.points;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.util.PointSources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class SkillPointManagerTest {
    private static final Identifier CATEGORY_ID = new Identifier("test", "category");
    private static final String SKILL_ID = "skill";

    @BeforeAll
    public static void setup() {
        SkillLevelingMod.init();
        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        manager.registerPerLevelRewardsReward(CATEGORY_ID, SKILL_ID, createReward(5));
    }

    private static PerLevelRewardsReward createReward(int pointsPerLevel) {
        try {
            var ctor = PerLevelRewardsReward.class.getDeclaredConstructor(
                    Map.class, String.class, int.class, int.class, Map.class, Map.class, boolean.class);
            ctor.setAccessible(true);
            return ctor.newInstance(new HashMap<>(), SKILL_ID, 1, pointsPerLevel, new HashMap<>(), new HashMap<>(), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDeductPointsForLevel() {
        ServerPlayerEntity player = null;
        Category category = Mockito.mock(Category.class);
        AtomicInteger points = new AtomicInteger(10);
        Mockito.when(category.getPointsLeft(Mockito.any())).thenAnswer(invocation -> points.get());
        Mockito.doAnswer(invocation -> {
            points.addAndGet((Integer) invocation.getArgument(2));
            return null;
        }).when(category).addPoints(Mockito.any(), Mockito.eq(PointSources.COMMANDS), Mockito.anyInt());

        try (MockedStatic<SkillsAPI> skills = Mockito.mockStatic(SkillsAPI.class)) {
            skills.when(() -> SkillsAPI.getCategory(CATEGORY_ID)).thenReturn(Optional.of(category));

            boolean result = SkillPointManager.deductPointsForLevel(player, CATEGORY_ID, SKILL_ID, 1);

            Assertions.assertTrue(result);
            Assertions.assertEquals(5, points.get());
        }
    }

    @Test
    public void testRefundPointsForLevel() {
        ServerPlayerEntity player = null;
        Category category = Mockito.mock(Category.class);
        AtomicInteger points = new AtomicInteger(0);
        Mockito.doAnswer(invocation -> {
            points.addAndGet((Integer) invocation.getArgument(2));
            return null;
        }).when(category).addPoints(Mockito.any(), Mockito.eq(PointSources.COMMANDS), Mockito.anyInt());

        try (MockedStatic<SkillsAPI> skills = Mockito.mockStatic(SkillsAPI.class)) {
            skills.when(() -> SkillsAPI.getCategory(CATEGORY_ID)).thenReturn(Optional.of(category));

            SkillPointManager.refundPointsForLevel(player, CATEGORY_ID, SKILL_ID, 1);

            Assertions.assertEquals(5, points.get());
        }
    }
}
