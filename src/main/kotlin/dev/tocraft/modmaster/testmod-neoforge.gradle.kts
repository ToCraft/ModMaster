package dev.tocraft.modmaster

projectDir.mkdirs()

plugins {
    id("com.modrinth.minotaur")
    id("net.darkhax.curseforgegradle")
    id("dev.tocraft.modmaster.general")
    id("net.neoforged.moddev")
}

extensions.configure<BasePluginExtension> {
    archivesName = "testmod-" + project.name
}

extensions.configure<BasePluginExtension> {
    archivesName = rootProject.properties["modid"] as String + "-" + project.name
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

neoForge {
    version = property("neoforge") as String

    runs {
        mods {
            create(rootProject.properties["modid"] as String) {
                sourceSet(project(":neoforge").sourceSets.main.get())
                sourceSet(commonDummy)
            }
            create("testmod") {
                sourceSet(sourceSets.main.get())
            }
        }
        create("client") {
            client()
        }

        create("server") {
            server()
            programArgument("--nogui")
        }
    }
}

dependencies {
    commonJava(project(":testmod-common", "commonJava"))
    commonResources(project(":testmod-common", "commonResources"))

    implementation(project(":neoforge"))

    // Needed to compile common sources that use @Environment(EnvType.CLIENT)
    val fabricenv: String? = findProperty("use_fabricenv") as String?
    if (!fabricenv.equals("false", ignoreCase = true)) {
        compileOnly("dev.tocraft:fabricenv:1.0")
    }

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
