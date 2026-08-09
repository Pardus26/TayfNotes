import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionProps.load(FileInputStream(versionPropsFile))
}

val majorVersion = versionProps.getProperty("major.version") ?: "01"
val buildNumberFromFile = versionProps.getProperty("build.number") ?: "1"
val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: buildNumberFromFile
val fullVersionName = "$majorVersion.$buildNumber"
val buildNumberInt = buildNumber.toInt()

android {
    namespace = "com.eldora25.tayfnotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eldora25.tayfnotes"
        minSdk = 24
        targetSdk = 35
        versionCode = buildNumberInt
        versionName = fullVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain(11)
    }
    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val mainOutput = output as? com.android.build.api.variant.impl.VariantOutputImpl
            mainOutput?.outputFileName?.set("TayfNotes_v$fullVersionName.apk")
        }
    }
}

tasks.register("incrementBuildNumber") {
    doLast {
        if (System.getenv("GITHUB_RUN_NUMBER") == null) {
            val currentBuildNumber = versionProps.getProperty("build.number").toInt()
            versionProps.setProperty("build.number", (currentBuildNumber + 1).toString())
            versionProps.store(versionPropsFile.outputStream(), null)
            println("Build number incremented to ${currentBuildNumber + 1}")
        }
    }
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy("incrementBuildNumber")
    tasks.findByName("assembleRelease")?.finalizedBy("incrementBuildNumber")
}

dependencies {
    implementation(project(":shared"))
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Markdown
    implementation(libs.markdown.renderer)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
