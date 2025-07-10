@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.architectury.plugin.ArchitectPluginExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask

projectDir.mkdirs()

plugins {
    id("com.gradleup.shadow")
    id("dev.tocraft.modmaster.general")
}

extensions.configure<BasePluginExtension> {
    archivesName = rootProject.properties["archives_base_name"] as String + "-" + project.name
}

val commonAccessWidener: RegularFileProperty =
    project(":testmod-common").extensions.getByName<LoomGradleExtensionAPI>("loom").accessWidenerPath

if (commonAccessWidener.isPresent) {
    extensions.configure<LoomGradleExtensionAPI> {
        accessWidenerPath.set(commonAccessWidener)
    }
}

extensions.configure<ArchitectPluginExtension> {
    platformSetupLoomIde()
    fabric()
}

configurations {
    maybeCreate("common")
    maybeCreate("shadowCommon")
    maybeCreate("compileClasspath").extendsFrom(getByName("common"))
    maybeCreate("runtimeClasspath").extendsFrom(getByName("common"))
    maybeCreate("developmentFabric").extendsFrom(getByName("common"))
}

dependencies {
    "modImplementation"("net.fabricmc:fabric-loader:${parent!!.properties["fabric_loader"]}")
    "modApi"("net.fabricmc.fabric-api:fabric-api:${parent!!.properties["fabric"]}+${parent!!.properties["minecraft"]}")

    "common"(project(":testmod-common", configuration = "namedElements")) {
        isTransitive = false
    }
    "shadowCommon"(project(":testmod-common", configuration = "transformProductionFabric")) {
        isTransitive = false
    }
    "common"(project(":common", configuration = "namedElements")) {
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
    val commonSources = project(":testmod-common").tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}

components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(project.configurations["shadowRuntimeElements"]) {
        skip()
    }
}
