package net.puffish.skillsmod.api.calculation.operation;

import java.util.Optional;
import java.util.function.Function;

public interface Operation<T, R> extends Function<T, Optional<R>> {

}
