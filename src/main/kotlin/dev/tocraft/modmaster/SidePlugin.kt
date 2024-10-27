package dev.tocraft.modmaster

import dev.tocraft.crafted.annotations.side.SideProcessor
import dev.tocraft.modmaster.ext.VerMasterExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
class SidePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("vermaster", VerMasterExtension::class)

        project.tasks.withType(JavaCompile::class.java).configureEach {
            options.compilerArgs.add("-AmodLoader=${project.extensions.getByType(VerMasterExtension::class).modLoader.lowercase()}")
            options.compilerArgs.add("-processor=${SideProcessor::class.qualifiedName!!}")
        }
    }
}
