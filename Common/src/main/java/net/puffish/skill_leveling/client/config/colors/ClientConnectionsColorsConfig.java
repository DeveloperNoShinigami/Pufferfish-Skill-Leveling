package net.puffish.skill_leveling.client.config.colors;

public record ClientConnectionsColorsConfig(
		ClientFillStrokeColorsConfig locked,
		ClientFillStrokeColorsConfig available,
		ClientFillStrokeColorsConfig affordable,
		ClientFillStrokeColorsConfig unlocked,
		ClientFillStrokeColorsConfig excluded
) { }