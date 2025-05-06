plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dev.rikka.tools.materialthemebuilder")
}

android {
    namespace = "yangFenTuoZi.runner.plus"
    compileSdk = rootProject.ext["compileSdk"] as Int

    defaultConfig {
        applicationId = "yangFenTuoZi.runner.plus"
        minSdk = rootProject.ext["minSdk"] as Int
        targetSdk = rootProject.ext["targetSdk"] as Int
        versionCode = rootProject.ext["versionCode"] as Int
        versionName = rootProject.ext["versionName"] as String
        ndk {
            abiFilters.addAll(listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a"))
        }
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

materialThemeBuilder {
    themes {
        for ((name, color) in listOf(
            "Red" to "F44336",
            "Pink" to "E91E63",
            "Purple" to "9C27B0",
            "DeepPurple" to "673AB7",
            "Indigo" to "3F51B5",
            "Blue" to "2196F3",
            "LightBlue" to "03A9F4",
            "Cyan" to "00BCD4",
            "Teal" to "009688",
            "Green" to "4FAF50",
            "LightGreen" to "8BC3A4",
            "Lime" to "CDDC39",
            "Yellow" to "FFEB3B",
            "Amber" to "FFC107",
            "Orange" to "FF9800",
            "DeepOrange" to "FF5722",
            "Brown" to "795548",
            "BlueGrey" to "607D8F"
        )) {
            create("Material$name") {
                lightThemeFormat = "ThemeOverlay.Light.%s"
                darkThemeFormat = "ThemeOverlay.Dark.%s"
                primaryColor = "#$color"
            }
        }
    }
    // Add Material Design 3 color tokens (such as palettePrimary100) in generated theme
    // rikka.material >= 2.0.0 provides such attributes
    generatePalette = true
}

dependencies {
    // RikkaX
    implementation("dev.rikka.rikkax.appcompat:appcompat:1.6.1")
    implementation("dev.rikka.rikkax.core:core:1.4.1")
    implementation("dev.rikka.rikkax.insets:insets:1.3.0")
    implementation("dev.rikka.rikkax.material:material:2.7.2")
    implementation("dev.rikka.rikkax.material:material-preference:2.0.0")
    implementation("dev.rikka.rikkax.recyclerview:recyclerview-ktx:1.3.2")
    implementation("dev.rikka.rikkax.recyclerview:recyclerview-adapter:1.3.0")
    implementation("dev.rikka.rikkax.widget:borderview:1.1.0")
    implementation("dev.rikka.rikkax.widget:mainswitchbar:1.0.2")
    implementation("dev.rikka.rikkax.layoutinflater:layoutinflater:1.3.0")
    implementation("dev.rikka.rikkax.lifecycle:lifecycle-resource-livedata:1.0.1")
    implementation("dev.rikka.rikkax.lifecycle:lifecycle-shared-viewmodel:1.0.1")
    implementation("dev.rikka.rikkax.lifecycle:lifecycle-viewmodel-lazy:2.0.0")
    implementation("dev.rikka.rikkax.html:html-ktx:1.1.2")

    // AndroidX
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
//    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.core:core-ktx:1.16.0")
//    implementation("androidx.activity:activity:1.10.1")
//    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
//    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")

    // Shizuku
    val shizukuVersion = "13.1.5"
    implementation("dev.rikka.shizuku:api:$shizukuVersion")
    implementation("dev.rikka.shizuku:provider:$shizukuVersion")

    // Hidden API
    compileOnly("dev.rikka.hidden:stub:4.3.3")
    implementation("dev.rikka.hidden:compat:4.3.3")
}

configurations.configureEach {
    exclude(group = "androidx.appcompat", module = "appcompat")
}
