package dev.tocraft.modmaster

import gradle.kotlin.dsl.accessors._58b8aed1b243e11ec84f3ca9a5f259a9.compileOnly

projectDir.mkdirs()

plugins {
    id("dev.tocraft.modmaster.general")
    id("net.fabricmc.fabric-loom")
}

extensions.configure<BasePluginExtension> {
    archivesName = "testmod-" + project.name
}

val javaVersion = (property("java") as String).toInt()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

// Resolve common sources from :common subproject
val commonJava: Configuration by configurations.creating { isCanBeResolved = true }
val commonResources: Configuration by configurations.creating { isCanBeResolved = true }

// make use of common dummy
val commonDummy: SourceSet = project(":testmod-common").extensions.getByType<SourceSetContainer>().getByName("dummy")
val sourceSets = extensions.getByType<SourceSetContainer>()
sourceSets["main"].compileClasspath += commonDummy.output
sourceSets["main"].runtimeClasspath += commonDummy.output

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft")}")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader")}")

    implementation(project(":fabric"))

    commonJava(project(":testmod-common", "commonJava"))
    commonResources(project(":testmod-common", "commonResources"))

    // for IDEA detection
    compileOnly(project(":testmod-common"))
}

// Include common sources in this compilation
tasks.compileJava { source(commonJava) }
tasks.javadoc { source(commonJava) }

tasks.named<Jar>("sourcesJar") {
    from(commonJava)
    from(commonResources)
}

tasks.processResources {
    from(commonResources)
}
