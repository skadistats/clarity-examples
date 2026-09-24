import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    id("examples-base")
    id("com.gradleup.shadow")
}

tasks.named("shadowJar") { enabled = false }

val withCoverage = providers.gradleProperty("withCoverage").isPresent
val coverageDir = rootProject.layout.buildDirectory.dir("jacoco")

the<JacocoPluginExtension>().applyTo(tasks.withType<JavaExec>())

val srcRoot = file("src/main/java")
if (srcRoot.isDirectory) {
    srcRoot.walkTopDown()
        .filter { it.isFile && it.name == "Main.java" }
        .forEach { mainFile ->
            val exampleDir = mainFile.parentFile
            val exampleName = exampleDir.name
            val pkgPath = exampleDir.relativeTo(srcRoot).invariantSeparatorsPath
            val fqMainClass = "${pkgPath.replace('/', '.')}.Main"
            tasks.register<JavaExec>("${exampleName}Run") {
                classpath = sourceSets["main"].runtimeClasspath
                mainClass.set(fqMainClass)
                maxHeapSize = "4g"
                workingDir = rootProject.projectDir
                val jacoco = extensions.findByType<JacocoTaskExtension>()
                    ?: error("jacoco extension not registered on $name")
                jacoco.isEnabled = withCoverage
                jacoco.setDestinationFile(
                    coverageDir.map { it.file("$exampleName.exec").asFile }.get()
                )
            }
            tasks.register<ShadowJar>("${exampleName}Package") {
                archiveBaseName.set(exampleName)
                archiveVersion.set("")
                archiveClassifier.set("")
                from(sourceSets["main"].output)
                configurations.set(listOf(project.configurations["runtimeClasspath"]))
                manifest.attributes("Main-Class" to fqMainClass, "Multi-Release" to "true")
                filesMatching(listOf("META-INF/services/**", "META-INF/clarity/providers.txt", "META-INF/annotations/**")) {
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                }
                mergeServiceFiles()
                append("META-INF/clarity/providers.txt")
                append("META-INF/annotations/skadistats.clarity.examples.shared.Example")
                exclude(
                    "module-info.class",
                    "META-INF/versions/*/module-info.class",
                    "META-INF/INDEX.LIST",
                    "META-INF/*.SF",
                    "META-INF/*.DSA",
                    "META-INF/*.RSA",
                )
            }
        }
}
