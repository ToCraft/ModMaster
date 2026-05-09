package dev.tocraft.modmaster

import com.diluv.schoomp.Webhook
import com.diluv.schoomp.message.Message
import com.diluv.schoomp.message.embed.Embed
import java.io.FileWriter
import java.io.IOException

allprojects {
    version = project.properties["mod_version"] as String
    group = project.properties["maven_group"] as String
}

subprojects {
    apply(plugin = "java")

    repositories {
        maven("https://maven.tocraft.dev/public")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(Integer.parseInt(properties["java"] as String))
    }
}

val supportedVersions = ArrayList<String>()
supportedVersions.add(properties["minecraft"] as String)
if (properties["supported_versions"] != null) {
    (properties["supported_versions"] as String).split(",").forEach {
        val version = it.trim()
        if (!supportedVersions.contains(version)) {
            supportedVersions.add(version)
        }
    }
}
project.extra["supported_versions"] = supportedVersions

project.extra["releaseChangelog"] = releaseChangelog(1)

fun releaseChangelog(versions: Int): String {
    try {
        var changelog = ""
        var match = 0
        var previousIT = ""
        rootProject.file("CHANGELOG.md").forEachLine {
            if (it.contains("================")) {
                match++
            }
            if (match <= versions) {
                changelog += previousIT + "\n"
            }
            previousIT = it
            return@forEachLine
        }
        return changelog + "\n\n"
    } catch (exception: Exception) {
        return "${rootProject.properties["modid"]} ${rootProject.properties["mod_version"]}\n==========\nThere was an error generating the changelog" + exception.localizedMessage
    }
}

//Splits the changelog into multiple parts if they get bigger than discords embed field size (1024)
fun discordChangelog(): List<String> {
    val changelog = releaseChangelog(1)
    val res = mutableListOf<String>()
    if (changelog.length < 1024) {
        res.add(changelog)
        return res
    }
    var temp = ""
    changelog.split("\n").forEach {
        val it2 = it + "\n"
        if ((temp.length + it2.length) >= 1024) {
            res.add(temp)
            temp = it2
        } else
            temp += it2
    }
    res.add(temp)
    return res
}

// based on the code by Flemmli97
tasks.register("discordRelease") {
    doLast {
        try {
            val webhook = Webhook(System.getenv("DISCORD_WEB_HOOK"), "${project.name} Upload")

            val message = Message()
            message.username = "Mod Release"
            var content = "${project.name} ${rootProject.properties["mod_version"]} has been released!"
            if (rootProject.hasProperty("ping_role")) {
                content = "<@&${rootProject.properties["ping_role"]}> " + content
            }
            message.content = content
            message.avatarUrl = "https://avatars.githubusercontent.com/u/38883321"

            val embed = Embed()

            val changelog = discordChangelog()
            if (changelog.size == 1)
                embed.addField("Change Log", "```md\n${changelog[0]}```", false)
            else
                changelog.forEach {
                    embed.addField("Change Log", "```md\n${it}```", false)
                }
            embed.color = 0xFF8000
            message.addEmbed(embed)

            webhook.sendMessage(message)
        } catch (_: IOException) {
            println("Failed to push to the Discord webhook.")
        }
        println("Send Changelog to Discord.")
    }
}

tasks.register("extractNewestChangelog") {
    val fileName = "extracted.CHANGELOG.md"
    // delete file if exists
    delete(fileName)
    doLast {
        // write changelog
        val fw = FileWriter(fileName)
        fw.write(releaseChangelog(1))
        fw.close()
        println("Extracted newest Changelog to \"extracted.CHANGELOG.md\"")
    }
}


tasks.register<Zip>("packTheMod") {
    archiveFileName.set("${rootProject.properties["modid"]}-${rootProject.version}.zip")
    destinationDirectory.set(project.layout.buildDirectory)

    // Include paths
    from(layout.buildDirectory) {
        include("**/build/libs/**")
        // Exclude paths
        exclude("reports/")
        exclude("tmp/")
        exclude("*/libs/")
        exclude("libs/")
        exclude("**/*-dev.jar")
        exclude("**/*-shadow.jar")
        exclude("**/*-transformProduction*.jar")
        exclude("**/testmod*/**")
        exclude("**/build/tmp/")
        exclude("**/build/resources/")
        exclude("**/build/processIncludeJars/")
        exclude("**/build/generated/")
        exclude("**/build/devlibs/")
        exclude("**/build/classes/")
        exclude("**/build/publications/")
        exclude("**/build/loom-cache/")
    }

    subprojects.forEach { sub ->
        if (!sub.name.startsWith("testmod")) {
            from(sub.layout.buildDirectory.dir("libs/")) {
                // Exclude paths
                exclude("*-dev.jar")
                exclude("*-shadow.jar")
                exclude("*-transformProduction*.jar")
            }
        }

        dependsOn(sub.tasks.getByName("build"))
    }
}

tasks.register("release") {
    subprojects.forEach { sub ->
        if (!sub.name.startsWith("testmod")) {
            dependsOn(sub.tasks.getByName("publish"))
        }
    }
    subprojects.forEach { sub ->
        if (!sub.name.startsWith("testmod") && !sub.name.contains("common")) {
            dependsOn(sub.tasks.getByName("modrinth"))
        }
    }
    subprojects.forEach { sub ->
        if (!sub.name.startsWith("testmod") && !sub.name.contains("common")) {
            dependsOn(sub.tasks.getByName("curseforge"))
        }
    }
    dependsOn(tasks.getByPath("packTheMod"))
    dependsOn(tasks.getByPath("extractNewestChangelog"))

    if (properties["artifact_type"] == "release") {
        dependsOn(tasks.getByPath("discordRelease"))
    }
}