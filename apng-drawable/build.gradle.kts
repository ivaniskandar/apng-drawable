@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ktlint.gradle)
    alias(libs.plugins.dokka)
    `maven-publish`
}

group = "com.linecorp"
version = libs.versions.apng.drawable.get()

android {
    namespace = "com.linecorp.apng"

    defaultConfig {
        minSdk = libs.versions.build.minSdk.get().toInt()
        compileSdk = libs.versions.build.compileSdk.get().toInt()
        lint.targetSdk = libs.versions.build.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles(
            file("proguard-rules.pro")
        )
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                cppFlags += "-fno-rtti"
                cppFlags += "-fexceptions"
                arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            externalNativeBuild {
                cmake {
                    arguments += "-DCMAKE_BUILD_TYPE=DEBUG"
                    cppFlags += "-DBUILD_DEBUG"
                    cFlags += "-DBUILD_DEBUG"
                }
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
            externalNativeBuild {
                cmake {
                    arguments += "-DCMAKE_BUILD_TYPE=RELEASE"
                }
            }
        }
    }
    ndkVersion = libs.versions.build.ndk.get()
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    lint {
        xmlReport = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

ktlint {
    android.set(true)
    reporters {
        reporter(ReporterType.CHECKSTYLE)
    }
}

dependencies {
    api(libs.androidx.annotation)
    api(libs.androidx.animated.vectordrawable)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.kotlin)
}

publishing {
    publications {
        create<MavenPublication>("apngDrawable") {
            groupId = "com.linecorp"
            artifactId = "apng"
            version = libs.versions.apng.drawable.get()
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
