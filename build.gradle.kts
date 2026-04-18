plugins {
    id("java-library")
    id("me.champeau.jmh") version "0.7.3"
}

group = "com.skadistats"
version = "4.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    api("com.skadistats:clarity:4.0.1-SNAPSHOT")
    api("ch.qos.logback:logback-classic:1.5.32")
    annotationProcessor("com.skadistats:clarity:4.0.1-SNAPSHOT")

    jmhRuntimeOnly("ch.qos.logback:logback-classic:1.5.32")
}

tasks.register("verifyExampleNames") {
    description = "Fail the build if two @Example annotations share a name."
    group = "verification"
    val subprojects = listOf("examples", "repro", "dev", "bench")
    val pattern = Regex("""@Example\s*\(\s*name\s*=\s*"([^"]+)"""")
    doLast {
        val seen = mutableMapOf<String, MutableList<String>>()
        subprojects.forEach { sp ->
            val root = rootDir.resolve("$sp/src/main/java")
            if (!root.isDirectory) return@forEach
            root.walkTopDown()
                .filter { it.isFile && it.name == "Main.java" }
                .forEach { f ->
                    pattern.find(f.readText())?.groupValues?.get(1)?.let { name ->
                        seen.getOrPut(name) { mutableListOf() }.add(f.relativeTo(rootDir).invariantSeparatorsPath)
                    }
                }
        }
        val dupes = seen.filterValues { it.size > 1 }
        if (dupes.isNotEmpty()) {
            val msg = dupes.entries.joinToString("\n") { (n, files) ->
                "  '$n' used in:\n    - ${files.joinToString("\n    - ")}"
            }
            throw GradleException("Duplicate @Example.name values found:\n$msg")
        }
        logger.lifecycle("Verified ${seen.size} unique @Example names across ${subprojects.size} subprojects.")
    }
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("verifyExampleNames"))
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
