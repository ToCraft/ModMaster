@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.modrinth.minotaur.ModrinthExtension
import dev.architectury.plugin.ArchitectPluginExtension
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import java.util.*

projectDir.mkdirs()

extensions.configure<SourceSetContainer> {
    named("main") {
        java {
            srcDir(rootDir.resolve("neoforge/src/main/java"))
        }
        resources {
            srcDir(rootDir.resolve("neoforge/src/main/resources"))
        }
    }
}

plugins {
    id("maven-publish")
    id("com.gradleup.shadow")
    id("com.modrinth.minotaur")
    id("net.darkhax.curseforgegradle")
    id("dev.tocraft.modmaster.general")
}

val commonAccessWidener: RegularFileProperty =
    project(":${parent!!.properties["minecraft"]}:common").extensions.getByName<LoomGradleExtensionAPI>("loom").accessWidenerPath

if (commonAccessWidener.isPresent) {
    extensions.configure<LoomGradleExtensionAPI> {
        accessWidenerPath.set(commonAccessWidener)
    }
}

extensions.configure<ArchitectPluginExtension> {
    platformSetupLoomIde()
    neoForge()
}

configurations {
    maybeCreate("common")
    maybeCreate("shadowCommon")
    maybeCreate("compileClasspath").extendsFrom(getByName("common"))
    maybeCreate("runtimeClasspath").extendsFrom(getByName("common"))
    maybeCreate("developmentNeoforge").extendsFrom(getByName("common"))
}

dependencies {
    "neoForge"("net.neoforged:neoforge:${parent!!.properties["neoforge"]}")

    "common"(project(":${parent!!.properties["minecraft"]}:common", configuration = "namedElements")) {
        isTransitive = false
    }
    "shadowCommon"(project(":${parent!!.properties["minecraft"]}:common", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
}

tasks.getByName<ShadowJar>("shadowJar") {
    exclude("fabric.mod.json")
    exclude("architectury.common.json")
    configurations = listOf(project.configurations["shadowCommon"])
    archiveClassifier = "dev-shadow"
}


tasks.getByName<RemapJarTask>("remapJar") {
    dependsOn(tasks.getByName<ShadowJar>("shadowJar"))
    inputFile.set(tasks.getByName<ShadowJar>("shadowJar").archiveFile)
}

tasks.getByName<Jar>("sourcesJar") {
    val commonSources = project(":${parent!!.properties["minecraft"]}:common").tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}

components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(project.configurations["shadowRuntimeElements"]) {
        skip()
    }
}

val modrinthId = parent!!.properties["modrinth_id"]
if (modrinthId != null) {
    extensions.configure<ModrinthExtension> {
        token = System.getenv("MODRINTH_TOKEN")
        projectId = modrinthId as String
        versionNumber = "${parent!!.properties["minecraft"]}-${project.name}-${project.version}"
        versionType = "${parent!!.properties["artifact_type"]}"
        uploadFile = tasks.getByName("remapJar")
        gameVersions = listOf()
        loaders = listOf("neoforge")
        changelog.set(rootProject.ext.get("releaseChangelog") as String)
        dependencies {
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

        (parent!!.properties["supported_versions"] as List<*>).forEach { gameVersions.add((it as String).trim()) }
    }
}

val cfId = parent!!.properties["curseforge_id"]
if (cfId != null) {
    tasks.register<TaskPublishCurseForge>("curseforge") {
        apiToken = System.getenv("CURSEFORGE_TOKEN")

        // The main file to upload
        val mainFile = upload("$cfId", tasks.getByName("remapJar"))
        mainFile.displayName = "${parent!!.properties["minecraft"]}-${project.name}-${project.version}"
        mainFile.releaseType = "${parent!!.properties["artifact_type"]}"
        mainFile.changelog = rootProject.ext.get("releaseChangelog")
        mainFile.changelogType = "markdown"
        mainFile.addModLoader("neoforge")
        mainFile.addJavaVersion("Java ${parent!!.properties["java"]}")
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

        (parent!!.properties["supported_versions"] as List<*>).forEach { mainFile.addGameVersion((it as String).trim()) }
    }
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenNeoForge") {
            artifactId = "${rootProject.properties["archives_base_name"]}-${project.name}"
            version = (parent!!.properties["minecraft"] as String) + "-" + rootProject.properties["mod_version"]
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

