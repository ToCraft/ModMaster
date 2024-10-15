@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import dev.architectury.plugin.ArchitectPluginExtension
import dev.tocraft.gradle.preprocess.PreprocessExtension
import java.util.Properties

projectDir.mkdirs()

extensions.configure<SourceSetContainer> {
    named("main") {
        java {
            setSrcDirs(listOf(rootDir.resolve("testmod-common/src/main/java")))
        }
        resources {
            setSrcDirs(listOf(rootDir.resolve("testmod-common/src/main/resources")))
        }
    }
}

plugins {
    id("dev.tocraft.preprocessor")
    id("dev.tocraft.modmaster.general")
}

configurations {
    maybeCreate("dev")
}

artifacts {
    add("dev", tasks["jar"])
}

extensions.configure<PreprocessExtension> {
    val mcId = (parent!!.ext["props"] as Properties)["mc_id"] ?: project.name.replace(".", "")
    vars["MC"] = mcId
}

var useArchPlugin = rootProject.extensions.findByType(ModMasterExtension::class)?.useArchPlugin;

if (useArchPlugin != false) {
    extensions.configure<ArchitectPluginExtension> {
        val props = parent!!.ext.get("props") as Properties
        val platforms = mutableListOf<String>()
        if (props["fabric"] != null) {
            platforms.add("fabric")
        }
        if (props["forge"] != null) {
            platforms.add("forge")
        }
        if (props["neoforge"] != null) {
            platforms.add("neoforge")
        }
        common(platforms)
    }
}

dependencies {
    "implementation"(project(":${parent!!.name}:common", configuration = "namedElements"))
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:${parent!!.properties["fabric_loader"]}")
}
