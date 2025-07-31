package net.puffish.skillsmod.impl.calculation;

import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.Variables;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonArray;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.calculation.CalculationCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CalculationImpl<T> implements Calculation<T> {
	private final Variables<T, Double> variables;
	private final List<CalculationCase> cases;

	private CalculationImpl(Variables<T, Double> variables, List<CalculationCase> cases) {
		this.variables = variables;
		this.cases = cases;
	}

	@Override
	public double evaluate(T t) {
		return cases.stream()
				.mapToDouble(calc -> calc.getValue(variables.evaluate(t)).orElse(0.0))
				.sum();
	}

	public static <T> Result<CalculationImpl<T>, Problem> create(
			JsonElement rootElement,
			Variables<T, Double> variables,
			ConfigContext context
	) {
		var variableNames = variables.streamNames().collect(Collectors.toSet());

		return rootElement.getAsArray().flatMap(
				rootObject -> create(rootObject, variables, variableNames, context),
				failure -> CalculationCase.parseSimplified(rootElement, variableNames)
						.mapSuccess(calculationCase -> new CalculationImpl<>(variables, List.of(calculationCase)))
		);
	}

	private static <T> Result<CalculationImpl<T>, Problem> create(
			JsonArray rootArray,
			Variables<T, Double> variables,
			Set<String> variableNames,
			ConfigContext context
	) {
		var problems = new ArrayList<Problem>();

		var optCalculations = rootArray.getAsList((i, element) -> CalculationCase.parse(element, variableNames, context))
						.mapFailure(Problem::combine)
						.ifFailure(problems::add)
						.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new CalculationImpl<>(
					variables,
					optCalculations.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

}
