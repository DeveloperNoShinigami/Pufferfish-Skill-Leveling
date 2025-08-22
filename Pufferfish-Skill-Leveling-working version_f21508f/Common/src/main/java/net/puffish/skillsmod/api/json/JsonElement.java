package net.puffish.skillsmod.api.json;

import com.google.gson.JsonParser;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.impl.json.JsonElementImpl;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public interface JsonElement {
	static Result<JsonElement, Problem> parseString(String jsonData, JsonPath jsonPath) {
		return parseReader(new StringReader(jsonData), jsonPath);
	}

	static Result<JsonElement, Problem> parseReader(Reader reader, JsonPath jsonPath) {
		try {
			return Result.success(new JsonElementImpl(
					JsonParser.parseReader(reader),
					jsonPath
			));
		} catch (Exception e) {
			return Result.failure(jsonPath.createProblem("Could not parse JSON due to malformed syntax"));
		}
	}

	static Result<JsonElement, Problem> parseFile(Path filePath, JsonPath jsonPath) {
		try {
			var content = Files.readString(filePath);
			if (content.isEmpty()) {
				return Result.failure(jsonPath.createProblem("File is empty"));
			}
			return parseString(content, jsonPath);
		} catch (Exception e) {
			return Result.failure(jsonPath.createProblem("Could not read file"));
		}
	}

	Result<JsonObject, Problem> getAsObject();

	Result<JsonArray, Problem> getAsArray();

	Result<String, Problem> getAsString();

	Result<Float, Problem> getAsFloat();

	Result<Double, Problem> getAsDouble();

	Result<Integer, Problem> getAsInt();

	Result<Boolean, Problem> getAsBoolean();

	JsonPath getPath();

	com.google.gson.JsonElement getJson();
}
