rootProject.name = "apng-drawable-root"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

include(":apng-drawable")
include(":sample-app")

project(":apng-drawable").name = "apng-drawable"
project(":sample-app").name = "sample-app"
