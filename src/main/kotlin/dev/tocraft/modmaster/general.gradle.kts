@file:Suppress("UnstableApiUsage")

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
    minecraft("com.mojang:minecraft:${parent!!.properties["minecraft"]}")
    mappings(loom.layered {
        officialMojangMappings()
        if (parent!!.properties["mappings"] != null) {
            parchment("org.parchmentmc.data:parchment-${parent!!.properties["minecraft"]}:" + parent!!.properties["mappings"] + "@zip")
        }
    })
}
