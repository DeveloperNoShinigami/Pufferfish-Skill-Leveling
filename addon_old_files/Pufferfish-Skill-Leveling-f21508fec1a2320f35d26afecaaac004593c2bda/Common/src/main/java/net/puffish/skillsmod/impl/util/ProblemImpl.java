package net.puffish.skillsmod.impl.util;

import net.puffish.skillsmod.api.util.Problem;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProblemImpl implements Problem {
	private final List<String> messages;

	public ProblemImpl(String message) {
		this.messages = List.of(message);
	}

	public ProblemImpl(Collection<Problem> problems) {
		this.messages = problems.stream().flatMap(ProblemImpl::streamMessages).toList();
	}

	public ProblemImpl(Problem... problems) {
		this.messages = Arrays.stream(problems).flatMap(ProblemImpl::streamMessages).toList();
	}

	@Override
	public String toString() {
		return messages.stream().collect(Collectors.joining(System.lineSeparator()));
	}

	public static Stream<String> streamMessages(Problem problem) {
		if (problem instanceof ProblemImpl impl) {
			return impl.messages.stream();
		} else {
			return Stream.of(problem.toString());
		}
	}
}
