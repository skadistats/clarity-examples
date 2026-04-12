import com.needhamsoftware.unojar.gradle.PackageUnoJarTask

plugins {
    id("java-library")
    id("com.needhamsoftware.unojar") version "1.1.0"
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

File("src/main/java/skadistats/clarity/examples").walk().maxDepth(1).forEach {
    tasks.register<JavaExec>("${it.name}Run") {
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("skadistats.clarity.examples.${it.name}.Main")
        maxHeapSize = "4g"
    }
    tasks.register<PackageUnoJarTask>("${it.name}Package") {
        dependsOn("jar")
        archiveBaseName.set(it.name)
        archiveVersion.set("")
        archiveClassifier.set("")
        mainClass.set("skadistats.clarity.examples.${it.name}.Main")
    }
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
