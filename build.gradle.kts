import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    java
    `maven-publish`
}

group = "dev.tocraft"

java {
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net")
    maven("https://maven.tocraft.dev/public")
    mavenLocal()
}

dependencies {
    // gradle stuff
    implementation(gradleApi())
    implementation(localGroovy())
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")

    // publishing
    api("net.darkhax.curseforgegradle:net.darkhax.curseforgegradle.gradle.plugin:1.1.28")
    api("com.modrinth.minotaur:Minotaur:2.9.0")
    api("com.diluv.schoomp:Schoomp:1.2.7")

    // mod loader
    api("net.neoforged.moddev:net.neoforged.moddev.gradle.plugin:2.0.141")
    api("net.fabricmc.fabric-loom:net.fabricmc.fabric-loom.gradle.plugin:1.16.1")
}

publishing {
    repositories {
        if (System.getenv("MAVEN_PASS") != null) {
            maven("https://maven.tocraft.dev/public") {
                credentials {
                    username = "tocraft"
                    password = System.getenv("MAVEN_PASS")
                }
            }
        }
    }
}
