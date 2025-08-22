package net.puffish.skillsmod.api.calculation.operation;

import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.Optional;
import java.util.function.Function;

public interface OperationFactory<T, R> {
	default <V> OperationFactory<T, V> andThen(Function<? super R, ? extends V> after) {
		return context -> this.apply(context).mapSuccess(o -> v -> o.apply(v).map(after));
	}

	default <V> OperationFactory<V, R> compose(Function<? super V, ? extends T> before) {
		return context -> this.apply(context).mapSuccess(o -> v -> o.apply(before.apply(v)));
	}

	default OperationFactory<Optional<T>, R> optional() {
		return context -> this.apply(context).mapSuccess(o -> t -> t.flatMap(o));
	}

	static <T, R> OperationFactory<T, R> create(Function<T, R> function) {
		return context -> Result.success(t -> Optional.of(function.apply(t)));
	}

	static <T, R> OperationFactory<T, R> createOptional(Operation<T, R> operation) {
		return context -> Result.success(operation);
	}

	Result<? extends Operation<T, R>, Problem> apply(OperationConfigContext context);
}
