package dev.tocraft.modmaster

val sourceSets = extensions.getByType<SourceSetContainer>()

val dummy: SourceSet by sourceSets.creating {
    // Only inherit main's dependencies (not output) to avoid circularity
    compileClasspath = sourceSets["main"].compileClasspath
    runtimeClasspath = sourceSets["main"].runtimeClasspath
}

// compile dummy before main
tasks.named("compileJava") {
    dependsOn(tasks.named("compileDummyJava"))
}

// make main use dummy
sourceSets["main"].compileClasspath += dummy.output
sourceSets["main"].runtimeClasspath += dummy.output