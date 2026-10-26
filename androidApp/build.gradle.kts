plugins { id("com.android.application"); kotlin("multiplatform"); id("org.jetbrains.kotlin.plugin.compose") }
kotlin {
    androidTarget()
    sourceSets.androidMain.dependencies {
        implementation(project(":composeApp"))
        implementation(project(":shared"))
        implementation("app.cash.sqldelight:android-driver:2.1.0")
        implementation("androidx.activity:activity-compose:1.10.1")
        implementation("androidx.core:core-ktx:1.16.0")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
        implementation("androidx.compose.runtime:runtime:1.9.4")
    }
    jvmToolchain(21)
}
android {
    namespace = "app.cashflow.android"
    compileSdk = 36
    defaultConfig { applicationId = "app.cashflow.mobile"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "0.1.0" }
    buildFeatures { compose = true }
}
