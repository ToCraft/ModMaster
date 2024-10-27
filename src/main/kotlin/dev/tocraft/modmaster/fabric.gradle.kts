@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.modrinth.minotaur.ModrinthExtension
import dev.architectury.plugin.ArchitectPluginExtension
import dev.tocraft.gradle.preprocess.data.PreprocessExtension
import dev.tocraft.gradle.preprocess.tasks.PreProcessTask
import dev.tocraft.modmaster.ext.ModMasterExtension
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import java.util.*

projectDir.mkdirs()

extensions.configure<SourceSetContainer> {
    named("main") {
        java {
            srcDir(rootDir.resolve("fabric/src/main/java"))
        }
        resources {
            srcDir(rootDir.resolve("fabric/src/main/resources"))
        }
    }
}

plugins {
    id("com.gradleup.shadow")
    id("dev.tocraft.preprocessor")
    id("com.modrinth.minotaur")
    id("net.darkhax.curseforgegradle")
    id("dev.tocraft.modmaster.general")
}

extensions.configure<PreprocessExtension> {
    val mcId = (parent!!.ext["props"] as Properties)["mc_id"] ?: project.name.replace(".", "")
    vars["MC"] = mcId
}

val commonAccessWidener: RegularFileProperty = project(":${parent!!.name}:common").extensions.getByName<LoomGradleExtensionAPI>("loom").accessWidenerPath

if (commonAccessWidener.isPresent) {
    extensions.configure<LoomGradleExtensionAPI> {
        accessWidenerPath.set(commonAccessWidener)
    }
}

var useArchPlugin = rootProject.extensions.findByType(ModMasterExtension::class)?.useArchPlugin;

if (useArchPlugin != false) {
    extensions.configure<ArchitectPluginExtension> {
        platformSetupLoomIde()
        fabric()
    }
} else {
    apply(plugin = "dev.tocraft.modmaster.sideprocessor")

    dependencies {
        implementation("dev.tocraft.crafted.annotations:side:1.0")
    }

    extensions.configure<PreprocessExtension> {
        remapper["dev.tocraft.crafted.annotations.side.Side"] = "net.fabricmc.api.Environment"
        remapper["dev.tocraft.crafted.annotations.side.Env"] = "net.fabricmc.api.EnvType"
        remapper["@Side(Env.CLIENT)"] = "@Environment(EnvType.CLIENT)"
        remapper["@Side(Env.DEDICATED_SERVER)"] = "@Environment(EnvType.SERVER)"
    }
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
    "modApi"("net.fabricmc.fabric-api:fabric-api:${(parent!!.ext.get("props") as Properties)["fabric"]}+${parent!!.name}")

    if (useArchPlugin != false) {
        "common"(project(":${parent!!.name}:common", configuration = "namedElements")) {
            isTransitive = false
        }
        "shadowCommon"(project(":${parent!!.name}:common", configuration = "transformProductionFabric")) {
            isTransitive = false
        }
    } else {
        "compileOnly"(project(":${parent!!.name}:common", configuration = "namedElements")) {
            isTransitive = false
        }
    }
}

tasks.getByName<ShadowJar>("shadowJar") {
    exclude("architectury.common.json")
    configurations = listOf(project.configurations["shadowCommon"])
    archiveClassifier = "dev-shadow"
}


tasks.getByName<RemapJarTask>("remapJar") {
    if (commonAccessWidener.isPresent) {
        injectAccessWidener = true
    }
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

extensions.configure<ModrinthExtension> {
    token = System.getenv("MODRINTH_TOKEN")
    projectId = parent!!.properties["modrinth_id"] as String
    versionNumber = "${parent!!.name}-${project.name}-${project.version}"
    versionType = "${parent!!.properties["artifact_type"]}"
    uploadFile = tasks.getByName("remapJar")
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

    (parent!!.properties["supported_versions"] as List<*>).forEach { gameVersions.add((it as String).trim()) }
}

tasks.create<TaskPublishCurseForge>("curseforge") {
    apiToken = System.getenv("CURSEFORGE_TOKEN")

    // The main file to upload
    val mainFile = upload("${parent!!.properties["curseforge_id"]}", tasks.getByName("remapJar"))
    mainFile.displayName = "${parent!!.name}-${project.name}-${project.version}"
    mainFile.releaseType = "${parent!!.properties["artifact_type"]}"
    mainFile.changelog = rootProject.ext.get("releaseChangelog")
    mainFile.changelogType = "markdown"
    mainFile.addModLoader("fabric")
    mainFile.addModLoader("quilt")
    mainFile.addJavaVersion("Java ${(parent!!.ext.get("props") as Properties)["java"]}")
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

    (parent!!.properties["supported_versions"] as List<*>).forEach { mainFile.addGameVersion((it as String).trim()) }
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenFabric") {
            artifactId = "${rootProject.properties["archives_base_name"]}-${project.name}"
            version = parent!!.name + "-" + rootProject.properties["mod_version"]
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

