plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.njst.gaming"
  compileSdk = libs.versions.compileSdk.get().toInt()
  ndkVersion = "30.0.14904198-beta1"

  defaultConfig {
    applicationId = "com.njst.gaming"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation(project(":engine-platform-android"))
  implementation(project(":battle-arena-core"))
}
