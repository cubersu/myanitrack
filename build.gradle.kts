// MyAniTrack - MyAnimeList Android istemcisi
//
// Not: AGP 9 ile birlikte Kotlin destegi AGP icine gomuldu. Bu yuzden
// "org.jetbrains.kotlin.android" plugin-i UYGULANMAMALI; AGP acikca hata veriyor.
// Compose / serialization / KSP plugin-leri normal sekilde uygulanmaya devam ediyor.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
