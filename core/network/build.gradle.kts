import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// MAL Client ID asla kaynak koda gomulmez: local.properties -> BuildConfig.
// CI ortaminda MAL_CLIENT_ID ortam degiskeni de kullanilabilir.
val malClientId: String = run {
    val props = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(props::load)
    props.getProperty("MAL_CLIENT_ID")
        ?: System.getenv("MAL_CLIENT_ID")
        ?: ""
}

android {
    namespace = "com.myanitrack.core.network"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MAL_CLIENT_ID", "\"$malClientId\"")
        buildConfigField("String", "MAL_API_BASE_URL", "\"https://api.myanimelist.net/v2/\"")
        buildConfigField("String", "MAL_OAUTH_BASE_URL", "\"https://myanimelist.net/v1/oauth2/\"")
        buildConfigField("String", "MAL_WEB_BASE_URL", "\"https://myanimelist.net/\"")
        buildConfigField("String", "JIKAN_BASE_URL", "\"https://api.jikan.moe/v4/\"")
        buildConfigField("String", "OAUTH_REDIRECT_URI", "\"myanitrack://auth\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        encoding = "UTF-8"
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))

    api(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.retrofit.converter.scalars)
    api(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
