plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "app.wardrobe"
    // Compose 1.12, Navigation 3 1.2 and core 1.19 require compileSdk 37.
    compileSdk = 37

    defaultConfig {
        applicationId = "app.wardrobe"
        // API 26 has java.time built in. Navigation 3 1.2.0 alone would allow 24.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // R8 shrinks the APK. Release builds are not debuggable, so Compose runs at full speed.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // INFO(limit): signed with the debug key so it installs over the debug build, use a real key before publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    splits {
        abi {
            // One APK per processor type. ML Kit's custom labeling ships about 7 to 12 MB of native code per
            // type, so a single APK for all four was about 50 MB. The install tasks pick the device's APK.
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric screenshot tests need the app's resources.
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        // MigrationTestHelper reads the schemas as assets, and Robolectric tests see only the app's merged
        // assets, so the debug build carries them. Release builds don't.
        // WARN: the first test run after a schema change fails with "Cannot find the schema file", because the
        // assets are merged before KSP writes the new file. Run the tests again.
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }
}

room {
    // Each schema version is saved here and committed, so later migrations can be tested against it.
    schemaDirectory("$projectDir/schemas")
}

roborazzi {
    // Reference screenshots live with the tests. verifyRoborazziDebug compares against them.
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    // ML Kit's standard labeling model comes through Google Play services. The ImageNet model runs in
    // the bundled custom pipeline, because the Play services version of that pipeline is still in beta.
    implementation(libs.mlkit.image.labeling)
    implementation(libs.mlkit.image.labeling.custom)
    implementation(libs.mlkit.subject.segmentation)
    // Already on the classpath through other libraries, declared because the recognizer reads EXIF itself.
    implementation(libs.androidx.exifinterface)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.coil.test)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
