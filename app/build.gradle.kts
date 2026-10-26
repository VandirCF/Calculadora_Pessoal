import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose.compiler)
}

// Release signing data lives OUTSIDE the repository. Create keystore.properties next to this
// module's parent folder (it is git-ignored) or point KEYSTORE_PROPERTIES to a file elsewhere:
//   storeFile=C:/path/outside/the/repo/release.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
// Without it, release builds are produced UNSIGNED, which is the safe default.
val keystorePropertiesFile: File? =
    (System.getenv("KEYSTORE_PROPERTIES")?.let { file(it) } ?: rootProject.file("keystore.properties"))
        .takeIf { it.exists() }
val keystoreProperties = Properties().apply {
    keystorePropertiesFile?.inputStream()?.use { load(it) }
}

android {
    // The code package keeps its original name to avoid a risky mass refactor; the public
    // application id below is what identifies the app on devices and stores.
    namespace = "com.darkempire78.opencalculator"
    compileSdk = 35

    defaultConfig {
        // Provisional id (see docs/RELEASE_CHECKLIST.md). It must be decided before the first
        // publication and can never change afterwards.
        applicationId = "io.github.vandircf.calculadorafinanceira"
        resourceConfigurations += listOf("pt-rBR")
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    lint {
        // Pre-existing issues from upstream (InvalidVectorPath, NewApi); only new issues fail the build
        baseline = file("lint-baseline.xml")
    }

    viewBinding {
        enable = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidslidinguppanel)
    implementation(libs.androidx.preference.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.gson)
}
