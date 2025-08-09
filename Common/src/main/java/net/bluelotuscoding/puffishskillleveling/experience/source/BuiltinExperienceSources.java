package net.bluelotuscoding.puffishskillleveling.experience.source;

import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.BreakBlockExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.CraftItemExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.DealDamageExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.EatFoodExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.EnchantItemExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.FishItemExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.HealExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.IncreaseStatExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.KillEntityExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.MineBlockExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.SharedKillEntityExperienceSource;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.TakeDamageExperienceSource;

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
