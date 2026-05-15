package dev.tocraft.modmaster

import gradle.kotlin.dsl.accessors._c78d6f6431bc0f1c6a7b6cafff4f4931.compileOnly

projectDir.mkdirs()

plugins {
    id("dev.tocraft.modmaster.general")
    id("net.neoforged.moddev")
}

extensions.configure<BasePluginExtension> {
    archivesName = "testmod-" + project.name
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
    compileOnly(project(":common"))

    // Needed to compile common sources that use @Environment(EnvType.CLIENT)
    val fabricenv: String? = findProperty("use_fabricenv") as String?
    if (!fabricenv.equals("false", ignoreCase = true)) {
        compileOnly("dev.tocraft:fabricenv:1.0")
    }
}

