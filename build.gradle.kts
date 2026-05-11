plugins {
    id("java-library")
    id("jacoco")
}

group = "com.skadistats"
version = "5.0-SNAPSHOT"

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
    api("com.skadistats:clarity:5.0.0-SNAPSHOT")
    api("ch.qos.logback:logback-classic:1.5.32")
    annotationProcessor("com.skadistats:clarity:5.0.0-SNAPSHOT")
}

tasks.register("verifyExampleNames") {
    description = "Fail the build if two @Example annotations share a name."
    group = "verification"
    val subprojects = listOf("examples", "repro", "dev")
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

val claritySrcRoot = file("../clarity")
if (claritySrcRoot.isDirectory) {
    tasks.register<JacocoReport>("coverageReport") {
        description = "Aggregate JaCoCo report over parser classes for all *Run tasks executed with -PwithCoverage."
        group = "verification"
        dependsOn(gradle.includedBuild("clarity").task(":compileJava"))
        executionData.setFrom(
            fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") }
        )
        classDirectories.setFrom(
            files(claritySrcRoot.resolve("build/classes/java/main"))
        )
        sourceDirectories.setFrom(
            files(
                claritySrcRoot.resolve("src/main/java"),
                claritySrcRoot.resolve("build/generated/sources/annotationProcessor/java/main"),
            )
        )
        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
        }
        doFirst {
            if (executionData.files.none { it.exists() }) {
                throw GradleException(
                    "no exec files in ${layout.buildDirectory.dir("jacoco").get().asFile}. " +
                        "Run an example with -PwithCoverage first, e.g. " +
                        "./gradlew entityRunRun -PwithCoverage --args=\"<replay>\"."
                )
            }
        }
    }
}
