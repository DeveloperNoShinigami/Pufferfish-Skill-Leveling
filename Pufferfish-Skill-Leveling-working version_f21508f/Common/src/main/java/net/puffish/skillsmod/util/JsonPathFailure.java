package net.puffish.skillsmod.util;

import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.impl.json.JsonPathImpl;

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
