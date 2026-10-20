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
        versionCode = 35
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 code shrinking + resource shrinking for the store build. Room and Moshi
            // (codegen adapters) ship their own consumer rules; the extra keeps we need are in
            // proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
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
    sourceSets {
        // The exported Room schemas, as assets of the instrumented test APK, so the migration tests
        // can build a database at any past version and migrate it forward.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
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

        // Release builds are obfuscated by R8, so a crash log from a GitHub APK is unreadable without
        // that exact build's mapping.txt - and the next build overwrites it. This zips it per version
        // into release-mappings/ (git-ignored, ~5 MB zipped vs ~60 MB raw) so it survives a clean and
        // can be attached to the GitHub release. Play needs none of this: the .aab carries its own.
        if (variant.buildType == "release") {
            val versionName = android.defaultConfig.versionName
            tasks.register<Zip>("archiveReleaseMapping") {
                description = "Zips the R8 mapping for this release version into release-mappings/."
                from(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.OBFUSCATION_MAPPING_FILE))
                archiveFileName.set("safekeeping-for-ffx-$versionName-mapping.zip")
                destinationDirectory.set(rootProject.layout.projectDirectory.dir("release-mappings"))
            }
        }
    }
}

// Every release build - the APK for GitHub and the .aab for Play share one R8 run - leaves its
// mapping archived, including builds made through Android Studio's "Generate Signed Bundle / APK".
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    finalizedBy("archiveReleaseMapping")
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
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
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
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)

    constraints {
        // Not used directly - lifecycle pulls in 1.7.3, and the instrumented test APK is pinned to the
        // app's versions. Room's MigrationTestHelper reads the schema files with serializers built for
        // 1.8+, which crash on 1.7.3 with an AbstractMethodError, so the floor is raised here.
        implementation(libs.kotlinx.serialization.core) {
            because("room-testing's schema reader needs kotlinx-serialization 1.8+")
        }
    }
}
// Several unit tests parse the bundled CSVs straight off disk rather than through a fixture, so
// Gradle cannot see the dependency and would report a stale pass after a data-only edit. Declaring
// the assets directory as an input makes a CSV change re-run the tests.
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/main/assets"))
        .withPropertyName("bundledAssets")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    // FrozenIdsTest checks stored ids against these lists, so editing one must re-run it too.
    inputs.dir(layout.projectDirectory.dir("src/test/frozen-ids"))
        .withPropertyName("frozenIds")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    // `./gradlew test -PupdateFrozenIds` appends newly added ids to those lists. It never removes one.
    systemProperty(
        "updateFrozenIds",
        providers.gradleProperty("updateFrozenIds").map { "true" }.getOrElse("false")
    )
}
