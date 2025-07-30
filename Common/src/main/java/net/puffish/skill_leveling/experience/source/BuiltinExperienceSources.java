package net.puffish.skill_leveling.experience.source;

import net.puffish.skill_leveling.experience.source.builtin.BreakBlockExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.CraftItemExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.DealDamageExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.EatFoodExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.EnchantItemExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.FishItemExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.HealExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.IncreaseStatExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.KillEntityExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.MineBlockExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.SharedKillEntityExperienceSource;
import net.puffish.skill_leveling.experience.source.builtin.TakeDamageExperienceSource;

public class BuiltinExperienceSources {
	public static void register() {
		BreakBlockExperienceSource.register();
		CraftItemExperienceSource.register();
		DealDamageExperienceSource.register();
		EatFoodExperienceSource.register();
		EnchantItemExperienceSource.register();
		FishItemExperienceSource.register();
		HealExperienceSource.register();
		IncreaseStatExperienceSource.register();
		KillEntityExperienceSource.register();
		MineBlockExperienceSource.register();
		SharedKillEntityExperienceSource.register();
		TakeDamageExperienceSource.register();
	}
}
