import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dev.rikka.tools.materialthemebuilder")
    id("dev.rikka.tools.refine")
}

val ksFile = rootProject.file("signing.properties")
val props = Properties()
if (ksFile.canRead()) {
    props.load(FileInputStream(ksFile))
    android.signingConfigs.create("sign").apply {
        storeFile = file(props["KEYSTORE_FILE"] as String)
        storePassword = props["KEYSTORE_PASSWORD"] as String
        keyAlias = props["KEYSTORE_ALIAS"] as String
        keyPassword = props["KEYSTORE_ALIAS_PASSWORD"] as String
    }
} else {
    android.signingConfigs.create("sign").apply {
        storeFile = android.signingConfigs.getByName("debug").storeFile
        storePassword = android.signingConfigs.getByName("debug").storePassword
        keyAlias = android.signingConfigs.getByName("debug").keyAlias
        keyPassword = android.signingConfigs.getByName("debug").keyPassword
    }
}

android {
    namespace = "yangfentuozi.runner"
    compileSdk {
        version = release(rootProject.ext["compileSdk"] as Int)
    }

    defaultConfig {
        applicationId = "yangfentuozi.runner"
        minSdk = rootProject.ext["minSdk"] as Int
        targetSdk = rootProject.ext["targetSdk"] as Int
        versionCode = rootProject.ext["versionCode"] as Int
        versionName = rootProject.ext["versionName"] as String
        ndk {
            abiFilters.addAll(listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a"))
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=none")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("sign")
        }
        debug {
            signingConfig = signingConfigs.getByName("sign")
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
        prefab = true
    }
    kotlin {
        jvmToolchain(21)
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    ndkVersion = "28.1.13356709"
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "runner-v${versionName}-${name}.apk"
            assembleProvider.get().doLast {
                val outDir = File(rootDir, "out")
                val mappingDir = File(outDir, "mapping").absolutePath
                val apkDir = File(outDir, "apk").absolutePath

                if (buildType.isMinifyEnabled) {
                    copy {
                        from(mappingFileProvider.get())
                        into(mappingDir)
                        rename { _ -> "mapping-${versionName}.txt" }
                    }
                    copy {
                        from(outputFile)
                        into(apkDir)
                    }
                }
            }
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
//    implementation("dev.rikka.rikkax.widget:mainswitchbar:1.1.0")
//    implementation("dev.rikka.rikkax.layoutinflater:layoutinflater:1.3.0")
//    implementation("dev.rikka.rikkax.lifecycle:lifecycle-resource-livedata:1.0.1")
//    implementation("dev.rikka.rikkax.lifecycle:lifecycle-shared-viewmodel:1.0.1")
//    implementation("dev.rikka.rikkax.lifecycle:lifecycle-viewmodel-lazy:2.0.0")
//    implementation("dev.rikka.rikkax.html:html-ktx:1.1.2")

    // AndroidX
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.5")
//    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.core:core-ktx:1.17.0")
//    implementation("androidx.activity:activity:1.10.1")
//    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
//    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")

    // Shizuku
    val shizukuVersion = "13.1.5"
    implementation("dev.rikka.shizuku:api:$shizukuVersion")
    implementation("dev.rikka.shizuku:provider:$shizukuVersion")

    // Hidden API
    compileOnly("dev.rikka.hidden:stub:4.4.0")
    implementation("dev.rikka.hidden:compat:4.4.0")

    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.lsposed.libcxx:libcxx:28.1.13356709")


    implementation(project(":emulatorview"))
    implementation(project(":term"))
    implementation(project(":rish"))
}

configurations.configureEach {
    exclude(group = "androidx.appcompat", module = "appcompat")
}
