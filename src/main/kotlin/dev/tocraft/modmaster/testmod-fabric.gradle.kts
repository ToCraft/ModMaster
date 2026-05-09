package dev.tocraft.modmaster

projectDir.mkdirs()

plugins {
    id("com.modrinth.minotaur")
    id("net.darkhax.curseforgegradle")
    id("dev.tocraft.modmaster.general")
    id("net.fabricmc.fabric-loom")
}

val javaVersion = (property("java") as String).toInt()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

// Resolve common sources from :common subproject
val commonJava: Configuration by configurations.creating { isCanBeResolved = true }
val commonResources: Configuration by configurations.creating { isCanBeResolved = true }

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft")}")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader")}")

    implementation(project(":fabric"))

    commonJava(project(":testmod-common", "commonJava"))
    commonResources(project(":testmod-common", "commonResources"))
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
