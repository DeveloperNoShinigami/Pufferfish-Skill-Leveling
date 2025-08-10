plugins {
    id("dev.architectury.loom")
    id("checkstyle")
}

base.archivesName.set("${project.properties["archives_base_name"]}")
version = "${project.properties["mod_version"]}-${project.properties["minecraft_version"]}-fabric"
group = "${project.properties["maven_group"]}"

evaluationDependsOn(":common")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    minecraft("com.mojang:minecraft:${project.properties["minecraft_version"]}")
    mappings("net.fabricmc:yarn:${project.properties["yarn_mappings"]}:v2")

    modImplementation("net.fabricmc:fabric-loader:${project.properties["fabric_loader_version"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.properties["fabric_api_version"]}")

    implementation(project(path = ":common", configuration = "namedElements"))
}

loom {
    mixin.defaultRefmapName.set("puffish_skill_leveling-refmap.json")
}

tasks.test {
    dependsOn(project(":common").tasks.test)
}

tasks.check {
    dependsOn(project(":common").tasks.check)
}

tasks.jar {
    from(project.rootDir.resolve("LICENSE.txt"))
    from(project.rootDir.resolve("LICENSE-RESOURCES.txt"))
}

tasks.processResources {
    from(project(":common").sourceSets.main.get().resources)

    inputs.property("version", project.properties["mod_version"])
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.properties["mod_version"]))
    }
}

tasks.compileJava {
    source(project(":common").sourceSets.main.get().java)
}