package net.puffish.skill_leveling.api.calculation;

import net.puffish.skill_leveling.api.config.ConfigContext;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.impl.calculation.CalculationImpl;

public interface Calculation<T> {

	static <T> Result<Calculation<T>, Problem> parse(
			JsonElement rootElement,
			Variables<T, Double> variables,
			ConfigContext context
	) {
		return CalculationImpl.create(rootElement, variables, context)
				.mapSuccess(c -> c);
	}

	double evaluate(T t);
}
