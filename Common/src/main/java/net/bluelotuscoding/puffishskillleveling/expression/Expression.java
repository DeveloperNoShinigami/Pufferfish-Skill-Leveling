package net.bluelotuscoding.puffishskillleveling.expression;

import java.util.Map;

public interface Expression<T> {
	T eval(Map<String, T> variables);
}
