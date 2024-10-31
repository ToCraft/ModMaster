import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    `kotlin-dsl`
    java
    `maven-publish`
}

group = "dev.tocraft"

java {
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
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

    api("dev.architectury.loom:dev.architectury.loom.gradle.plugin:1.7-SNAPSHOT")
    api("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")

    api("dev.tocraft:preprocessor:1.2")
    api("com.gradleup.shadow:shadow-gradle-plugin:8.3.3")
    api("net.darkhax.curseforgegradle:CurseForgeGradle:1.1.15")
    api("com.modrinth.minotaur:Minotaur:2.8.7")
    api("com.diluv.schoomp:Schoomp:1.2.6")

    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")
}

gradlePlugin {
    plugins {
        create("settings") {
            id = "dev.tocraft.modmaster.settings"
            implementationClass = "dev.tocraft.modmaster.SettingsPlugin"
        }
    }
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
