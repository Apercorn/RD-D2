pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.brott.dev") }
        maven { url = uri("https://mymaven.bylazar.com/releases") }
    }
}

include(":FtcRobotController")
include(":TeamCode")
