package net.bluelotuscoding.puffishskillleveling.api.calculation.prototype;

import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.api.calculation.operation.Operation;
import net.bluelotuscoding.puffishskillleveling.api.calculation.operation.OperationConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.impl.calculation.prototype.PrototypeOperationImpl;

import java.util.Optional;

public interface PrototypeOperation<T, R> extends Operation<T, R> {
	static <U> PrototypeOperation<U, U> createIdentity(Prototype<U> prototype) {
		return new PrototypeOperationImpl<>(prototype, Optional::of);
	}

	Prototype<R> getReturnPrototype();

	<U> Optional<PrototypeOperation<T, U>> recoverReturnType(Prototype<U> prototype);

	Optional<Result<PrototypeOperation<T, ?>, Problem>> andThen(Identifier id, OperationConfigContext context);
}
