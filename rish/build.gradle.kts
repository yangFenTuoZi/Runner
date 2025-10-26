plugins {
    id("com.android.library")
}

android {
    namespace = "rikka.rish"
    compileSdk {
        version = release(rootProject.ext["compileSdk"] as Int)
    }

    defaultConfig {
        minSdk = rootProject.ext["minSdk"] as Int
        ndk {
            abiFilters.addAll(listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a"))
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=none")
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        aidl = true
        prefab = true
    }
    ndkVersion = "28.1.13356709"
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("org.lsposed.libcxx:libcxx:28.1.13356709")
    implementation(project(":shared"))
}