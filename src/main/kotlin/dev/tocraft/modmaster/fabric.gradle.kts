package dev.tocraft.modmaster

import com.modrinth.minotaur.ModrinthExtension
import net.darkhax.curseforgegradle.TaskPublishCurseForge

projectDir.mkdirs()

plugins {
    id("maven-publish")
    id("com.modrinth.minotaur")
    id("net.darkhax.curseforgegradle")
    id("dev.tocraft.modmaster.general")
    id("net.fabricmc.fabric-loom")
}

val javaVersion = (property("java") as String).toInt()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

// Resolve common sources from :common subproject
val commonJava: Configuration by configurations.creating { isCanBeResolved = true }
val commonResources: Configuration by configurations.creating { isCanBeResolved = true }

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft")}")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader")}")

    commonJava(project(":common", "commonJava"))
    commonResources(project(":common", "commonResources"))
}

// Include common sources in this compilation
tasks.compileJava { source(commonJava) }
tasks.javadoc { source(commonJava) }

tasks.named<Jar>("sourcesJar") {
    from(commonJava)
    from(commonResources)
}

tasks.processResources {
    from(commonResources)
}

val modrinthId = parent!!.properties["modrinth_id"]
if (modrinthId != null) {
    extensions.configure<ModrinthExtension> {
        token = System.getenv("MODRINTH_TOKEN")
        projectId = modrinthId as String
        versionNumber = "${project.name}-${project.version}"
        versionType = "${parent!!.properties["artifact_type"]}"
        uploadFile = components["java"]
        gameVersions = listOf()
        loaders = listOf("fabric", "quilt")
        changelog.set(rootProject.ext.get("releaseChangelog") as String)
        dependencies {
            required.project("fabric-api")
            if (project.hasProperty("required_dependencies") && (project.properties["required_dependencies"] as String).isNotBlank()) {
                (project.properties["required_dependencies"] as String).split(',').forEach {
                    required.project(it)
                }
            }
            if (project.hasProperty("optional_dependencies") && (project.properties["optional_dependencies"] as String).isNotBlank()) {
                (project.properties["optional_dependencies"] as String).split(',').forEach {
                    optional.project(it)
                }
            }
        }

        (parent!!.extra["supported_versions"] as List<*>).forEach { gameVersions.add((it as String).trim()) }
    }
}

val cfId = parent!!.properties["curseforge_id"]
if (cfId != null) {
    tasks.register<TaskPublishCurseForge>("curseforge") {
        apiToken = System.getenv("CURSEFORGE_TOKEN")

        // The main file to upload
        val mainFile = upload("$cfId", components["java"])
        mainFile.displayName = "${project.name}-${project.version}"
        mainFile.releaseType = "${parent!!.properties["artifact_type"]}"
        mainFile.changelog = rootProject.ext.get("releaseChangelog")
        mainFile.changelogType = "markdown"
        mainFile.addModLoader("fabric")
        mainFile.addModLoader("quilt")
        mainFile.addJavaVersion("Java ${parent!!.properties["java"]}")
        mainFile.addRequirement("fabric-api")
        if (project.hasProperty("required_dependencies") && (project.properties["required_dependencies"] as String).isNotBlank()) {
            (project.properties["required_dependencies"] as String).split(',').forEach {
                mainFile.addRequirement(it)
            }
        }
        if (project.hasProperty("optional_dependencies") && (project.properties["optional_dependencies"] as String).isNotBlank()) {
            (project.properties["optional_dependencies"] as String).split(',').forEach {
                mainFile.addOptional(it)
            }
        }

        (parent!!.extra["supported_versions"] as List<*>).forEach { mainFile.addGameVersion((it as String).trim()) }
    }
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenFabric") {
            artifactId = "${rootProject.properties["modid"]}-${project.name}"
            version = rootProject.properties["mod_version"] as String
            from(components["java"])
        }
    }
    repositories {
        if (System.getenv("MAVEN_PASS") != null) {
            maven("https://maven.tocraft.dev/public") {
                name = "ToCraftMavenPublic"
                credentials {
                    username = "tocraft"
                    password = System.getenv("MAVEN_PASS")
                }
            }
        }
    }
}

