import com.needhamsoftware.unojar.gradle.PackageUnoJarTask
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    id("examples-base")
    id("com.needhamsoftware.unojar")
}

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
            tasks.register<PackageUnoJarTask>("${exampleName}Package") {
                dependsOn("jar")
                archiveBaseName.set(exampleName)
                archiveVersion.set("")
                archiveClassifier.set("")
                mainClass.set(fqMainClass)
            }
        }
}
