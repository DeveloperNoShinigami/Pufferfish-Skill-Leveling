package net.bluelotuscoding.puffishskillleveling.util;

import net.bluelotuscoding.puffishskillleveling.api.config.ConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonObject;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.config.Config;
import net.bluelotuscoding.puffishskillleveling.impl.json.JsonObjectTrackingImpl;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class LegacyUtils {
	public static boolean isRemoved(int removalVersion, ConfigContext context) {
		if (context instanceof VersionContext versionContext) {
			return versionContext.getVersion() >= removalVersion;
		}
		return true;
	}

	public static <S, F> Optional<S> deprecated(Supplier<? extends Result<S, F>> supplier, int removalVersion, ConfigContext context) {
		if (isRemoved(removalVersion, context)) {
			return Optional.empty();
		}
		return supplier.get().getSuccess();
	}

	public static <S, F> Function<F, Result<S, F>> wrapDeprecated(Supplier<? extends Result<S, F>> supplier, int removalVersion, ConfigContext context) {
		if (isRemoved(removalVersion, context)) {
			return Result::failure;
		}
		return f -> supplier.get().mapFailure(f2 -> f);
	}

	public static <S> Function<JsonObject, Result<S, Problem>> wrapNoUnused(Function<JsonObject, ? extends Result<S, Problem>> function, ConfigContext context) {
		return jsonObject -> {
			var tracking = new JsonObjectTrackingImpl(jsonObject);
			var result = function.apply(tracking);

			var problems = tracking.reportUnusedEntries();

			if (!problems.isEmpty()) {
				if (LegacyUtils.isRemoved(3, context)) {
					// support for unused fields is removed, report problem
					problems = new ArrayList<>(problems);
					result.ifFailure(problems::add);
					return Result.failure(Problem.combine(problems));
				} else {
					// unused fields are still accepted, report warning
					context.emitWarning(Problem.combine(problems).toString());
				}
			}

			return result;
		};
	}

	public static <C extends Config> Function<JsonObject, Result<C, Problem>> wrapNoUnusedConfig(Function<JsonObject, ? extends Result<C, Problem>> function, ConfigContext context) {
		return jsonObject -> {
			var tracking = new JsonObjectTrackingImpl(jsonObject);
			return function.apply(tracking)
					.andThen(config -> {
						var problems = tracking.reportUnusedEntries();

						if (!problems.isEmpty()) {
							if (config.version() >= 3) {
								// support for unused fields is removed, report problem
								return Result.failure(Problem.combine(problems));
							} else {
								// unused fields are still accepted, report warning
								context.emitWarning(Problem.combine(problems).toString());
							}
						}

						return Result.success(config);
					});
		};
	}

}
