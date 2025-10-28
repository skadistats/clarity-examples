import com.needhamsoftware.unojar.gradle.PackageUnoJarTask

plugins {
    id("java-library")
    id("com.needhamsoftware.unojar") version "1.1.0"
}

group = "com.skadistats"
version = "3.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.skadistats:clarity:3.1.2")
    api("ch.qos.logback:logback-classic:1.5.20")
    annotationProcessor("org.atteo.classindex:classindex:3.13")
}

File("src/main/java/skadistats/clarity/examples").walk().maxDepth(1).forEach {
    tasks.register<JavaExec>("${it.name}Run") {
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("skadistats.clarity.examples.${it.name}.Main")
    }
    tasks.register<PackageUnoJarTask>("${it.name}Package") {
        dependsOn("jar")
        archiveBaseName.set(it.name)
        archiveVersion.set("")
        archiveClassifier.set("")
        mainClass.set("skadistats.clarity.examples.${it.name}.Main")
    }
}
