package net.puffish.skillsmod.api.calculation;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.impl.calculation.CalculationImpl;

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
