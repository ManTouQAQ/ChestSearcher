plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
    id("io.papermc.paperweight.userdev") version "1.7.7"
    id("xyz.jpenilla.run-paper") version "2.2.0"
}

group = "me.mantou"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks{
    test{
        useJUnitPlatform()
    }
    processResources {
        filteringCharset = "UTF-8"
        expand(project.properties)
    }
    assemble {
        dependsOn(reobfJar)
    }
    runMojangMappedServer{
        systemProperty("file.encoding", "UTF-8")
    }
    runServer {
        minecraftVersion("1.20.6")
        systemProperty("file.encoding", "UTF-8")
    }
}

kotlin{
    jvmToolchain(21)
}