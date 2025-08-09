package net.bluelotuscoding.puffishskillleveling.client.config.colors;

public record ClientConnectionsColorsConfig(
		ClientFillStrokeColorsConfig locked,
		ClientFillStrokeColorsConfig available,
		ClientFillStrokeColorsConfig affordable,
		ClientFillStrokeColorsConfig unlocked,
		ClientFillStrokeColorsConfig excluded
) { }