package net.bluelotuscoding.puffishskillleveling.api.calculation.prototype;

import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.api.calculation.operation.OperationConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.calculation.operation.OperationFactory;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.impl.calculation.prototype.PrototypeImpl;

import java.util.Optional;

public interface Prototype<T> {
	static <T> Prototype<T> create(Identifier id) {
		return new PrototypeImpl<>(id);
	}

	Identifier getId();

	<R> void registerOperation(Identifier id, Prototype<R> prototype, OperationFactory<T, R> factory);

	Optional<Result<PrototypeOperation<T, ?>, Problem>> getOperation(Identifier id, OperationConfigContext context);
}
