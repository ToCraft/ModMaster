@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import java.util.*

plugins {
    id("dev.architectury.loom")
}

extensions.configure<LoomGradleExtensionAPI> {
    silentMojangMappingsLicense()
}

// dummy source set so I do not need to implement any mod dependency at all - I just need dummies
val dummySource: SourceSet by sourceSets.creating {
    java.srcDir("src/dummy/java")

    // Only inherit main's dependencies (not output) to avoid circularity
    compileClasspath += sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["main"].runtimeClasspath
}

// compile dummy before main
tasks.named("compileJava") {
    dependsOn(tasks.named("compileDummySourceJava"))
}

// make main use dummy
sourceSets["main"].compileClasspath += dummySource.output
sourceSets["main"].runtimeClasspath += dummySource.output

dependencies {
    minecraft("com.mojang:minecraft:${parent!!.properties["minecraft"]}")
    mappings(loom.layered {
        officialMojangMappings()
        if (parent!!.properties["mappings"] != null) {
            parchment("org.parchmentmc.data:parchment-${parent!!.properties["minecraft"]}:" + parent!!.properties["mappings"] + "@zip")
        }
    })
}
