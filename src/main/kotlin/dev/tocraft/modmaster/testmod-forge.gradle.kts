@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.modrinth.minotaur.ModrinthExtension
import dev.architectury.plugin.ArchitectPluginExtension
import dev.tocraft.gradle.preprocess.PreprocessExtension
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import java.util.*

projectDir.mkdirs()

extensions.configure<SourceSetContainer> {
    named("main") {
        java {
            setSrcDirs(listOf(rootDir.resolve("testmod-forge/src/main/java")))
        }
        resources {
            setSrcDirs(listOf(rootDir.resolve("testmod-forge/src/main/resources")))
        }
    }
}

plugins {
    id("com.github.johnrengelman.shadow")
    id("dev.tocraft.preprocessor")
    id("dev.tocraft.modmaster.general")
}

extensions.configure<PreprocessExtension> {
    val mcId = (parent!!.ext["props"] as Properties)["mc_id"] ?: project.name.replace(".", "")
    vars["MC"] = mcId
}

val commonAccessWidener: RegularFileProperty = project(":${parent!!.name}:testmod-common").extensions.getByName<LoomGradleExtensionAPI>("loom").accessWidenerPath

if (commonAccessWidener.isPresent) {
    extensions.configure<LoomGradleExtensionAPI> {
        accessWidenerPath.set(commonAccessWidener)

        forge {
            convertAccessWideners = true
            extraAccessWideners.add(accessWidenerPath.get().asFile.name)
        }
    }
}

extensions.configure<ArchitectPluginExtension> {
    platformSetupLoomIde()
    forge()
}

configurations {
    maybeCreate("common")
    maybeCreate("shadowCommon")
    maybeCreate("compileClasspath").extendsFrom(getByName("common"))
    maybeCreate("runtimeClasspath").extendsFrom(getByName("common"))
    maybeCreate("developmentForge").extendsFrom(getByName("common"))
}

dependencies {
    "forge"("net.minecraftforge:forge:${parent!!.name}-${(parent!!.ext["props"] as Properties)["forge"]}")

    "common"(project(":${parent!!.name}:testmod-common", configuration = "namedElements")) {
        isTransitive = false
    }
    "shadowCommon"(project(":${parent!!.name}:testmod-common", configuration = "transformProductionForge")) {
        isTransitive = false
    }
    "common"(project(":${parent!!.name}:common", configuration = "namedElements")) {
        isTransitive = false
    }
}

tasks.getByName<ShadowJar>("shadowJar") {
    exclude("architectury.common.json")
    configurations = listOf(project.configurations["shadowCommon"])
    archiveClassifier = "dev-shadow"
}


tasks.getByName<RemapJarTask>("remapJar") {
    dependsOn(tasks.getByName<ShadowJar>("shadowJar"))
    inputFile.set(tasks.getByName<ShadowJar>("shadowJar").archiveFile)
}

tasks.getByName<Jar>("sourcesJar") {
    val commonSources = project(":${parent!!.name}:common").tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}

components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(project.configurations["shadowRuntimeElements"]) {
        skip()
    }
}
