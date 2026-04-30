plugins {
  alias(libs.plugins.android.library)
}

val desktopResourcesDir = rootProject.projectDir.resolve("../engine-platform-desktop/src/main/resources")
val battleArenaResourcesDir = rootProject.projectDir.resolve("../battle-arena-core/src/main/resources")
val androidAssetsDir = projectDir.resolve("src/main/assets")

val syncDesktopResourcesToAndroidAssets by tasks.registering(Sync::class) {
  group = "build"
  description = "Sync desktop resources into Android assets before debug builds."
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

  from(desktopResourcesDir)
  from(battleArenaResourcesDir)
  into(androidAssetsDir)
}

android {
  namespace = "com.njst.gaming.android"
  compileSdk = libs.versions.compileSdk.get().toInt()
  ndkVersion = "30.0.14904198-beta1"

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    externalNativeBuild {
      cmake {
        cppFlags += "-std=c++17"
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  sourceSets {
    getByName("main") {
      assets.srcDir("src/main/assets")
    }
  }

  externalNativeBuild {
    cmake {
      path = file("CMakeLists.txt")
      version = "3.22.1"
    }
  }
}

dependencies {
  api(project(":engine-core"))
  implementation("com.google.code.gson:gson:2.10.1")
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.ext.junit)
  androidTestImplementation("androidx.test:runner:1.5.2")
}

tasks.named("preBuild") {
  dependsOn(syncDesktopResourcesToAndroidAssets)
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
  dependsOn(syncDesktopResourcesToAndroidAssets)
}
