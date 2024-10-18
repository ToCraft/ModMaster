package dev.tocraft.modmaster

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
class SidePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val modLoader = project.extensions.getByType<ModMasterExtension>().modLoader.lowercase()
        project.tasks.withType(JavaCompile::class.java).configureEach {
            options.compilerArgs.add("-AmodLoader=${modLoader}")
        }
    }
}
