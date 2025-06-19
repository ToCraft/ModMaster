@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import dev.architectury.plugin.ArchitectPluginExtension

projectDir.mkdirs()

extensions.configure<SourceSetContainer> {
    named("main") {
        java {
            srcDir(rootDir.resolve("common/src/main/java"))
        }
        resources {
            srcDir(rootDir.resolve("common/src/main/resources"))
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
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:${parent!!.properties["fabric_loader"]}")
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenCommon") {
            artifactId = rootProject.properties["archives_base_name"] as String
            version = parent!!.properties["minecraft"] + "-" + rootProject.properties["mod_version"]
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
