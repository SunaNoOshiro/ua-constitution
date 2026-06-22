import java.net.URL
import java.net.HttpURLConnection

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.detekt)
}

// Dev-only static analysis. Narrow, complexity-focused config (see config/detekt/detekt.yml)
// so the report measures exactly the refactor targets: cyclomatic complexity (<= 8),
// parameter count (<= 8), nesting and method length. A baseline records the accepted
// pre-existing findings (the irreducible declarative @Composable shells) so the build stays green.
detekt {
  buildUponDefaultConfig = false
  config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
  baseline = file("$rootDir/config/detekt/baseline.xml")
  parallel = true
}

android {
  namespace = "ua.constitution"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.uaconstitution.qdxtlz"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Use .env / .env.example for the Secrets Gradle Plugin.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}

// Opt-in utility to refresh the committed anthem.ogg from Wikimedia. Intentionally NOT
// wired into preBuild, so normal builds never depend on the network. The .ogg is checked
// into res/raw; run this manually only when the asset needs updating:
//   ./gradlew :app:downloadAnthem
tasks.register("downloadAnthem") {
    val destDir = file("src/main/res/raw")
    val destFile = file("src/main/res/raw/anthem.ogg")
    inputs.property("url", "https://upload.wikimedia.org/wikipedia/commons/8/8d/%D0%9F%D1%80%D0%B5%D0%B7%D0%B8%D0%B4%D0%B5%D0%BD%D1%82%D1%81%D1%8C%D0%BA%D0%B8%D0%B9_%D0%BE%D1%80%D0%BA%D0%B5%D1%81%D1%82%D1%80_%D0%93%D1%96%D0%BC%D0%BD.ogg")
    outputs.file(destFile)
    doLast {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val url = URL("https://upload.wikimedia.org/wikipedia/commons/8/8d/%D0%9F%D1%80%D0%B5%D0%B7%D0%B8%D0%B4%D0%B5%D0%BD%D1%82%D1%81%D1%8C%D0%BA%D0%B8%D0%B9_%D0%BE%D1%80%D0%BA%D0%B5%D1%81%D1%82%D1%80_%D0%93%D1%96%D0%BC%D0%BD.ogg")
        println("Downloading anthem from: $url")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.inputStream.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        println("Anthem downloaded successfully to $destFile")
    }
}

// Note: the app loads constitution_ua.json directly from assets/ (ConstitutionLoader /
// IntegrityChecker) and never from res/raw, so there is no copy-to-raw build step.

