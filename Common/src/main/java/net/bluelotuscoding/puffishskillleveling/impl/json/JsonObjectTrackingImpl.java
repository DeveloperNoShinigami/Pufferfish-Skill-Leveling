package net.bluelotuscoding.puffishskillleveling.impl.json;

import net.bluelotuscoding.puffishskillleveling.api.json.JsonArray;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonObject;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonObjectTrackingImpl extends ForwardingJsonObject {
        private final Set<String> usedKeys = new HashSet<>();

        public JsonObjectTrackingImpl(JsonObject parent) {
                super(parent);
        }

	public List<Problem> reportUnusedEntries() {
		return parent.stream()
				.filter(entry -> !usedKeys.contains(entry.getKey()))
				.map(entry -> parent.getPath().createProblem("Unused field `" + entry.getKey() + "`"))
				.toList();
	}

        @Override
        public Result<JsonElement, Problem> get(String key) {
                usedKeys.add(key);
                return super.get(key);
        }

        @Override
        public Result<JsonObject, Problem> getObject(String key) {
                usedKeys.add(key);
                return super.getObject(key);
        }

        @Override
        public Result<JsonArray, Problem> getArray(String key) {
                usedKeys.add(key);
                return super.getArray(key);
        }

        @Override
        public Result<String, Problem> getString(String key) {
                usedKeys.add(key);
                return super.getString(key);
        }

        @Override
        public Result<Float, Problem> getFloat(String key) {
                usedKeys.add(key);
                return super.getFloat(key);
        }

        @Override
        public Result<Double, Problem> getDouble(String key) {
                usedKeys.add(key);
                return super.getDouble(key);
        }

        @Override
        public Result<Integer, Problem> getInt(String key) {
                usedKeys.add(key);
                return super.getInt(key);
        }

        @Override
        public Result<Boolean, Problem> getBoolean(String key) {
                usedKeys.add(key);
                return super.getBoolean(key);
        }
}
