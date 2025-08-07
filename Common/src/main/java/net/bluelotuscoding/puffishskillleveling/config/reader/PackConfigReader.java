package net.bluelotuscoding.puffishskillleveling.config.reader;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.api.SkillsAPI;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonPath;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.util.PathUtils;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;

import java.nio.file.Path;

public class PackConfigReader extends ConfigReader {
	private final ResourceManager resourceManager;
	private final String namespace;

	public PackConfigReader(ResourceManager resourceManager, String namespace) {
		this.resourceManager = resourceManager;
		this.namespace = namespace;
	}

	public Result<JsonElement, Problem> readResource(Identifier id, Resource resource) {
		try (var reader = resource.getReader()) {
			return JsonElement.parseReader(reader, JsonPath.create(id.toString()));
		} catch (Exception e) {
			return Result.failure(Problem.message("Failed to read resource `" + id + "`"));
		}
	}

	@Override
	public Result<JsonElement, Problem> read(Path path) {
		var id = Identifier.of(namespace, PathUtils.pathToString(Path.of(SkillsAPI.MOD_ID).resolve(path)));

		return resourceManager.getResource(id)
				.map(resource -> readResource(id, resource))
				.orElseGet(() -> Result.failure(Problem.message("Resource `" + id + "` does not exist")));
	}

	@Override
	public boolean exists(Path path) {
		var id = Identifier.of(namespace, PathUtils.pathToString(Path.of(SkillsAPI.MOD_ID).resolve(path)));

		return resourceManager.getResource(id).isPresent();
	}
}
