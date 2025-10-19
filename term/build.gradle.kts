plugins {
    id("com.android.library")
}

android {
    namespace = "yangfentuozi.runner.app.ui.fragment.terminal"
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
    implementation(project(":emulatorview"))
    implementation(project(":rish"))
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.activity:activity:1.11.0")
    implementation("com.google.android.material:material:1.13.0")
}
