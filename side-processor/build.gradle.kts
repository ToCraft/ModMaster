plugins {
    `java-library`
    `maven-publish`
}

group = "dev.tocraft.crafted.annotations"
version = "1.0"

base {
    archivesName = "side"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = group as String
            artifactId = base.archivesName.get()
            version = version as String
        }
    }
    repositories {
        addAll(rootProject.publishing.repositories)
    }
}