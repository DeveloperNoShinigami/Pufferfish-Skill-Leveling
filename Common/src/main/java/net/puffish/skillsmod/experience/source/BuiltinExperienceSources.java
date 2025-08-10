package net.puffish.skillsmod.experience.source;

import net.puffish.skillsmod.experience.source.builtin.BreakBlockExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.ExtendedCraftItemExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.DealDamageExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.ExtendedEatFoodExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.EnchantItemExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.FishItemExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.HealExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.ExtendedIncreaseStatExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.ExtendedKillEntityExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.ExtendedMineBlockExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.SharedKillEntityExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.ExtendedTakeDamageExperienceSource;

public class BuiltinExperienceSources {
	public static void register() {
		BreakBlockExperienceSource.register();
		ExtendedCraftItemExperienceSource.register();
		DealDamageExperienceSource.register();
		ExtendedEatFoodExperienceSource.register();
		EnchantItemExperienceSource.register();
		FishItemExperienceSource.register();
		HealExperienceSource.register();
		ExtendedIncreaseStatExperienceSource.register();
		ExtendedKillEntityExperienceSource.register();
		ExtendedMineBlockExperienceSource.register();
		SharedKillEntityExperienceSource.register();
		ExtendedTakeDamageExperienceSource.register();
	}
}
