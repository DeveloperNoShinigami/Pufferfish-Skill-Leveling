package net.bluelotuscoding.puffishskillleveling.api.calculation;

import net.bluelotuscoding.puffishskillleveling.api.calculation.prototype.Prototype;
import net.bluelotuscoding.puffishskillleveling.api.config.ConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.impl.calculation.VariablesImpl;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public interface Variables<T, R> {

	static <T> Result<Variables<T, Double>, Problem> parse(
			JsonElement rootElement,
			Prototype<T> prototype,
			ConfigContext context
	) {
		return VariablesImpl.parse(rootElement, prototype, context);
	}

	static <T, R> Variables<T, R> create(
			Map<String, Function<T, R>> operations
	) {
		return VariablesImpl.create(operations);
	}

	static <T, R> Variables<T, R> combine(
			Collection<Variables<T, R>> variables
	) {
		return VariablesImpl.combine(variables);
	}

	@SafeVarargs
	static <T, R> Variables<T, R> combine(
			Variables<T, R>... variables
	) {
		return VariablesImpl.combine(variables);
	}

	Stream<String> streamNames();

	Map<String, R> evaluate(T t);
}
