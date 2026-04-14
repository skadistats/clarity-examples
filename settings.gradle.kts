pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "clarity-examples"

if (file("../clarity").exists())
    includeBuild("../clarity")

include(":examples", ":repro", ":dev", ":bench", ":shared")
