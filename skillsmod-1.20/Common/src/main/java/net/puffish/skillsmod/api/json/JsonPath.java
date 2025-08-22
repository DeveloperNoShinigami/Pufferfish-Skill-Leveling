package net.puffish.skillsmod.api.json;

import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.impl.json.JsonPathImpl;

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
