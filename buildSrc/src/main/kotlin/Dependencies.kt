object Versions {
    // Language
    const val kotlinVersion = "2.1.0"

    // Plugin
    const val androidPluginVersion = "8.7.2"
    const val ktlintGradleVersion = "12.1.2"
    const val dokkaVersion = "1.9.20"
    const val gradleVersionsPluginVersion = "0.36.0"
    const val nexusStagingVersion = "0.22.0"

    // Android
    const val minSdkVersion = 21
    const val compileSdkVersion = 35
    const val targetSdkVersion = 30
    const val ndkVersion = "27.2.12479018"

    // Library
    const val androidxAppCompatVersion = "1.0.1"
    const val androidxAnnotationVersion = "1.9.1"
    const val androidxConstraintLayoutVersion = "1.1.3"
    const val androidxAnimatedVectorDrawable = "1.2.0"
    const val kotlinxCoroutinesVersion = "1.4.3"
    const val lichLifecycleVersion = "1.3.0"
    const val junitVersion = "4.13.2"
    const val robolectricVersion = "4.5.1"
    const val mockitoVersion = "3.7.7"
    const val mockitoKotlinVersion = "2.1.0"
}

object Libs {
    // Library
    const val androidxAppcompat =
        "androidx.appcompat:appcompat:${Versions.androidxAppCompatVersion}"
    const val androidxAnnotation =
        "androidx.annotation:annotation:${Versions.androidxAnnotationVersion}"
    const val androidxVectorDrawable =
        "androidx.vectordrawable:vectordrawable-animated:${Versions.androidxAnimatedVectorDrawable}"
    const val androidxConstraintLayout =
        "androidx.constraintlayout:constraintlayout:${Versions.androidxConstraintLayoutVersion}"
    const val kotlinxCoroutines =
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinxCoroutinesVersion}"
    const val lichLifecycle = "com.linecorp.lich:lifecycle:${Versions.lichLifecycleVersion}"
    const val junit = "junit:junit:${Versions.junitVersion}"
    const val robolectric = "org.robolectric:robolectric:${Versions.robolectricVersion}"
    const val mockitoInline = "org.mockito:mockito-inline:${Versions.mockitoVersion}"
    const val mockitoKotlin =
        "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlinVersion}"
}
