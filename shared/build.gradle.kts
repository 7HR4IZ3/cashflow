plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("app.cash.sqldelight")
}
kotlin {
    androidTarget()
    jvm()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("app.cash.sqldelight:runtime:2.1.0")
        }
        androidMain.dependencies { implementation("app.cash.sqldelight:android-driver:2.1.0") }
        iosMain.dependencies { implementation("app.cash.sqldelight:native-driver:2.1.0") }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmTest.dependencies { implementation("app.cash.sqldelight:sqlite-driver:2.1.0") }
    }
    jvmToolchain(21)
}
android { namespace = "app.cashflow.core"; compileSdk = 36; defaultConfig { minSdk = 26 } }
sqldelight { databases { create("LedgerDatabase") { packageName.set("app.cashflow.db") } } }
