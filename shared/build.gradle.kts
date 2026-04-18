plugins {
    id("examples-base")
}

dependencies {
    implementation("com.formdev:flatlaf:3.6")
    runtimeOnly(project(":examples"))
    runtimeOnly(project(":repro"))
    runtimeOnly(project(":dev"))
    runtimeOnly(project(":bench"))
}

tasks.register<JavaExec>("launcher") {
    description = "Run the ExampleLauncher (Swing picker over every @Example)."
    group = "application"
    // runtimeOnly(project(":examples")) et al. create a cycle (examples depends on shared),
    // so shared's runtimeClasspath transitively contains shared.jar. Use the output dirs
    // directly and filter shared.jar out of the rest to avoid duplicate logback.xml.
    val selfJar = tasks.named<Jar>("jar")
    classpath = files(
        sourceSets["main"].output,
        sourceSets["main"].runtimeClasspath.filter {
            it != selfJar.get().archiveFile.get().asFile
                    && it != sourceSets["main"].output.resourcesDir
                    && !sourceSets["main"].output.classesDirs.contains(it)
        }
    )
    mainClass.set("skadistats.clarity.examples.shared.ExampleLauncher")
    workingDir = rootProject.projectDir
    maxHeapSize = "4g"
    dependsOn(selfJar)
}
