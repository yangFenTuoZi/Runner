import com.android.build.api.artifact.SingleArtifact
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val ksFile = rootProject.file("signing.properties")
val props = Properties()
val appVersionName = rootProject.ext["versionName"] as String
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
        versionName = appVersionName
        ndk {
            abiFilters.addAll(listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a"))
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
        compose = true
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
    ndkVersion = rootProject.ext["ndkVersion"] as String
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("runner-v${appVersionName}-${variant.name}.apk")
        }

        val copyArtifacts = tasks.register<Copy>("copy${variant.name.replaceFirstChar(Char::titlecase)}Artifacts") {
            into(rootProject.layout.projectDirectory.dir("out"))
            from(variant.artifacts.get(SingleArtifact.APK)) {
                include("*.apk")
                into("apk")
            }
            if (variant.isMinifyEnabled) {
                from(variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)) {
                    into("mapping")
                    rename { "mapping-${appVersionName}.txt" }
                }
            }
        }

        tasks.matching { it.name == "assemble${variant.name.replaceFirstChar(Char::titlecase)}" }.configureEach {
            finalizedBy(copyArtifacts)
        }
    }
}


dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.androidx.compose.runtime.livedata)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // AndroidX
    implementation(libs.google.material)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.core.ktx)

    // Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Hidden API
    compileOnly(libs.hidden.stub)
    implementation(libs.hidden.compat)

    implementation(libs.commons.compress)
    implementation(libs.capsule)

    implementation(project(":rish"))
    implementation(project(":shared"))
}
