import com.needhamsoftware.unojar.gradle.PackageUnoJarTask

plugins {
    id("examples-base")
    id("com.needhamsoftware.unojar")
}

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
