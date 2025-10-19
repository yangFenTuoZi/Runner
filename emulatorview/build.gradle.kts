plugins {
    id("com.android.library")
}

android {
    namespace = "jackpal.androidterm.emulatorview"
    compileSdk {
        version = release(rootProject.ext["compileSdk"] as Int)
    }

    defaultConfig {
        minSdk = rootProject.ext["minSdk"] as Int
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
}
