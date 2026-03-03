plugins {
    id("dev.architectury.loom")
    // id("checkstyle")
}

repositories {
    maven(url = "https://maven.puffish.net/")
}

base.archivesName.set("${project.properties["archives_base_name"]}")
version = "${project.properties["mod_version"]}-${project.properties["minecraft_version"]}-common"
group = "${project.properties["maven_group"]}"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.properties["minecraft_version"]}")
    mappings("net.fabricmc:yarn:${project.properties["yarn_mappings"]}:v2")

    compileOnly("net.fabricmc:sponge-mixin:${project.properties["mixin_version"]}")
    // Provide EnvType and other loader classes used by the mapped Mojang sources
    compileOnly("net.fabricmc:fabric-loader:${project.properties["fabric_loader_version"]}")

    // Dependency on the core Pufferfish Skills mod from maven repository
    modImplementation("net.puffish:skillsmod:${project.properties["skills_version"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:${project.properties["junit_version"]}")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Fabric-Loom-Remap"] = "true"
    }
}

loom {
    mixin.defaultRefmapName.set("puffish_skill_leveling-refmap.json")
}