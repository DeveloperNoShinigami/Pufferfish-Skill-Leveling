package net.puffish.skillsmod.api.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface Result<S, F> permits Result.Success, Result.Failure {

	static <S, R> Result<S, R> success(S s) {
		return new Success<>(s);
	}

	static <L, F> Result<L, F> failure(F f) {
		return new Failure<>(f);
	}

	Optional<S> getSuccess();

	Optional<F> getFailure();

	S getSuccessOrElse(Function<? super F, ? extends S> f);

	F getFailureOrElse(Function<? super S, ? extends F> f);

	Result<S, F> ifSuccess(Consumer<? super S> c);

	Result<S, F> ifFailure(Consumer<? super F> c);

	<L2> Result<L2, F> mapSuccess(Function<? super S, ? extends L2> f);

	<F2> Result<S, F2> mapFailure(Function<? super F, ? extends F2> f);

	<S2> Result<S2, F> andThen(Function<? super S, ? extends Result<S2, F>> function);

	<F2> Result<S, F2> orElse(Function<? super F, ? extends Result<S, F2>> function);

	<S2, F2> Result<S2, F2> flatMap(Function<? super S, ? extends Result<S2, F2>> fSuccess, Function<? super F, ? extends Result<S2, F2>> fFailure);

	final class Success<S, F> implements Result<S, F> {
		private final S s;

		private Success(S l) {
			this.s = l;
		}

		public S get() {
			return s;
		}

		@Override
		public Optional<S> getSuccess() {
			return Optional.of(s);
		}

		@Override
		public Optional<F> getFailure() {
			return Optional.empty();
		}

		@Override
		public S getSuccessOrElse(Function<? super F, ? extends S> f) {
			return s;
		}

		@Override
		public F getFailureOrElse(Function<? super S, ? extends F> f) {
			return f.apply(s);
		}

		@Override
		public Result<S, F> ifSuccess(Consumer<? super S> c) {
			c.accept(s);
			return this;
		}

		@Override
		public Result<S, F> ifFailure(Consumer<? super F> c) {
			return this;
		}

		@Override
		public <L2> Result<L2, F> mapSuccess(Function<? super S, ? extends L2> f) {
			return Result.success(f.apply(s));
		}

		@Override
		public <R2> Result<S, R2> mapFailure(Function<? super F, ? extends R2> f) {
			return Result.success(s);
		}

		@Override
		public <S2> Result<S2, F> andThen(Function<? super S, ? extends Result<S2, F>> function) {
			return function.apply(s);
		}

		@Override
		public <F2> Result<S, F2> orElse(Function<? super F, ? extends Result<S, F2>> function) {
			return Result.success(s);
		}

		@Override
		public <S2, F2> Result<S2, F2> flatMap(Function<? super S, ? extends Result<S2, F2>> fSuccess, Function<? super F, ? extends Result<S2, F2>> fFailure) {
			return fSuccess.apply(s);
		}
	}

	final class Failure<S, F> implements Result<S, F> {
		private final F f;

		private Failure(F r) {
			this.f = r;
		}

		public F get() {
			return f;
		}

		@Override
		public Optional<S> getSuccess() {
			return Optional.empty();
		}

		@Override
		public Optional<F> getFailure() {
			return Optional.of(f);
		}

		@Override
		public S getSuccessOrElse(Function<? super F, ? extends S> f) {
			return f.apply(this.f);
		}

		@Override
		public F getFailureOrElse(Function<? super S, ? extends F> f) {
			return this.f;
		}

		@Override
		public Result<S, F> ifSuccess(Consumer<? super S> c) {
			return this;
		}

		@Override
		public Result<S, F> ifFailure(Consumer<? super F> c) {
			c.accept(f);
			return this;
		}

		@Override
		public <L2> Result<L2, F> mapSuccess(Function<? super S, ? extends L2> f) {
			return Result.failure(this.f);
		}

		@Override
		public <F2> Result<S, F2> mapFailure(Function<? super F, ? extends F2> f) {
			return Result.failure(f.apply(this.f));
		}

		@Override
		public <S2> Result<S2, F> andThen(Function<? super S, ? extends Result<S2, F>> function) {
			return Result.failure(f);
		}

		@Override
		public <F2> Result<S, F2> orElse(Function<? super F, ? extends Result<S, F2>> function) {
			return function.apply(f);
		}

		@Override
		public <S2, F2> Result<S2, F2> flatMap(Function<? super S, ? extends Result<S2, F2>> fSuccess, Function<? super F, ? extends Result<S2, F2>> fFailure) {
			return fFailure.apply(f);
		}
	}
}
