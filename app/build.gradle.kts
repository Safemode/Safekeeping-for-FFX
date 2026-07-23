plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.safemode.safekeepingforffx"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.safemode.safekeepingforffx"
        minSdk = 24
        targetSdk = 36
        versionCode = 15
        versionName = "0.7.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // Generates BuildConfig.VERSION_NAME / VERSION_CODE so the About section in Settings
        // reads the version straight from defaultConfig above instead of a hardcoded string.
        buildConfig = true
    }
}

// The app bundle (.aab) output filename derives from the archives base name, so this renames it to
// safekeeping-for-ffx-release.aab / safekeeping-for-ffx-debug.aab. The APK names are set explicitly
// in the androidComponents block below, whose outputFileName override takes precedence for APKs.
base {
    archivesName = "safekeeping-for-ffx"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

androidComponents {
    onVariants { variant ->
        val apkName = when (variant.buildType) {
            "debug" -> "safekeeping-for-ffx-debug.apk"
            else -> "safekeeping-for-ffx.apk"
        }
        variant.outputs
            .filterIsInstance<com.android.build.api.variant.impl.VariantOutputImpl>()
            .forEach { it.outputFileName.set(apkName) }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.accompanist.permissions)
    implementation(libs.play.services.location)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)
    implementation(libs.moshi.kotlin)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
}
// Several unit tests parse the bundled CSVs straight off disk rather than through a fixture, so
// Gradle cannot see the dependency and would report a stale pass after a data-only edit. Declaring
// the assets directory as an input makes a CSV change re-run the tests.
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/main/assets"))
        .withPropertyName("bundledAssets")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
