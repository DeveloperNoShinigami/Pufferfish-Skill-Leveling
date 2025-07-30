package net.puffish.skill_leveling.util;

import net.puffish.skill_leveling.api.json.JsonPath;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.impl.json.JsonPathImpl;

public class JsonPathFailure {
	public static Problem expectedToExist(JsonPath path) {
		return ((JsonPathImpl) path).expectedToExist();
	}

	public static Problem expectedToExistAndBe(JsonPath path, String str) {
		return ((JsonPathImpl) path).expectedToExistAndBe(str);
	}

	public static Problem expectedToBe(JsonPath path, String str) {
		return ((JsonPathImpl) path).expectedToBe(str);
	}
}
