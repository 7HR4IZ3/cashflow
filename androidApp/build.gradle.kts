import java.util.Properties

plugins { id("com.android.application"); kotlin("multiplatform"); id("org.jetbrains.kotlin.plugin.compose") }
kotlin {
    androidTarget()
    sourceSets.androidMain.dependencies {
        implementation(project(":composeApp"))
        implementation(project(":shared"))
        implementation("com.android.billingclient:billing:8.3.0")
        implementation("androidx.work:work-runtime-ktx:2.11.2")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        implementation("app.cash.sqldelight:android-driver:2.1.0")
        implementation("androidx.activity:activity-compose:1.10.1")
        implementation("androidx.core:core-ktx:1.16.0")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
        implementation("androidx.compose.runtime:runtime:1.9.4")
    }
    jvmToolchain(21)
}
val releaseSigningFile = file(System.getProperty("user.home") + "/.config/cashflow/signing/release.properties")
val releaseSigning = Properties().apply {
    if (releaseSigningFile.isFile) releaseSigningFile.inputStream().use { load(it) }
}
android {
    namespace = "app.cashflow.android"
    compileSdk = 36
    defaultConfig { applicationId = "app.cashflow.mobile"; minSdk = 26; targetSdk = 36; versionCode = 3; versionName = "0.2.1" }
    buildFeatures { compose = true; buildConfig = true }
    val backendUrl = providers.gradleProperty("cashflowBackendUrl").orElse("").get()
    defaultConfig { buildConfigField("String", "CASHFLOW_BACKEND_URL", "\"" + backendUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\"") }
    if (releaseSigningFile.isFile) {
        signingConfigs.create("release") {
            storeFile = file(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
        buildTypes.getByName("release") { signingConfig = signingConfigs.getByName("release") }
    }
}
