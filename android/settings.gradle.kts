pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "android"
include(":app")
include(":engine-platform-android")
include(":engine-core")
include(":battle-arena-core")
project(":engine-core").projectDir = file("../engine-core")
project(":battle-arena-core").projectDir = file("../battle-arena-core")
