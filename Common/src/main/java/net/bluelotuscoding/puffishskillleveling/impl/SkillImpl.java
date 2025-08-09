package net.bluelotuscoding.puffishskillleveling.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import net.bluelotuscoding.puffishskillleveling.SkillsMod;
import net.bluelotuscoding.puffishskillleveling.api.Category;
import net.bluelotuscoding.puffishskillleveling.api.Skill;

public class SkillImpl implements Skill {
	private final Category category;
	private final String skillId;

	public SkillImpl(Category category, String skillId) {
		this.category = category;
		this.skillId = skillId;
	}

	@Override
	public Category getCategory() {
		return category;
	}

	@Override
	public String getId() {
		return skillId;
	}

	@Override
	public State getState(ServerPlayerEntity player) {
		return SkillsMod.getInstance().getSkillState(player, category.getId(), skillId).orElseThrow();
	}

	@Override
	public void unlock(ServerPlayerEntity player) {
		SkillsMod.getInstance().unlockSkill(player, category.getId(), skillId);
	}

	@Override
	public void lock(ServerPlayerEntity player) {
		SkillsMod.getInstance().lockSkill(player, category.getId(), skillId);
	}
}
