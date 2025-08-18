plugins {
    id("dev.architectury.loom") version "1.5.388" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven(url = "https://maven.puffish.net/")
        maven(url = "https://maven.fabricmc.net")
        maven(url = "https://maven.minecraftforge.net")
        maven(url = "https://maven.architectury.dev/")
    }
}

tasks.register("publishAll") {
    group = "publishing"
    description = "Publishes all projects"
    
    subprojects {
        afterEvaluate {
            if (plugins.hasPlugin("maven-publish")) {
                dependsOn(tasks.named("publish"))
            }
        }
    }
}
