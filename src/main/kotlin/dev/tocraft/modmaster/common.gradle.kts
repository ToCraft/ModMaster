@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import dev.architectury.plugin.ArchitectPluginExtension
import dev.tocraft.gradle.preprocess.data.PreprocessExtension
import dev.tocraft.modmaster.ext.ModMasterExtension
import java.util.Properties

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


    dependencies {
        // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
        // Do NOT use other classes from fabric loader
        modImplementation("net.fabricmc:fabric-loader:${parent!!.properties["fabric_loader"]}")
    }
} else {
    dependencies {
        implementation("dev.tocraft.crafted.annotations:side:1.0")
    }
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenCommon") {
            artifactId = rootProject.properties["archives_base_name"] as String
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
