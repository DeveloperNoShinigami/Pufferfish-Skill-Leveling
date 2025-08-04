plugins {
    id("dev.architectury.loom")
    id("checkstyle")
}

base.archivesName.set("${project.properties["archives_base_name"]}")
version = "${project.properties["mod_version"]}-${project.properties["minecraft_version"]}-common"
group = "${project.properties["maven_group"]}"

repositories {
    maven("https://maven.puffish.net")
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.properties["minecraft_version"]}")
    mappings(loom.officialMojangMappings())

    compileOnly("net.fabricmc:sponge-mixin:${project.properties["mixin_version"]}")
    compileOnly("net.puffish:skillsmod:${project.properties["puffish_skills_dependency_version"]}:forge")
    compileOnly("org.apache.commons:commons-lang3:${project.properties["commons_lang_version"]}")

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
