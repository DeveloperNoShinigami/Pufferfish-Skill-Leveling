package net.puffish.skillsmod.client.data;

import net.puffish.skillsmod.client.config.colors.ClientFillStrokeColorsConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;

public class ClientSkillConnectionData {
	private final ClientSkillConfig skillA;
	private final ClientSkillConfig skillB;

	private ClientFillStrokeColorsConfig color;

	public ClientSkillConnectionData(
			ClientSkillConfig skillA,
			ClientSkillConfig skillB,
			ClientFillStrokeColorsConfig color
	) {
		this.skillA = skillA;
		this.skillB = skillB;
		this.color = color;
	}

	public ClientSkillConfig getSkillA() {
		return skillA;
	}

	public ClientSkillConfig getSkillB() {
		return skillB;
	}

	public ClientFillStrokeColorsConfig getColor() {
		return color;
	}

	public void setColor(ClientFillStrokeColorsConfig color) {
		this.color = color;
	}
}
