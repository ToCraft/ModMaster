package dev.tocraft.modmaster

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

@Suppress("unused")
class SettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.rootProject.buildFileName = "root.gradle.kts"

        val forcedVersion: String = settings.providers.gradleProperty("minecraft").getOrElse("")
        val useTestmod: String = settings.providers.gradleProperty("testmod").getOrElse("false")

        settings.rootDir.resolve("props").listFiles()?.forEach { file ->
            // Check if the file's name matches the forced version
            if (forcedVersion.isNotBlank() && !file.name.startsWith(forcedVersion)) {
                return@forEach
            }

            val props = file.readLines()
            val foundFabric = props.any { it.startsWith("fabric") }
            val foundForge = props.any { it.startsWith("forge") }
            val foundNeoForge = props.any { it.startsWith("neoforge") }

            val version = file.name.removeSuffix(".properties")

            // Include main version
            settings.include(":$version")
            settings.project(":$version").apply {
                projectDir = settings.rootDir.resolve("versions/$version")
                buildFileName = "../../build.gradle.kts"
            }

            // Include common project
            settings.include("$version:common")
            settings.project("$version:common").apply {
                buildFileName = "../../../common/build.gradle.kts"
            }

            // Include testmod if applicable
            if (useTestmod == "true") {
                settings.include("$version:testmod-common")
                settings.project("$version:testmod-common").apply {
                    buildFileName = "../../../testmod-common/build.gradle.kts"
                }
            }

            // Handle fabric, forge, and neoforge projects
            if (foundFabric) {
                settings.include("$version:fabric")
                settings.project("$version:fabric").apply {
                    buildFileName = "../../../fabric/build.gradle.kts"
                }
                if (useTestmod == "true") {
                    settings.include("$version:testmod-fabric")
                    settings.project("$version:testmod-fabric").apply {
                        buildFileName = "../../../testmod-fabric/build.gradle.kts"
                    }
                }
            }

            if (foundForge) {
                settings.include("$version:forge")
                settings.project("$version:forge").apply {
                    buildFileName = "../../../forge/build.gradle.kts"
                }
                if (useTestmod == "true") {
                    settings.include("$version:testmod-forge")
                    settings.project("$version:testmod-forge").apply {
                        buildFileName = "../../../testmod-forge/build.gradle.kts"
                    }
                }
            }

            if (foundNeoForge) {
                settings.include("$version:neoforge")
                settings.project("$version:neoforge").apply {
                    buildFileName = "../../../neoforge/build.gradle.kts"
                }
                if (useTestmod == "true") {
                    settings.include("$version:testmod-neoforge")
                    settings.project("$version:testmod-neoforge").apply {
                        buildFileName = "../../../testmod-neoforge/build.gradle.kts"
                    }
                }
            }
        }
    }
}
