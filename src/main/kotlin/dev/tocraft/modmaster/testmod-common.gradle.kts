@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import dev.architectury.plugin.ArchitectPluginExtension
import java.util.*

projectDir.mkdirs()

plugins {
    id("dev.tocraft.modmaster.general")
}

extensions.configure<BasePluginExtension> {
    archivesName = rootProject.properties["archives_base_name"] as String + "-" + project.name
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
    "implementation"(project(":common", configuration = "namedElements"))
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:${parent!!.properties["fabric_loader"]}")
}
