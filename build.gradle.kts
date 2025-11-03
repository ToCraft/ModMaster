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
    maven("https://maven.architectury.dev/")
    maven("https://maven.tocraft.dev/public")
    mavenLocal()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())

    api("dev.architectury.loom:dev.architectury.loom.gradle.plugin:1.11.456")
    api("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")

    api("com.gradleup.shadow:shadow-gradle-plugin:9.2.2")
    api("net.darkhax.curseforgegradle:CurseForgeGradle:1.1.27")
    api("com.modrinth.minotaur:Minotaur:2.8.10")
    api("com.diluv.schoomp:Schoomp:1.2.7")

    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")
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
