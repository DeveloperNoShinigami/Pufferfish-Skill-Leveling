package net.puffish.skill_leveling.expression;

import java.util.Map;

public interface Expression<T> {
	T eval(Map<String, T> variables);
}
