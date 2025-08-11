plugins {
    id("dev.architectury.loom")
    id("checkstyle")
}

base.archivesName.set("${project.properties["archives_base_name"]}")
version = "${project.properties["mod_version"]}-${project.properties["minecraft_version"]}-common"
group = "${project.properties["maven_group"]}"

repositories {
    maven("https://maven.puffish.net/releases")
}

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

    // Depend on the core Skills mod for the classes removed from this addon
    compileOnly("net.puffish.skillsmod:puffish_skills:${project.properties["mod_version"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:${project.properties["junit_version"]}")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Fabric-Loom-Remap"] = "true"
    }
}