plugins {
	id("dev.architectury.loom")
	id("checkstyle")
}

repositories {
	maven(url = "https://maven.puffish.net/")
	maven(url = "https://maven.theillusivec4.top/")
}

base.archivesName.set("${project.properties["archives_base_name"]}")
version = "${project.properties["mod_version"]}-${project.properties["minecraft_version"]}-forge"
group = "${project.properties["maven_group"]}"

evaluationDependsOn(":Common")

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
	minecraft("com.mojang:minecraft:${project.properties["minecraft_version"]}")
	mappings("net.fabricmc:yarn:${project.properties["yarn_mappings"]}:v2")

	forge("net.minecraftforge:forge:${project.properties["minecraft_version"]}-${project.properties["forge_version"]}")

	// Curios API - Compile against API, use full jar at runtime
	modCompileOnly("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1:api")
	modRuntimeOnly("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1")

	// Dependency on the core Pufferfish Skills mod from maven repository
	modImplementation("net.puffish:skillsmod:${project.properties["skills_version"]}:forge")

	implementation(project(path = ":Common", configuration = "namedElements"))
}

loom {
	mixin.defaultRefmapName.set("puffish_skill_leveling-refmap.json")
	forge.mixinConfig("puffish_skill_leveling.mixins.json")
	forge.mixinConfig("puffish_skill_leveling_bridge.mixins.json")
}

tasks.test {
	dependsOn(project(":Common").tasks.test)
}

tasks.check {
	dependsOn(project(":Common").tasks.check)
}

tasks.jar {
	from(project.rootDir.resolve("LICENSE.txt"))
	from(project.rootDir.resolve("LICENSE-RESOURCES.txt"))
}

tasks.processResources {
	from(project(":Common").sourceSets.main.get().resources)

	inputs.property("version", project.properties["mod_version"])
	filesMatching("META-INF/mods.toml") {
		expand(mapOf("version" to project.properties["mod_version"]))
	}
}

tasks.compileJava {
	source(project(":Common").sourceSets.main.get().java)
}