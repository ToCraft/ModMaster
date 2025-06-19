@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import dev.architectury.plugin.ArchitectPluginExtension
import java.util.*

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
    id("dev.tocraft.modmaster.general")
}

configurations {
    maybeCreate("dev")
}

artifacts {
    add("dev", tasks["jar"])
}

extensions.configure<ArchitectPluginExtension> {
    val platforms = mutableListOf<String>()
    if (parent!!.properties["fabric"] != null) {
        platforms.add("fabric")
    }
    if (parent!!.properties["forge"] != null) {
        platforms.add("forge")
    }
    if (parent!!.properties["neoforge"] != null) {
        platforms.add("neoforge")
    }
    common(platforms)
}

dependencies {
    "implementation"(project(":${parent!!.properties["minecraft"]}:common", configuration = "namedElements"))
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:${parent!!.properties["fabric_loader"]}")
}
