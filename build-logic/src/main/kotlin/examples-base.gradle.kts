plugins {
    id("java-library")
}

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
    "api"("com.skadistats:clarity:4.0.1-SNAPSHOT")
    "api"("ch.qos.logback:logback-classic:1.5.20")
    "annotationProcessor"("com.skadistats:clarity:4.0.1-SNAPSHOT")
}
