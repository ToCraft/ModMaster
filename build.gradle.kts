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
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
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

    api("dev.architectury.loom:dev.architectury.loom.gradle.plugin:1.17-SNAPSHOT")
    api("architectury-plugin:architectury-plugin.gradle.plugin:3.5-SNAPSHOT")

    api("dev.tocraft:preprocessor:1.4")
    api("com.gradleup.shadow:shadow-gradle-plugin:9.6.1")
    api("net.darkhax.curseforgegradle:net.darkhax.curseforgegradle.gradle.plugin:1.3.33")
    api("com.modrinth.minotaur:Minotaur:2.9.0")
    api("com.diluv.schoomp:Schoomp:1.2.7")

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
