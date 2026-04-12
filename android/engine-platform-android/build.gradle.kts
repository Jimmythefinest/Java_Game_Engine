plugins {
  alias(libs.plugins.android.library)
}

val desktopResourcesDir = rootProject.projectDir.resolve("../engine-platform-desktop/src/main/resources")
val androidAssetsDir = projectDir.resolve("src/main/assets")

val syncDesktopResourcesToAndroidAssets by tasks.registering(Sync::class) {
  group = "build"
  description = "Sync desktop resources into Android assets before debug builds."

  from(desktopResourcesDir)
  into(androidAssetsDir)
}

android {
  namespace = "com.njst.gaming.android"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
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
}

dependencies {
  api(project(":engine-core"))
  implementation("com.google.code.gson:gson:2.10.1")
}

tasks.named("preBuild") {
  dependsOn(syncDesktopResourcesToAndroidAssets)
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
  dependsOn(syncDesktopResourcesToAndroidAssets)
}
