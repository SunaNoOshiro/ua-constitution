import java.net.URL
import java.net.HttpURLConnection

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.uaconstitution.qdxtlz"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

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

tasks.register("copyConstitutionJson") {
    val srcFile = file("src/main/assets/constitution_ua.json")
    val destDir = file("src/main/res/raw")
    val destFile = file("src/main/res/raw/constitution_ua.json")
    inputs.file(srcFile)
    outputs.file(destFile)
    doLast {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        if (srcFile.exists()) {
            srcFile.copyTo(destFile, overwrite = true)
            println("Successfully copied constitution_ua.json to raw resources!")
        }
    }
}

tasks.matching { it.name.startsWith("preBuild") }.all {
    dependsOn("downloadAnthem", "copyConstitutionJson")
}

tasks.register("checkBraces") {
    doLast {
        val mainFile = file("src/main/java/com/example/MainActivity.kt")
        if (mainFile.exists()) {
            val lines = mainFile.readLines()
            val openBraces = mutableListOf<Pair<Int, String>>()
            for ((idx, line) in lines.withIndex()) {
                val lineNum = idx + 1
                var stripped = line
                if (stripped.contains("//") && !stripped.contains("http://") && !stripped.contains("https://")) {
                    stripped = stripped.substring(0, stripped.indexOf("//"))
                }
                stripped = stripped.replace("\".*?\"".toRegex(), "\"\"")
                
                val wasSize = openBraces.size
                for (char in stripped) {
                    if (char == '{') {
                        openBraces.add(Pair(lineNum, line.trim()))
                    } else if (char == '}') {
                        if (openBraces.isNotEmpty()) {
                            openBraces.removeAt(openBraces.size - 1)
                        } else {
                            println("EXTRA CLOSING BRACE at line $lineNum: ${line.trim()}")
                        }
                    }
                }
                if (lineNum in 2770..2953) {
                    println("Line $lineNum [Braces: $wasSize -> ${openBraces.size}]: ${line.trim()}")
                }
            }
            println("--- BRACE BALANCE ANALYSIS ---")
            println("Total lines: ${lines.size}")
            if (openBraces.isEmpty()) {
                println("All braces are perfectly balanced!")
            } else {
                println("UNCLOSED BRACES (${openBraces.size}):")
                openBraces.forEach { (lineNum, text) ->
                    println("Line $lineNum: $text")
                }
            }
            println("------------------------------")
        } else {
            println("MainActivity.kt does not exist!")
        }
    }
}

