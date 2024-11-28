import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jlleitschuh.gradle.ktlint") version Versions.ktlintGradleVersion
    id("org.jetbrains.dokka") version Versions.dokkaVersion
    id("com.github.ben-manes.versions") version Versions.gradleVersionsPluginVersion
    `maven-publish`
    signing
}

group = ModuleConfig.groupId
version = ModuleConfig.version

android {
    namespace = "com.linecorp.apng"

    defaultConfig {
        minSdk = Versions.minSdkVersion
        compileSdk = Versions.compileSdkVersion
        lint.targetSdk = Versions.targetSdkVersion
        version = ModuleConfig.version
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
    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
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
                getDefaultProguardFile("proguard-android.txt"),
                file("proguard-rules.pro")
            )
            externalNativeBuild {
                cmake {
                    arguments += "-DCMAKE_BUILD_TYPE=RELEASE"
                }
            }
        }
    }
    ndkVersion = Versions.ndkVersion
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    lint {
        xmlReport = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

ktlint {
    android.set(true)
    reporters {
        reporter(ReporterType.CHECKSTYLE)
    }
}

dependencies {
    api(kotlin("stdlib", Versions.kotlinVersion))
    api(Libs.androidxAnnotation)
    api(Libs.androidxVectorDrawable)

    testImplementation(Libs.junit)
    testImplementation(Libs.robolectric)
    testImplementation(Libs.mockitoInline)
    testImplementation(Libs.mockitoKotlin)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("apngDrawable") {
                groupId = ModuleConfig.groupId
                artifactId = ModuleConfig.artifactId
                version = ModuleConfig.version
                pom {
                    packaging = "aar"
                    name.set(ModuleConfig.name)
                    description.set(ModuleConfig.description)
                    url.set(ModuleConfig.siteUrl)
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            name.set("LINE Corporation")
                            email.set("dl_oss_dev@linecorp.com")
                            url.set("https://engineering.linecorp.com/en/")
                        }
                    }
                    scm {
                        connection.set(ModuleConfig.scmConnectionUrl)
                        developerConnection.set(ModuleConfig.scmDeveloperConnectionUrl)
                        url.set(ModuleConfig.scmUrl)
                    }
                    issueManagement {
                        system.set("GitHub")
                        url.set(ModuleConfig.issueTrackerUrl)
                    }
                }

                from(components["release"])
            }
        }
    }
}
