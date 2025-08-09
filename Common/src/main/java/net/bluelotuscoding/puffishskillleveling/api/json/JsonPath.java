package net.bluelotuscoding.puffishskillleveling.api.json;

import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.impl.json.JsonPathImpl;

import java.util.List;
import java.util.Optional;

public interface JsonPath {
	static JsonPath create(String name) {
		return new JsonPathImpl(List.of("`" + name + "`"));
	}

	JsonPath getArray(long index);

	JsonPath getObject(String key);

	Optional<JsonPath> getParent();

	Problem createProblem(String message);

	@Override
	String toString();
}
