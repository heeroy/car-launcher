plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// P4 安裝器：用 PackageInstaller session 安裝 APK。
// Android 13 只會把「用檔案管理員點 APK 安裝」的 App 標成「受限制的設定」，
// 用這個安裝器裝的 App 才能開通知存取。
android {
    namespace = "tw.p4.installer"
    compileSdk = 34

    defaultConfig {
        applicationId = "tw.p4.installer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // 與 P4 桌面共用固定的 debug 簽章
    signingConfigs {
        getByName("debug") {
            storeFile = file("../app/debug.p12")
            storeType = "PKCS12"
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = false }
}
