package dev.tocraft.modmaster

import java.util.Locale
import java.util.Locale.getDefault

projectDir.mkdirs()

plugins {
    id("dev.tocraft.modmaster.general")
    id("maven-publish")
    id("net.neoforged.moddev")
}

extensions.configure<BasePluginExtension> {
    archivesName = rootProject.properties["modid"] as String + "-" + project.name
}

val javaVersion = (property("java") as String).toInt()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

neoForge {
    neoFormVersion = property("neoform_version") as String
}

// Expose common sources as consumable artifacts for loader subprojects
val commonJava: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}
val commonResources: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add("commonJava", sourceSets.main.get().java.sourceDirectories.singleFile)
    add("commonResources", sourceSets.main.get().resources.sourceDirectories.singleFile)
}

dependencies {
    // Needed to compile common sources that use @Environment(EnvType.CLIENT)
    val fabricenv : String? = findProperty("use_fabricenv") as String?
    if (!fabricenv.equals("false", ignoreCase = true)) {
        compileOnly("dev.tocraft:fabricenv:1.0")
    }
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenCommon") {
            artifactId = rootProject.properties["modid"] as String
            version = rootProject.properties["mod_version"] as String
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
