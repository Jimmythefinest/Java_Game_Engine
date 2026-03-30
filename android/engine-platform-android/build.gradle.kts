plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.njst.gaming.android"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    externalNativeBuild {
      cmake {
        cppFlags("-O3")
      }
    }
    ndk {
      abiFilters += listOf("arm64-v8a", "x86_64")
    }
  }

  externalNativeBuild {
    cmake {
      path = file("CMakeLists.txt")
      version = "3.22.1"
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
}

dependencies {
  api(project(":engine-core"))
  implementation("com.google.code.gson:gson:2.10.1")
  androidTestImplementation("androidx.test:runner:1.5.2")
  androidTestImplementation("androidx.test:rules:1.5.0")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("junit:junit:4.13.2")
}
