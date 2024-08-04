package dev.tocraft.modmaster

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import java.util.*

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