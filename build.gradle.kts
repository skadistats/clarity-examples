plugins {
    id("java-library")
    id("me.champeau.jmh") version "0.7.2"
}

group = "com.skadistats"
version = "4.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    api("com.skadistats:clarity:4.0.1-SNAPSHOT")
    api("ch.qos.logback:logback-classic:1.5.20")
    annotationProcessor("com.skadistats:clarity:4.0.1-SNAPSHOT")

    jmhRuntimeOnly("ch.qos.logback:logback-classic:1.5.20")
}

tasks.register("bench") {
    description = "Run the entity state benchmark harness. Pass args with -PbenchArgs=\"...\"."
    group = "benchmark"
    dependsOn("jmhCompileGeneratedClasses")
    doLast {
        val cp = (files(
            "build/jmh-generated-classes",
            "build/jmh-generated-resources",
        ) + sourceSets["jmh"].runtimeClasspath).asPath
        val javaHome = System.getProperty("java.home")
        val userArgs = (project.findProperty("benchArgs") as? String)
            ?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
        val cmd = listOf(
            "$javaHome/bin/java",
            "-Xmx4g",
            "-cp", cp,
            "skadistats.clarity.bench.Main",
        ) + userArgs
        val pb = ProcessBuilder(cmd)
        pb.directory(rootDir)
        pb.inheritIO()
        val exit = pb.start().waitFor()
        if (exit != 0) throw GradleException("bench exited with $exit")
    }
}
