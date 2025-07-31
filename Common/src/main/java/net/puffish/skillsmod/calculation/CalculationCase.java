package net.puffish.skillsmod.calculation;

import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.expression.DefaultParser;
import net.puffish.skillsmod.expression.Expression;
import net.puffish.skillsmod.impl.util.ProblemImpl;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CalculationCase {
	private final Expression<Double> condition;
	private final Expression<Double> expression;
	private final JsonPath expressionElementPath;

	private CalculationCase(Expression<Double> condition, Expression<Double> expression, JsonPath expressionElementPath) {
		this.condition = condition;
		this.expression = expression;
		this.expressionElementPath = expressionElementPath;
	}

	public static Result<CalculationCase, Problem> parseSimplified(JsonElement rootElement, Set<String> expressionVariables) {
		var problems = new ArrayList<Problem>();

		var optExpression = rootElement.getAsString()
				.andThen(string -> DefaultParser.parse(string, expressionVariables)
						.mapFailure(problem -> Problem.combine(
								ProblemImpl.streamMessages(problem)
										.map(msg -> rootElement.getPath().createProblem(msg))
										.toList()
						))
				)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new CalculationCase(
					v -> 1.0,
					optExpression.orElseThrow(),
					rootElement.getPath()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	public static Result<CalculationCase, Problem> parse(JsonElement rootElement, Set<String> expressionVariables, ConfigContext context) {
		return rootElement.getAsObject().andThen(
				LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, expressionVariables), context)
		);
	}

	private static Result<CalculationCase, Problem> parse(JsonObject rootObject, Set<String> expressionVariables) {
		var problems = new ArrayList<Problem>();

		var condition = rootObject.get("condition")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsString()
						.andThen(string -> DefaultParser.parse(string, expressionVariables)
								.mapFailure(problem -> Problem.combine(
										ProblemImpl.streamMessages(problem)
												.map(msg -> element.getPath().createProblem(msg))
												.toList()
								))
						)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(p -> 1.0); // no condition, so always true

		var optExpressionElement = rootObject.get("expression")
				.ifFailure(problems::add)
				.getSuccess();

		var optExpression = optExpressionElement
				.flatMap(element -> element.getAsString()
						.andThen(string -> DefaultParser.parse(string, expressionVariables)
								.mapFailure(problem -> Problem.combine(
										ProblemImpl.streamMessages(problem)
												.map(msg -> element.getPath().createProblem(msg))
												.toList()
								))
						)
						.ifFailure(problems::add)
						.getSuccess()
				);

		if (problems.isEmpty()) {
			return Result.success(new CalculationCase(
					condition,
					optExpression.orElseThrow(),
					optExpressionElement.orElseThrow().getPath()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	public boolean test(Map<String, Double> variables) {
		return condition.eval(variables) != 0;
	}

	public double eval(Map<String, Double> variables) {
		return expression.eval(variables);
	}

	public Optional<Double> getValue(Map<String, Double> variables) {
		if (test(variables)) {
			var value = eval(variables);
			if (Double.isFinite(value)) {
				return Optional.of(value);
			} else {
				SkillsMod.getInstance().getLogger().warn(
						expressionElementPath
								.createProblem("Expression returned a value that is not finite")
								.toString()
				);
				return Optional.of(0.0);
			}
		}
		return Optional.empty();
	}

}
