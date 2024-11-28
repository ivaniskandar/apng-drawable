plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.linecorp.apngsample"

    defaultConfig {
        applicationId = "com.linecorp.apngsample"
        minSdk = Versions.minSdkVersion
        compileSdk = Versions.compileSdkVersion
        targetSdk = Versions.targetSdkVersion
        versionCode = 1
        versionName = "1.0"
        missingDimensionStrategy("env", "androidx")
    }
    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"), file("proguard-rules.pro")
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    implementation(kotlin("stdlib", Versions.kotlinVersion))
    implementation(Libs.androidxAppcompat)
    implementation(Libs.androidxConstraintLayout)
    implementation(Libs.kotlinxCoroutines)
    implementation(Libs.lichLifecycle)

    implementation(project(":apng-drawable"))
}

