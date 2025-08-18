package net.puffish.skillsmod.api.util;

import net.puffish.skillsmod.impl.util.ProblemImpl;

import java.util.Collection;

public interface Problem {
	static Problem message(String message) {
		return new ProblemImpl(message);
	}

	static Problem combine(Collection<Problem> problems) {
		return new ProblemImpl(problems);
	}

	static Problem combine(Problem... problems) {
		return new ProblemImpl(problems);
	}

	@Override
	String toString();
}
