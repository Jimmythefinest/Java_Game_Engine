plugins {
  alias(libs.plugins.android.library)
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
