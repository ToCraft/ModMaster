@file:Suppress("UnstableApiUsage")

package dev.tocraft.modmaster

import dev.tocraft.gradle.preprocess.data.PreprocessExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import java.util.*
import kotlin.collections.HashMap

plugins {
    id("dev.architectury.loom")
}

extensions.configure<LoomGradleExtensionAPI> {
    silentMojangMappingsLicense()
}

dependencies {
    minecraft("com.mojang:minecraft:${parent!!.name}")
    mappings(loom.layered {
        officialMojangMappings()
        if ((parent!!.ext.get("props") as Properties)["mappings"] != null) {
            parchment("org.parchmentmc.data:parchment-${parent!!.name}:" + (parent!!.ext.get("props") as Properties)["mappings"] + "@zip")
        }
    })
}

val mcId = Integer.parseInt(((parent!!.ext["props"] as Properties)["mc_id"] ?: project.name.replace(".", "")).toString())

val remap = HashMap<String, String>()
rootDir.resolve("props").listFiles()?.forEach { file ->
    if (file.name.endsWith(".remap")) {
        val ver = Integer.parseInt(file.name.replace(".remap", ""))
        val lines = file.readLines()

        val verReMap = HashMap<String, String>()

        lines.forEach { line ->
            val arg = line.split(" -> ")
            if (ver > mcId) {
                verReMap[arg[1]] = arg[0]
            } else {
                verReMap[arg[0]] = arg[1]
            }
        }

        remap.putAll(verReMap)
    }
}

extensions.configure<PreprocessExtension> {
    remapper.putAll(remap)
}