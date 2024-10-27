package dev.tocraft.modmaster

import dev.architectury.plugin.ArchitectPluginExtension
import dev.tocraft.modmaster.ext.ModMasterExtension
import java.util.*

var useArchPlugin = rootProject.extensions.findByType(ModMasterExtension::class)?.useArchPlugin

if (useArchPlugin == false) {
    plugins {
        id("dev.tocraft.preprocessor")
    }
} else {
    plugins {
        id("dev.tocraft.preprocessor")
        id("architectury-plugin")
    }

    extensions.configure<ArchitectPluginExtension> {
        minecraft = project.name
    }
}

projectDir.mkdirs()

// Read version-specific properties file
val props = Properties()
val inStream = file("../../props/${project.name}.properties").inputStream()
props.load(inStream)
project.ext.set("props", props)

println()
println("Minecraft: ${project.name} (Java ${props["java"]})")
if (props["mappings"] != null) {
    println("├── Parchment: ${props["mappings"]}")
}
if (props["fabric"] != null) {
    println("├── Fabric: ${props["fabric"]}")
}
if (props["forge"] != null) {
    println("├── Forge: ${props["forge"]}")
}
if (props["neoforge"] != null) {
    println("├── NeoForge: ${props["neoforge"]}")
}

val supportedVersions = ArrayList<String>()
supportedVersions.add(project.name)
if (props["supported_versions"] != null) {
    (props["supported_versions"] as String).split(",").forEach {
        val version = it.trim()
        if (!supportedVersions.contains(version)) {
            supportedVersions.add(version)
        }
    }
}
project.ext.set("supported_versions", supportedVersions)
println("└── Supported Minecraft Versions: $supportedVersions")
println()
layout.buildDirectory.set(rootDir.toPath().resolve("build/${project.name}").toFile())

subprojects {
    layout.buildDirectory.set(parent!!.layout.buildDirectory.file(project.name).get().asFile)

    apply(plugin = "maven-publish")

    repositories {
        mavenLocal()
    }
}

allprojects {
    apply(plugin = "java")
    if (useArchPlugin != false) {
        apply(plugin = "architectury-plugin")
    }

    repositories {
        maven("https://maven.tocraft.dev/public")
    }

    extensions.configure<BasePluginExtension> {
        archivesName = rootProject.properties["archives_base_name"] as String
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(Integer.parseInt(props["java"] as String))
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }
}
