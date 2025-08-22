package net.puffish.skillsmod.config.colors;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;

public record ConnectionsColorsConfig(
		FillStrokeColorsConfig locked,
		FillStrokeColorsConfig available,
		FillStrokeColorsConfig affordable,
		FillStrokeColorsConfig unlocked,
		FillStrokeColorsConfig excluded
) {
	private static final FillStrokeColorsConfig DEFAULT_LOCKED = new FillStrokeColorsConfig(
			new ColorConfig(0xffffffff),
			new ColorConfig(0xff000000)
	);
	private static final FillStrokeColorsConfig DEFAULT_AVAILABLE_AFFORDABLE = new FillStrokeColorsConfig(
			new ColorConfig(0xffffffff),
			new ColorConfig(0xff000000)
	);
	private static final FillStrokeColorsConfig DEFAULT_UNLOCKED = new FillStrokeColorsConfig(
			new ColorConfig(0xffffffff),
			new ColorConfig(0xff000000)
	);
	private static final FillStrokeColorsConfig DEFAULT_EXCLUDED = new FillStrokeColorsConfig(
			new ColorConfig(0xffff0000),
			new ColorConfig(0xff000000)
	);

	public static ConnectionsColorsConfig createDefault() {
		return new ConnectionsColorsConfig(
				DEFAULT_LOCKED,
				DEFAULT_AVAILABLE_AFFORDABLE,
				DEFAULT_AVAILABLE_AFFORDABLE,
				DEFAULT_UNLOCKED,
				DEFAULT_EXCLUDED
		);
	}

	public static Result<ConnectionsColorsConfig, Problem> parse(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject().andThen(
				LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context)
		);
	}

	private static Result<ConnectionsColorsConfig, Problem> parse(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var locked = rootObject.get("locked")
				.getSuccess()
				.flatMap(element -> FillStrokeColorsConfig.parse(element, DEFAULT_LOCKED, context)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(DEFAULT_LOCKED);

		var available = rootObject.get("available")
				.getSuccess()
				.flatMap(element -> FillStrokeColorsConfig.parse(element, DEFAULT_AVAILABLE_AFFORDABLE, context)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(DEFAULT_AVAILABLE_AFFORDABLE);

		var affordable = rootObject.get("affordable")
				.getSuccess()
				.flatMap(element -> FillStrokeColorsConfig.parse(element, DEFAULT_AVAILABLE_AFFORDABLE, context)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(available);

		var unlocked = rootObject.get("unlocked")
				.getSuccess()
				.flatMap(element -> FillStrokeColorsConfig.parse(element, DEFAULT_UNLOCKED, context)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(DEFAULT_UNLOCKED);

		var excluded = rootObject.get("excluded")
				.getSuccess()
				.flatMap(element -> FillStrokeColorsConfig.parse(element, DEFAULT_EXCLUDED, context)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(DEFAULT_EXCLUDED);

		if (problems.isEmpty()) {
			return Result.success(new ConnectionsColorsConfig(
					locked,
					available,
					affordable,
					unlocked,
					excluded
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

}