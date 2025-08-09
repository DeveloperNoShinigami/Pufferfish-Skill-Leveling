package net.bluelotuscoding.puffishskillleveling.util;

import net.bluelotuscoding.puffishskillleveling.api.json.JsonPath;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.impl.json.JsonPathImpl;

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
