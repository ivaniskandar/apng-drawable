import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.linecorp.apngsample"

    ndkVersion = libs.versions.build.ndk.get()

    defaultConfig {
        applicationId = "com.linecorp.apngsample"
        minSdk = libs.versions.build.minSdk.get().toInt()
        compileSdk = libs.versions.build.compileSdk.get().toInt()
        targetSdk = libs.versions.build.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        missingDimensionStrategy("env", "androidx")
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), file("proguard-rules.pro")
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.lich.lifecycle)

    implementation(project(":apng-drawable"))
}

